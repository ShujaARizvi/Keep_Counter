package com.syncbros.keepcounter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ListView;
import android.support.v7.widget.Toolbar;
import android.widget.RelativeLayout;

public class SettingsActivity extends AppCompatActivity {

    private SettingsAdapter adapter;
    private ListView settingsListView;

    private boolean sounds, haptic, keepAwake, isDarkTheme;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getSettingsFromSharedPreferences();
        setTheme(isDarkTheme ? R.style.ThemeOverlay_AppCompat_Dark : R.style.ThemeOverlay_AppCompat_Light);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (keepAwake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RelativeLayout mainLayout = findViewById(R.id.main_layout);

        if (isDarkTheme){
            mainLayout.setBackgroundColor(getResources().getColor(R.color.colorBackground_Dark_Theme));
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary_Dark_Theme));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Settings");
        actionBar.setDisplayHomeAsUpEnabled(true);

        adapter = new SettingsAdapter(this, SettingsActivity.this, isDarkTheme);

        adapter.addSectionHeaderItem(new Settings("Controls"));
        adapter.addItem(new Settings(R.drawable.ic_volume, "Click Sounds", "Play sounds when clicking", "sounds", sounds));
        adapter.addItem(new Settings(R.drawable.ic_vibration, "Haptic Feedback", "Vibrate phone when clicking", "haptics", haptic));
        adapter.addItem(new Settings(R.drawable.ic_awake, "Keep Awake", "Keep the screen on while the app is running", "keep_awake", keepAwake));


        adapter.addSectionHeaderItem(new Settings("Personalization"));
        adapter.addItem(new Settings(R.drawable.ic_theme, "Theme", "Toggles dark mode", "dark_mode", isDarkTheme));

        adapter.addSectionHeaderItem(new Settings("Others"));
        adapter.addItem(new Settings(R.drawable.ic_delete, "Delete Counters", "Remove all counters and their history", "remove_counters", false));
        adapter.addItem(new Settings(R.drawable.ic_info, "App Version", getResources().getString(R.string.app_version), "app_version", false));

        settingsListView = findViewById(R.id.settings_list_view);
        settingsListView.setAdapter(adapter);

    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void restartActivity(){
        Intent intent = getIntent();
        finish();
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
        startActivity(intent);
    }

    private void getSettingsFromSharedPreferences(){
        SharedPreferences preferences = getSharedPreferences(getResources().getString(R.string.settings_preference), Context.MODE_PRIVATE);
        sounds = preferences.getBoolean("sounds", true);
        haptic = preferences.getBoolean("haptics", false);
        keepAwake = preferences.getBoolean("keep_awake", false);
        isDarkTheme = preferences.getBoolean("dark_mode", false);
    }

}
