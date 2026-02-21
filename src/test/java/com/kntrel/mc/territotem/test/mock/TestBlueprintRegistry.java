package com.kntrel.mc.territotem.test.mock;

import com.kntrel.mc.territotem.blueprint.BlueprintRegistry;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class TestBlueprintRegistry extends BlueprintRegistry {

    public TestBlueprintRegistry() {
        super(null);
    }

    @Override
    protected <T> CompletableFuture<T> runInMainThreadAsync(Callable<T> task) {
        try {
            return CompletableFuture.completedFuture(task.call());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
