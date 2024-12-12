package com.jkantrell.landlords.totem;

import com.google.gson.*;
import com.jkantrell.landlords.io.Serializer;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.rules.Rule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.lang.reflect.Type;
import java.util.*;

public class Blueprint {

    //STATIC FIELDS
    private static final ArrayList<Blueprint> blueprints_ = new ArrayList<>();

    //STATIC METHODS
    public static int getHighestId() {
        if (Blueprint.blueprints_.isEmpty()) { return 0; }
        Blueprint.blueprints_.sort(Comparator.comparingInt(Blueprint::getId));
        return Blueprint.blueprints_.get(Blueprint.blueprints_.size() - 1).getId();
    }
    public static Blueprint get(int id) {
        return Blueprint.blueprints_.stream().filter(b -> b.getId() == id).findFirst().orElse(null);
    }

    //FIELDS
    private final Elements elements_ = new Elements(this);
    private final Lecterns lecterns_ = new Lecterns(this);
    private int id_;
    private double[] regionInitialVertex_ = new double[6];
    private double[] regionGrowthRate_ = new double[6];
    private double regionDirectionalGrowth_ = 0;
    private Vector regionMaxSize_;
    private Hierarchy hierarchy_;
    private List<Rule> rules_;
    private BoundingBox regionBaseBox_;
    private final BoundingBox structuralBox_ = new BoundingBox();

    //CONSTRUCTORS
    public Blueprint(double[] regionBaseSize, double[] regionGrowthRate, Vector regionMaxSize, Hierarchy hierarchy){
        this.id_ = Blueprint.getHighestId() + 1;
        this.setRegionInitialVertex(regionBaseSize);
        this.setRegionGrowthRate(regionGrowthRate);
        this.setRegionMaxSize(regionMaxSize);
        this.setHierarchy(hierarchy);
        Blueprint.blueprints_.add(this);
    }

    //GETTERS
    public int getId() {
        return this.id_;
    }
    public double[] getRegionInitialVertex(){
        return this.regionInitialVertex_;
    }
    public double[] getRegionGrowthRate(){
        return this.regionGrowthRate_;
    };
    public double getRegionDirectionalGrowth() {
        return this.regionDirectionalGrowth_;
    }
    public Vector getRegionMaxSize() {
        return this.regionMaxSize_;
    }
    public BoundingBox getBaseSizeBox() {
        return new BoundingBox().copy(this.regionBaseBox_);
    }
    public BoundingBox getStructuralBox() {
        return new BoundingBox().copy(this.structuralBox_);
    }
    public double[] getRegionInitialSize(){

        double[] r = new double[3];
        double[] vert = this.getRegionInitialVertex();
        for (int i = 0; i < 3; i++) {
            r[i] = Math.abs(vert[i] - vert[i+3]);
        }
        return r;
    }
    public Hierarchy getHierarchy() {
        return this.hierarchy_;
    }
    public Rule[] getRules() {
        return this.rules_.toArray(new Rule[0]);
    }
    public TotemLectern[] getLecterns() {
        return this.lecterns_.toArray(new TotemLectern[0]);
    }
    public TotemElement<?>[] getElements() {
        return this.elements_.toArray(new TotemElement<?>[0]);
    }

    //SETTERS
    private void setId(int id) {
        if (this.getId() == id) { return; }
        if (Blueprint.blueprints_.stream().anyMatch(b -> b.getId() == id)) {
            throw new IllegalArgumentException("There's already a blueprint with ID " + id + ".");
        }
        this.id_ = id;
    }
    public void setRegionInitialVertex(double[] baseSizeArray){
        this.regionInitialVertex_ = baseSizeArray;
        this.regionBaseBox_ = new BoundingBox(baseSizeArray[0], baseSizeArray[1], baseSizeArray[2], baseSizeArray[3], baseSizeArray[4], baseSizeArray[5]);
    }
    public void setRegionGrowthRate(double[] growthRateArray){
        regionGrowthRate_ = growthRateArray;
    };
    public void setRegionDirectionalGrowth(double magnitude) {
        this.regionDirectionalGrowth_ = magnitude;
    }
    public void setRegionMaxSize(Vector maxSize) {
        this.regionMaxSize_ = maxSize;
    }
    public void setHierarchy(Hierarchy hierarchy) {
        this.hierarchy_ = hierarchy;
    }
    public void setRules (List<Rule> rules) {
        this.rules_ = rules;
    }
    public void addElement(TotemElement<?> element) {
        this.elements_.add(element);
        this.setStructureBox_();
    }
    public <T> void addElement(T element, int x, int y, int z) {
        this.elements_.add(element,x,y,z);
        this.setStructureBox_();
    }

