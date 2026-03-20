package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.event.AbilityTriggeredEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.runical.core.Placeholder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class RegionListener implements Listener {

    //FIELDS
    private final Translator deniedAbilityTranslator_;


    //CONSTRUCTOR
    public RegionListener(Translator deniedAbilityTranslator) {
        this.deniedAbilityTranslator_ = deniedAbilityTranslator;
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
}
