package com.artbating.solly;

import com.squareup.otto.Bus;

/**
 * Created by kim on 2017. 3. 21..
 */

public final class BusProvider {
    private static final CustomBus BUS = new CustomBus();

    public static CustomBus getInstance() {
        return BUS;
    }

    private BusProvider() {
        // No instances.
    }
}