    //PUBLIC METHODS
    public boolean testStructure(int x, int y, int z, World world){

        boolean r = true;
        for (TotemElement<?> i : this.elements_){
            Vector pos = i.getPosition();
            Block b = world.getBlockAt(
                    x + pos.getBlockX(),
                    y + pos.getBlockY(),
                    z + pos.getBlockZ()
                    );
            if(i instanceof TotemBlock){
                Material type = b.getType();
                if(!type.equals(i.getType())){
                    r=false;
                    break;
                }
            }
            if(i instanceof TotemEntity){
                BoundingBox bounding = new BoundingBox(
                        b.getX(),
                        b.getY(),
                        b.getZ(),
                        b.getX() + 1,
                        b.getY() + 1,
                        b.getZ() + 1
                        );
                Collection<Entity> entities = world.getNearbyEntities(bounding);
                boolean found = false;
                for (Entity e : entities){
                    if(e.getType().equals(i.getType())){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    r=false;
                    break;
                }
            }
        }
        return r;
    }
    public boolean testStructure(Location location) {
        World world = location.getWorld();
        if (world == null) { return false; }
        Block block = location.getBlock();
        return this.testStructure(block.getX(),block.getY(),block.getZ(), world);
    }

    //PRIVATE METHODS
    private void setStructureBox_() {
        Elements elements = this.elements_;

        int[] mins = elements.get(0).getBlockPositionArray();
        int[] maxs = elements.get(0).getBlockPositionArray();

        for (TotemElement<?> e : elements) {
            int[] pos = e.getBlockPositionArray();
            for(int i = 0; i < 3; i++){
                maxs[i] = Math.max(maxs[i],pos[i]);
                mins[i] = Math.min(mins[i],pos[i]);
            }
        }

        double[] toResize = new double[6];
        for (int i = 0; i < 6 ; i++){
            toResize[i] = (i < 3) ? mins[i] : maxs[i-3]+1;
        }
        this.structuralBox_.resize(toResize[0],toResize[1],toResize[2],toResize[3],toResize[4],toResize[5]);
    }

    //CLASSES
    private static class Elements extends ArrayList<TotemElement<?>> {
        private final Blueprint structure_;

        private Elements(Blueprint structure) {
            super();
            this.structure_ = structure;
        }

        @Override
        @Deprecated
        public boolean add(TotemElement<?> totemElement) {
            return super.add(totemElement);
        }

        public <T> boolean add (T type,int x, int y, int z) {

            TotemElement<?> element;
            if (type instanceof EntityType entityType) {
                element = new TotemEntity(entityType,x,y,z,structure_);
            } else if (type instanceof Material material) {
                if (!material.isBlock()) { return false; }
                element = new TotemBlock(material,x,y,z,structure_);
            } else { return false; }
            return super.add(element);
        }
    }
    private static class Lecterns extends ArrayList<TotemLectern> {
        private final Blueprint structure_;

        private Lecterns(Blueprint structure) {
            this.structure_ = structure;
        }
        @Override
        @Deprecated
        public boolean add(TotemLectern totemLectern) {
            return super.add(totemLectern);
        }

        public boolean add (int x, int y, int z, BlockFace facing) {
            return super.add(new TotemLectern(x,y,z,facing,structure_));
        }
    }

    public static class JDeserializer implements JsonDeserializer<Blueprint> {

        @Override
        public Blueprint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonStructure = json.getAsJsonObject();
            JsonObject jsonRegion = jsonStructure.get("region").getAsJsonObject();

            double[] maxSize = Serializer.GSON.fromJson(jsonRegion.get("max_size"),double[].class);
            if (maxSize == null) { maxSize = new double[] {-1,-1,-1}; }
            Blueprint blueprint = new Blueprint(
                    Serializer.GSON.fromJson(jsonRegion.get("initial_size"),double[].class),
                    Serializer.GSON.fromJson(jsonRegion.get("growth_rate"),double[].class),
                    new Vector(maxSize[0], maxSize[1], maxSize[2]),
                    Hierarchy.get(jsonRegion.get("hierarchy").getAsInt())
            );

            blueprint.setId(jsonStructure.get("id").getAsInt());

            blueprint.setRegionDirectionalGrowth(
                jsonRegion.has("directional_growth") ?
                        jsonRegion.get("directional_growth").getAsDouble() :
                        Arrays.stream(blueprint.getRegionGrowthRate()).reduce(0, Double::sum)
            );

            JsonObject jsonObject; TotemElement.ElementType type; int[] pos; String name;
            for (JsonElement elm : jsonStructure.get("elements").getAsJsonArray()) {
                jsonObject = elm.getAsJsonObject();
                type = TotemElement.ElementType.valueOf(jsonObject.get("type").getAsString());
                pos = Serializer.GSON.fromJson(jsonObject.get("position"),int[].class);
                name = jsonObject.get("name").getAsString();

                switch (type) {
                    case block -> blueprint.addElement(
                            Material.valueOf(name),
                            pos[0], pos[1], pos [2]
                    );

                    default -> blueprint.addElement(
                            EntityType.valueOf(name),
                            pos[0], pos[1], pos [2]
                    );
                }
            }
            BlockFace facing;
            for (JsonElement elm : jsonStructure.get("lecterns").getAsJsonArray()) {
                jsonObject = elm.getAsJsonObject();
                pos = Serializer.GSON.fromJson(jsonObject.get("position"),int[].class);
                facing = BlockFace.valueOf(jsonObject.get("direction").getAsString());

                blueprint.lecterns_.add(pos[0],pos[1],pos[2],facing);
            }

            if (jsonRegion.has("rules")) {
                JsonObject jsonRules = jsonRegion.get("rules").getAsJsonObject();
                ArrayList<Rule> rules = new ArrayList<>();
                for (Map.Entry<String,JsonElement> entry : jsonRules.entrySet()) {
                    JsonObject jsonRule = new JsonObject();
                    jsonRule.add(entry.getKey(),entry.getValue());
                    rules.add(Serializer.GSON.fromJson(jsonRule,Rule.class));
                }
                blueprint.setRules(rules);
            }

            return blueprint;
        }
    }
}

