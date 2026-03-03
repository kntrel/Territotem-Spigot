package com.kntrel.mc.territotem.totem;

import com.kntrel.util.Vec3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Map;
import java.util.Objects;


public class TotemCore {

    //CONSTANTS
    private static final Vector ZEROES = new Vector(0, 0, 0),
                                ENDER_EYE_OFFSET = new Vector(.5, .9, .5),
                                CENTER = new Vector(.5, .5, .5),
                                CENTER_BOTTOM = new Vector(.5, 0, .5);
    private static final Display.Brightness BRIGHTNESS = new Display.Brightness(15, 0);
    private static final Map<Direction, Vector3f> TRANSLATIONS = Map.of(
            Direction.UP, new Vector3f(.4f, 1.2f, .4f),
            Direction.DOWN, new Vector3f(.4f, .4f, .4f),
            Direction.EAST, new Vector3f(.9f, .8f, .4f),
            Direction.WEST, new Vector3f(-.1f, .8f, .4f),
            Direction.NORTH, new Vector3f(.4f, .8f, -.1f),
            Direction.SOUTH, new Vector3f(.4f, .8f, .9f)
    );


    //ENUMS
    public enum State { EMPTY, AMETHIST, END_EYE, FULL, ACTIVE }
    public enum Direction{
        UP, DOWN, SOUTH, NORTH, EAST, WEST, ALL;

        public boolean isOnX() { return this == EAST || this == WEST; }
        public boolean isOnY() { return this == UP || this == DOWN; }
        public boolean isOnZ() { return this == SOUTH || this == NORTH; }
    }


    //FIELDS
    private final Vec3i coordinates_;
    private final World world_;
    private BlockDisplay amethist_, directional_;
    private ItemDisplay enderEye_;
    private Interaction hitBox_;
    private State state_;
    private Direction direction_;


    TotemCore(Vec3i coordinates, World world, State state, Direction direction) {
        this.coordinates_ = coordinates;
        this.world_ = world;
        this.amethist_ = null;
        this.enderEye_ = null;
        this.hitBox_ = null;
        this.directional_ = null;
        this.direction_ = null;
        this.state_ = null;     //set to null here so setState() runs any state invariants (including UNINITIALIZED)

        this.setState(state);
        this.setDirection(direction);
    }
    TotemCore(Vec3i coordinates, World world, State state) {
        this(coordinates, world,  state, Direction.ALL);
    }


    //GETTERS
    public Vec3i getCoordinates() {
        return this.coordinates_;
    }
    public World getWorld() {
        return this.world_;
    }
    public @Nullable BlockDisplay getAmethist() {
        return this.amethist_;
    }
    public @Nullable ItemDisplay getEnderEye() {
        return this.enderEye_;
    }
    public @Nullable Interaction getHitBox() {
        return this.hitBox_;
    }
    public @Nullable BlockDisplay getDirectional() {
        return this.directional_;
    }
    public State getState() {
        return this.state_;
    }
    public Direction getDirection() {
        return this.direction_;
    }


    //ACTIONS
    public void setState(@NonNull State state) {
        Objects.requireNonNull(state);
        if (state == this.state_) { return; }
        this.state_ = state;

        if (state == State.EMPTY) {
            kill(this.amethist_);
            kill(this.enderEye_);
            kill(this.hitBox_);
            kill(this.directional_);
            this.amethist_ = null;
            this.enderEye_ = null;
            this.hitBox_ = null;
            this.directional_ = null;
            return;
        }

        if (state == State.FULL || state == State.END_EYE || state == State.ACTIVE) {
            if (this.enderEye_ == null) {
                this.enderEye_ = this.spawnItemDisplay();
            }
            this.refinishEndEye();
            this.enderEye_.setBillboard(
                    (state == State.ACTIVE) ? Display.Billboard.CENTER : Display.Billboard.FIXED
            );
        } else {
            kill(this.enderEye_);
            this.enderEye_ = null;
        }


        if (state == State.AMETHIST || state == State.FULL) {
            if (this.amethist_ == null) {
                this.amethist_ = this.spawnBlockDisplay();
            }
            this.refinishAmethist();
        } else {
            kill(this.amethist_);
            this.amethist_ = null;
        }

        if (state == State.ACTIVE) {
            if (this.hitBox_ == null) {
                this.hitBox_ = this.spawnInteraction();
            }
            this.refinishHitBox();
        } else {
            kill(this.hitBox_);
            this.hitBox_ = null;
        }

        this.refinishBlock();
    }
    public void setDirection(@NonNull Direction direction) {
        Objects.requireNonNull(direction);
        if (direction == this.direction_) { return; }
        this.direction_ = direction;

        if (direction == Direction.ALL) {
            kill(this.directional_);
            this.directional_ = null;
        } else if (this.directional_ == null) {
            this.directional_ = spawnBlockDisplay();
        }
        this.refinishDirectional();
        this.refinishDirectionalEnderEye();
    }
    public void breakDown() {
        Block b = this.world_.getBlockAt(this.coordinates_.x(), this.coordinates_.y(), this.coordinates_.z());
        b.setType(Material.AIR, false);

        if (this.state_ != State.ACTIVE) {
            this.world_.dropItemNaturally(this.location(CENTER), new ItemStack(Material.TINTED_GLASS, 1));
        }
        if (this.state_ == State.END_EYE || this.state_ == State.FULL || this.state_ == State.ACTIVE) {
            this.world_.dropItemNaturally(this.location(CENTER), new ItemStack(Material.ENDER_EYE, 1));
        }
        if (this.state_ != State.END_EYE) {
            this.world_.dropItemNaturally(this.location(CENTER), new ItemStack(Material.AMETHYST_SHARD, 1));
        }
        if (this.state_ == State.ACTIVE && this.direction_ != Direction.ALL) {
            this.world_.dropItemNaturally(this.location(CENTER), new ItemStack(Material.AMETHYST_BLOCK, 1));
        }

        this.setState(State.EMPTY);
    }
    public void removeDirection() {
        if (this.direction_ == Direction.ALL) { return; }

        this.world_.dropItemNaturally(this.location(CENTER), new ItemStack(Material.AMETHYST_BLOCK, 1));
        this.setDirection(Direction.ALL);
    }



