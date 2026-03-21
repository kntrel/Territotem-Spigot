package com.kntrel.mc.territotem.totem.deeds;

public sealed interface DeedsDeTranspilingError {

    record PlayerNotFound(String name) implements DeedsDeTranspilingError {}

    record UnexpectedCharacter(int index) implements DeedsDeTranspilingError {}

    record UnknownGroup(String groupName) implements DeedsDeTranspilingError {}

    record NoGroupProvided() implements DeedsDeTranspilingError {}
}
