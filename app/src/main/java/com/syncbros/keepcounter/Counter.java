package com.syncbros.keepcounter;

import java.util.Comparator;

public class Counter {

    private final int DEFAULT_COLOR = R.color.cardview_light_background;

    private int counterNumber;
    private String title;
    private int defaultValue;
    private int count;
    private int incrementValue;
    private int backgroundColorResourceId = DEFAULT_COLOR;
    private boolean isPinned = false;

    public void setBackgroundColorResourceId(int backgroundColorResourceId) {
        this.backgroundColorResourceId = backgroundColorResourceId;
    }

    public Counter(String title, boolean pinned){ // Constructor for recyclerview header
        this.title = title;
        this.isPinned = pinned;

        this.counterNumber = -1;
        this.defaultValue = -1;
        this.count = -1;
        this.incrementValue = -1;
        this.backgroundColorResourceId = android.R.color.black;

    }

    public Counter(Counter counter){
        this.title = counter.title;

        this.counterNumber = counter.counterNumber;
        this.defaultValue = counter.defaultValue;
        this.count = counter.count;
        this.incrementValue = counter.incrementValue;
        this.backgroundColorResourceId = counter.backgroundColorResourceId;
        this.isPinned = counter.isPinned;
    }

    public void invertPinned() {
        isPinned = !isPinned;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public Counter(int counterNumber, String title, int defaultValue, int count, int incrementValue,
                   int backgroundColorResourceId, boolean isPinned) {
        this.counterNumber = counterNumber;
        this.title = title;
        this.defaultValue = defaultValue;
        this.count = count;
        this.incrementValue = incrementValue;
        this.backgroundColorResourceId = backgroundColorResourceId;
        this.isPinned = isPinned;
    }

    public int getCounterNumber() {
        return counterNumber;
    }

    public String getTitle() {
        return title;
    }

    public int getDefaultValue(){
        return defaultValue;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;

    }

    public int getIncrementValue() {
        return incrementValue;
    }

    public void setIncrementValue(int incrementValue) {
        this.incrementValue = incrementValue;
    }

    public int getBackgroundColorResourceId() {
        return backgroundColorResourceId;
    }


    public static Comparator<Counter> pinnedComparator = new Comparator<Counter>() {
        @Override
        public int compare(Counter o1, Counter o2) {
            Boolean o1Pinned = o1.isPinned;
            Boolean o2Pinned = o2.isPinned;
            return o2Pinned.compareTo(o1Pinned);
        }
    };

    @Override
    public String toString() {
        return "Counter{" +
                "counterNumber=" + counterNumber +
                ", title='" + title + '\'' +
                ", defaultValue=" + defaultValue +
                ", count=" + count +
                ", incrementValue=" + incrementValue +
                ", isPinned=" + isPinned +
                '}';
    }
}
