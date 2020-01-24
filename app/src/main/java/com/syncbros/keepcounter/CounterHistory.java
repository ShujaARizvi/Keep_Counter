package com.syncbros.keepcounter;

public class CounterHistory {

    private int mCounterNumber;
    private String mActionTaken;
    private String mActionDate;

    public CounterHistory(int mCounterNumber, String mActionTaken, String actionDate) {
        this.mCounterNumber = mCounterNumber;
        this.mActionTaken = mActionTaken;

        String[] splitDate = actionDate.split(" ");
        String finalDate = splitDate[1] + " " + splitDate[2] + ", " + splitDate[5] + " " + splitDate[3];

        this.mActionDate = finalDate;
    }

    public String getActionTaken() {
        return mActionTaken;
    }

    public String getActionDate() {
        return mActionDate;
    }
}
