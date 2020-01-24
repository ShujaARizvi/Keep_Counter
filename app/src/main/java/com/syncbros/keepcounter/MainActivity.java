package com.syncbros.keepcounter;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.SearchView;

import com.github.javiersantos.appupdater.AppUpdater;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.kobakei.ratethisapp.RateThisApp;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    DrawerLayout drawerLayout;
    Toolbar toolbar;
    ActionBar actionBar;

    CounterDbHelper mDbHelper;
    AudioManager audioManager;
    Vibrator vibrator;

    RecyclerView recyclerView;
    CounterAdapter counterAdapter;

    private String counterNameDialog, counterValueDialog;
    private int counterBackgroundColorResourceId;

    private boolean sounds, haptic, keepAwake, isDarkTheme;
    boolean tourComplete;

    FloatingActionButton addCounter;
    TourGuide tourGuide;

    String preferenceName;

    AdView mAdView;
    AdRequest adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        getIntent().setAction("Already created");

        preferenceName = getResources().getString(R.string.settings_preference);
        getSettingsFromSharedPreferences();
        setTheme(isDarkTheme ? R.style.ThemeOverlay_AppCompat_Dark : R.style.ThemeOverlay_AppCompat_Light);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AppUpdater appUpdater = new AppUpdater(this);
        appUpdater.start();

        // Monitor launch times and interval from installation
        RateThisApp.onCreate(this);
        // If the condition is satisfied, "Rate this app" dialog will be shown
        RateThisApp.showRateDialogIfNeeded(this);

        String adMobAppId = getResources().getString(R.string.admob_app_id);
        MobileAds.initialize(this, adMobAppId);

        mAdView = findViewById(R.id.adView);
        adRequest = new AdRequest.Builder().build();
        mAdView.setAdListener(new AdListener(){
            @Override
            public void onAdLoaded(){
                mAdView.setVisibility(View.VISIBLE);
            }
        });

        tourGuide = TourGuide.init(this).with(TourGuide.Technique.CLICK);

        mDbHelper = new CounterDbHelper(this);
        if (!tourComplete){
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            db.delete(CountersContract.CounterEntry.TABLE_NAME, "", new String[]{});
            db.delete(CountersContract.CounterHistory.TABLE_NAME, "", new String[]{});
        }else{
            mAdView.loadAd(adRequest);
        }

        if (keepAwake){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);

        actionBar = getSupportActionBar();
        actionBar.setTitle("My Counters");
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (isDarkTheme){
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white);
            drawerLayout.setBackgroundColor(getResources().getColor(R.color.colorBackground_Dark_Theme));
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary_Dark_Theme));
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()){
                    case R.id.nav_feedback:
                        sendFeedback();
                        break;
                    case R.id.nav_rate:
                        rateApp();
                        break;
                    case R.id.nav_share:
                        shareApp();
                        break;
                    case R.id.nav_settings:
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
                        break;
                }

                drawerLayout.closeDrawers();
                return true;
            }
        });


        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        recyclerView = findViewById(R.id.counter_recycler_view);

        counterAdapter = new CounterAdapter(MainActivity.this, sounds, haptic, tourComplete);
        populateCountersList();

        recyclerView.setAdapter(counterAdapter);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);


        int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
        };

        int[] colors = new int[]{
                Color.rgb(159,37,143)
        };

        addCounter = findViewById(R.id.addCounter);
        ColorStateList myList = new ColorStateList(states, colors);
        addCounter.setBackgroundTintList(myList);

        addCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog();
                tourGuide.cleanUp();
            }
        });

        if (!tourComplete){
            tourGuide.setToolTip(new ToolTip()
                    .setDescription("Click to add a new counter")
                    .setGravity(Gravity.TOP)
                    .setWidth(1000)
                    .setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark)));
            tourGuide.setOverlay(new Overlay());
            tourGuide.playOn(addCounter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showInputDialog(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Counter");

        // Main layout to contain all the other views
        final LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(80,20,80,0);

        // Set up the counter name input title
        final TextView counterNameTitle = new TextView(this);
        counterNameTitle.setText("Counter Name");
        mainLayout.addView(counterNameTitle);

        // Set up the counter name input
        final EditText counterName = new EditText(this);

        // Specify the type of input expected
        counterName.setInputType(InputType.TYPE_CLASS_TEXT);
        counterName.setMaxLines(1);

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(20);
        counterName.setFilters(filters);
        mainLayout.addView(counterName);

        // Set up the horizontal linear layout to manage inputs of initial value and increment value
        LinearLayout valueAndIncrementTitleLayout = new LinearLayout(this);
        valueAndIncrementTitleLayout.setOrientation(LinearLayout.HORIZONTAL);
        valueAndIncrementTitleLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        valueAndIncrementTitleLayout.setPadding(0,50,0,0);

        // Set up the counter initial value input title
        final TextView counterValueTitle = new TextView(this);
        counterValueTitle.setText("Initial Value");
        counterValueTitle.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        valueAndIncrementTitleLayout.addView(counterValueTitle);

        // Set up the counter increment value input title
        final TextView counterIncrementTitle = new TextView(this);
        counterIncrementTitle.setText("Increment Value");
        counterIncrementTitle.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.55f));
        counterIncrementTitle.setGravity(View.FOCUS_LEFT);
        valueAndIncrementTitleLayout.addView(counterIncrementTitle);

        mainLayout.addView(valueAndIncrementTitleLayout);

        LinearLayout valueAndIncrementValueLayout = new LinearLayout(this);
        valueAndIncrementValueLayout.setOrientation(LinearLayout.HORIZONTAL);
        valueAndIncrementValueLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        valueAndIncrementValueLayout.setPadding(0,50,0,0);

        // Set the filter for limiting number of digits allowed
        InputFilter[] FilterArrayCounterValue = new InputFilter[1];
        FilterArrayCounterValue[0] = new InputFilter.LengthFilter(8);

        // Set up the counter initial value input
        final EditText initialValue = new EditText(this);
        // Specify the type of input expected
        initialValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        initialValue.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        initialValue.setFilters(FilterArrayCounterValue);
        valueAndIncrementValueLayout.addView(initialValue);

        InputFilter[] FilterArrayIncrementValue = new InputFilter[1];
        FilterArrayIncrementValue[0] = new InputFilter.LengthFilter(5);

        // Set up the counter increment value input
        final EditText incrementValue = new EditText(this);
        // Specify the type of input expected
        incrementValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        incrementValue.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        incrementValue.setFilters(FilterArrayIncrementValue);
        valueAndIncrementValueLayout.addView(incrementValue);

        mainLayout.addView(valueAndIncrementValueLayout);

        // Specify a horizontal linear layout for pinned status checkbox
        LinearLayout pinnedLayout = new LinearLayout(this);
        pinnedLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Set up the checkbox for pinned status of the Counter
        final CheckBox pinnedCheckbox = new CheckBox(this);
        pinnedCheckbox.setChecked(false);
        pinnedLayout.addView(pinnedCheckbox);

        // Set up the text for pinned status checkbox
        TextView pinnedStatusTitle = new TextView(this);
        pinnedStatusTitle.setPadding(10,0,0,0);
        pinnedStatusTitle.setText("Pinned");
        pinnedLayout.addView(pinnedStatusTitle);

        // Setting pinnedLayout as a child of mainLayout
        mainLayout.addView(pinnedLayout);

        HorizontalScrollView scrollView = new HorizontalScrollView(this);

        LinearLayout colorSelectionLayout = new LinearLayout(this);
        colorSelectionLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorSelectionLayout.setPadding(0,20,0,0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            100,
            100
        );
        params.rightMargin = 10;
        params.bottomMargin = 10;

        final Button whiteButton = new Button(this);
        whiteButton.setLayoutParams(params);
        whiteButton.setBackground(ContextCompat.getDrawable(this, R.drawable.white_color_selected));
        counterBackgroundColorResourceId = R.color.cardview_light_background;

        final Button blueButton = new Button(this);
        blueButton.setLayoutParams(params);
        blueButton.setBackground(ContextCompat.getDrawable(this, R.drawable.blue_color_default));

        final Button redButton = new Button(this);
        redButton.setLayoutParams(params);
        redButton.setBackground(ContextCompat.getDrawable(this, R.drawable.red_color_default));

        final Button greenButton = new Button(this);
        greenButton.setLayoutParams(params);
        greenButton.setBackground(ContextCompat.getDrawable(this, R.drawable.green_color_default));

        final Button purpleButton = new Button(this);
        purpleButton.setLayoutParams(params);
        purpleButton.setBackground(ContextCompat.getDrawable(this, R.drawable.purple_color_default));

        final Button[] colorButtons = { whiteButton, blueButton, redButton, greenButton, purpleButton };
        final int[] colorButtonsDefaults = { R.drawable.white_color_default, R.drawable.blue_color_default, R.drawable.red_color_default, R.drawable.green_color_default, R.drawable.purple_color_default };

        whiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                whiteButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.white_color_selected));
                counterBackgroundColorResourceId = R.color.cardview_light_background;
            }
        });

        blueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                blueButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.blue_color_selected));
                counterBackgroundColorResourceId = R.color.card_blue;
            }
        });

        redButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                redButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.red_color_selected));
                counterBackgroundColorResourceId = R.color.card_red;
            }
        });

        greenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                greenButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.green_color_selected));
                counterBackgroundColorResourceId = R.color.card_green;
            }
        });

        purpleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                purpleButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.purple_color_selected));
                counterBackgroundColorResourceId = R.color.card_purple;
            }
        });

        // Setting all the buttons on the colorSelectionLayout
        for (Button colorButton : colorButtons){
            colorSelectionLayout.addView(colorButton);
        }

        scrollView.addView(colorSelectionLayout);

        // Setting colorSelectionLayout as a child of mainLayout
        mainLayout.addView(scrollView);

        // Setting mainLayout on the alert dialog
        builder.setView(mainLayout);

        // Set up the buttons
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                counterNameDialog = counterName.getText().toString();
                counterValueDialog = initialValue.getText().toString();
                String counterIncrementDialog = incrementValue.getText().toString();
                boolean pinned = pinnedCheckbox.isChecked();

                if (counterNameDialog.compareTo("") == 0){
                    counterNameDialog = "No Title";
                }

                if (counterValueDialog.compareTo("") == 0){
                    counterValueDialog = "0";
                }

                if (counterIncrementDialog.compareTo("") == 0){
                    counterIncrementDialog = "1";
                }

                addCounter(counterNameDialog, Integer.parseInt(counterValueDialog), Integer.parseInt(counterIncrementDialog),
                        counterBackgroundColorResourceId, pinned);



            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                if (!tourComplete) {
                    tourGuide.setToolTip(new ToolTip()
                            .setDescription("Click to add a new counter")
                            .setGravity(Gravity.TOP)
                            .setWidth(1000)
                            .setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark)));
                    tourGuide.playOn(addCounter);
                }
            }
        });

        builder.setCancelable(false);
        builder.show();

    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

    public void setColorButtonDefaults (Button[] colorButtons, int[] colorButtonDefaults){

        for (int i = 0; i < colorButtons.length; i++){
            colorButtons[i].setBackground(ContextCompat.getDrawable(this, colorButtonDefaults[i]));
        }

    }

    private void addCounter(String title, int value, int incrementValue, int background, boolean pinned){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CountersContract.CounterEntry.TITLE, title);
        values.put(CountersContract.CounterEntry.DEFAULT_VALUE, value);
        values.put(CountersContract.CounterEntry.CURRENT_VALUE, value);
        values.put(CountersContract.CounterEntry.INCREMENT_VALUE, incrementValue);
        values.put(CountersContract.CounterEntry.COLOR_RESOURCEID, background);

        int isPinned = pinned ? 1 : 0;

        values.put(CountersContract.CounterEntry.PINNED, isPinned);
        int counterNumber = (int) db.insert(CountersContract.CounterEntry.TABLE_NAME, null, values);

        db.close();

        updateCountersList(counterNumber, title, value, incrementValue, background, pinned);

    }

    private void updateCountersList(int counterNumber, String title, int value, int incrementValue, int background, boolean pinned){

        Counter counterItem = new Counter(counterNumber, title, value, value, incrementValue, background, pinned);

        if (pinned) {
            int currentPosition = 0;

            while (currentPosition < counterAdapter.getItemCount()) {
                if (counterAdapter.getItem(currentPosition).isPinned()) {
                    currentPosition++;
                } else {
                    break;
                }
            }
            counterAdapter.addItemAtPosition(currentPosition, counterItem);
            counterAdapter.changePositionOfOthers("Increment");
        }else{
            counterAdapter.addItem(counterItem);
        }

    }

    public void populateCountersList(){

        counterAdapter.addSectionHeaderItem(new Counter("PINNED", true));

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                CountersContract.CounterEntry.COUNTER_NUMBER,
                CountersContract.CounterEntry.TITLE,
                CountersContract.CounterEntry.DEFAULT_VALUE,
                CountersContract.CounterEntry.CURRENT_VALUE,
                CountersContract.CounterEntry.INCREMENT_VALUE,
                CountersContract.CounterEntry.COLOR_RESOURCEID,
                CountersContract.CounterEntry.PINNED
        };

        String selection = CountersContract.CounterEntry.PINNED + " = ?";
        String[] selectionArgs = { "1" };

        Cursor cursor = db.query(
            CountersContract.CounterEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        );

        while(cursor.moveToNext()) {

            boolean isPinned = false;

            switch (cursor.getInt(6)){
                case 0:
                    isPinned = false;
                    break;
                case 1:
                    isPinned = true;
                    break;
            }

            Counter counterItem = new Counter(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getInt(2),
                    cursor.getInt(3),
                    cursor.getInt(4),
                    cursor.getInt(5),
                    isPinned

            );
            counterAdapter.addItem(counterItem);
            Log.v("CounterItemNumber", counterItem.toString() + "");
        }
        cursor.close();

        counterAdapter.addSectionHeaderItem(new Counter("OTHERS", false));

        selectionArgs = new String[]{ "0" };

        cursor = db.query(
                CountersContract.CounterEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );


        while(cursor.moveToNext()) {

            boolean isPinned = false;

            switch (cursor.getInt(6)){
                case 0:
                    isPinned = false;
                    break;
                case 1:
                    isPinned = true;
                    break;
            }

            Counter counterItem = new Counter(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getInt(2),
                    cursor.getInt(3),
                    cursor.getInt(4),
                    cursor.getInt(5),
                    isPinned

            );
            counterAdapter.addItem(counterItem);
            Log.v("CounterItemNumber", counterItem.toString() + "");
        }
        cursor.close();
        db.close();
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
//        MenuItem menuItem = menu.findItem(R.id.action_search);
//        SearchView searchView = (SearchView) menuItem.getActionView();
//        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
//        searchView.setOnQueryTextListener(this);
//        return true;
//    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String constraint) {
        counterAdapter.getFilter().filter(constraint);
        return true;
    }

    private void getSettingsFromSharedPreferences(){
        SharedPreferences preferences = getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        sounds = preferences.getBoolean("sounds", true);
        haptic = preferences.getBoolean("haptics", false);
        keepAwake = preferences.getBoolean("keep_awake", false);
        isDarkTheme = preferences.getBoolean("dark_mode", false);
        tourComplete = preferences.getBoolean("tour_complete", false);
    }

    private void sendFeedback(){

        String feedbackGuide = "Type your feedback. Thank You!";
        String feedbackAddress = getResources().getString(R.string.feedback_address);

        Intent toEmail = new Intent(Intent.ACTION_SENDTO);
        toEmail.setData(Uri.parse("mailto:" + feedbackAddress));
        toEmail.putExtra(Intent.EXTRA_SUBJECT, "Customer feedback for " + getResources().getString(R.string.app_name));
        toEmail.putExtra(Intent.EXTRA_TEXT,feedbackGuide);

        if (toEmail.resolveActivity(getPackageManager()) != null) {
            startActivity(toEmail);
        }

    }

    private void shareApp(){

        String appLink = "Did you use this wonderful app about counters? Try out now for free.\n" + "https://play.google.com/store/apps/details?id=" + getPackageName();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, appLink);
        startActivity(Intent.createChooser(intent, "Share using"));
    }

    private void rateApp(){

        final String appPackageName = getPackageName();

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException exception) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }

    }

}
