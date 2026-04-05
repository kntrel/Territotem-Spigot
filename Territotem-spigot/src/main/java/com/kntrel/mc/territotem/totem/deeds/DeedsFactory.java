package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.runical.bukkit.ComponentMarkupCompiler;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.runical.core.argument.Argument;
import com.kntrel.mc.territotem.region.RegionArgument;
import com.kntrel.mc.territotem.totem.Totem;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jspecify.annotations.NonNull;
import java.util.Arrays;
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
    private final Translator translator_;


    //CONSTRUCTOR
    public DeedsFactory(RegionContext regionContext, Translator translator) {
        this.regionContext_ = regionContext;
        this.nameSpace_ = this.regionContext_.getNamespace();
        this.transpiler_ = new DeedsTranspiler(this.regionContext_.getServer(), translator);
        this.deedsNsk_ = new NamespacedKey(this.regionContext_.getPlugin(), DEEDS_KEY);
        this.translator_ = translator;

        this.transpiler_.setPageSizeLines(PAGE_LINE_COUNT);
        this.transpiler_.setPrologueTranslationKey("prologue");
    }


    //API
    public Deeds generate(@NonNull Player player, @NonNull Totem totem) {
        ItemStack deedsStack = new ItemStack(Material.WRITABLE_BOOK, 1);
        if (!(deedsStack.getItemMeta() instanceof BookMeta bookMeta)) {
            throw new IllegalStateException("Writable books must produce book metadata");
        }

        Region region = totem.region();
        List<Permission> perms = region.getPermissions();

        DeedsPersistentData data = new DeedsPersistentData(
                this.nameSpace_,
                region.getId(),
                totem.incrementAndGetDeedsVersion()
        );
        PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
        pdc.set(this.deedsNsk_, DeedsPersistentDataType.instance(), data);
        Deeds deeds = new Deeds(region, bookMeta, perms, data.version());

        String[] rawPages = this.transpiler_.transpile(player, deeds);
        List<BaseComponent[]> pages = Arrays.stream(rawPages)
                .map(ComponentMarkupCompiler::compile)
                .map(c -> new BaseComponent[]{c})
                .toList();
        bookMeta.spigot().setPages(pages);

        Argument[] arguments = new Argument[] {
                RegionArgument.of(region),
                Argument.of("playerName", player.getName()),
                Argument.of("version", deeds.version())
        };

        String name = this.translator_.translate(player, "item.name").arguments(arguments).orNull().message();
        String rawLore = this.translator_.translate(player, "item.lore").arguments(arguments).orNull().message();

        if (name != null) {
            bookMeta.setItemName(name);
        }
        if (rawLore != null) {
            bookMeta.setLore(Arrays.stream(rawLore.split("\n")).toList());
        }
        bookMeta.setEnchantmentGlintOverride(true);

        return new Deeds(region, bookMeta, perms, data.version());
    }
    public DeedsInterpretationResult interpret(@NonNull BookMeta bookMeta) {

        PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
        if (!pdc.has(this.deedsNsk_)) {
            return DeedsInterpretationResult.notADeedsBook(bookMeta);
        }

        DeedsPersistentData data = pdc.get(this.deedsNsk_, DeedsPersistentDataType.instance());
        if (data == null) {
            return DeedsInterpretationResult.notADeedsBook(bookMeta);
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
            int zeroBasedLineNumber = e.getLineNumber() - 1;
            int page = zeroBasedLineNumber / PAGE_LINE_COUNT + 1;
            int line = zeroBasedLineNumber % PAGE_LINE_COUNT + 1;
            return new DeedsInterpretationResult.DeTranspileError(page, line, e.getLine(), e.getErrorCause(), region);
        }

        return DeedsInterpretationResult.success(new Deeds(region, bookMeta, perms, data.version()));
    }
}
