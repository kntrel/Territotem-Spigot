package com.kntrel.mc.territotem.totem;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TotemServiceListenerTest {

    @Test
    void rollDropBackCountEvaluatesEachConsumedItemIndependently() {
        double[] rolls = {0.1d, 0.6d, 0.49d, 0.5d, 0.9d};
        AtomicInteger index = new AtomicInteger();

        int refunded = TotemServiceListener.rollDropBackCount(
                5,
                0.5d,
                () -> rolls[index.getAndIncrement()]
        );

        assertEquals(3, refunded);
    }
}