    //HELPERS
    private Location location(Vector offset) {
        return new Location(
                this.world_,
                this.coordinates_.x() + offset.getX(),
                this.coordinates_.y() + offset.getY(),
                this.coordinates_.z() + offset.getZ()
        );
    }
    private Location location() {
        return this.location(ZEROES);
    }
    private void refinishAmethist() {
        if (this.amethist_ == null) { return; }

        this.amethist_.setBlock(Material.MEDIUM_AMETHYST_BUD.createBlockData());
        this.amethist_.setBrightness(BRIGHTNESS);
        this.amethist_.teleport(this.location());
    }
    private void refinishEndEye() {
        if (this.enderEye_ == null) { return; }

        this.enderEye_.setItemStack(new ItemStack(Material.ENDER_EYE));
        this.enderEye_.teleport(this.location(ENDER_EYE_OFFSET));
        this.enderEye_.setBrightness(BRIGHTNESS);
    }
    private void refinishBlock() {
        Block b = this.world_.getBlockAt(this.coordinates_.x(), this.coordinates_.y(), this.coordinates_.z());
        Material type = (this.state_ == State.ACTIVE) ? Material.MEDIUM_AMETHYST_BUD : Material.TINTED_GLASS;
        b.setType(type, false);
    }
    private void refinishHitBox() {
        if (this.hitBox_ == null) { return; }

        this.hitBox_.setInteractionHeight(1);
        this.hitBox_.setInteractionWidth(1);
        this.hitBox_.teleport(this.location(CENTER_BOTTOM));
    }
    public void refinishDirectional() {
        if (this.directional_ == null) { return; }
        if (this.direction_ == null || this.direction_ == Direction.ALL) { return; }

        this.directional_.setBlock(Material.AMETHYST_BLOCK.createBlockData());
        this.directional_.teleport(this.location());

        Vector3f scale = new Vector3f(
            this.direction_.isOnX() ? .25f : .2f,
            this.direction_.isOnY() ? .25f : .2f,
            this.direction_.isOnZ() ? .25f : .2f
        );
        Vector3f translation = TRANSLATIONS.get(this.direction_);
        AxisAngle4f rotation = new AxisAngle4f(0, 0, 0, 1);
        Transformation transformation = new Transformation(translation, rotation, scale, rotation);

        this.directional_.setTransformation(transformation);
    }
    private void refinishDirectionalEnderEye() {
        if (this.enderEye_ == null) { return; }
        if (this.state_ != State.ACTIVE) { return; }

        Direction dir = (this.direction_ == null) ? Direction.ALL : this.direction_;

        Vector correction = switch (dir) {
            case UP -> new Vector(0, -.05, 0);
            case DOWN -> new Vector(0, .2, 0);
            case SOUTH -> new Vector(0, 0, -.1);
            case NORTH -> new Vector(0, 0, .1);
            case EAST -> new Vector(-.1, 0, 0);
            case WEST -> new Vector(.1, 0, 0);
            case ALL -> new Vector(0, 0, 0);
        };
        Vector3f scale = (dir == Direction.ALL) ? new Vector3f(1, 1, 1) : new Vector3f(.9f,.9f,.9f);
        AxisAngle4f rotation = new AxisAngle4f(0, 0, 0, 1);
        Transformation transformation = new Transformation(new Vector3f(0, 0, 0), rotation, scale, rotation);

        this.enderEye_.setTransformation(transformation);
        this.enderEye_.teleport(this.location(correction.add(ENDER_EYE_OFFSET)));
    }

    private BlockDisplay spawnBlockDisplay() {
        BlockDisplay out = (BlockDisplay) this.world_.spawnEntity(this.location(), EntityType.BLOCK_DISPLAY);
        tweakEntity(out);
        return out;
    }
    private ItemDisplay spawnItemDisplay() {
        ItemDisplay out = (ItemDisplay) this.world_.spawnEntity(this.location(), EntityType.ITEM_DISPLAY);
        tweakEntity(out);
        return out;
    }
    private Interaction spawnInteraction() {
        Interaction out = (Interaction) this.world_.spawnEntity(this.location(), EntityType.INTERACTION);
        tweakEntity(out);
        return out;
    }
    private static void kill(Entity entity) {
        if (entity == null) { return; }
        entity.remove();
    }
    private static void tweakEntity(Entity entity) {
        entity.setPersistent(false);
        entity.setInvulnerable(true);
    }
}
