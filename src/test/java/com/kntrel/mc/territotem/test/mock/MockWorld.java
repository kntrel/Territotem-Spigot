package com.kntrel.mc.territotem.test.mock;

import com.kntrel.util.Vec3i;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.*;
import org.bukkit.util.Vector;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.kntrel.mc.territotem.test.mock.Mock.*;

public class MockWorld implements World {

    //FIELDS
    private final String name_;
    private final Map<Vec3i, Block> blockMap_;


    //CONSTRUCTOR
    public MockWorld(String name) {
        this.name_ = name;
        this.blockMap_ = new HashMap<>();
    }


    @Override
    public Block getBlockAt(int x, int y, int z) {
        return this.blockMap_.computeIfAbsent(
                new Vec3i(x, y, z),
                v -> new MockBlock(this, v)
        );
    }

    @Override
    public Block getBlockAt(Location location) {
        return this.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public Block getHighestBlockAt(int x, int z) {
        return this.blockMap_.values().stream()
                .filter(b -> b.getX() == x && b.getZ() == z)
                .max(Comparator.comparingInt(Block::getY))
                .orElse(this.getBlockAt(x, this.getMinHeight(), z));
    }

    @Override
    public Block getHighestBlockAt(Location location) {
        return this.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
    }

    @Override
    public Block getHighestBlockAt(int x, int z, HeightMap heightMap) {
        return this.getHighestBlockAt(x, z);
    }

    @Override
    public Block getHighestBlockAt(Location location, HeightMap heightMap) {
        return this.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
    }

    @Override
    public Chunk getChunkAt(int x, int z) {
        unimplemented();
        return null;
    }

    @Override
    public Chunk getChunkAt(int x, int z, boolean generate) {
        unimplemented();
        return null;
    }

    @Override
    public Chunk getChunkAt(Location location) {
        unimplemented();
        return null;
    }

    @Override
    public Chunk getChunkAt(Block block) {
        unimplemented();
        return null;
    }

    @Override
    public boolean isChunkLoaded(Chunk chunk) {
        unimplemented();
        return false;
    }

    @Override
    public Chunk[] getLoadedChunks() {
        return new Chunk[0];
    }

    @Override
    public void loadChunk(Chunk chunk) {

    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return false;
    }

    @Override
    public boolean isChunkGenerated(int x, int z) {
        return false;
    }

    @Override
    public boolean isChunkInUse(int x, int z) {
        return false;
    }

    @Override
    public void loadChunk(int x, int z) {

    }

    @Override
    public boolean loadChunk(int x, int z, boolean generate) {
        return false;
    }

    @Override
    public boolean unloadChunk(Chunk chunk) {
        return false;
    }

    @Override
    public boolean unloadChunk(int x, int z) {
        return false;
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        return false;
    }

    @Override
    public boolean unloadChunkRequest(int x, int z) {
        return false;
    }

    @Override
    public boolean regenerateChunk(int x, int z) {
        return false;
    }

    @Override
    public boolean refreshChunk(int x, int z) {
        return false;
    }

    @Override
    public Collection<Player> getPlayersSeeingChunk(Chunk chunk) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Player> getPlayersSeeingChunk(int x, int z) {
        return Collections.emptyList();
    }

    @Override
    public boolean isChunkForceLoaded(int x, int z) {
        return false;
    }

    @Override
    public void setChunkForceLoaded(int x, int z, boolean forced) {

    }

    @Override
    public Collection<Chunk> getForceLoadedChunks() {
        return Collections.emptyList();
    }

    @Override
    public boolean addPluginChunkTicket(int x, int z, Plugin plugin) {
        return false;
    }

    @Override
    public boolean removePluginChunkTicket(int x, int z, Plugin plugin) {
        return false;
    }

    @Override
    public void removePluginChunkTickets(Plugin plugin) {

    }

    @Override
    public Collection<Plugin> getPluginChunkTickets(int x, int z) {
        return Collections.emptyList();
    }

    @Override
    public Map<Plugin, Collection<Chunk>> getPluginChunkTickets() {
        return Collections.emptyMap();
    }

    @Override
    public Collection<Chunk> getIntersectingChunks(BoundingBox box) {
        return Collections.emptyList();
    }

    @Override
    public Item dropItem(Location location, ItemStack item) {
        unimplemented();
        return null;
    }

    @Override
    public Item dropItem(Location location, ItemStack item, Consumer<? super Item> function) {
        unimplemented();
        return null;
    }

    @Override
    public Item dropItemNaturally(Location location, ItemStack item) {
        unimplemented();
        return null;
    }

    @Override
    public Item dropItemNaturally(Location location, ItemStack item, Consumer<? super Item> function) {
        unimplemented();
        return null;
    }

    @Override
    public Arrow spawnArrow(Location location, Vector direction, float speed, float spread) {
        unimplemented();
        return null;
    }

    @Override
    public <T extends AbstractArrow> T spawnArrow(Location location, Vector direction, float speed, float spread, Class<T> clazz) {
        unimplemented();
        return null;
    }

    @Override
    public boolean generateTree(Location location, TreeType type) {
        return false;
    }

    @Override
    public boolean generateTree(Location loc, TreeType type, BlockChangeDelegate delegate) {
        return false;
    }

    @Override
    public LightningStrike strikeLightning(Location loc) {
        unimplemented();
        return null;
    }

    @Override
    public LightningStrike strikeLightningEffect(Location loc) {
        unimplemented();
        return null;
    }

    @Override
    public Biome getBiome(Location location) {
        unimplemented();
        return null;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        unimplemented();
        return null;
    }

    @Override
    public void setBiome(Location location, Biome biome) {

    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {

    }

    @Override
    public BlockState getBlockState(Location location) {
        return this.getBlockAt(location).getState();
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return this.getBlockAt(x, y, z).getState();
    }

    @Override
    public BlockData getBlockData(Location location) {
        unimplemented();
        return null;
    }

    @Override
    public BlockData getBlockData(int x, int y, int z) {
        unimplemented();
        return null;
    }

    @Override
    public Material getType(Location location) {
        unimplemented();
        return null;
    }

    @Override
    public Material getType(int x, int y, int z) {
        unimplemented();
        return null;
    }

    @Override
    public void setBlockData(Location location, BlockData blockData) {

    }

    @Override
    public void setBlockData(int x, int y, int z, BlockData blockData) {

    }

    @Override
    public void setType(Location location, Material material) {

    }

    @Override
    public void setType(int x, int y, int z, Material material) {

    }

    @Override
    public boolean generateTree(Location location, Random random, TreeType type) {
        return false;
    }

    @Override
    public boolean generateTree(Location location, Random random, TreeType type, Consumer<? super BlockState> stateConsumer) {
        return false;
    }

    @Override
    public boolean generateTree(Location location, Random random, TreeType type, Predicate<? super BlockState> statePredicate) {
        return false;
    }

    @Override
    public Entity spawnEntity(Location location, EntityType type) {
        unimplemented();
        return null;
    }

    @Override
    public Entity spawnEntity(Location loc, EntityType type, boolean randomizeData) {
        unimplemented();
        return null;
    }

    @Override
    public List<Entity> getEntities() {
        return Collections.emptyList();
    }

    @Override
    public List<LivingEntity> getLivingEntities() {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> Collection<T> getEntitiesByClass(Class<T>... classes) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> Collection<T> getEntitiesByClass(Class<T> cls) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Entity> getEntitiesByClasses(Class<?>... classes) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> T createEntity(Location location, Class<T> clazz) {
        unimplemented();
        return null;
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> clazz) throws IllegalArgumentException {
        unimplemented();
        return null;
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> clazz, Consumer<? super T> function) throws IllegalArgumentException {
        unimplemented();
        return null;
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> clazz, boolean randomizeData, Consumer<? super T> function) throws IllegalArgumentException {
        unimplemented();
        return null;
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        return 0;
    }

    @Override
    public int getHighestBlockYAt(Location location) {
        return 0;
    }

    @Override
    public int getHighestBlockYAt(int x, int z, HeightMap heightMap) {
        return 0;
    }

    @Override
    public int getHighestBlockYAt(Location location, HeightMap heightMap) {
        return 0;
    }

    @Override
    public <T extends Entity> T addEntity(T entity) {
        unimplemented();
        return null;
    }

    @Override
    public List<Player> getPlayers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Entity> getNearbyEntities(Location location, double x, double y, double z) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Entity> getNearbyEntities(Location location, double x, double y, double z, Predicate<? super Entity> filter) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Entity> getNearbyEntities(BoundingBox boundingBox) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Entity> getNearbyEntities(BoundingBox boundingBox, Predicate<? super Entity> filter) {
        return Collections.emptyList();
    }

    @Override
    public RayTraceResult rayTraceEntities(Location start, Vector direction, double maxDistance) {
        unimplemented();
        return null;
    }

    @Override
    public RayTraceResult rayTraceEntities(Location start, Vector direction, double maxDistance, double raySize) {
        unimplemented();
        return null;
    }

    @Override
    public RayTraceResult rayTraceEntities(Location start, Vector direction, double maxDistance, Predicate<? super Entity> filter) {
        unimplemented();
        return null;
    }

    @Override
    public RayTraceResult rayTraceEntities(Location start, Vector direction, double maxDistance, double raySize, Predicate<? super Entity> filter) {
        unimplemented();
        return null;
    }

    @Override
    public RayTraceResult rayTraceBlocks(Location start, Vector direction, double maxDistance) {
        unimplemented();
        return null;
    }

    @Override
    public RayTraceResult rayTraceBlocks(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode) {
        unimplemented();
        return null;
    }

    @Override
    public RayTraceResult rayTraceBlocks(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode, boolean ignorePassableBlocks) {
        unimplemented();
        return null;
    }

    @Override
    public RayTraceResult rayTrace(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode, boolean ignorePassableBlocks, double raySize, Predicate<? super Entity> filter) {
        unimplemented();
        return null;
    }

    @Override
    public Location getSpawnLocation() {
        unimplemented();
        return null;
    }

    @Override
    public boolean setSpawnLocation(Location location) {
        return false;
    }

    @Override
    public boolean setSpawnLocation(int x, int y, int z, float angle) {
        return false;
    }

    @Override
    public boolean setSpawnLocation(int x, int y, int z) {
        return false;
    }

    @Override
    public long getTime() {
        return 0;
    }

    @Override
    public void setTime(long time) {

    }

    @Override
    public long getFullTime() {
        return 0;
    }

    @Override
    public void setFullTime(long time) {

    }

    @Override
    public long getGameTime() {
        return 0;
    }

    @Override
    public boolean hasStorm() {
        return false;
    }

    @Override
    public void setStorm(boolean hasStorm) {

    }

    @Override
    public int getWeatherDuration() {
        return 0;
    }

    @Override
    public void setWeatherDuration(int duration) {

    }

    @Override
    public boolean isThundering() {
        return false;
    }

    @Override
    public void setThundering(boolean thundering) {

    }

    @Override
    public int getThunderDuration() {
        return 0;
    }

    @Override
    public void setThunderDuration(int duration) {

    }

    @Override
    public boolean isClearWeather() {
        return false;
    }

    @Override
    public void setClearWeatherDuration(int duration) {

    }

    @Override
    public int getClearWeatherDuration() {
        return 0;
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power) {
        return false;
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire) {
        return false;
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire, boolean breakBlocks) {
        return false;
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire, boolean breakBlocks, Entity source) {
        return false;
    }

    @Override
    public boolean createExplosion(Location loc, float power) {
        return false;
    }

    @Override
    public boolean createExplosion(Location loc, float power, boolean setFire) {
        return false;
    }

    @Override
    public boolean createExplosion(Location loc, float power, boolean setFire, boolean breakBlocks) {
        return false;
    }

    @Override
    public boolean createExplosion(Location loc, float power, boolean setFire, boolean breakBlocks, Entity source) {
        return false;
    }

    @Override
    public boolean getPVP() {
        return false;
    }

    @Override
    public void setPVP(boolean pvp) {

    }

    @Override
    public ChunkGenerator getGenerator() {
        unimplemented();
        return null;
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        unimplemented();
        return null;
    }

    @Override
    public void save() {

    }

    @Override
    public List<BlockPopulator> getPopulators() {
        return Collections.emptyList();
    }

    @Override
    public <T extends LivingEntity> T spawn(Location location, Class<T> clazz, CreatureSpawnEvent.SpawnReason spawnReason, boolean randomizeData, Consumer<? super T> function) throws IllegalArgumentException {
        return null;
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, MaterialData data) throws IllegalArgumentException {
        return null;
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, BlockData data) throws IllegalArgumentException {
        return null;
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, Material material, byte data) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void playEffect(Location location, Effect effect, int data) {

    }

    @Override
    public void playEffect(Location location, Effect effect, int data, int radius) {

    }

    @Override
    public <T> void playEffect(Location location, Effect effect, T data) {

    }

    @Override
    public <T> void playEffect(Location location, Effect effect, T data, int radius) {

    }

    @Override
    public ChunkSnapshot getEmptyChunkSnapshot(int x, int z, boolean includeBiome, boolean includeBiomeTemp) {
        return null;
    }

    @Override
    public void setSpawnFlags(boolean allowMonsters, boolean allowAnimals) {

    }

    @Override
    public boolean getAllowAnimals() {
        return false;
    }

    @Override
    public boolean getAllowMonsters() {
        return false;
    }

    @Override
    public Biome getBiome(int x, int z) {
        return null;
    }

    @Override
    public void setBiome(int x, int z, Biome bio) {

    }

    @Override
    public double getTemperature(int x, int z) {
        return 0;
    }

    @Override
    public double getTemperature(int x, int y, int z) {
        return 0;
    }

    @Override
    public double getHumidity(int x, int z) {
        return 0;
    }

    @Override
    public double getHumidity(int x, int y, int z) {
        return 0;
    }

    @Override
    public int getLogicalHeight() {
        return 0;
    }

    @Override
    public boolean isNatural() {
        return false;
    }

    @Override
    public boolean isBedWorks() {
        return false;
    }

    @Override
    public boolean hasSkyLight() {
        return false;
    }

    @Override
    public boolean hasCeiling() {
        return false;
    }

    @Override
    public boolean isPiglinSafe() {
        return false;
    }

    @Override
    public boolean isRespawnAnchorWorks() {
        return false;
    }

    @Override
    public boolean hasRaids() {
        return false;
    }

    @Override
    public boolean isUltraWarm() {
        return false;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public boolean getKeepSpawnInMemory() {
        return false;
    }

    @Override
    public void setKeepSpawnInMemory(boolean keepLoaded) {

    }

    @Override
    public boolean isAutoSave() {
        return false;
    }

    @Override
    public void setAutoSave(boolean value) {

    }

    @Override
    public void setDifficulty(Difficulty difficulty) {

    }

    @Override
    public Difficulty getDifficulty() {
        unimplemented();
        return null;
    }

    @Override
    public int getViewDistance() {
        return 0;
    }

    @Override
    public int getSimulationDistance() {
        return 0;
    }

    @Override
    public File getWorldFolder() {
        unimplemented();
        return null;
    }

    @Override
    public WorldType getWorldType() {
        unimplemented();
        return null;
    }

    @Override
    public boolean canGenerateStructures() {
        return false;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public void setHardcore(boolean hardcore) {

    }

    @Override
    public long getTicksPerAnimalSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerAnimalSpawns(int ticksPerAnimalSpawns) {

    }

    @Override
    public long getTicksPerMonsterSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerMonsterSpawns(int ticksPerMonsterSpawns) {

    }

    @Override
    public long getTicksPerWaterSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerWaterSpawns(int ticksPerWaterSpawns) {

    }

    @Override
    public long getTicksPerWaterAmbientSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerWaterAmbientSpawns(int ticksPerAmbientSpawns) {

    }

    @Override
    public long getTicksPerWaterUndergroundCreatureSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerWaterUndergroundCreatureSpawns(int ticksPerWaterUndergroundCreatureSpawns) {

    }

    @Override
    public long getTicksPerAmbientSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerAmbientSpawns(int ticksPerAmbientSpawns) {

    }

    @Override
    public long getTicksPerSpawns(SpawnCategory spawnCategory) {
        return 0;
    }

    @Override
    public void setTicksPerSpawns(SpawnCategory spawnCategory, int ticksPerCategorySpawn) {

    }

    @Override
    public int getMonsterSpawnLimit() {
        return 0;
    }

    @Override
    public void setMonsterSpawnLimit(int limit) {

    }

    @Override
    public int getAnimalSpawnLimit() {
        return 0;
    }

    @Override
    public void setAnimalSpawnLimit(int limit) {

    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return 0;
    }

    @Override
    public void setWaterAnimalSpawnLimit(int limit) {

    }

    @Override
    public int getWaterUndergroundCreatureSpawnLimit() {
        return 0;
    }

    @Override
    public void setWaterUndergroundCreatureSpawnLimit(int limit) {

    }

    @Override
    public int getWaterAmbientSpawnLimit() {
        return 0;
    }

    @Override
    public void setWaterAmbientSpawnLimit(int limit) {

    }

    @Override
    public int getAmbientSpawnLimit() {
        return 0;
    }

    @Override
    public void setAmbientSpawnLimit(int limit) {

    }

    @Override
    public int getSpawnLimit(SpawnCategory spawnCategory) {
        return 0;
    }

    @Override
    public void setSpawnLimit(SpawnCategory spawnCategory, int limit) {

    }

    @Override
    public void playNote(Location loc, Instrument instrument, Note note) {

    }

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {

    }

    @Override
    public void playSound(Location location, String sound, float volume, float pitch) {

    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSound(Entity entity, Sound sound, float volume, float pitch) {

    }

    @Override
    public void playSound(Entity entity, String sound, float volume, float pitch) {

    }

    @Override
    public void playSound(Entity entity, Sound sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void playSound(Entity entity, Sound sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public String[] getGameRules() {
        unimplemented();
        return new String[0];
    }

    @Override
    public boolean isGameRule(String rule) {
        return false;
    }

    @Override
    public <T> T getGameRuleValue(GameRule<T> rule) {
        unimplemented();
        return null;
    }

    @Override
    public <T> T getGameRuleDefault(GameRule<T> rule) {
        unimplemented();
        return null;
    }

    @Override
    public <T> boolean setGameRule(GameRule<T> rule, T newValue) {
        return false;
    }

    @Override
    public WorldBorder getWorldBorder() {
        unimplemented();
        return null;
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count) {

    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {

    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ) {

    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, T data) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, T data) {

    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {

    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data, boolean force) {

    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data, boolean force) {

    }

    @Override
    public Location locateNearestStructure(Location origin, StructureType structureType, int radius, boolean findUnexplored) {
        unimplemented();
        return null;
    }

    @Override
    public StructureSearchResult locateNearestStructure(Location origin, org.bukkit.generator.structure.StructureType structureType, int radius, boolean findUnexplored) {
        unimplemented();
        return null;
    }

    @Override
    public StructureSearchResult locateNearestStructure(Location origin, Structure structure, int radius, boolean findUnexplored) {
        unimplemented();
        return null;
    }

    @Override
    public Spigot spigot() {
        unimplemented();
        return null;
    }

    @Override
    public BiomeSearchResult locateNearestBiome(Location origin, int radius, Biome... biomes) {
        unimplemented();
        return null;
    }

    @Override
    public BiomeSearchResult locateNearestBiome(Location origin, int radius, int horizontalInterval, int verticalInterval, Biome... biomes) {
        unimplemented();
        return null;
    }

    @Override
    public Raid locateNearestRaid(Location location, int radius) {
        unimplemented();
        return null;
    }

    @Override
    public List<Raid> getRaids() {
        return Collections.emptyList();
    }

    @Override
    public DragonBattle getEnderDragonBattle() {
        unimplemented();
        return null;
    }

    @Override
    public Set<FeatureFlag> getFeatureFlags() {
        return Collections.emptySet();
    }

    @Override
    public Collection<GeneratedStructure> getStructures(int x, int z) {
        return Collections.emptyList();
    }

    @Override
    public Collection<GeneratedStructure> getStructures(int x, int z, Structure structure) {
        return Collections.emptyList();
    }

    @Override
    public NamespacedKey getKey() {
        unimplemented();
        return null;
    }

    @Override
    public String getName() {
        return this.name_;
    }

    @Override
    public UUID getUID() {
        return UUID.nameUUIDFromBytes(this.name_.getBytes());
    }

    @Override
    public Environment getEnvironment() {
        return World.Environment.NORMAL;
    }

    @Override
    public long getSeed() {
        return 0;
    }

    @Override
    public int getMinHeight() {
        return -64;
    }

    @Override
    public int getMaxHeight() {
        return 320;
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {

    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return false;
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {

    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        unimplemented();
        return null;
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {

    }

    @Override
    public Set<String> getListeningPluginChannels() {
        return Collections.emptySet();
    }
}