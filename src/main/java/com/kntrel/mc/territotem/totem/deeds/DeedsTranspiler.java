package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.runical.bukkit.Runical;
import com.kntrel.mc.runical.core.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class DeedsTranspiler {

    private static final String LINE_SEPARATOR = "\n";
    private static final String COMMENT_PREFIX = "-";
    private static final String BLOCK_COMMENT_DELIMITER = "---";
    private static final String PROLOGUE_PAGE_DELIMITER = "/page";
    private static final char MINECRAFT_FORMATTING_PREFIX = '\u00A7';

    //FIELDS
    private final Server server_;
    private final Runical runical_;
    private String prologueKey_;
    private int pageSize_;


    //CONSTRUCTOR
    DeedsTranspiler(Server server, Runical runical) {
        this.server_ = server;
        this.runical_ = runical;
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

        Map<String, Hierarchy.Group> groupMap = toLowerCaseGroupMap(region.getHierarchy());
        List<Permission> permissions = new ArrayList<>();
        Hierarchy.Group currentGroup = null;
        boolean inBlockComment = false;
        for (int i = 0; i < lines.size(); i++) {
            int l = i + 1;
            String rawLine = stripMinecraftFormatting(lines.get(i));
            String line = rawLine.strip();
            if (isBlockCommentDelimiter(line)) {
                inBlockComment = !inBlockComment;
                continue;
            }
            if (inBlockComment || line.isEmpty() || isSingleLineComment(line)) { continue; }

            boolean isGroup = line.endsWith(":");
            this.validateLine(src, rawLine, l, isGroup);
            if (isGroup) {
                line = line.substring(0, line.length() - 1);
            }

            if (isGroup) {
                String groupName = line.strip();
                if (groupName.isEmpty()) {
                    throw new DeedsDeTranspilingException(src, l, new DeedsDeTranspilingError.EmptyGroupName());
                }

                currentGroup = groupMap.get(groupName.toLowerCase());
                if (currentGroup == null) {
                    throw new DeedsDeTranspilingException(src, l, new DeedsDeTranspilingError.UnknownGroup(groupName));
                }
                continue;
            }

            if (currentGroup == null) {
                throw new DeedsDeTranspilingException(src, l, new DeedsDeTranspilingError.NoGroupNameProvided());
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
                Placeholder.of("regionName", region.getName()),
                Placeholder.of("regionId", region.getId()),
                Placeholder.of("version", deeds.version())
        };

        List<String> pages = new ArrayList<>();
        if (this.prologueKey_ != null) {
            String prologue = this.runical_.translateOrNull(player, this.prologueKey_, placeholders);
            if (prologue != null) {
                pages.addAll(renderCommentPages(prologue));
            }
        }
        if (perms.isEmpty()) {
            return pages.toArray(String[]::new);
        }

        int membersPerPage = this.pageSize_ - 1;
        List<Hierarchy.Group> groups = region.getHierarchy().getGroups().stream().sorted().toList();
        for (Hierarchy.Group group : groups) {
            List<String> players = playersByGroup.getOrDefault(group, List.of()).stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            if (players.isEmpty()) {
                pages.add(renderSection(group.getName(), List.of()));
                continue;
            }

            for (int start = 0; start < players.size(); start += membersPerPage) {
                int end = Math.min(start + membersPerPage, players.size());
                pages.add(renderSection(group.getName(), players.subList(start, end)));
            }
        }

        return pages.toArray(String[]::new);
    }


    //INTERNALS
    private void validateLine(String src, String rawLine, int lineNumber, boolean isGroup) throws DeedsDeTranspilingException {
        String line = rawLine.strip();
        if (line.isEmpty()) {
            return;
        }

        if (isGroup) {
            String trailingTrimmed = rawLine.stripTrailing();
            int firstColon = trailingTrimmed.indexOf(':');
            if (!line.endsWith(":")) {
                throw new DeedsDeTranspilingException(src, lineNumber, new DeedsDeTranspilingError.UnexpectedCharacter(firstColon + 1));
            }

            int extraSeparatorIndex = trailingTrimmed.substring(0, trailingTrimmed.length() - 1).indexOf(':');
            if (extraSeparatorIndex >= 0) {
                throw new DeedsDeTranspilingException(src, lineNumber, new DeedsDeTranspilingError.UnexpectedCharacter(extraSeparatorIndex + 1));
            }
            return;
        }

        for (int i = 0; i < rawLine.length(); i++) {
            if (!isValidCharacter(rawLine.charAt(i))) {
                throw new DeedsDeTranspilingException(src, lineNumber, new DeedsDeTranspilingError.UnexpectedCharacter(i + 1));
            }
        }
    }

    private static Map<String, Hierarchy.Group> toLowerCaseGroupMap(Hierarchy hierarchy) {
        return hierarchy.getGroups().stream()
                .collect(Collectors.toMap(
                        g -> g.getName().toLowerCase(),
                        Function.identity()
                ));
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

    private static String renderSection(String groupName, List<String> players) {
        List<String> sortedPlayers = players.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> lines = new ArrayList<>();
        lines.add(groupName + ':');
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

    private static boolean isSingleLineComment(String line) {
        return line.startsWith(COMMENT_PREFIX);
    }

    private static boolean isBlockCommentDelimiter(String line) {
        return BLOCK_COMMENT_DELIMITER.equals(line);
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
}
