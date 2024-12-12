package com.jkantrell.landlords.totem;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.event.DeedsCreateEvent;
import com.jkantrell.landlords.event.PlayerInteractTotemEvent;
import com.jkantrell.landlords.event.TotemDestroyedByPlayerEvent;
import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.totem.Exception.*;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.events.RegionDestroyEvent;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.Permission;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.Regions;
import com.jkantrell.regionslib.regions.dataContainers.RegionData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TotemListener implements Listener {

    @EventHandler
    public void onPlaceTotem(PlayerInteractEvent e) {
        //Validating it was a RIGHT_CLICK_BLOCK_ACTION
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) { return; }

        //Validating the player right-clicked with an end crystal in hand
        ItemStack item = e.getItem();
        if (item == null) { return; }
        if (!item.getType().equals(Material.END_CRYSTAL)) { return; }

        //Checking if the clicked block is null
        Block block = e.getClickedBlock();
        if (block == null) { return; }

        //Checking if there's valid blueprint at this point.
        Location loc = block.getRelative(e.getBlockFace()).getLocation().add(.5,.5,.5);
        Blueprint blueprint = TotemManager.chekStructuresFromPoint(loc);
        if (blueprint == null) { return; }

        //Validating hand is not null
        EquipmentSlot hand = e.getHand();
        if (hand == null) { return; }

        //Place totem
        e.setCancelled(true);
        Player player = e.getPlayer();
        try {
            new Totem(loc,blueprint).place(player);
        } catch (Exception ex) {
            //An exception is not expected here, but this is a defensive measure
            String exName = ex.getClass().getSimpleName();
            player.sendMessage(ChatColor.RED + Landlords.getLangProvider().getEntry(player, "totems.unplacezable", exName));
            Landlords.getMainInstance().getLogger().severe(
            "A totem could not be placed at [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlock() + "], by " + player.getName() + ", due to " + exName + "."
            );
            ex.printStackTrace();
            return;
        }

        //Checking if the player is in creative mode
        if (player.getGameMode().equals(GameMode.CREATIVE)) { return; }

        //Removing the ender crystal from player's inventory
        PlayerInventory inventory = player.getInventory();
        item = inventory.getItem(hand);
        int amount = item.getAmount();
        inventory.setItem(
            hand,
            (amount > 1) ? new ItemStack(Material.END_CRYSTAL, amount - 1) : null
        );
    }

    @EventHandler
    public void onRingBell(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) { return; }

        Block block = e.getClickedBlock();
        if (block == null) { return; }
        if (!block.getType().equals(Material.BELL)) { return; }

        Region[] regions = Regions.getAt(block.getLocation().add(.5,.5,.5));
        if (regions.length < 1) {return; }

        Arrays.stream(regions)
                .map(Totem::fromRegion)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(t -> t.displayBorders(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlaceDeeds(PlayerInteractEvent e) {
        //Checking if the interaction was a right-clicked block
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) { return; }

        //Checking if the clicked block is a Totem lectern
        Block block = e.getClickedBlock();
        if (block == null) { return; }
        if (!TotemLectern.isTotemLectern(block)) { return; }
        TotemLectern lectern = TotemLectern.getAt(block);
        if (lectern == null) { return; }

        //Checking if the item used is a book
        ItemStack item = e.getItem();
        if (item == null) { return; }
        if (!(item.getItemMeta() instanceof BookMeta bookMeta)) { return; }

        //Checking if the book is a Deeds book
        Player player = e.getPlayer();
        Deeds deeds = Deeds.fromBook(bookMeta, player).orElse(null);
        if (deeds == null) {
            e.setCancelled(true);
            String notDeedsMessage = Landlords.getLangProvider().getEntry(player,"deeds.error_message.place.not_deeds");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(notDeedsMessage));
            return;
        }

        //Checking if the totem has a region.
        Region region = lectern.getTotem().getRegion().orElse(null);
        if (region == null) { return; }

        //Reading deeds
        Map<Player, List<String>> msgMap = Arrays.stream(region.getOnlineMembers()).collect(Collectors.toMap(p -> p, p -> new LinkedList<>()));
        List<Permission> oldPerms = new ArrayList<>(Arrays.asList(region.getPermissions().clone())), newPerms;
        String oldName = region.getName(), newName;
        try {
            lectern.readDeeds(deeds,player);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ex.getMessage());
            e.setCancelled(true);
        } catch (TotemLectern.unreadableDeedsException ex) {
            //Presenting reading errors to player and ending method
            StringBuilder message = new StringBuilder();
            message.append(ex.getMessage()).append("§r");
            for (String error : ex.errors) {
                message.append("\n").append(error);
            }
            player.sendMessage(message.toString());
            e.setCancelled(true);
            return;
        }

        //If the name changed
        StringBuilder messagePath = new StringBuilder();
        newName = region.getName();
        if(!oldName.equals(newName)){
            msgMap.forEach((p,l) -> {
                messagePath.setLength(0);
                messagePath
                        .append("regions.name_update.")
                        .append(p.equals(player) ? "firstPerson" : "thirdPerson");

                l.add(Landlords.getLangProvider().getEntry(p,messagePath.toString(),oldName,newName,player.getName()));
            });
        }

        //If permissions didn't change, return
        newPerms = new ArrayList<>(Arrays.asList(region.getPermissions()));
        if (oldPerms.equals(newPerms)) { return; }

        //Mapping old permissions
        HashMap<String,List<Hierarchy.Group>> oldPermsMap = new HashMap<>();
        oldPerms.forEach(p -> oldPermsMap.computeIfAbsent(p.getPlayerName(), n -> new LinkedList<>()).add(p.getGroup()));

        //Scanning new permissions
        Map<Player,List<Hierarchy.Group>> newMembers = new HashMap<>();
        for (Permission perm : newPerms) {
            String affected = perm.getPlayerName();
            Hierarchy.Group group = perm.getGroup();
            List<Hierarchy.Group> oldGroups = oldPermsMap.get(affected);
            if(oldGroups == null){
                msgMap.forEach((p,l) -> {
                    messagePath.setLength(0);
                    messagePath
                            .append("regions.permissions.add.")
                            .append(p.equals(player) ? "firstPerson" : "thirdPerson")
                            .append("_thirdPerson");
                    l.add(Landlords.getLangProvider().getEntry(p,messagePath.toString(),affected,player.getName(),perm.getGroup().getName(),newName));
                });
                perm.getPlayer().ifPresent(p -> newMembers.computeIfAbsent(p, p_ -> new LinkedList<>()).add(group));
            } else {
                if (oldGroups.remove(group)) { continue; }
                Hierarchy.Group oldGroup = oldGroups.remove(0);
                msgMap.forEach((p,l) -> {
                    messagePath.setLength(0);
                    messagePath
                        .append("regions.permissions.change.")
                        .append(p.equals(player) ? "firstPerson" : "thirdPerson").append("_")
                        .append(p.getName().equals(affected) ? "firstPerson" : "thirdPerson");
                    l.add(Landlords.getLangProvider().getEntry(p,messagePath.toString(),affected,player.getName(),oldGroup.getName(),group.getName(),newName));
                });
            }
        }

        //Handling new members
        newMembers.forEach((p,l) -> {
            messagePath.setLength(0);
            messagePath
                    .append("regions.permissions.add.")
                    .append(p.equals(player) ? "firstPerson" : "thirdPerson")
                    .append("_firstPerson");
            l.forEach(g -> msgMap.computeIfAbsent(p,p_ -> new LinkedList<>()).add(Landlords.getLangProvider().getEntry(p,messagePath.toString(),p.getName(),player.getName(),g.getName(),newName)));
        });

        //Handling deleted permissions
        oldPermsMap.forEach((n,lg) -> lg.forEach(g -> msgMap.forEach((p, l) -> {
            messagePath.setLength(0);
            messagePath
                    .append("regions.permissions.remove.")
                    .append(p.equals(player) ? "firstPerson" : "thirdPerson").append("_")
                    .append(p.getName().equals(n) ? "firstPerson" : "thirdPerson");
            l.add(Landlords.getLangProvider().getEntry(p,messagePath.toString(),n,player.getName(),g.getName(),newName));
        })));

        //Messaging players
        msgMap.forEach((p, m) -> m.forEach(p::sendMessage));
    }

    @EventHandler
    public void onDeedsCreate(PlayerInteractTotemEvent e) {
        //Checking if the clicked Totem has a region
        Totem totem = e.getTotem();
        Optional<Region> region = totem.getRegion();
        if (region.isEmpty()) { return; }

        //Checking if the item used can be converted to deeds
        Player player = e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        EquipmentSlot hand = e.getHand();
        ItemStack item = inventory.getItem(hand);
        if (item == null) { return; }
        if (!item.getType().equals(Landlords.CONFIG.deedsExchangeItem)) { return; }

        //CHeckling if the item isn't already a deeds item
        if(Deeds.isTotemDeeds(item)) { return; }

        //Closing players inventory and cancelling event
        e.setCancelled(true);
        player.closeInventory();

        //Firing DeedsCreateEvent and checking if it hasn't been cancelled.
        Deeds deeds = new Deeds(totem.getRegion().get(),player);
        DeedsCreateEvent event = new DeedsCreateEvent(player,deeds,totem);
        Landlords.getMainInstance().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }

        //Giving deeds to player
        ItemStack book = deeds.write();
        inventory.getItem(hand).setItemMeta(book.getItemMeta());
    }

    @EventHandler
    public void onEntityInteraction(PlayerInteractEntityEvent e) {
        //Checking if the entity is a totem
        if (!(e.getRightClicked() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }

        e.setCancelled(true);
        Landlords.getMainInstance().getServer().getPluginManager().callEvent(new PlayerInteractTotemEvent(
                e.getPlayer(),
                Totem.fromEnderCrystal(crystal),
                e.getHand(),
                PlayerInteractTotemEvent.Action.RIGHT_CLICK
        ));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        //Checking if damager is a player
        if (!(e.getDamager() instanceof Player player)) { return; }

        //Checking if the entity is a totem
        if (!(e.getEntity() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }

        //Calling event
        PlayerInteractTotemEvent event = new PlayerInteractTotemEvent(
                player,
                Totem.fromEnderCrystal(crystal),
                EquipmentSlot.HAND,
                PlayerInteractTotemEvent.Action.LEFT_CLICK
        );
        Landlords.getMainInstance().getServer().getPluginManager().callEvent(event);
        e.setCancelled(event.isCancelled());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void OnFeedTotem(PlayerInteractTotemEvent e) {
        //Checking if the totem has a region
        Totem totem = e.getTotem();
        Optional<Region> region = totem.getRegion();
        if (region.isEmpty()) { return; }

        //Checking if the item can interact with a totem
        Player player = e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(e.getHand());
        if (item == null) { return; }
        Config.TotemInteractionData totemUpgrade = Landlords.CONFIG.totemUpgradeItem;

        boolean normal = totemUpgrade.item().equals(item.getType()) && e.getAction().equals(PlayerInteractTotemEvent.Action.RIGHT_CLICK);
        boolean directional = item.getType().equals(Landlords.CONFIG.totemDirectionalItem) && e.getAction().equals(PlayerInteractTotemEvent.Action.LEFT_CLICK);
        if (!(normal || directional)) { return; }

        //Checking if the player has enough items to feed
        e.setCancelled(true);
        if (totemUpgrade.consume() && !inventory.contains(totemUpgrade.item(), totemUpgrade.count())) { return; }

        //Expanding the region
        boolean resized = true;
        List<UnresizableReason> unresizableReasons = null;
        try {
            if (directional) {
                BlockFace face = e.getClickedFace();
                if (face == null) { return; }
                totem.feed(player, face.getOppositeFace(), 1);
            } else {
                totem.feed(player, 1);
            }
        } catch (TotemUnresizableException ex) {
            resized = ex.wasResized();
            unresizableReasons = ex.getReasons(); //Fills placeholder for exception handling to execute.
        }

        //Checks if the region's size actually changed.
        String regionName = region.get().getName();
        if (resized) {
            if (totemUpgrade.consume() && !player.getGameMode().equals(GameMode.CREATIVE)) {
                inventory.removeItem(new ItemStack(totemUpgrade.item(), totemUpgrade.count()));
            }
            Vector size = region.get().getDimensions();
            DecimalFormat formater = new DecimalFormat("#0.00");
            player.sendMessage(Landlords.getLangProvider().getEntry(player, "totems.resized", regionName, formater.format(size.getX()), formater.format(size.getY()), formater.format(size.getZ())));
            return;
        }

        //Checks if there is any exception to handle
        if (unresizableReasons == null) { return; }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 3f, 0.5f);

        String  header = Landlords.getLangProvider().getEntry(player,"totems.unrezisable.header",regionName),
                basePath = "totems.unrezisable.", subPath = "default";
        String[] params = {};
        LinkedList<String> messages = new LinkedList<>();
        LinkedList<MaxSizeUnresizableReason> maxSizeReasons = new LinkedList<>();

        //Properly handling all unscaling reasons
        for (UnresizableReason reason : unresizableReasons) {
            if (reason instanceof MaxSizeUnresizableReason r) {
                maxSizeReasons.add(r);
            } else if (reason instanceof RegionCollisionUnresizableReason r) {
                subPath = "region_collision";
                params = new String[] {regionName, r.getCollidingRegion().getName(), r.getDirection().toString().toLowerCase()};
            } else if (reason instanceof OverShrunkUnresizableReason r) {
                subPath = "overshrink";
                params = new String[] {regionName, r.getDirection().toString().toLowerCase()};
            } else {
                continue;
            }
            messages.add(Landlords.getLangProvider().getEntry(player, basePath + subPath, params));
        }

        //Unifying maz size unrezisable reasons, if eny
        if (!maxSizeReasons.isEmpty()) {
            Function<Axis, String> extractor = (a) -> {
                Vector v = totem.getBlueprint().getRegionMaxSize();
                return Double.toString(switch (a) { case X -> v.getX(); case Y -> v.getY(); case Z -> v.getZ(); });
            };
            int size = maxSizeReasons.size();
            subPath = "max_size.";
            if (size < 2) {
                Axis axis = maxSizeReasons.get(0).getAxis();
                subPath += "one";
                params = new String[] {regionName, axis.toString(), extractor.apply(axis)};
            } else if (size < 3) {
                Axis axis1 = maxSizeReasons.get(0).getAxis(), axis2 = maxSizeReasons.get(1).getAxis();
                subPath += "two";
                params = new String[] {regionName, axis1.toString(), axis2.toString(), extractor.apply(axis1), extractor.apply(axis2)};
            } else {
                subPath += "three";
                params = new String[] {regionName, extractor.apply(Axis.X), extractor.apply(Axis.Y), extractor.apply(Axis.Z)};
            }
            messages.add(Landlords.getLangProvider().getEntry(player,basePath + subPath,params));
        }

        //If not a singe reason could be handled, adds a default message to the header.
        if (messages.isEmpty()) {
            String def = Landlords.getLangProvider().getEntry(player,"totems.unrezisable.default",region.get().getName());
            if (!def.equals("")) { header += " " + def; }
        }
        messages.push(header);

        //Warns the player of why the region could not be resized
        messages.stream().distinct().filter(m -> !m.equals("")).forEach(player::sendMessage);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHurtTotem(EntityDamageByEntityEvent e) {
        //Checking if entity is an ender crystal
        if (!(e.getEntity() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }

        //Cancelling the event
        e.setCancelled(true);
        Totem totem = Totem.fromEnderCrystal(crystal);

        //Getting the damager player
        Player player = null;
        Arrow arrow = null;
        Entity destroyer = e.getDamager();
        if (destroyer instanceof Player player_) {
            player = player_;
        } else if (destroyer instanceof Arrow arrow_) {
            if (!(arrow_.getShooter() instanceof Player player_)) { return; }
            List<PotionType> effects = Landlords.CONFIG.totemDestroyArrowEffects;
            if (!effects.isEmpty() && !effects.contains(arrow_.getBasePotionData().getType())) { return; }
            player = player_; arrow = arrow_;
        }
        if (player == null) { return; }

        //Firing TotemDestroyedByPlayerEvent
        TotemDestroyedByPlayerEvent event = new TotemDestroyedByPlayerEvent(player,totem,arrow);
        RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }

        //Checking if there's any blocks in between (Lagged players protection)
        if (!player.hasLineOfSight(totem.getEndCrystal())) { return; }

        //Hurting totem
        Player finalPlayer = player;
        boolean destroyed = false;
        if (totem.getLevel() > 0) {
            //Hurtting totem
            try {
                totem.hurt(player, 1);

                //Drop back update item
                if (Math.random() > Landlords.CONFIG.totemDropBackRate) { return; }
                Config.TotemInteractionData itemData = Landlords.CONFIG.totemUpgradeItem;
                totem.getWorld().dropItemNaturally(totem.getLocation(),new ItemStack(itemData.item(), itemData.count())).setInvulnerable(true);
            } catch (TotemUnresizableException ignored) {}
        } else {
            //Destroying totem
            totem.getRegion().ifPresent(r -> r.destroy(finalPlayer));
            destroyed = true;
        }

        //Alerting members
        if (!Landlords.CONFIG.totemAttackAlerting) { return; }

        boolean finalDestroyed = destroyed;
        Player[] onlineMembers = totem.getRegion().map(Region::getOnlineMembers).orElse(new Player[] {});
        Arrays.stream(onlineMembers)
            .filter(p -> !p.equals(finalPlayer))
            .forEach(p -> p.sendMessage(
                ChatColor.RED + Landlords.getLangProvider().getEntry(p, "totems." + ((finalDestroyed) ? "destroyed" : "hurt"), totem.getRegion().get().getName(), finalPlayer.getName())
            ));
    }

    @EventHandler
    public void onHurtTotemWithUpdateItem(PlayerInteractTotemEvent e) {
        if (!e.getAction().equals(PlayerInteractTotemEvent.Action.LEFT_CLICK)) { return; }

        ItemStack item = e.getPlayer().getInventory().getItem(e.getHand());
        if (item == null) { return; }

        if (item.getType().equals(Landlords.CONFIG.totemUpgradeItem.item())) {
            e.setCancelled(true);
        }
        //This protects mobile bedrock players from accidentally hurting the totem
    }

    @EventHandler
    public void OnTotemKilled(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }
        Totem.fromEnderCrystal(crystal)
                .getRegion()
                .ifPresent(Region::destroy);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        Totem.loadAll(e.getEntities().stream());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        e.getEntities().stream()
                .filter(ent -> ent instanceof EnderCrystal)
                .map(ent -> (EnderCrystal) ent)
                .filter(Totem::isTotem)
                .map(Totem::fromEnderCrystal)
                .filter(Objects::nonNull)
                .forEach(t -> {
                    Region r = t.getRegion().orElse(null);
                    Logger logger = Landlords.getMainInstance().getLogger();
                    if (r == null) {
                        t.destroy();
                        logger.warning("Destroyed totem with no region during regular unload. Region Id: " + t.getRegionId() + ". Totem Id: " + t.getUniqueId().toString() + ".");
                    } else { t.unload(); }
                });
    }

    @EventHandler
    public void onBlockUpdate(BlockPhysicsEvent e) {
        List<Totem> totems = Arrays.stream(Totem.getAll()).filter(t -> t.contains(e.getBlock())).toList();
        for(Totem t : totems){
            t.setEnabled(t.getBlueprint().testStructure(t.getLocation()));
        }
    }

    @EventHandler
    public void onPlaceLectern(BlockPlaceEvent e) {
        Block block = e.getBlock();
        if (!(block.getState() instanceof Lectern)) { return; }
        TotemLectern totemLectern = TotemLectern.getAt(block);
        if (totemLectern == null) { return; }
        Directional dir = (Directional) block.getBlockData();
        dir.setFacing(totemLectern.getFacing());
        totemLectern.convert(block);
        block.setBlockData(dir);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onRegionDestroy(RegionDestroyEvent e) {
        Region region = e.getRegion();
        RegionData regionData = region.getDataContainer().get("totemId");
        if (regionData == null) { return; }

        Totem.fromRegion(region).ifPresent(Totem::destroy);
    }

    @EventHandler
    public void onEditBook(PlayerEditBookEvent e){
        //Checking if the edited book is a deeds book
        Player player = e.getPlayer();
        BookMeta oldMeta = e.getPreviousBookMeta();
        Deeds deeds = Deeds.fromBook(oldMeta, player).orElse(null);
        if (deeds == null) { return; }

        //Checking and reacting if the book is being signed
        if (Landlords.CONFIG.deedsUnsignable && e.isSigning()) {
            e.setSigning(false);
            String message = Landlords.getLangProvider().getEntry(player,"deeds.error_message.unsignable");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }

        //Reading the book
        LinkedList<String> errors = new LinkedList<>();
        BookMeta newMeta = e.getNewBookMeta();
        int size = newMeta.getPages().size();
        for (int i = 1; i <= size; i++) {
            try {
                deeds.readPage(newMeta,i);
            } catch (IllegalArgumentException ex) {
                errors.add(ex.getMessage());
            }
        }

        //Checking if there were no reading errors
        if (errors.isEmpty() && size > 0) { return; }       //The size check is for bedrock support. An EditBookEvent is triggered right away.

        //Presenting errors to the player
        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append(Landlords.getLangProvider().getEntry(player, "deeds.error_message.compose.header", Integer.toString(errors.size()))).append("§r");
            for (String error : errors) {
                message.append("\n").append(error);
            }
            player.sendMessage(message.toString());
        }

        //Overwriting book to old one
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        book.setItemMeta(oldMeta);
        e.setCancelled(true);
        try {
            player.getInventory().setItem(e.getSlot(),book);
        } catch (IndexOutOfBoundsException ex) {
            player.getInventory().setItem(EquipmentSlot.OFF_HAND, book);
        }
    }
}
