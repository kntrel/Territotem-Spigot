package com.kntrel.mc.territotem;

import com.kntrel.mc.territotem.region.TerritotemRuleKeys;
import com.kntrel.mc.territotem.region.RegionListener;
import com.kntrel.mc.territotem.totem.Totem;
import com.kntrel.mc.territotem.totem.TotemListener;
import com.kntrel.mc.territotem.totem.TotemManager;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class Territotem extends JavaPlugin {


    //IMPLEMENTATION
    @Override
    public void onEnable() {
        RegionLib.enable(this);
        RegionContext ctx = RegionLib.createDefaultContext(this);


        //Initializing totems
        TotemManager.loadTotemStructures();
        Totem.loadAll(this.getServer().getWorlds().stream().flatMap(w -> w.getEntities().stream()));

        //Registering listeners
        this.getServer().getPluginManager().registerEvents(new TotemListener(),this);
        this.getServer().getPluginManager().registerEvents(new RegionListener(),this);
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //METHODS
    @Override
    @Nonnull
    public Logger getLogger() {
        return this.LOGGER_;
    }
    public TerritotemRuleKeys getRuleKeys() {
        return this.ruleKeys_;
    }


    private static class LandlordsLogger extends Logger {

        private final Logger logger_;

        protected LandlordsLogger(String name, String resourceBundleName, Logger logger) {
            super(name, resourceBundleName);
            this.logger_ = logger;
        }

        @Override
        public void log(LogRecord record){
            Level level = record.getLevel();
            if (level.intValue() < Level.INFO.intValue()) {
                if (!this.isLoggable(level)) { return; }
                record.setLevel(Level.INFO);
                String message = record.getMessage();
                record.setMessage("[" + level + "] " + message);
            }

            logger_.log(record);
        }
    }

    public static class Utils {
        public static void broadcastMessageLang(String path, String[] args, Collection<? extends Player> players) {
            for (Player player : players) {
                assert player != null;
                player.sendMessage(Territotem.getLangProvider().getEntry(player, path, args));
            }
        }

        public static void broadcastMessageLang(String path, String[] args) {
            broadcastMessageLang(path, args, Territotem.getMainInstance().getServer().getOnlinePlayers());
        }
    }
}

