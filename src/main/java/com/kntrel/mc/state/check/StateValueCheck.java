package com.kntrel.mc.state.check;

import com.kntrel.mc.state.State;

public abstract class StateValueCheck implements StateCheck {

    //FIELDS
    private String key_;


    //CONSTRUCTORS
    public StateValueCheck(String key) {
        this.key_ = key;
    }


    //IMPLEMENTATION
    @Override public boolean test(State state) {
        State.Value val = state.get(this.key_);
        if (val == null) { return false; }
        return this.test(val);
    }


    //CONTRACT
    public abstract boolean test(State.Value value);
}
