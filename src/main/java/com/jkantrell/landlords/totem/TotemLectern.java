package com.jkantrell.landlords.totem;

import com.jkantrell.regionslib.regions.Permission;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.landlords.Landlords;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TotemLectern implements TotemRelative, Cloneable {

    //FIELDS
    private final Vector pos_;
    private BlockFace facing_;
    private final Blueprint blueprint_;
    private Totem totem_;

    //STATIC FIELDS
    private static final NamespacedKey totemIdKey_ = new NamespacedKey(Landlords.getMainInstance(),"totemId");

    //STATIC METHODS
    public static boolean isTotemLectern(Block block) {
        if (!(block.getState() instanceof Lectern lectern)) { return false; }
        PersistentDataContainer dataContainer = lectern.getPersistentDataContainer();
        if (!dataContainer.has(totemIdKey_,PersistentDataType.STRING)) { return false; }
        return Totem.exists(dataContainer.get(totemIdKey_,PersistentDataType.STRING));
    }
    public static TotemLectern getAt(Block block) {
        TotemLectern r = null;
        for (Totem t : Totem.getAll()){
            r = t.getLecternAt(block);
            if (r != null) { break; }
        }
        return r;
    }

    //CONSTRUCTORS
    public TotemLectern(Vector pos, BlockFace facing, Blueprint blueprint) {
        this.setFacing(facing);
        this.pos_ = pos;
        this.blueprint_ = blueprint;
    }
    public TotemLectern(int x, int y, int z, BlockFace facing, Blueprint blueprint){
        this(new Vector(x,y,z), facing, blueprint);
    }

    //SETTERS
    void setFacing(BlockFace face) {
        facing_= face;
    }
    void setTotem(Totem totem) {
        this.totem_ = totem;
    }

    //GETTERS
    public Vector getPosition() {
        return this.pos_.clone();
    }
    public BlockFace getFacing() {
        return facing_;
    }
    public Blueprint getStructure() {
        return this.blueprint_;
    }
    public Totem getTotem() {
        return this.totem_;
    }

    //METHODS
    public void convert(Block block) {
        if (!(block.getState() instanceof Lectern lectern)) { return; }
        PersistentDataContainer data = lectern.getPersistentDataContainer();
        data.set(totemIdKey_, PersistentDataType.STRING, this.getTotem().getUniqueId().toString());
        lectern.update();
    }
    public Deeds.ReadingResults readDeeds(ItemStack itemStack, Player player) {
        Optional<Deeds> deeds = Deeds.fromBook(itemStack,player);
        if (deeds.isEmpty()) { return new Deeds.ReadingResults("", Collections.emptyList(),Collections.emptyList()); }
        return this.readDeeds(deeds.get(), player);
    }
    public Deeds.ReadingResults readDeeds(Deeds deeds, Player player) {

        Region deedsRegion = deeds.getRegion(), lecternRegion = this.getTotem().getRegion().orElseThrow();
        if (!deedsRegion.equals(lecternRegion)) {
            throw new IllegalArgumentException(Landlords.getLangProvider().getEntry(player,"deeds.error_message.place.region_mismatch",deedsRegion.getName(),lecternRegion.getName()));
        }
        Integer deedsMinutes = deeds.getMinutes(), regionMinutes = Deeds.idOf(lecternRegion).orElse(-1);
        if (!deedsMinutes.equals(regionMinutes)) {
            throw new IllegalArgumentException(Landlords.getLangProvider().getEntry(player,"deeds.error_message.place.id_mismatch",deedsMinutes.toString(),regionMinutes.toString()));
        }

        Deeds.ReadingResults read = deeds.read();
        if(!read.errors().isEmpty()){
            throw new unreadableDeedsException(Landlords.getLangProvider().getEntry(player, "deeds.error_message.place.reading_errors"),read.errors());
        }

        if (read.name() != null) {
            lecternRegion.setName(read.name());
        }
        lecternRegion.setPermissions(read.permissions().toArray(new Permission[0]));
        lecternRegion.save();

        return read;
    }
    @Override
    public TotemLectern clone() {
        try {
            TotemLectern clone = (TotemLectern) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    //EXCEPTIONS
    public class unreadableDeedsException extends RuntimeException {

        public final List<String> errors;

        public unreadableDeedsException(List<String> errors) {
            super();
            this.errors = errors;
        }

        public unreadableDeedsException(String message, List<String> errors) {
            super(message);
            this.errors = errors;
        }
    }
}
