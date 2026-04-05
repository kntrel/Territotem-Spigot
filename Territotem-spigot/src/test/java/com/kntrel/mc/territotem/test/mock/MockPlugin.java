package com.kntrel.mc.territotem.test.mock;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import static com.kntrel.mc.territotem.test.mock.Mock.*;

public class MockPlugin implements Plugin {

    //FIELDS
    private final String name_;
    private final Server server_;


    //CONSTRUCTOR
    public MockPlugin(String name, Server server) {
        this.name_ = name;
        this.server_ = server;
    }


    @Override
    public File getDataFolder() {
        unimplemented();
        return null;
    }

    @Override
    public PluginDescriptionFile getDescription() {
        unimplemented();
        return null;
    }

    @Override
    public FileConfiguration getConfig() {
        unimplemented();
        return null;
    }

    @Override
    public InputStream getResource(String filename) {
        unimplemented();
        return null;
    }

    @Override
    public void saveConfig() {

    }

    @Override
    public void saveDefaultConfig() {

    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {

    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public PluginLoader getPluginLoader() {
        unimplemented();
        return null;
    }

    @Override
    public Server getServer() {
        return this.server_;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public boolean isNaggable() {
        return false;
    }

    @Override
    public void setNaggable(boolean canNag) {

    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        unimplemented();
        return null;
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(String worldName, String id) {
        unimplemented();
        return null;
    }

    @Override
    public Logger getLogger() {
        return Logger.getGlobal();
    }

    @Override
    public String getName() {
        return this.name_;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return null;
    }
}
