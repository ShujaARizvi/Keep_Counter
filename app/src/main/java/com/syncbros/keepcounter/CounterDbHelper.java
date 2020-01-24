package com.syncbros.keepcounter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CounterDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "Counters.db";


    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + CountersContract.CounterEntry.TABLE_NAME + " (" +
                    CountersContract.CounterEntry.COUNTER_NUMBER + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CountersContract.CounterEntry.TITLE + " TEXT," +
                    CountersContract.CounterEntry.DEFAULT_VALUE + " INTEGER," +
                    CountersContract.CounterEntry.CURRENT_VALUE + " INTEGER," +
                    CountersContract.CounterEntry.INCREMENT_VALUE + " INTEGER," +
                    CountersContract.CounterEntry.COLOR_RESOURCEID + " INTEGER," +
                    CountersContract.CounterEntry.PINNED + " INTEGER" + ")";

    private static final String SQL_CREATE_HISTORY_ENTRIES =
            "CREATE TABLE " + CountersContract.CounterHistory.TABLE_NAME + " (" +
                    CountersContract.CounterHistory._ID + " INTEGER PRIMARY KEY," +
                    CountersContract.CounterHistory.COUNTER_NUMBER + " INTEGER," +
                    CountersContract.CounterHistory.ACTION + " TEXT," +
                    CountersContract.CounterHistory.ACTION_DATETIME + " TEXT" + ")";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + CountersContract.CounterEntry.TABLE_NAME;

    private static final String SQL_DELETE_HISTORY_ENTRIES =
            "DROP TABLE IF EXISTS " + CountersContract.CounterHistory.TABLE_NAME;


    public CounterDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_HISTORY_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL(SQL_DELETE_ENTRIES);
//        db.execSQL(SQL_DELETE_HISTORY_ENTRIES);
//        onCreate(db);
    }
}
