package com.syncbros.keepcounter;

public class Settings {

    private int mImageResourceId;
    private String mHeading, mDescription;
    private String mActionKey;
    public boolean isChecked;

    public Settings(String heading){
        this.mHeading = heading;
        mDescription = "";
        mActionKey = "";
        mImageResourceId = -1;
        isChecked = false;
    }

    public Settings(int mImageResourceId, String mHeading, String mDescription, String mActionKey, boolean isChecked) {
        this.mImageResourceId = mImageResourceId;
        this.mHeading = mHeading;
        this.mDescription = mDescription;
        this.mActionKey = mActionKey;
        this.isChecked = isChecked;
    }

    public int getmImageResourceId() {
        return mImageResourceId;
    }

    public String getmHeading() {
        return mHeading;
    }

    public String getmDescription() {
        return mDescription;
    }

    public String getmActionKey() {
        return mActionKey;
    }

    public boolean isChecked(){
        return isChecked;
    }

}
