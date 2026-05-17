package com.kntrel.mc.territotem.geyser.block;

import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;
import org.geysermc.geyser.api.block.custom.property.PropertyType;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockTotemCore implements CustomBlockData {

    //CONSTANTS
    public static final String NAME = "totem_core";
    public static final String IDENTIFIER = "geyser_custom:" + NAME;
    public static final String STATE_PROPERTY = "territotem:state";
    public static final String DIRECTION_PROPERTY = "territotem:direction";

    private static final String GEOMETRY = "geometry.totem_core";
    private static final List<String> STATES = List.of(
            "amethyst",
            "eye",
            "complete",
            "active",
            "inactive"
    );
    private static final List<String> DIRECTIONS = List.of(
            "all",
            "north",
            "south",
            "east",
            "west",
            "up",
            "down"
    );
    private static final List<String> DIRECTIONAL_EYE_BONES = List.of(
            "eye_north",
            "eye_south",
            "eye_east",
            "eye_west",
            "eye_up",
            "eye_down"
    );


    //FIELDS
    private final CustomBlockComponents components;
    private final Map<String, CustomBlockProperty<?>> properties;
    private final List<CustomBlockPermutation> permutations;
    private final CustomBlockState defaultBlockState;


    //CONSTRUCTORS
    public BedrockTotemCore() {
        this.components = buildComponents();
        this.properties = buildProperties();
        this.permutations = buildPermutations();
        this.defaultBlockState = this.blockStateBuilder()
                .stringProperty(STATE_PROPERTY, STATES.getFirst())
                .stringProperty(DIRECTION_PROPERTY, DIRECTIONS.getFirst())
                .build();
    }


    //IMPLEMENTATION
    @Override public @NonNull String name() { return NAME; }
    @Override public @NonNull String identifier() { return IDENTIFIER; }
    @Override public boolean includedInCreativeInventory() { return false; }
    @Override public @Nullable CreativeCategory creativeCategory() { return null; }
    @Override public @Nullable String creativeGroup() { return null; }
    @Override public CustomBlockComponents components() { return this.components; }
    @Override public @NonNull Map<String, CustomBlockProperty<?>> properties() { return this.properties; }
    @Override public @NonNull List<CustomBlockPermutation> permutations() { return this.permutations; }
    @Override public @NonNull CustomBlockState defaultBlockState() { return this.defaultBlockState; }
    @Override public CustomBlockState.@NonNull Builder blockStateBuilder() { return new TotemCoreBlockState.Builder(this); }

    //HELPERS
    private static CustomBlockComponents buildComponents() {
        return CustomBlockComponents.builder()
                .displayName("Totem Core")
                .geometry(geometry(Map.of()))
                .materialInstance("*", material("tinted_glass", "blend"))
                .materialInstance("totem_core_amethyst", material("large_amethyst_bud", "alpha_test"))
                .materialInstance("totem_core_glass", material("tinted_glass", "blend"))
                .materialInstance("totem_core_eye_item", material("ender_eye", "alpha_test"))
                .materialInstance("totem_core_eye", material("ender_eye", "opaque"))
                .materialInstance("totem_core_directonal", material("amethyst_block", "opaque"))
                .build();
    }
    private static Map<String, CustomBlockProperty<?>> buildProperties() {
        Map<String, CustomBlockProperty<?>> properties = new LinkedHashMap<>();
        properties.put(STATE_PROPERTY, new StringProperty(STATE_PROPERTY, STATES));
        properties.put(DIRECTION_PROPERTY, new StringProperty(DIRECTION_PROPERTY, DIRECTIONS));
        return Collections.unmodifiableMap(properties);
    }
    private static List<CustomBlockPermutation> buildPermutations() {
        List<CustomBlockPermutation> permutations = new ArrayList<>();

        permutations.add(permutation(
                stateCondition("amethyst"),
                hiddenBones(
                        "eye_inactive",
                        "eye",
                        "eye_north",
                        "eye_south",
                        "eye_east",
                        "eye_west",
                        "eye_up",
                        "eye_down"
                )
        ));
        permutations.add(permutation(
                stateCondition("eye"),
                hiddenBones(
                        "amethyst",
                        "eye",
                        "eye_north",
                        "eye_south",
                        "eye_east",
                        "eye_west",
                        "eye_up",
                        "eye_down"
                )
        ));
        permutations.add(permutation(
                stateCondition("complete"),
                hiddenBones(
                        "eye",
                        "eye_north",
                        "eye_south",
                        "eye_east",
                        "eye_west",
                        "eye_up",
                        "eye_down"
                )
        ));
        permutations.add(permutation(
                stateCondition("inactive"),
                hiddenBones(
                        "glass",
                        "eye",
                        "eye_north",
                        "eye_south",
                        "eye_east",
                        "eye_west",
                        "eye_up",
                        "eye_down"
                )
        ));

        for (String direction : DIRECTIONS) {
            permutations.add(permutation(
                    stateAndDirectionCondition("active", direction),
                    hiddenBones(activeHiddenBones(direction))
            ));
        }

        return List.copyOf(permutations);
    }
    private static CustomBlockPermutation permutation(String condition, Map<String, String> hiddenBones) {
        CustomBlockComponents components = CustomBlockComponents.builder()
                .geometry(geometry(hiddenBones))
                .build();
        return new CustomBlockPermutation(components, condition);
    }
    private static GeometryComponent geometry(Map<String, String> boneVisibility) {
        GeometryComponent.Builder builder = GeometryComponent.builder()
                .identifier(GEOMETRY);
        if (!boneVisibility.isEmpty()) {
            builder.boneVisibility(boneVisibility);
        }
        return builder.build();
    }
    private static MaterialInstance material(String texture, String renderMethod) {
        return MaterialInstance.builder()
                .texture(texture)
                .renderMethod(renderMethod)
                .faceDimming(true)
                .ambientOcclusion(true)
                .build();
    }
    private static String stateCondition(String state) {
        return "query.block_property('" + STATE_PROPERTY + "') == '" + state + "'";
    }
    private static String stateAndDirectionCondition(String state, String direction) {
        return stateCondition(state) + " && query.block_property('" + DIRECTION_PROPERTY + "') == '" + direction + "'";
    }
    private static Map<String, String> hiddenBones(String... bones) {
        return hiddenBones(Arrays.asList(bones));
    }
    private static Map<String, String> hiddenBones(List<String> bones) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String bone : bones) {
            out.put(bone, "false");
        }
        return Collections.unmodifiableMap(out);
    }
    private static List<String> activeHiddenBones(String direction) {
        List<String> hidden = new ArrayList<>();
        hidden.add("glass");
        hidden.add("eye_inactive");

        if (!"all".equals(direction)) {
            hidden.add("eye");
        }

        for (String directionalBone : DIRECTIONAL_EYE_BONES) {
            if (!directionalBone.equals("eye_" + direction)) {
                hidden.add(directionalBone);
            }
        }

        return hidden;
    }
    private record StringProperty(String name, List<String> values) implements CustomBlockProperty<String> {

        private StringProperty {
            values = List.copyOf(values);
        }

        @Override
        public PropertyType type() {
            return PropertyType.stringProp();
        }
    }


    //SUBTYPES
    private record TotemCoreBlockState(BedrockTotemCore block, Map<String, Object> properties) implements CustomBlockState {

        private TotemCoreBlockState {
            properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        }

        @Override
        public @NonNull String name() {
            return this.block.name();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T property(@NonNull String propertyName) {
            return (T) this.properties.get(propertyName);
        }

        private static final class Builder implements CustomBlockState.Builder {

            private final BedrockTotemCore block;
            private final Map<String, Object> properties = new LinkedHashMap<>();

            private Builder(BedrockTotemCore block) {
                this.block = block;
            }

            @Override
            public @NonNull Builder booleanProperty(String propertyName, boolean value) {
                this.properties.put(propertyName, value);
                return this;
            }

            @Override
            public @NonNull Builder intProperty(String propertyName, int value) {
                this.properties.put(propertyName, value);
                return this;
            }

            @Override
            public @NonNull Builder stringProperty(String propertyName, String value) {
                this.properties.put(propertyName, value);
                return this;
            }

            @Override
            public @NonNull TotemCoreBlockState build() {
                for (String propertyName : this.block.properties().keySet()) {
                    if (!this.properties.containsKey(propertyName)) {
                        throw new IllegalArgumentException("Missing property: " + propertyName);
                    }
                }

                for (Map.Entry<String, Object> entry : this.properties.entrySet()) {
                    CustomBlockProperty<?> property = this.block.properties().get(entry.getKey());
                    if (property == null) {
                        throw new IllegalArgumentException("Unknown property: " + entry.getKey());
                    }
                    if (!property.values().contains(entry.getValue())) {
                        throw new IllegalArgumentException("Invalid value " + entry.getValue() + " for property " + entry.getKey());
                    }
                }

                return new TotemCoreBlockState(this.block, this.properties);
            }
        }
    }
}
