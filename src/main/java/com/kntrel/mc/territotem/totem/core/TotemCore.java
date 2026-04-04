package com.kntrel.mc.territotem.totem.core;

import com.kntrel.util.Vec3i;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class TotemCore {

    //CONSTANTS
    private static final Map<Direction, Vector3f> TRANSLATIONS = Map.of(
            Direction.UP        , new Vector3f(.4f, 1.2f, .4f),
            Direction.DOWN      , new Vector3f(.4f, .4f, .4f),
            Direction.EAST      , new Vector3f(.9f, .8f, .4f),
            Direction.WEST      , new Vector3f(-.1f, .8f, .4f),
            Direction.NORTH     , new Vector3f(.4f, .8f, -.1f),
            Direction.SOUTH     , new Vector3f(.4f, .8f, .9f)
    );
    private static final Vector
            ZEROES              = new Vector(0, 0, 0),
            ENDER_EYE_OFFSET    = new Vector(.5, .9, .5),
            CENTER              = new Vector(.5, .5, .5),
            CENTER_BOTTOM       = new Vector(.5, 0, .5);
    private static final Display.Brightness
            ACTIVE_BRIGHTNESS   = new Display.Brightness(15, 15),
            INACTIVE_BRIGHTNESS = new Display.Brightness(8, 0);
    private static final AxisAngle4f
            NULL_ROTATION       = new AxisAngle4f(0, 0, 0, 1);
    private static final Vector3f
            NULL_TRANSLATION    = new Vector3f(0, 0, 0),
            NULL_SCALE          = new Vector3f(1, 1, 1);
    static final String METADATA_KEY = "totem_core";
    public static final Sound
            ACTIVATE_SOUND      = Sound.BLOCK_BEACON_ACTIVATE,
            DEACTIVATE_SOUND    = Sound.BLOCK_BEACON_DEACTIVATE,
            AMETHIST_SOUND      = Sound.BLOCK_AMETHYST_BLOCK_PLACE,
            ENDER_EYE_SOUND     = Sound.BLOCK_END_PORTAL_FRAME_FILL,
            GLASS_BREAK_SOUND   = Sound.BLOCK_GLASS_BREAK,
            DIRECTIONAL_PLACED_SOUND = Sound.BLOCK_IRON_BREAK,
            DIRECTIONAL_REMOVE_SOUND = Sound.BLOCK_IRON_BREAK,
            DIRECTIONAL_SWAP_SOUND   = Sound.BLOCK_SHELF_MULTI_SWAP;


    //ENUMS
    public enum State { EMPTY, AMETHIST, END_EYE, FULL, ACTIVE, INACTIVE }
    public enum Direction{
        UP, DOWN, SOUTH, NORTH, EAST, WEST, ALL;

        public boolean isOnX() { return this == EAST || this == WEST; }
        public boolean isOnY() { return this == UP || this == DOWN; }
        public boolean isOnZ() { return this == SOUTH || this == NORTH; }
    }


    //FIELDS
    private final UUID id_;
    private final Plugin plugin_;
    private final Material directionalSelectorItem_;
    private final Vec3i coordinates_;
    private final World world_;
    private BlockDisplay amethist_, directional_;
    private ItemDisplay enderEye_;
    private Interaction hitBox_;
    private State state_;
    private Direction direction_;


    TotemCore(Plugin plugin, Vec3i coordinates, World world, State state, Direction direction, Material directionalSelectorItem) {
        this.id_ = UUID.randomUUID();
        this.plugin_ = plugin;
        this.directionalSelectorItem_ = directionalSelectorItem;
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
    TotemCore(Plugin plugin, Vec3i coordinates, World world, State state, Material directionalSelectorItem) {
        this(plugin, coordinates, world,  state, Direction.ALL, directionalSelectorItem);
    }


    //GETTERS
    public UUID getRuntimeId() {
        return this.id_;
    }
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
    public Location getCenter() {
        return this.getLocation(CENTER);
    }
    public Location getLocation() {
        return this.getLocation(ZEROES);
    }


    //ACTIONS
    public void setState(@NonNull State state) {
        Objects.requireNonNull(state);
        if (state == this.state_) { return; }
        State old = this.state_;
        this.state_ = state;

        if (state == State.EMPTY) {
            this.kill();
            this.refinishBlock();
            emitStateChangeSound(old, this.state_, this.world_, this.getLocation());
            return;
        }

        if (state == State.FULL || state == State.END_EYE || state == State.ACTIVE || state == State.INACTIVE) {
            if (this.enderEye_ == null) {
                this.enderEye_ = this.spawnItemDisplay();
            }
            this.refinishEndEye();
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

        if (state == State.ACTIVE || state == State.INACTIVE) {
            if (this.hitBox_ == null) {
                this.hitBox_ = this.spawnInteraction();
            }
            this.refinishHitBox();
        } else {
            kill(this.hitBox_);
            this.hitBox_ = null;
        }

        this.refinishBlock();
        emitStateChangeSound(old, this.state_, this.world_, this.getLocation());
    }
    public void setDirection(@NonNull Direction direction) {
        Objects.requireNonNull(direction);
        if (direction == this.direction_) { return; }
        Direction old = this.direction_;
        this.direction_ = direction;

        if (direction == Direction.ALL) {
            kill(this.directional_);
            this.directional_ = null;
        } else if (this.directional_ == null) {
            this.directional_ = this.spawnBlockDisplay();
        }
        this.refinishDirectional();
        this.refinishDirectionalEnderEye();
        emitDirectionChangeSound(old, this.direction_, this.world_, this.getLocation());
    }
    public void breakDown() {
        Block b = this.world_.getBlockAt(this.coordinates_.x(), this.coordinates_.y(), this.coordinates_.z());
        b.setType(Material.AIR, false);

        if (this.state_ != State.ACTIVE) {
            this.world_.dropItemNaturally(this.getLocation(CENTER), new ItemStack(Material.TINTED_GLASS, 1));
        }
        if (this.state_ == State.END_EYE || this.state_ == State.FULL || this.state_ == State.ACTIVE) {
            this.world_.dropItemNaturally(this.getLocation(CENTER), new ItemStack(Material.ENDER_EYE, 1));
        }
        if (this.state_ != State.END_EYE) {
            this.world_.dropItemNaturally(this.getLocation(CENTER), new ItemStack(Material.AMETHYST_SHARD, 1));
        }
        if (this.state_ == State.ACTIVE && this.direction_ != Direction.ALL) {
            this.world_.dropItemNaturally(this.getLocation(CENTER), new ItemStack(this.directionalSelectorItem_, 1));
        }

        this.kill();
        emitStateChangeSound(this.state_, State.EMPTY, this.world_, this.getLocation());
    }
    public void kill() {
        kill(this.amethist_);
        kill(this.enderEye_);
        kill(this.hitBox_);
        kill(this.directional_);
        this.amethist_ = null;
        this.enderEye_ = null;
        this.hitBox_ = null;
        this.directional_ = null;
    }
    public void removeDirection() {
        if (this.direction_ == Direction.ALL) { return; }

        this.world_.dropItemNaturally(this.getLocation(CENTER), new ItemStack(this.directionalSelectorItem_, 1));
        this.setDirection(Direction.ALL);
    }



    //HELPERS
    private Location getLocation(Vector offset) {
        return new Location(
                this.world_,
                this.coordinates_.x() + offset.getX(),
                this.coordinates_.y() + offset.getY(),
                this.coordinates_.z() + offset.getZ()
        );
    }
    private void refinishAmethist() {
        if (this.amethist_ == null) { return; }

        this.amethist_.setBlock(Material.MEDIUM_AMETHYST_BUD.createBlockData());
        this.amethist_.setBrightness(ACTIVE_BRIGHTNESS);
        this.amethist_.teleport(this.getLocation());
    }
    private void refinishEndEye() {
        if (this.enderEye_ == null) { return; }

        this.enderEye_.setItemStack(new ItemStack(Material.ENDER_EYE));

        Display.Brightness brightness = ACTIVE_BRIGHTNESS;
        Vector3f scale = NULL_SCALE;
        Vector offset = ENDER_EYE_OFFSET;
        if (this.state_ == State.END_EYE || this.state_ == State.FULL) {
            offset = offset.clone().subtract(new Vector(0, .2, 0));
            scale = new Vector3f(.6f, .6f, .6f);
            this.enderEye_.setBillboard(Display.Billboard.FIXED);
        }
        if (this.state_ == State.INACTIVE) {
            offset = offset.clone().subtract(new Vector(0, .5, 0));
            brightness = INACTIVE_BRIGHTNESS;
            this.enderEye_.setBillboard(Display.Billboard.VERTICAL);
        }
        if (this.state_ == State.ACTIVE) {
            this.enderEye_.setBillboard(Display.Billboard.CENTER);
        }

        this.enderEye_.setBrightness(brightness);
        this.enderEye_.setTransformation(new Transformation(NULL_TRANSLATION, NULL_ROTATION, scale, NULL_ROTATION));
        this.enderEye_.teleport(this.getLocation(offset));
    }
    private void refinishBlock() {
        Block b = this.world_.getBlockAt(this.coordinates_.x(), this.coordinates_.y(), this.coordinates_.z());
        Material type = (this.state_ == State.ACTIVE || this.state_ == State.INACTIVE)
                ? Material.MEDIUM_AMETHYST_BUD
                : Material.TINTED_GLASS;
        b.setType(type, false);
    }
    private void refinishHitBox() {
        if (this.hitBox_ == null) { return; }

        this.hitBox_.setMetadata(METADATA_KEY, new FixedMetadataValue(this.plugin_, this.id_.toString()));
        this.hitBox_.setInteractionHeight(1.4f);
        this.hitBox_.setInteractionWidth(1.3f);
        this.hitBox_.teleport(this.getLocation(CENTER_BOTTOM));
    }
    public void refinishDirectional() {
        if (this.directional_ == null) { return; }
        if (this.direction_ == null || this.direction_ == Direction.ALL) { return; }

        this.directional_.setBlock(this.directionalSelectorItem_.createBlockData());
        this.directional_.setBrightness(ACTIVE_BRIGHTNESS);
        this.directional_.teleport(this.getLocation());

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
        Transformation transformation = new Transformation(NULL_TRANSLATION, NULL_ROTATION, scale, NULL_ROTATION);

        this.enderEye_.setTransformation(transformation);
        this.enderEye_.teleport(this.getLocation(correction.add(ENDER_EYE_OFFSET)));
    }
    private BlockDisplay spawnBlockDisplay() {
        BlockDisplay out = (BlockDisplay) this.world_.spawnEntity(this.getLocation(), EntityType.BLOCK_DISPLAY);
        tweakEntity(out);
        return out;
    }
    private ItemDisplay spawnItemDisplay() {
        ItemDisplay out = (ItemDisplay) this.world_.spawnEntity(this.getLocation(), EntityType.ITEM_DISPLAY);
        tweakEntity(out);
        return out;
    }
    private Interaction spawnInteraction() {
        Interaction out = (Interaction) this.world_.spawnEntity(this.getLocation(), EntityType.INTERACTION);
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
    private static void emitStateChangeSound(State old, State current, World world, Location location) {
        if (old == current) { return; }

        List<Sound> sounds = new ArrayList<>(4);
        if (old == State.ACTIVE) {
            sounds.add(DEACTIVATE_SOUND);
        }
        if (current == State.ACTIVE) {
            sounds.add(ACTIVATE_SOUND);
        }
        if (old != State.INACTIVE && current == State.ACTIVE) {
            sounds.add(GLASS_BREAK_SOUND);
        }
        if (current == State.END_EYE) {
            sounds.add(ENDER_EYE_SOUND);
        }
        if (current == State.FULL && old == State.AMETHIST) {
            sounds.add(ENDER_EYE_SOUND);
        }
        if (current == State.AMETHIST) {
            sounds.add(AMETHIST_SOUND);
        }
        if (current == State.FULL && old == State.END_EYE) {
            sounds.add(AMETHIST_SOUND);
        }

        for (Sound sound : sounds) {
            world.playSound(location, sound, SoundCategory.BLOCKS, 5, 1);
        }
    }
    private static void emitDirectionChangeSound(Direction old, Direction current, World world, Location location) {
        if (old == current) { return; }

        Sound sound;
        if (old == Direction.ALL) {
            sound = DIRECTIONAL_REMOVE_SOUND;
        } else if (current == Direction.ALL) {
            sound = DIRECTIONAL_PLACED_SOUND;
        } else {
            sound = DIRECTIONAL_SWAP_SOUND;
        }

        world.playSound(location, sound, 1, 1);
    }
}
