package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.territotem.totem.Totem;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jspecify.annotations.NonNull;
import java.util.List;

public class DeedsFactory {

    //CONSTANTS
    private static final String DEEDS_KEY = "deeds";
    private static final int PAGE_LINE_COUNT = 13;      //Minecraft books have 14 lines per page. Using 13 to leave a 1 line buffer.


    //FIELDS
    private final RegionContext regionContext_;
    private final String nameSpace_;
    private final DeedsTranspiler transpiler_;
    private final NamespacedKey deedsNsk_;


    //CONSTRUCTOR
    public DeedsFactory(RegionContext regionContext) {
        this.regionContext_ = regionContext;
        this.nameSpace_ = this.regionContext_.getNamespace();
        this.transpiler_ = new DeedsTranspiler(this.regionContext_.getServer());
        this.deedsNsk_ = new NamespacedKey(this.regionContext_.getPlugin(), DEEDS_KEY);
    }


    //API
    public Deeds generate(ItemStack item, Totem totem) {
        if (!(item.getItemMeta() instanceof BookMeta bookMeta)) {
            throw new IllegalArgumentException("ItemStack must be a book");
        }

        Region region = totem.region();
        List<Permission> perms = region.getPermissions();
        String[] pages = this.transpiler_.transpile(perms, PAGE_LINE_COUNT);

        bookMeta.setPages(pages);
        bookMeta.setEnchantmentGlintOverride(true);
        item.setItemMeta(bookMeta);

        DeedsPersistentData data = new DeedsPersistentData(
                this.nameSpace_,
                region.getId(),
                totem.incrementAndGetDeedsVersion()
        );
        PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
        pdc.set(this.deedsNsk_, DeedsPersistentDataType.instance(), data);

        return new Deeds(region, bookMeta, item, perms);
    }
    public DeedsInterpretationResult interpret(@NonNull ItemStack item) {
        if (!(item.getItemMeta() instanceof BookMeta bookMeta)){
            return DeedsInterpretationResult.notADeedsBook(item);
        }

        PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
        if (!pdc.has(this.deedsNsk_)) {
            return DeedsInterpretationResult.notADeedsBook(item);
        }

        DeedsPersistentData data = pdc.get(this.deedsNsk_, DeedsPersistentDataType.instance());
        if (data == null) {
            return DeedsInterpretationResult.notADeedsBook(item);
        }
        if (!data.nameSpace().equals(this.nameSpace_)) {
            return DeedsInterpretationResult.wrongNameSpace(data.nameSpace());
        }

        Region region = this.regionContext_.get(data.regionId()).orElse(null);
        if (region == null) {
            return DeedsInterpretationResult.nonExistentRegion(data.regionId());
        }

        List<Permission> perms;
        try {
            perms = this.transpiler_.deTranspile(region, bookMeta.getPages());
        } catch (DeedsDeTranspilingException e) {
            int lNum = e.getLineNumber(),
                line = lNum % PAGE_LINE_COUNT,
                page = lNum / PAGE_LINE_COUNT;
            return new DeedsInterpretationResult.DeTranspileError(page, line, e.getLine(), e.getErrorCause());
        }

        return DeedsInterpretationResult.success(new Deeds(region, bookMeta, item, perms));
    }
}
