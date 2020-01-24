package com.syncbros.keepcounter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    ArrayList<CounterHistory> histories;

    ListView counterHistoryListView;
    HistoryAdapter historyAdapter;

    CounterDbHelper mDbHelper;

    boolean isDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getSettingsFromSharedPreferences();
        setTheme(isDarkTheme ? R.style.ThemeOverlay_AppCompat_Dark : R.style.ThemeOverlay_AppCompat_Light);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Counter History");
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (isDarkTheme){
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary_Dark_Theme));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        Intent intent = getIntent();

        int counterNumber = intent.getIntExtra("counter_number", -1);

        mDbHelper = new CounterDbHelper(this);

        counterHistoryListView = findViewById(R.id.counter_history_list_view);

        populateHistoryList(counterNumber);

        historyAdapter = new HistoryAdapter(this, histories);

        counterHistoryListView.setAdapter(historyAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void populateHistoryList(int counterNumber){

        histories = new ArrayList<>();

        if (counterNumber == -1){
            return;
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                CountersContract.CounterHistory.COUNTER_NUMBER,
                CountersContract.CounterHistory.ACTION,
                CountersContract.CounterHistory.ACTION_DATETIME,
        };

        String selection = CountersContract.CounterHistory.COUNTER_NUMBER + " = ?";
        String[] selectionArgs = { Integer.toString(counterNumber) };

        String sortOrder =
                CountersContract.CounterHistory.ACTION_DATETIME + " DESC";

        Cursor cursor = db.query(
                CountersContract.CounterHistory.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );

        while(cursor.moveToNext()) {

            CounterHistory historyItem = new CounterHistory(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            );
            this.histories.add(historyItem);
            Log.v("CounterHistoryItem", historyItem.toString() + "");
        }
        cursor.close();
        db.close();

    }

    private void getSettingsFromSharedPreferences(){
        SharedPreferences preferences = getSharedPreferences(getResources().getString(R.string.settings_preference), Context.MODE_PRIVATE);
        isDarkTheme = preferences.getBoolean("dark_mode", false);
    }
}
