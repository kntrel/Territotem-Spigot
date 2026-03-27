package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.runical.core.placeholder.Placeholder;
import com.kntrel.mc.territotem.region.RegionPlaceHolder;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import java.math.BigInteger;
import java.util.*;

class DeedsTranspiler {

    private static final String LINE_SEPARATOR = "\n";
    private static final String HIERARCHY_TRANSLATION_ROOT = "hierarchy";
    private static final String GROUP_NAME_TRANSLATION_KEY = "name";
    private static final String GROUP_DESCRIPTION_TRANSLATION_KEY = "description";
    private static final String COMMENT_PREFIX = "-";
    private static final String BLOCK_COMMENT_DELIMITER = "---";
    private static final String PROLOGUE_PAGE_DELIMITER = "/page";
    private static final char MINECRAFT_FORMATTING_PREFIX = '\u00A7';

    //FIELDS
    private final Server server_;
    private final Translator translator_;
    private String prologueKey_;
    private int pageSize_;


    //CONSTRUCTOR
    DeedsTranspiler(Server server, Translator translator) {
        this.server_ = server;
        this.translator_ = translator;
        this.prologueKey_ = null;
        this.pageSize_ = 13;
    }


    //SETTERS
    public void setPrologueTranslationKey(String key) {
        this.prologueKey_ = key;
    }
    public void setPageSizeLines(int lineCount) {
        this.pageSize_ = lineCount;
    }


    //API
    List<Permission> deTranspile(Region region, String src) throws DeedsDeTranspilingException {
        String normalizedSource = normalizeSource(src);
        List<String> lines = Arrays.asList(normalizedSource.split(LINE_SEPARATOR, -1));
        if (lines.isEmpty()) {
            return List.of();
        }

        List<Hierarchy.Group> groups = toGroupsSortedByLevel(region.getHierarchy());
        List<Permission> permissions = new ArrayList<>();
        Hierarchy.Group currentGroup = null;
        boolean inBlockComment = false;
        for (int i = 0; i < lines.size(); i++) {
            int l = i + 1;
            String strippedFormatting = stripMinecraftFormatting(lines.get(i));
            CommentStrippingResult commentResult = stripComments(strippedFormatting, inBlockComment);
            inBlockComment = commentResult.inBlockComment();

            String line = commentResult.line().strip();
            if (line.isEmpty()) {
                continue;
            }

            if (isGroupHeader(line)) {
                currentGroup = resolveGroupByLevel(src, l, groups, line);
                continue;
            }

            this.validatePlayerLine(src, line, l);

            if (currentGroup == null) {
                throw new DeedsDeTranspilingException(src, l, new DeedsDeTranspilingError.NoGroupProvided());
            }

            UUID playerId = this.resolvePlayerId(line).orElse(null);
            if (playerId == null) {
                throw new DeedsDeTranspilingException(src, l, new DeedsDeTranspilingError.PlayerNotFound(line));
            }
            permissions.add(new Permission(playerId, region, currentGroup.getLevel()));
        }

        return permissions;
    }

    List<Permission> deTranspile(Region region, String... src) throws DeedsDeTranspilingException {
        return this.deTranspile(region, String.join(LINE_SEPARATOR, src));
    }

    List<Permission> deTranspile(Region region, List<String> src) throws DeedsDeTranspilingException {
        StringJoiner joiner = new StringJoiner("\n");
        src.forEach(joiner::add);
        return this.deTranspile(region, joiner.toString());
    }

