package com.kntrel.mc.territotem.geyser;

import com.kntrel.mc.territotem.geyser.block.BedrockTotemCore;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
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


    //LISTENERS
    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.logger().info("Territotem Geyser extension loaded.");
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
        BedrockTotemCore totemCore = new BedrockTotemCore();
        event.register(totemCore);
        this.logger().info("Registered Totem Core custom block as " + totemCore.identifier() + ".");
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
}
