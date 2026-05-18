package com.kntrel.mc.territotem.geyser;

import com.kntrel.mc.territotem.Territotem;
import com.kntrel.mc.territotem.geyser.block.BedrockTotemCore;
import com.kntrel.mc.territotem.geyser.render.BedrockBlockRenderer;
import com.kntrel.mc.territotem.geyser.render.BedrockRegionDisplayer;
import com.kntrel.mc.territotem.geyser.render.TotemCoreBedrockRenderListener;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TerritotemGeyserExtension implements Extension {

    //CONSTANTS
    private static final String BEDROCK_RESOURCE_PACK = "territotem-bedrock.mcpack";
    private static final String BEDROCK_RESOURCE_PACK_RESOURCE = "packs/" + BEDROCK_RESOURCE_PACK;

    //FIELDS
    private final BedrockTotemCore bedrockTotemCore_ = new BedrockTotemCore();
    private Territotem territotem_ = null;
    private TotemCoreTracker totemCoreTracker_ = null;
    private BedrockBlockRenderer blockRenderer_ = null;
    private TotemCoreBedrockRenderListener renderListener_ = null;
    private BedrockRegionDisplayer regionDisplayer_ = null;


    //LISTENERS
    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.bindTerritotem();
        this.logger().info("Territotem Geyser extension loaded.");
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        this.unbindTerritotem();
    }

    @Subscribe
    public void onDefineResourcePacks(GeyserDefineResourcePacksEvent event) {
        try {
            Path pack = this.extractBundledResourcePack();
            event.register(ResourcePack.create(PackCodec.path(pack)), PriorityOption.HIGHEST);
            this.logger().info("Registered Territotem Bedrock resource pack.");
        } catch (IOException e) {
            this.logger().error("Failed to register Territotem Bedrock resource pack.", e);
        }
    }

    @Subscribe
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
        event.register(this.bedrockTotemCore_);
        this.logger().info("Registered Totem Core custom block as " + this.bedrockTotemCore_.identifier() + ".");
    }

    private Path extractBundledResourcePack() throws IOException {
        Path packsDirectory = this.dataFolder().resolve("packs");
        Files.createDirectories(packsDirectory);

        Path target = packsDirectory.resolve(BEDROCK_RESOURCE_PACK);
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(BEDROCK_RESOURCE_PACK_RESOURCE)) {
            if (input == null) {
                throw new IOException("Bundled resource pack not found: " + BEDROCK_RESOURCE_PACK_RESOURCE);
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return target;
    }

    private void bindTerritotem() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Territotem");
        if (!(plugin instanceof Territotem territotem)) {
            this.logger().warning("Territotem Bukkit plugin was not found. Totem Core Bedrock syncing is disabled.");
            return;
        }

        if (!territotem.isEnabled()) {
            this.logger().warning("Territotem Bukkit plugin is not enabled yet. Totem Core Bedrock syncing is disabled.");
            return;
        }

        TotemCoreTracker tracker = territotem.getTotemCoreTracker();
        if (tracker == null) {
            this.logger().warning("Territotem Bukkit plugin is enabled, but its TotemCoreTracker is not ready.");
            return;
        }

        this.territotem_ = territotem;
        this.totemCoreTracker_ = tracker;
        this.blockRenderer_ = new BedrockBlockRenderer(territotem, this);
        this.regionDisplayer_ = new BedrockRegionDisplayer(
                territotem,
                territotem.getRegionContext().getConfig().regionDisplayDurationSeconds
        );
        territotem.addRegionDisplayer(this.regionDisplayer_);
        this.renderListener_ = new TotemCoreBedrockRenderListener(this.territotem_, this.blockRenderer_, this.bedrockTotemCore_);
        territotem.getServer().getPluginManager().registerEvents(
                this.renderListener_,
                territotem
        );
        tracker.getLoadedCores().forEach(this.renderListener_::render);
        this.logger().info("Connected to Territotem Bukkit plugin with " + tracker.getLoadedCores().size() + " loaded Totem Core(s).");
    }

    private void unbindTerritotem() {
        if (this.territotem_ != null && this.regionDisplayer_ != null) {
            this.territotem_.removeRegionDisplayer(this.regionDisplayer_);
            this.regionDisplayer_.stopAll();
        }

        if (this.blockRenderer_ != null) {
            this.blockRenderer_.clearAll();
            this.blockRenderer_.stop();
        }
        if (this.renderListener_ != null) {
            HandlerList.unregisterAll(this.renderListener_);
        }

        this.regionDisplayer_ = null;
        this.renderListener_ = null;
        this.blockRenderer_ = null;
        this.totemCoreTracker_ = null;
        this.territotem_ = null;
    }

    public BedrockTotemCore getBedrockTotemCore() {
        return this.bedrockTotemCore_;
    }

    public BedrockBlockRenderer getBlockRenderer() {
        return this.blockRenderer_;
    }
}
