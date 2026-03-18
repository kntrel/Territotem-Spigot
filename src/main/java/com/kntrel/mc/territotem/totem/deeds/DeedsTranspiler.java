package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.runical.bukkit.Runical;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

class DeedsTranspiler {

    private static final String LINE_SEPARATOR = "\n";

    //FIELDS
    private final Server server_;


    //CONSTRUCTOR
    DeedsTranspiler(Server server) {
        this.server_ = server;
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
        for (int i = 0; i < lines.size(); i++) {
            int l = i + 1;
            String rawLine = lines.get(i);
            String line = rawLine.strip();
            if (line.isEmpty()) { continue; }

            boolean isGroup = line.endsWith(":");
            if (isGroup) {
                line = line.substring(0, line.length() - 1);
            }
            this.validateLine(src, line, l);

            if (isGroup) {
                String groupName = line.substring(0, line.length() - 1).strip();
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

    List<Permission> deTranspile(Region region, String[] src) throws DeedsDeTranspilingException {
        return this.deTranspile(region, String.join(LINE_SEPARATOR, src));
    }

    String[] transpile(Iterable<Permission> perms) {
        Map<Hierarchy.Group, List<String>> playersByGroup = new LinkedHashMap<>();
        for (Permission permission : perms) {
            if (permission == null || permission.getGroup() == null) {
                continue;
            }

            OfflinePlayer offlinePlayer = this.server_.getOfflinePlayer(permission.getPlayerId());
            String name = offlinePlayer.getName();

            if (name == null) { continue; }

            playersByGroup
                    .computeIfAbsent(permission.getGroup(), ignored -> new ArrayList<>())
                    .add(name);
        }

        return playersByGroup.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(entry -> renderSection(entry.getKey().getName(), entry.getValue()))
                .toArray(String[]::new);
    }


    //INTERNALS
    private void validateLine(String src, String line, int lineNumber) throws DeedsDeTranspilingException {
        for (int i = 0; i < line.length(); i++) {
            if (!isValidCharacter(line.charAt(i))) {
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

        StringBuilder builder = new StringBuilder(groupName)
                .append(':')
                .append(LINE_SEPARATOR);
        for (String player : sortedPlayers) {
            builder.append(player).append(LINE_SEPARATOR);
        }

        return builder.toString();
    }

    private static String normalizeSource(String src) {
        return src.replace("\r\n", LINE_SEPARATOR).replace('\r', '\n');
    }

    private static boolean isValidCharacter(char chr) {
        return chr == '_' || chr >= '0' && chr <= '9' || chr >= 'A' && chr <= 'Z' || chr >= 'a' && chr <= 'z';
    }
}
