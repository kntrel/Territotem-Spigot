package com.kntrel.mc.territotem.config.blueprint;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.state.StateMap;
import com.kntrel.mc.territotem.config.NbtYamlParser;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import com.kntrel.mc.territotem.totem.piece.TotemPieceDirection;
import com.kntrel.mc.territotem.totem.piece.TotemPieces;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.slf4j.Logger;
import java.util.Locale;
import java.util.Map;

final class TotemPieceResolver {

    private final TotemCoreTracker coreTracker_;
    private final Logger logger_;

    TotemPieceResolver(TotemCoreTracker coreTracker, Logger logger) {
        this.coreTracker_ = coreTracker;
        this.logger_ = logger;
    }

    Piece resolve(Map<String, Object> rawEntry, String path) {
        Object pieceName = rawEntry.get("piece");
        Object blockName = rawEntry.get("block");

        if (pieceName != null && blockName != null) {
            throw new BlueprintValidationException(
                    "Palette entry '" + path + "' cannot define both 'piece' and 'block'."
            );
        }
        if (pieceName == null && blockName == null) {
            throw new BlueprintValidationException(
                    "Palette entry '" + path + "' must define either 'piece' or 'block'."
            );
        }

        if (pieceName != null) {
            return this.resolveCustomPiece(pieceName, rawEntry, path);
        }
        return this.resolveBlockPiece(blockName, rawEntry, path);
    }

    private Piece resolveCustomPiece(Object rawPieceName, Map<String, Object> rawEntry, String path) {
        if (!(rawPieceName instanceof String text) || text.isBlank()) {
            throw new BlueprintValidationException(
                    "Palette entry '" + path + ".piece' must be a non-empty string."
            );
        }

        String pieceName = normalizePieceName(text);
        return switch (pieceName) {
            case "core" -> TotemPieces.core(this.coreTracker_);
            case "name_sign" -> TotemPieces.sign(parseDirection(rawEntry.get("direction"), path + ".direction"));
            case "deeds_lectern", "lectern" -> TotemPieces.lectern(parseDirection(rawEntry.get("direction"), path + ".direction"));
            case "empty" -> Piece.empty();
            case "any" -> Piece.any();
            default -> throw new BlueprintValidationException(
                    "Unknown custom piece '" + text + "' at '" + path + ".piece'."
            );
        };
    }

    private Piece resolveBlockPiece(Object rawBlockName, Map<String, Object> rawEntry, String path) {
        if (!(rawBlockName instanceof String text) || text.isBlank()) {
            throw new BlueprintValidationException(
                    "Palette entry '" + path + ".block' must be a non-empty string."
            );
        }

        Material material;
        try {
            material = Material.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BlueprintValidationException(
                    "Invalid block material '" + text + "' at '" + path + ".block'.",
                    ex
            );
        }

        StateMap state = new StateMap();
        NBTCompound nbt = null;

        for (Map.Entry<String, Object> entry : rawEntry.entrySet()) {
            String key = entry.getKey();
            if ("block".equals(key) || "piece".equals(key)) {
                continue;
            }
            if ("nbt".equals(key)) {
                nbt = NbtYamlParser.compound(entry.getValue(), path + ".nbt", this.logger_);
                if (nbt == null) {
                    throw new BlueprintValidationException(
                            "Invalid block NBT at '" + path + ".nbt'."
                    );
                }
                continue;
            }

            state.put(
                    key.trim().toLowerCase(Locale.ROOT),
                    scalarToStateValue(entry.getValue(), path + "." + key)
            );
        }

        if (Bukkit.getServer() != null) {
            try {
                state.createBlockData(material);
            } catch (RuntimeException ex) {
                throw new BlueprintValidationException(
                        "Invalid block state combination for material '" + material + "' at '" + path + "'.",
                        ex
                );
            }
        }

        if (nbt != null) {
            return Piece.block(material, state, nbt);
        }
        if (state.isEmpty()) {
            return Piece.block(material);
        }
        return Piece.block(material, state);
    }

    private static TotemPieceDirection parseDirection(Object rawDirection, String path) {
        if (!(rawDirection instanceof String text) || text.isBlank()) {
            throw new BlueprintValidationException("Missing direction at '" + path + "'.");
        }
        try {
            return TotemPieceDirection.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BlueprintValidationException(
                    "Invalid direction '" + text + "' at '" + path + "'.",
                    ex
            );
        }
    }

    private static String normalizePieceName(String raw) {
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static String scalarToStateValue(Object raw, String path) {
        if (raw == null) {
            throw new BlueprintValidationException(
                    "Block state '" + path + "' cannot be null."
            );
        }
        if (raw instanceof Map<?, ?> || raw instanceof Iterable<?>) {
            throw new BlueprintValidationException(
                    "Block state '" + path + "' must be a scalar value."
            );
        }
        return raw.toString();
    }
}
