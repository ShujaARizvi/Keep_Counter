package com.syncbros.keepcounter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.TreeSet;

public class SettingsAdapter extends ArrayAdapter<Settings> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;

    private TreeSet<Integer> sectionHeader = new TreeSet<>();
    private boolean isDarkTheme;

    ArrayList<Settings> settings;

    private Activity mSettingsActivity;
    Context context;

    SharedPreferences preferences;

    public SettingsAdapter(Activity context, final Activity settingsActivity, boolean isDarkTheme) {
        super(context, 0);
        settings = new ArrayList<>();

        this.context = context;
        this.mSettingsActivity = settingsActivity;

        this.isDarkTheme = isDarkTheme;
        this.preferences = context.getSharedPreferences(context.getResources().getString(R.string.settings_preference), Context.MODE_PRIVATE);
    }

    public void addItem(final Settings item) {
        settings.add(item);
        notifyDataSetChanged();
    }

    public void addSectionHeaderItem(final Settings item) {
        settings.add(item);
        sectionHeader.add(settings.size() - 1);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return sectionHeader.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return settings.size();
    }

    @Override
    public Settings getItem(int position) {
        return settings.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;
        int rowType = getItemViewType(position);

        if (listItemView == null){
            switch (rowType){
                case TYPE_ITEM:
                    listItemView = LayoutInflater.from(getContext()).inflate(
                            R.layout.settings_item, parent, false);
                    final Settings setting = settings.get(position);

                    ImageView imageView = listItemView.findViewById(R.id.image);
                    imageView.setImageResource(setting.getmImageResourceId());

                    TextView heading = listItemView.findViewById(R.id.heading);
                    heading.setText(setting.getmHeading());
                    if (isDarkTheme){
                        heading.setTextColor(context.getResources().getColor(android.R.color.white));
                    }

                    TextView description = listItemView.findViewById(R.id.description);
                    description.setText(setting.getmDescription());

                    Switch settingToggle = listItemView.findViewById(R.id.settings_toggle);
                    settingToggle.setChecked(setting.isChecked());
                    if (setting.getmActionKey().compareTo("remove_counters") == 0){
                        settingToggle.setVisibility(View.GONE);
                        listItemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showWarningDialog();
                            }
                        });
                    } else if (setting.getmActionKey().compareTo("app_version") == 0){
                        settingToggle.setVisibility(View.GONE);
                    } else {
                        settingToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                                String action = setting.getmActionKey();

                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putBoolean(action, isChecked);
                                editor.apply();

                                switch (action){
                                    case "dark_mode":
                                        ((SettingsActivity)mSettingsActivity).restartActivity();
                                        break;
                                }
                            }
                        });
                    }
                    break;
                case TYPE_SEPARATOR:
                    listItemView = LayoutInflater.from(getContext()).inflate(
                            R.layout.settings_header, parent, false);

                    Settings settingHeader = settings.get(position);

                    TextView headerText = listItemView.findViewById(R.id.header);

                    if (settingHeader.getmHeading().compareTo("Controls") != 0){
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.topMargin = 80;
                        headerText.setLayoutParams(layoutParams);
                    }

                    headerText.setText(settingHeader.getmHeading());
                    break;
            }

        }
        return listItemView;
    }

    private void showWarningDialog(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Are you sure?");

        String message = "Are you sure you want to delete all counters?";

        final LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(80,20,80,0);

        TextView alertTextView = new TextView(context);
        alertTextView.setText(message);
        mainLayout.addView(alertTextView);

        builder.setView(mainLayout);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteCounters();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void deleteCounters(){
        CounterDbHelper dbHelper = new CounterDbHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(CountersContract.CounterEntry.TABLE_NAME, "", new String[]{});
        db.delete(CountersContract.CounterHistory.TABLE_NAME, "", new String[]{});

        Toast.makeText(context, "All counters deleted", Toast.LENGTH_SHORT).show();
    }
}
