package com.kntrel.mc.territotem.region;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class TerritotemRegionFeaturesTest {

    @Test
    void attachedBlockFaceMatchesLeverMount() {
        BlockData wallData = mock(BlockData.class, withSettings().extraInterfaces(Directional.class, FaceAttachable.class));
        Directional wallDirectional = (Directional) wallData;
        FaceAttachable wallAttachable = (FaceAttachable) wallData;
        when(wallAttachable.getAttachedFace()).thenReturn(FaceAttachable.AttachedFace.WALL);
        when(wallDirectional.getFacing()).thenReturn(BlockFace.NORTH);

        Block wallLever = mock(Block.class);
        when(wallLever.getBlockData()).thenReturn(wallData);
        assertEquals(BlockFace.SOUTH, TerritotemRegionFeatures.attachedBlockFace(wallLever));

        BlockData floorData = mock(BlockData.class, withSettings().extraInterfaces(Directional.class, FaceAttachable.class));
        FaceAttachable floorAttachable = (FaceAttachable) floorData;
        when(floorAttachable.getAttachedFace()).thenReturn(FaceAttachable.AttachedFace.FLOOR);

        Block floorLever = mock(Block.class);
        when(floorLever.getBlockData()).thenReturn(floorData);
        assertEquals(BlockFace.DOWN, TerritotemRegionFeatures.attachedBlockFace(floorLever));
    }

    @Test
    void replantItemForMapsCommonCrops() {
        assertEquals(Material.WHEAT_SEEDS, TerritotemRegionFeatures.replantItemFor(Material.WHEAT));
        assertEquals(Material.CARROT, TerritotemRegionFeatures.replantItemFor(Material.CARROTS));
        assertEquals(Material.BAMBOO, TerritotemRegionFeatures.replantItemFor(Material.BAMBOO_SAPLING));
        assertEquals(Material.OAK_SAPLING, TerritotemRegionFeatures.replantItemFor(Material.OAK_SAPLING));
        assertNull(TerritotemRegionFeatures.replantItemFor(Material.STONE));
    }

    @Test
    void chargeReplantItemConsumesOneMatchingDrop() {
        Item seedDrop = mockDrop(Material.WHEAT_SEEDS, 3);
        Item carrotDrop = mockDrop(Material.CARROT, 2);
        List<Item> drops = new ArrayList<>(List.of(seedDrop, carrotDrop));

        assertTrue(TerritotemRegionFeatures.chargeReplantItem(drops, Material.WHEAT_SEEDS));
        assertEquals(2, seedDrop.getItemStack().getAmount());
        assertEquals(2, drops.size());

        assertTrue(TerritotemRegionFeatures.chargeReplantItem(drops, Material.CARROT));
        assertEquals(1, carrotDrop.getItemStack().getAmount());
    }

    @Test
    void chargeReplantItemRemovesSingletonDrop() {
        Item wartDrop = mockDrop(Material.NETHER_WART, 1);
        List<Item> drops = new ArrayList<>(List.of(wartDrop));

        assertTrue(TerritotemRegionFeatures.chargeReplantItem(drops, Material.NETHER_WART));
        assertTrue(drops.isEmpty());
    }

    @Test
    void packagedHierarchiesIncludeCustomAbilityNames() throws IOException {
        InputStream stream = TerritotemRegionFeaturesTest.class.getResourceAsStream("/hierarchies.json");
        assertNotNull(stream);

        String text;
        try (stream) {
            text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(text.contains("access_totem_lecterns"));
        assertTrue(text.contains("create_deeds"));
        assertTrue(text.contains("destroy_totems"));
    }

    private static Item mockDrop(Material material, int amount) {
        AtomicReference<ItemStack> stack = new AtomicReference<>(new ItemStack(material, amount));
        Item item = mock(Item.class);
        when(item.getItemStack()).thenAnswer(invocation -> stack.get());
        doAnswer(invocation -> {
            ItemStack updated = invocation.getArgument(0);
            stack.set(updated);
            return null;
        }).when(item).setItemStack(any(ItemStack.class));
        return item;
    }
}
