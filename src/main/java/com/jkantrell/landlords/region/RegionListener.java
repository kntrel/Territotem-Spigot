package com.jkantrell.landlords.region;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.io.Config;
import com.jkantrell.regionslib.events.AbilityTriggeredEvent;
import com.jkantrell.regionslib.events.PlayerEnterRegionEvent;
import com.jkantrell.regionslib.events.PlayerLeaveRegionEvent;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.Regions;
import com.jkantrell.regionslib.regions.abilities.Abilities;
import com.jkantrell.regionslib.regions.rules.Rule;
import com.jkantrell.regionslib.regions.rules.RuleDataType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class RegionListener implements Listener {

    @EventHandler
    public void onPlayerEnterRegion(PlayerEnterRegionEvent e) {
        if (!Landlords.CONFIG.regionsNameTitleEnabled) { return; }

        Player player = e.getPlayer();
        Region region = e.getRegion();
        String mainOwner = (region.getPermissions().length < 1) ? "" : region.getPermissions()[0].getPlayerName();
        Config.TitleData titleData = Landlords.CONFIG.regionsNameTitleData;
        player.sendTitle(
                Landlords.getLangProvider().getEntry(player,"regions.enter_title.title",region.getName(),mainOwner),
                Landlords.getLangProvider().getEntry(player,"regions.enter_title.subtitle",region.getName(),mainOwner),
                titleData.fadeIn(),
                titleData.stay(),
                titleData.fadeOut()
        );
    }

    @EventHandler
    public void onPlayerLeaveRegion(PlayerLeaveRegionEvent e) {
        //Nothing here
    }

    @EventHandler
    public void onAbilityTriggered(AbilityTriggeredEvent e) {
        if (e.isAllowed()) { return; }

        Player player = e.getPlayer();
        String  regionName = e.getRegion().getName(),
                abilityName = e.getAbility().getName().toLowerCase();

        try {
            String  message,
                    path = "not_allowed." + abilityName.toLowerCase();
            try {
                message = Landlords.getLangProvider().getEntry(player, path, regionName);
            } catch (NullPointerException ex) {
                message = Landlords.getLangProvider().getEntry(player, "not_allowed.default", regionName);
                Bukkit.getLogger().warning(String.format(
                        """
                            %1$s doesn't have the ability "%2$s" in the region "%3$s", but a specific denial message wasn't found in the "%4$s" lang file.
                            Displaying the default denied action message.
                            Include the "not_allowed.%2$s" entry in the lang file to provide an specific message.""",
                        player.getName(), abilityName, regionName, Landlords.getLangProvider().getLangFileName(player)
                ));
            }
            if (message.equals("")) { return; }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } catch (NullPointerException ex) {
            Bukkit.getLogger().warning(String.format(
                    """
                            %1$s doesn't have the ability "%2$s" in the region "%3$s", but neither a specific nor default denial message was found in the "%4$s" lang file.
                            Make sure to include the "not_allowed.%2$s" or "not_allowed.default" entry in the lang file.""",
                    player.getName(), abilityName, regionName, Landlords.getLangProvider().getLangFileName(player)
            ));
        }
    }

    @EventHandler
    public void onBreakCrops(AbilityTriggeredEvent e) {
        //is it the BREAK_CROPS event?
        if (!e.getAbility().equals(Abilities.BREAK_CROPS)) { return; }

        //Is the ability allowed?
        if (!e.isAllowed()) { return; }

        //Checking if the Region has de autoplant rule
        Region region = e.getRegion();
        if (!region.hasRule("autoplant", RuleDataType.BOOL)) { return; }

        //Getting the rule result
        if(!region.getRuleValue("autoplant",RuleDataType.BOOL)) { return; }

        //Checking if the blockData is ageable
        BlockBreakEvent event = (BlockBreakEvent) e.getTriggererEvent();
        Block block = event.getBlock() ;
        if(!(block.getBlockData() instanceof Ageable ageable)) { return; }

        //Applying
        Material type = block.getType();
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(type);
                ageable.setAge(0);
                block.setBlockData(ageable);

                List<Item> drops = block.getWorld().getNearbyEntities(block.getBoundingBox().expand(1)).stream()
                        .filter(e -> e instanceof Item)
                        .map(e -> (Item) e)
                        .filter(i -> i.getThrower() == null)
                        .toList();

                if(drops.isEmpty()) { return; }

                Item item = drops.stream()
                        .filter(i -> i.getItemStack().getType().toString().toLowerCase().contains("seed"))
                        .findFirst()
                        .orElse(null);

                if (item != null) {
                    item.remove();
                    return;
                }

                item = drops.get(0);
                ItemStack itemStack = item.getItemStack();
                int amount = itemStack.getAmount();

                if (amount > 1) {
                    itemStack.setAmount(amount - 1);
                    item.setItemStack(itemStack);
                } else {
                    item.remove();
                }
            }
        }.runTaskLater(Landlords.getMainInstance(),1);

    }

    @EventHandler
    public void onSteppingOnFarmLand(PlayerInteractEvent e) {
        //IS it a physical interaction?
        if (!e.getAction().equals(Action.PHYSICAL)) { return; }

        //Is there a block Involved
        Block block = e.getClickedBlock();
        if (block == null) { return; }

        //Was if farmland
        if (!block.getType().equals(Material.FARMLAND)) { return; }

        //Checking if the block is in a region
        Region[] regions = Regions.getRuleContainersAt("farmlandProtected", RuleDataType.BOOL, block.getLocation().add(.5,.5,.5));
        if (regions.length < 1) { return; }

        //Checking the "farmlandProtected" rule
        for (Region r : regions) {
            if (r.getRuleValue("farmlandProtected", RuleDataType.BOOL)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onMonsterSpawn(CreatureSpawnEvent e) {
        if (!e.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)) { return; }
        if (!(e.getEntity() instanceof Monster monster)) { return; }

        for (Region r : Regions.getRuleContainersAt("noMonsterSpawn",RuleDataType.BOOL,monster.getLocation())) {
            if (r.getRuleValue("noMonsterSpawn",RuleDataType.BOOL)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onFireSpread(BlockBurnEvent e) {
        for (Region r :  Regions.getRuleContainersAt("fireProtected",RuleDataType.BOOL,e.getBlock().getLocation().add(.5,.5,.5))) {
            if (r.getRuleValue("fireProtected",RuleDataType.BOOL)) {
                e.setCancelled(true);
                double extinguishChance = Landlords.CONFIG.regionFireExtinguishChance;
                if (extinguishChance <= 0) { return; }
                if (Math.random() <= extinguishChance) {
                    Optional.ofNullable(e.getIgnitingBlock()).ifPresent(b -> b.setType(Material.AIR));
                }
                return;
            }
        }
    }

    @EventHandler
    public void onRaidStart(RaidTriggerEvent e) {
        for (Region r :  Regions.getRuleContainersAt("raidProtected",RuleDataType.BOOL,e.getRaid().getLocation())) {
            if (r.getRuleValue("raidProtected",RuleDataType.BOOL)) {
                e.setCancelled(true);
                Landlords.getMainInstance().getLogger().fine(
                 "A raid was prevented from triggering in " + r.getName() + " as 'raidProtected' rule is enabled in the region."
                );
                return;
            }
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        boolean intercept = false;
        String ruleLabel = null;
        RuleDataType<?> dataType = null;
        BiPredicate<Rule, Region> predicate = null;

        if (e.getEntity() instanceof Creeper) {
            ruleLabel = "creeperProtected";
            dataType = RuleDataType.BOOL;
            predicate = (ru,re) -> ru.getValue(RuleDataType.BOOL);
            intercept = true;
        } else if (e.getEntity() instanceof TNTPrimed tnt) {
            ruleLabel = "tntProtected";
            dataType = Landlords.getMainInstance().getRuleKeys().TNT_PROTECTED.getDataType();
            predicate = (ru,re) -> {
                switch ((LandLordsRuleKeys.TntProtection) ru.getValue()) {
                    case ignitor -> {
                        Entity source = tnt.getSource();
                        if (source == null ) { return true; }
                        if (!(source instanceof Player p)) { return true; }
                        return !re.checkAbility(p, Abilities.IGNITE_TNT);
                    }
                    case all -> { return true; }
                    default -> { return false; }
                }
            };
            intercept = true;
        }

        if (!intercept) { return; }

        Iterator<Block> i = e.blockList().iterator();
        Block block;
        while (i.hasNext()) {
            block = i.next();
            Rule rule;
            for (Region r : Regions.getRuleContainersAt(ruleLabel,dataType,block.getLocation().add(.5,.5,.5))) {
                rule = r.getRule(ruleLabel);
                if (predicate.test(rule,r)) { i.remove(); }
            }
        }
    }
}
