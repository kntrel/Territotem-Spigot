package com.kntrel.mc.territotem.test.mock;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.util.Vec3i;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;
import java.util.*;

import static org.mockito.Mockito.*;

public class MockBlock implements Block {

    //FIELDS
    private final Vec3i coordinates;
    private final World world;
    private Material material;
    private BlockData blockData;
    private byte rawData;
    private Biome biome;
    private byte lightLevel;
    private byte lightFromSky;
    private byte lightFromBlocks;
    private boolean blockPowered;
    private boolean blockIndirectlyPowered;
    private int blockPower;
    private final Map<String, List<MetadataValue>> metadata;


    //CONSTRUCTOR
    public MockBlock(World world, Vec3i coordinates) {
        this.world = world;
        this.coordinates = coordinates;
        this.material = Material.AIR;
        this.blockData = mock(BlockData.class);
        this.rawData = 0;
        this.biome = null;
        this.lightLevel = 0;
        this.lightFromSky = 0;
        this.lightFromBlocks = 0;
        this.blockPowered = false;
        this.blockIndirectlyPowered = false;
        this.blockPower = 0;
        this.metadata = new HashMap<>();

        when(this.blockData.createBlockState()).then(m -> new MockBlockState(this));
    }

    @Override
    public byte getData() {
        return this.rawData;
    }

    @Override
    public BlockData getBlockData() {
        return this.blockData;
    }

    @Override
    public Block getRelative(int modX, int modY, int modZ) {
        return new MockBlock(this.world, new Vec3i (this.coordinates.x() + modX,
                             this.coordinates.y() + modY, 
                             this.coordinates.z() + modZ));
    }

    @Override
    public Block getRelative(BlockFace face) {
        return getRelative(face, 1);
    }

    @Override
    public Block getRelative(BlockFace face, int distance) {
        return getRelative(face.getModX() * distance, 
                          face.getModY() * distance, 
                          face.getModZ() * distance);
    }

    @Override
    public Material getType() {
        return this.material;
    }

    @Override
    public byte getLightLevel() {
        return this.lightLevel;
    }

    @Override
    public byte getLightFromSky() {
        return this.lightFromSky;
    }

    @Override
    public byte getLightFromBlocks() {
        return this.lightFromBlocks;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public int getX() {
        return this.coordinates.x();
    }

    @Override
    public int getY() {
        return this.coordinates.y();
    }

    @Override
    public int getZ() {
        return this.coordinates.z();
    }

    @Override
    public Location getLocation() {
        return new Location(this.world, this.coordinates.x(), 
                           this.coordinates.y(), 
                           this.coordinates.z());
    }

    @Override
    public Location getLocation(Location loc) {
        if (loc == null) {
            return null;
        }
        loc.setWorld(this.world);
        loc.setX(this.coordinates.x());
        loc.setY(this.coordinates.y());
        loc.setZ(this.coordinates.z());
        return loc;
    }

    @Override
    public Chunk getChunk() {
        return this.world.getChunkAt(this.coordinates.x() >> Constants.CHUNK_SHIFT,
                                     this.coordinates.y() >> Constants.CHUNK_SHIFT);
    }

    @Override
    public void setBlockData(BlockData data) {
        setBlockData(data, true);
    }

    @Override
    public void setBlockData(BlockData data, boolean applyPhysics) {
        this.blockData = data;
    }

    @Override
    public void setType(Material type) {
        setType(type, true);
    }

    @Override
    public void setType(Material type, boolean applyPhysics) {
        this.material = type;
    }

    @Override
    public BlockFace getFace(Block block) {
        if (block == null) {
            return null;
        }
        
        int dx = block.getX() - this.coordinates.x();
        int dy = block.getY() - this.coordinates.y();
        int dz = block.getZ() - this.coordinates.z();
        
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) {
            return null;
        }
        
        if (dx == 1) return BlockFace.EAST;
        if (dx == -1) return BlockFace.WEST;
        if (dy == 1) return BlockFace.UP;
        if (dy == -1) return BlockFace.DOWN;
        if (dz == 1) return BlockFace.SOUTH;
        if (dz == -1) return BlockFace.NORTH;
        
        return null;
    }

    @Override
    public BlockState getState() {
        return new MockBlockState(this);
    }

    @Override
    public Biome getBiome() {
        return this.biome;
    }

    @Override
    public void setBiome(Biome bio) {
        this.biome = bio;
    }

    @Override
    public boolean isBlockPowered() {
        return this.blockPowered;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return this.blockIndirectlyPowered;
    }

    @Override
    public boolean isBlockFacePowered(BlockFace face) {
        return this.blockPowered;
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return this.blockIndirectlyPowered;
    }

    @Override
    public int getBlockPower(BlockFace face) {
        return this.blockPower;
    }

    @Override
    public int getBlockPower() {
        return this.blockPower;
    }

    @Override
    public boolean isEmpty() {
        return this.material == Material.AIR;
    }

    @Override
    public boolean isLiquid() {
        return this.material == Material.WATER || this.material == Material.LAVA;
    }

    @Override
    public double getTemperature() {
        return 0;
    }

    @Override
    public double getHumidity() {
        return 0;
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return PistonMoveReaction.MOVE;
    }

    @Override
    public boolean breakNaturally() {
        return breakNaturally(null);
    }

    @Override
    public boolean breakNaturally(ItemStack tool) {
        this.material = Material.AIR;
        return true;
    }

    @Override
    public boolean applyBoneMeal(BlockFace face) {
        return false;
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return getDrops(null);
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool) {
        return getDrops(tool, null);
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool, Entity entity) {
        return new ArrayList<>();
    }

    @Override
    public boolean isPreferredTool(ItemStack tool) {
        return false;
    }

    @Override
    public float getBreakSpeed(Player player) {
        return 1.0f;
    }

    @Override
    public boolean isPassable() {
        return this.material == Material.AIR || 
               (this.material != null && this.material.isOccluding());
    }

    @Override
    public RayTraceResult rayTrace(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode) {
        return null;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(
                this.coordinates.x(),
                this.coordinates.y(),
                this.coordinates.z(),
                this.coordinates.x() + 1,
                this.coordinates.y() + 1,
                this.coordinates.z() + 1
        );
    }

    @Override
    public VoxelShape getCollisionShape() {
        return mock(VoxelShape.class);
    }

    @Override
    public boolean canPlace(BlockData data) {
        return true;
    }

    @Override
    public String getTranslationKey() {
        return "block.minecraft." + (this.material != null ? 
               this.material.getKey().getKey() : "air");
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.metadata.put(metadataKey, List.of(newMetadataValue));
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.metadata.getOrDefault(metadataKey, List.of());
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return this.metadata.containsKey(metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.metadata.remove(metadataKey);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!(o instanceof MockBlock other)) { return false; }
        return this.material == other.material && this.coordinates.equals(other.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.material, this.coordinates);
    }
}
