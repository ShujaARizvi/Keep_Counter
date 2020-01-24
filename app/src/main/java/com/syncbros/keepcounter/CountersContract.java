package com.syncbros.keepcounter;

import android.provider.BaseColumns;

public final class CountersContract {

    private CountersContract(){}

    public static class CounterEntry implements BaseColumns {
        public static final String TABLE_NAME = "counter";

        public static final String COUNTER_NUMBER = "counter_number";
        public static final String TITLE = "title";
        public static final String DEFAULT_VALUE = "default_value";
        public static final String CURRENT_VALUE = "current_value";
        public static final String INCREMENT_VALUE = "increment_value";
        public static final String COLOR_RESOURCEID = "color_resource_id";
        public static final String PINNED = "pinned";

    }

    public static class CounterHistory implements BaseColumns {
        public static final String TABLE_NAME = "counter_history";

        public static final String COUNTER_NUMBER = "counter_number";
        public static final String ACTION = "action";
        public static final String ACTION_DATETIME = "action_datetime";
    }
}
