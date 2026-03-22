package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.event.AbilityTriggeredEvent;
import com.kntrel.mc.regionLib.event.PlayerEnterRegionEvent;
import com.kntrel.mc.regionLib.event.RegionUpdatedEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.runical.core.Placeholder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RegionListener implements Listener {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionListener.class);
    private static final String UNKNOWN_RESPONSIBLE_PLAYER = "Someone";

    //FIELDS
    private final Plugin plugin_;
    private final Translator deniedAbilityTranslator_;
    private final Translator permissionTranslator_;


    //CONSTRUCTOR
    public RegionListener(Plugin plugin, Translator deniedAbilityTranslator, Translator permissionTranslator) {
        this.plugin_ = plugin;
        this.deniedAbilityTranslator_ = deniedAbilityTranslator;
        this.permissionTranslator_ = permissionTranslator;
    }


    @EventHandler
    void onAbilityDenied(AbilityTriggeredEvent e) {
        if (e.isAllowed()) { return; }

        Player player = e.getPlayer();
        Ability ability = e.getAbility();
        Region region = e.getRegion();
        Location where = e.getLocation();
        Placeholder[] placeholders = new Placeholder[] {
                Placeholder.of("regionName", region.getName()),
                Placeholder.of("regionId", region.getId()),
                Placeholder.of("player", player.getName()),
                Placeholder.of("abilityName", ability.name()),
                Placeholder.of("x", where.getX()),
                Placeholder.of("y", where.getY()),
                Placeholder.of("z", where.getZ())
        };


        this.deniedAbilityTranslator_.resolveAsync(
                player,
                ability.name().toLowerCase(Locale.ROOT),
                placeholders
        ).thenCompose(r -> {
            if (r.found()) {
                return CompletableFuture.completedFuture(r);
            }
            return this.deniedAbilityTranslator_.resolveAsync(player, "default", placeholders);
        }).thenAccept(r -> {
            if (!r.found()) { return; }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(r.value()));
        });
    }

    @EventHandler
    void onRegionUpdated(RegionUpdatedEvent e) {
        if (!e.permissionsChanged()) { return; }

        PermissionNotificationSeed seed = capturePermissionNotificationSeed(e);

        CompletableFuture.supplyAsync(() -> planPermissionNotifications(seed))
                .thenAccept(plan -> {
                    if (plan == null) { return; }
                    this.plugin_.getServer().getScheduler().runTask(
                            this.plugin_,
                            () -> this.sendPermissionNotifications(plan)
                    );
                })
                .exceptionally(ex -> {
                    LOGGER.error(
                            "Failed to send region permission change notifications for region {}",
                            seed.regionName(),
                            ex
                    );
                    return null;
                });
    }

    @EventHandler
    void onPlayerEntersRegion(PlayerEnterRegionEvent e) {
        e.getPlayer().sendTitle(e.getRegion().getName(), "", 10, 20, 10);
    }

    private static PermissionNotificationSeed capturePermissionNotificationSeed(RegionUpdatedEvent e) {
        List<RegionSnapshot.Permission> oldPermissions = List.copyOf(e.getOldState().permissions());
        List<RegionSnapshot.Permission> currentPermissions = List.copyOf(e.getCurrentState().permissions());

        Map<Integer, String> groupNames = e.getRegion().getHierarchy().getGroups().stream()
                .collect(Collectors.toMap(
                        Hierarchy.Group::getLevel,
                        Hierarchy.Group::getName,
                        (left, right) -> right
                ));

        UUID updaterId = e.getUpdater()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .map(Player::getUniqueId)
                .orElse(null);

        String responsiblePlayer = e.getUpdater()
                .map(RegionListener::entityName)
                .filter(name -> !name.isBlank())
                .orElse(UNKNOWN_RESPONSIBLE_PLAYER);

        return new PermissionNotificationSeed(
                e.getCurrentState().name(),
                groupNames,
                updaterId,
                responsiblePlayer,
                oldPermissions,
                currentPermissions
        );
    }

    private static PermissionNotificationPlan planPermissionNotifications(PermissionNotificationSeed seed) {
        List<PermissionChange> changes = diffPermissions(seed.oldPermissions(), seed.currentPermissions());
        if (changes.isEmpty()) {
            return null;
        }

        Set<UUID> recipientIds = collectRecipientIds(seed.oldPermissions(), seed.currentPermissions());
        if (recipientIds.isEmpty()) {
            return null;
        }

        return new PermissionNotificationPlan(
                seed.regionName(),
                seed.groupNames(),
                seed.updaterId(),
                seed.responsiblePlayer(),
                changes,
                recipientIds
        );
    }

    private void sendPermissionNotifications(PermissionNotificationPlan plan) {
        List<Player> recipients = plan.recipientIds().stream()
                .map(id -> this.plugin_.getServer().getPlayer(id))
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .toList();
        if (recipients.isEmpty()) { return; }

        Map<UUID, String> playerNames = new HashMap<>();
        for (PermissionChange change : plan.changes()) {
            playerNames.computeIfAbsent(change.playerId(), this::resolvePlayerName);
        }

        for (PermissionChange change : plan.changes()) {
            String affectedPlayer = playerNames.getOrDefault(change.playerId(), change.playerId().toString());
            String oldGroupName = resolveGroupName(plan.groupNames(), change.oldLevel());
            String newGroupName = resolveGroupName(plan.groupNames(), change.newLevel());
            String groupName = switch (change.type()) {
                case ADD -> newGroupName;
                case REMOVE -> oldGroupName;
                case CHANGE -> newGroupName;
            };

            Placeholder[] placeholders = new Placeholder[] {
                    Placeholder.of("affected_player", affectedPlayer),
                    Placeholder.of("responsible_player", plan.responsiblePlayer()),
                    Placeholder.of("group_name", groupName),
                    Placeholder.of("old_group_name", oldGroupName),
                    Placeholder.of("new_group_name", newGroupName),
                    Placeholder.of("region_name", plan.regionName())
            };

            for (Player recipient : recipients) {
                if (!recipient.isOnline()) { continue; }
                this.permissionTranslator_.sendTranslation(
                        recipient,
                        change.translationKeyFor(recipient.getUniqueId(), plan.updaterId()),
                        placeholders
                );
            }
        }
    }

    private String resolvePlayerName(UUID playerId) {
        Player player = this.plugin_.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            return player.getName();
        }

        String offlineName = this.plugin_.getServer().getOfflinePlayer(playerId).getName();
        if (offlineName != null && !offlineName.isBlank()) {
            return offlineName;
        }
        return playerId.toString();
    }

    static List<PermissionChange> diffPermissions(
            Collection<RegionSnapshot.Permission> oldPermissions,
            Collection<RegionSnapshot.Permission> currentPermissions
    ) {
        Map<UUID, Integer> oldLevels = toLevelMap(oldPermissions);
        Map<UUID, Integer> currentLevels = toLevelMap(currentPermissions);

        Set<UUID> playerIds = new HashSet<>(oldLevels.keySet());
        playerIds.addAll(currentLevels.keySet());

        List<PermissionChange> changes = new ArrayList<>();
        for (UUID playerId : playerIds) {
            Integer oldLevel = oldLevels.get(playerId);
            Integer newLevel = currentLevels.get(playerId);
            if (Objects.equals(oldLevel, newLevel)) { continue; }

            PermissionChangeType type;
            if (oldLevel == null) {
                type = PermissionChangeType.ADD;
            } else if (newLevel == null) {
                type = PermissionChangeType.REMOVE;
            } else {
                type = PermissionChangeType.CHANGE;
            }

            changes.add(new PermissionChange(type, playerId, oldLevel, newLevel));
        }

        changes.sort(Comparator
                .comparing((PermissionChange change) -> change.playerId().toString())
                .thenComparing(change -> change.type().ordinal()));
        return List.copyOf(changes);
    }

    static Set<UUID> collectRecipientIds(
            Collection<RegionSnapshot.Permission> oldPermissions,
            Collection<RegionSnapshot.Permission> currentPermissions
    ) {
        Set<UUID> playerIds = oldPermissions.stream()
                .map(RegionSnapshot.Permission::playerUUID)
                .collect(Collectors.toCollection(HashSet::new));
        currentPermissions.stream()
                .map(RegionSnapshot.Permission::playerUUID)
                .forEach(playerIds::add);
        return Set.copyOf(playerIds);
    }

    private static Map<UUID, Integer> toLevelMap(Collection<RegionSnapshot.Permission> permissions) {
        return permissions.stream()
                .collect(Collectors.toMap(
                        RegionSnapshot.Permission::playerUUID,
                        RegionSnapshot.Permission::level,
                        (left, right) -> right,
                        HashMap::new
                ));
    }

    private static String resolveGroupName(Map<Integer, String> groupNames, Integer level) {
        if (level == null) {
            return "";
        }
        return groupNames.getOrDefault(level, Integer.toString(level));
    }

    private static String entityName(Entity entity) {
        if (entity instanceof Player player) {
            return player.getName();
        }

        String customName = entity.getCustomName();
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        return entity.getName();
    }

    private static String perspectiveKey(UUID recipientId, UUID affectedPlayerId, UUID responsiblePlayerId) {
        boolean recipientIsResponsible = responsiblePlayerId != null && responsiblePlayerId.equals(recipientId);
        boolean recipientIsAffected = affectedPlayerId.equals(recipientId);
        if (recipientIsResponsible) {
            return recipientIsAffected ? "first_person_first_person" : "first_person_third_person";
        }
        return recipientIsAffected ? "third_person_first_person" : "third_person_third_person";
    }

    record PermissionNotificationSeed(
            String regionName,
            Map<Integer, String> groupNames,
            UUID updaterId,
            String responsiblePlayer,
            List<RegionSnapshot.Permission> oldPermissions,
            List<RegionSnapshot.Permission> currentPermissions
    ) {}

    record PermissionNotificationPlan(
            String regionName,
            Map<Integer, String> groupNames,
            UUID updaterId,
            String responsiblePlayer,
            List<PermissionChange> changes,
            Set<UUID> recipientIds
    ) {}

    enum PermissionChangeType {
        ADD("add"),
        REMOVE("remove"),
        CHANGE("change");

        private final String translationPath_;

        PermissionChangeType(String translationPath) {
            this.translationPath_ = translationPath;
        }

        String translationPath() {
            return this.translationPath_;
        }
    }

    record PermissionChange(PermissionChangeType type, UUID playerId, Integer oldLevel, Integer newLevel) {
        String translationKeyFor(UUID recipientId, UUID responsiblePlayerId) {
            return this.type().translationPath() + "." + perspectiveKey(recipientId, this.playerId(), responsiblePlayerId);
        }
    }
}