    String[] transpile(Player player, Deeds deeds) {

        List<Permission> perms = deeds.permissions();
        Region region = deeds.region();
        Map<Hierarchy.Group, List<String>> playersByGroup = new LinkedHashMap<>();
        for (Permission permission : perms) {
            if (permission == null) {
                continue;
            }

            if (permission.getGroup() == null) { continue; }

            OfflinePlayer offlinePlayer = this.server_.getOfflinePlayer(permission.getPlayerId());
            String name = offlinePlayer.getName();

            if (name == null) { continue; }

            playersByGroup
                    .computeIfAbsent(permission.getGroup(), ignored -> new ArrayList<>())
                    .add(name);
        }

        Placeholder[] placeholders = new Placeholder[]{
                RegionPlaceHolder.of(region),
                Placeholder.of("version", deeds.version()),
                Placeholder.of("player", player.getName())
        };

        List<String> pages = new ArrayList<>();
        if (this.prologueKey_ != null) {
            String prologue = this.translator_.translate(player, this.prologueKey_, placeholders).orNull().message();
            if (prologue != null) {
                pages.addAll(renderCommentPages(prologue));
            }
        }
        if (perms.isEmpty()) {
            return pages.toArray(String[]::new);
        }

        List<Hierarchy.Group> groups = toGroupsSortedByLevel(region.getHierarchy());
        for (Hierarchy.Group group : groups) {
            List<String> sectionHeader = this.renderSectionHeader(player, region, group);
            List<String> players = playersByGroup.getOrDefault(group, List.of()).stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            int membersPerPage = Math.max(1, this.pageSize_ - sectionHeader.size());
            if (players.isEmpty()) {
                pages.add(renderSection(sectionHeader, List.of()));
                continue;
            }

            for (int start = 0; start < players.size(); start += membersPerPage) {
                int end = Math.min(start + membersPerPage, players.size());
                pages.add(renderSection(sectionHeader, players.subList(start, end)));
            }
        }

        return pages.toArray(String[]::new);
    }


    //INTERNALS
    private void validatePlayerLine(String src, String line, int lineNumber) throws DeedsDeTranspilingException {
        for (int i = 0; i < line.length(); i++) {
            if (!isValidCharacter(line.charAt(i))) {
                throw new DeedsDeTranspilingException(src, lineNumber, new DeedsDeTranspilingError.UnexpectedCharacter(i + 1));
            }
        }
    }

    private static List<Hierarchy.Group> toGroupsSortedByLevel(Hierarchy hierarchy) {
        return hierarchy.getGroups().stream().sorted().toList();
    }

    private static Hierarchy.Group resolveGroupByLevel(
            String src,
            int lineNumber,
            List<Hierarchy.Group> groups,
            String groupLevel
    ) throws DeedsDeTranspilingException {
        BigInteger requestedLevel = new BigInteger(groupLevel);
        Hierarchy.Group resolved = null;
        for (Hierarchy.Group group : groups) {
            if (BigInteger.valueOf(group.getLevel()).compareTo(requestedLevel) > 0) {
                break;
            }
            resolved = group;
        }

        if (resolved == null) {
            throw new DeedsDeTranspilingException(src, lineNumber, new DeedsDeTranspilingError.UnknownGroup(groupLevel));
        }
        return resolved;
    }

    private Optional<UUID> resolvePlayerId(String playerName) {
        Player onlinePlayer = this.server_.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return Optional.of(onlinePlayer.getUniqueId());
        }

