package com.kntrel.mc.territotem.config;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.territotem.totem.region.ExpansionTable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ExpansionTableParser {

    private ExpansionTableParser() {}

    static ExpansionTable read(ConfigNode node, String key, ExpansionTable defaultValue) {
        Object raw = node.raw(key);
        String path = node.pathOf(key);
        Logger logger = node.logger();

        if (raw == null) {
            logger.error("Missing config key '{}'. Using default expansion table.", path);
            return defaultValue;
        }
        if (!(raw instanceof List<?> rawList)) {
            logger.error("Invalid config value type for key '{}'. Using default expansion table.", path);
            return defaultValue;
        }

        List<ExpansionTable.Row> rows = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            Object element = rawList.get(i);
            String rowPath = path + "[" + i + "]";
            if (!(element instanceof Map<?, ?> rowMap)) {
                logger.error("Ignoring non-section expansion row at '{}'.", rowPath);
                continue;
            }

            ConfigNode row = ConfigNode.fromMap(rowMap, logger, rowPath);
            var item = row.requiredMaterial("item");
            var consume = row.requiredPositiveInt("consume");
            var min = row.requiredNonNegativeDouble("min");
            var max = row.requiredNonNegativeDouble("max");
            if (item == null || consume == null || min == null || max == null) {
                continue;
            }
            if (min > max) {
                logger.error(
                        "Ignoring expansion row at '{}'. 'min' ({}) must be <= 'max' ({}).",
                        rowPath,
                        min,
                        max
                );
                continue;
            }

            NBTCompound nbt = null;
            if (row.has("nbt")) {
                nbt = NbtYamlParser.compound(row.raw("nbt"), row.pathOf("nbt"), logger);
                if (nbt == null) {
                    continue;
                }
            }

            Double dropBackRate = null;
            if (row.has("drop_back_rate")) {
                dropBackRate = row.optionalUnitDouble("drop_back_rate");
                if (dropBackRate == null) {
                    continue;
                }
            }

            rows.add(new ExpansionTable.Row(item, consume, nbt, min, max, dropBackRate));
        }

        return new ExpansionTable(rows);
    }
}