        return Arrays.stream(this.server_.getOfflinePlayers())
                .filter(Objects::nonNull)
                .filter(player -> player.getName() != null)
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .map(OfflinePlayer::getUniqueId)
                .findFirst();
    }

    private List<String> renderSectionHeader(Player player, Region region, Hierarchy.Group group) {
        String groupName = resolveGroupDisplayName(player, region, group);
        String description = resolveGroupDescription(player, region, group).orElse(null);
        if (description == null || description.isBlank()) {
            return List.of(group.getLevel() + " " + COMMENT_PREFIX + " " + groupName);
        }

        List<String> lines = new ArrayList<>();
        lines.add(group.getLevel() + " " + BLOCK_COMMENT_DELIMITER + " " + groupName);
        lines.addAll(Arrays.asList(normalizeSource(description).split(LINE_SEPARATOR, -1)));
        lines.add(BLOCK_COMMENT_DELIMITER);
        return lines;
    }

    private String resolveGroupDisplayName(Player player, Region region, Hierarchy.Group group) {
        return this.resolveGroupTranslation(player, region, group, GROUP_NAME_TRANSLATION_KEY)
                .filter(name -> !name.isBlank())
                .orElse(group.getName());
    }

    private Optional<String> resolveGroupDescription(Player player, Region region, Hierarchy.Group group) {
        return this.resolveGroupTranslation(player, region, group, GROUP_DESCRIPTION_TRANSLATION_KEY)
                .filter(description -> !description.isBlank());
    }

    private Optional<String> resolveGroupTranslation(Player player, Region region, Hierarchy.Group group, String field) {
        Placeholder[] placeholders = new Placeholder[] {
                Placeholder.of("groupLevel", group.getLevel()),
                Placeholder.of("groupName", group.getName()),
                Placeholder.of("hierarchyId", region.getHierarchy().getId()),
                Placeholder.of("hierarchyName", region.getHierarchy().getName()),
                RegionPlaceHolder.of(region)
        };
        String key = HIERARCHY_TRANSLATION_ROOT
                + '.'
                + region.getHierarchy().getId()
                + '.'
                + group.getLevel()
                + '.'
                + field;
        return Optional.ofNullable(this.translator_.translate(player, key, placeholders).orNull().message());
    }

    private static String renderSection(List<String> headerLines, List<String> players) {
        List<String> sortedPlayers = players.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> lines = new ArrayList<>(headerLines);
        lines.addAll(sortedPlayers);
        return renderLines(lines);
    }

    private static String normalizeSource(String src) {
        return src.replace("\r\n", LINE_SEPARATOR).replace('\r', '\n');
    }

    private static String stripMinecraftFormatting(String src) {
        StringBuilder builder = new StringBuilder(src.length());
        for (int i = 0; i < src.length(); i++) {
            char chr = src.charAt(i);
            if (chr == MINECRAFT_FORMATTING_PREFIX && i + 1 < src.length() && src.charAt(i + 1) != '\n') {
                i++;
                continue;
            }
            builder.append(chr);
        }
        return builder.toString();
    }

    private static CommentStrippingResult stripComments(String line, boolean inBlockComment) {
        StringBuilder content = new StringBuilder(line.length());
        int i = 0;
        while (i < line.length()) {
            if (startsWithAt(line, i, BLOCK_COMMENT_DELIMITER)) {
                inBlockComment = !inBlockComment;
                i += BLOCK_COMMENT_DELIMITER.length();
                continue;
            }

            if (inBlockComment) {
                i++;
                continue;
            }

            if (startsSingleLineComment(line, i)) {
                break;
            }

            content.append(line.charAt(i));
            i++;
        }
        return new CommentStrippingResult(content.toString(), inBlockComment);
    }

    private static boolean startsSingleLineComment(String line, int index) {
        return line.charAt(index) == COMMENT_PREFIX.charAt(0)
                && (index == 0 || Character.isWhitespace(line.charAt(index - 1)));
    }

    private static boolean startsWithAt(String line, int index, String token) {
        return line.regionMatches(index, token, 0, token.length());
    }

    private static boolean isGroupHeader(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isDigit(line.charAt(i))) {
                return false;
            }
        }
        return !line.isEmpty();
    }

    private static List<String> renderCommentPages(String prologue) {
        List<List<String>> pageLines = new ArrayList<>();
        List<String> currentPage = new ArrayList<>();
        for (String rawLine : normalizeSource(prologue).split(LINE_SEPARATOR, -1)) {
            if (!isProloguePageDelimiter(rawLine)) {
                currentPage.add(rawLine);
                continue;
            }
            trimTrailingBlankLines(currentPage);
            pageLines.add(currentPage);
            currentPage = new ArrayList<>();
        }
        pageLines.add(currentPage);

        pageLines.getFirst().addFirst(BLOCK_COMMENT_DELIMITER);
        pageLines.getLast().add(BLOCK_COMMENT_DELIMITER);

        return pageLines.stream()
                .map(DeedsTranspiler::renderLines)
                .toList();
    }

    private static boolean isProloguePageDelimiter(String line) {
        return stripMinecraftFormatting(line).strip().startsWith(PROLOGUE_PAGE_DELIMITER);
    }

    private static void trimTrailingBlankLines(List<String> lines) {
        while (!lines.isEmpty() && lines.getLast().isBlank()) {
            lines.removeLast();
        }
    }

    private static String renderLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append(LINE_SEPARATOR);
        }
        return builder.toString();
    }

    private static boolean isValidCharacter(char chr) {
        return chr == '_' || chr >= '0' && chr <= '9' || chr >= 'A' && chr <= 'Z' || chr >= 'a' && chr <= 'z';
    }

    private record CommentStrippingResult(String line, boolean inBlockComment) {}
}
