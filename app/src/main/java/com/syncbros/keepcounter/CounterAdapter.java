package com.syncbros.keepcounter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.NoSuchElementException;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

public class CounterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final int TYPE_SEPARATOR = 0;
    private static final int TYPE_ITEM = 1;

    private int positionOfOthers;

    private Activity mMainActivity;
    private Context context;
    private ArrayList<Counter> counterList;
    private CounterFilter mCounterFilter;
    private TourGuide tourGuide;

    private boolean sounds, haptics, tourComplete;
    private int counterBackgroundColorResourceId;

    public CounterAdapter(final Activity mainActivity, boolean sounds, boolean haptics, boolean tourComplete) {
        this.counterList = new ArrayList<>();
        this.mMainActivity = mainActivity;
        this.sounds = sounds;
        this.haptics = haptics;
        this.tourComplete = tourComplete;
    }

    public void addItem(Counter item) {
        counterList.add(item);
        notifyDataSetChanged();
    }

    public void addItemAtPosition(int position, Counter item){
        counterList.add(position, item);
        notifyDataSetChanged();
    }

    public void addSectionHeaderItem(Counter item) {
        counterList.add(item);
        if (item.getTitle().compareTo("OTHERS") == 0){
            positionOfOthers = counterList.size() - 1;
        }
        notifyDataSetChanged();
    }

    public Counter getItem(int position){
        return counterList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 || position == positionOfOthers) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void changePositionOfOthers(String action){
        switch (action) {
            case "Increment":
                positionOfOthers++;
                break;
            case "Decrement":
                positionOfOthers--;
                break;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Log.v("ViewTypes", viewType + "");

        View itemView;

        if (viewType == TYPE_SEPARATOR){
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.counters_header, parent, false);

            return new MyViewHolderHeader(itemView);

        }else if (viewType == TYPE_ITEM){

            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_counter_item, parent, false);

            return new MyViewHolder(itemView);
        }else{
            throw new NoSuchElementException();
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder currentHolder, final int position) {

        if (currentHolder instanceof MyViewHolderHeader){
            final Counter counter = counterList.get(position);
            MyViewHolderHeader holder = (MyViewHolderHeader) currentHolder;

            String title = counter.getTitle();

            holder.header.setText(title);

        } else if (currentHolder instanceof MyViewHolder) {

            final Counter counter = counterList.get(position);
            final MyViewHolder holder = (MyViewHolder) currentHolder;

            holder.count.setText(Integer.toString(counter.getCount()));
            holder.count.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCountValueInputDialog(counter, holder);
                }
            });

            holder.title.setText(counter.getTitle());

            holder.title.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {

                    if (hasFocus){
                        if (!tourComplete){
                            tourGuide.cleanUp();
                            tourGuide.setToolTip(new ToolTip()
                                    .setDescription("Press enter key to update the title and change focus")
                                    .setGravity(Gravity.BOTTOM)
                                    .setWidth(1000)
                                    .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                            tourGuide.playOn(holder.title);
                        }
                    }else{
                        if (!tourComplete){
                            tourGuide.cleanUp();
                            tourGuide.setToolTip(new ToolTip()
                                    .setDescription("Click to edit the value for this counter")
                                    .setGravity(Gravity.BOTTOM)
                                    .setWidth(1000)
                                    .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                            tourGuide.playOn(holder.count);
                        }
                        counter.setTitle(holder.title.getText().toString());
                        updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Title", counter.getTitle());
                        createHistoryEntry(counter.getCounterNumber(), "Counter title updated to '" + counter.getTitle() + "'");
                    }
                }
            });

            holder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PopupMenu menu = new PopupMenu(context, holder.menu);
                    menu.inflate(R.menu.card_menu);
                    MenuItem pinnedItem = menu.getMenu().findItem(R.id.menu_pinned);
                    pinnedItem.setTitle(counter.isPinned() ? "Unpin" : "Pin");

                    menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            if (!tourComplete){
                                tourGuide.cleanUp();
                                tourGuide.setToolTip(new ToolTip()
                                        .setDescription("Click to increment the counter value")
                                        .setGravity(Gravity.BOTTOM)
                                        .setWidth(1000)
                                        .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                                tourGuide.playOn(holder.increment);
                            }
                        }
                    });
                    if (tourComplete){
                        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                switch (item.getItemId()) {
                                    case R.id.menu_pinned:
                                        counter.invertPinned();
                                        String pinned = counter.isPinned() ? "1" : "0";
                                        updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Pinned", pinned);
                                        if (counter.isPinned()) {
                                            createHistoryEntry(counter.getCounterNumber(), "Counter pinned");
                                            holder.pinnedLayout.setVisibility(View.VISIBLE);
                                            pinner(position);
                                        } else {
                                            createHistoryEntry(counter.getCounterNumber(), "Counter un-pinned");
                                            holder.pinnedLayout.setVisibility(View.GONE);
                                            unPinner(position);
                                        }
                                        break;
                                    case R.id.menu_share:
                                        shareCounter(counter);
                                        break;
                                    case R.id.menu_color:
                                        showColorSelectionDialog(counter, holder);
                                        break;
                                    case R.id.menu_history:
                                        Intent intent = new Intent(context, HistoryActivity.class);
                                        intent.putExtra("counter_number", counter.getCounterNumber());
                                        context.startActivity(intent);
                                        break;
                                }

                                return false;
                                }
                         });
                    }

                    try {
                        Field[] fields = menu.getClass().getDeclaredFields();
                        for (Field field : fields) {
                            if ("mPopup".equals(field.getName())) {
                                field.setAccessible(true);
                                Object menuPopupHelper = field.get(menu);
                                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                                Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                                setForceIcons.invoke(menuPopupHelper, true);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.v("PopupError", e.toString());
                    } finally {
                        menu.show();
                    }

                }
            });

            holder.decrement.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!tourComplete){
//                        holder.title.requestFocus();
                        tourGuide.cleanUp();
                        tourGuide.setToolTip(new ToolTip()
                                .setDescription("Click to edit the title for this counter")
                                .setGravity(Gravity.END)
                                .setWidth(1000)
                                .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                        tourGuide.playOn(holder.title);
                    }

                    int value = Integer.parseInt(holder.count.getText().toString());

                    if (value - counter.getIncrementValue() >= 0) {
                        value -= counter.getIncrementValue();

                        if (sounds) {
                            ((MainActivity) mMainActivity).audioManager.playSoundEffect(0, 1.0f);
                        }
                        if (haptics) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ((MainActivity) mMainActivity).vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                //deprecated in API 26
                                ((MainActivity) mMainActivity).vibrator.vibrate(100);
                            }
                        }
                        counter.setCount(value);
                        holder.count.setText(Integer.toString(value));
                        updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Counter", Integer.toString(value));
                        createHistoryEntry(counter.getCounterNumber(), "Decremented counter value by " + counter.getIncrementValue());
                    }

                }
            });

            holder.increment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!tourComplete){
                        tourGuide.cleanUp();
                        tourGuide.setToolTip(new ToolTip()
                                .setDescription("Click to decrement the counter value")
                                .setGravity(Gravity.BOTTOM)
                                .setWidth(1000)
                                .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                        tourGuide.playOn(holder.decrement);
                    }

                    int value = Integer.parseInt(holder.count.getText().toString());

                    if (value + counter.getIncrementValue() <= 99999999) {
                        value += counter.getIncrementValue();
                        if (sounds) {
                            ((MainActivity) mMainActivity).audioManager.playSoundEffect(0, 1.0f);
                        }
                        if (haptics) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ((MainActivity) mMainActivity).vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                //deprecated in API 26
                                ((MainActivity) mMainActivity).vibrator.vibrate(100);
                            }
                        }
                        counter.setCount(value);
                        holder.count.setText(Integer.toString(value));
                        updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Counter", Integer.toString(value));
                        createHistoryEntry(counter.getCounterNumber(), "Incremented counter value by " + counter.getIncrementValue());
                    }


                }
            });

            holder.reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showWarningDialog(counter, holder, position, "Reset");
                }
            });
            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!tourComplete) {
                        tourGuide.cleanUp();
                        SharedPreferences preferences = context.getSharedPreferences(context.getResources().getString(R.string.settings_preference), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("tour_complete", true);
                        editor.apply();
                        tourComplete = true;
                        ((MainActivity)mMainActivity).tourComplete = true;
                        ((MainActivity)mMainActivity).mAdView.loadAd(((MainActivity)mMainActivity).adRequest);
                    }
                    showWarningDialog(counter, holder, position, "Delete");
                }
            });
            holder.pinnedLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    counter.invertPinned();
                    updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Pinned", "0");
                    createHistoryEntry(counter.getCounterNumber(), "Counter un-pinned");
                    holder.pinnedLayout.setVisibility(View.GONE);
                    unPinner(position);
                }
            });

            if (!tourComplete){
                tourGuide = TourGuide.init(mMainActivity).with(TourGuide.Technique.CLICK);
                tourGuide.setToolTip(new ToolTip()
                        .setDescription("Click to reset the value to default for this counter")
                        .setGravity(Gravity.END)
                        .setWidth(1000)
                        .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                tourGuide.setOverlay(new Overlay());
                tourGuide.playOn(holder.reset);
            }

            holder.mainLayout.setBackgroundColor(context.getResources().getColor(counter.getBackgroundColorResourceId()));
            if (counter.isPinned()) {
                holder.pinnedLayout.setVisibility(View.VISIBLE);
            } else {
                holder.pinnedLayout.setVisibility(View.GONE);
            }
        }
    }

    public void showColorSelectionDialog(final Counter counter, final MyViewHolder holder){

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose color");

        // Main layout to contain all the other views
        final LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(80,20,80,0);

        HorizontalScrollView scrollView = new HorizontalScrollView(context);

        LinearLayout colorSelectionLayout = new LinearLayout(context);
        colorSelectionLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorSelectionLayout.setPadding(0,20,0,0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                100,
                100
        );
        params.rightMargin = 10;
        params.bottomMargin = 10;

        final Button whiteButton = new Button(context);
        whiteButton.setLayoutParams(params);
        whiteButton.setBackground(ContextCompat.getDrawable(context, R.drawable.white_color_selected));
        counterBackgroundColorResourceId = R.color.cardview_light_background;

        final Button blueButton = new Button(context);
        blueButton.setLayoutParams(params);
        blueButton.setBackground(ContextCompat.getDrawable(context, R.drawable.blue_color_default));

        final Button redButton = new Button(context);
        redButton.setLayoutParams(params);
        redButton.setBackground(ContextCompat.getDrawable(context, R.drawable.red_color_default));

        final Button greenButton = new Button(context);
        greenButton.setLayoutParams(params);
        greenButton.setBackground(ContextCompat.getDrawable(context, R.drawable.green_color_default));

        final Button purpleButton = new Button(context);
        purpleButton.setLayoutParams(params);
        purpleButton.setBackground(ContextCompat.getDrawable(context, R.drawable.purple_color_default));

        final Button[] colorButtons = { whiteButton, blueButton, redButton, greenButton, purpleButton };
        final int[] colorButtonsDefaults = { R.drawable.white_color_default, R.drawable.blue_color_default, R.drawable.red_color_default, R.drawable.green_color_default, R.drawable.purple_color_default };

        whiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                whiteButton.setBackground(ContextCompat.getDrawable(context, R.drawable.white_color_selected));
                counterBackgroundColorResourceId = R.color.cardview_light_background;
            }
        });

        blueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                blueButton.setBackground(ContextCompat.getDrawable(context, R.drawable.blue_color_selected));
                counterBackgroundColorResourceId = R.color.card_blue;
            }
        });

        redButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                redButton.setBackground(ContextCompat.getDrawable(context, R.drawable.red_color_selected));
                counterBackgroundColorResourceId = R.color.card_red;
            }
        });

        greenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                greenButton.setBackground(ContextCompat.getDrawable(context, R.drawable.green_color_selected));
                counterBackgroundColorResourceId = R.color.card_green;
            }
        });

        purpleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorButtonDefaults(colorButtons, colorButtonsDefaults);
                purpleButton.setBackground(ContextCompat.getDrawable(context, R.drawable.purple_color_selected));
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
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // call database update function here
                changeCounterColor(counter, holder, counterBackgroundColorResourceId);


            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private void setColorButtonDefaults (Button[] colorButtons, int[] colorButtonDefaults){

        for (int i = 0; i < colorButtons.length; i++){
            colorButtons[i].setBackground(ContextCompat.getDrawable(context, colorButtonDefaults[i]));
        }

    }

    private void changeCounterColor(Counter counter, MyViewHolder holder, int colorResource){

        counter.setBackgroundColorResourceId(colorResource);
        holder.mainLayout.setBackgroundColor(colorResource);

        updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Color", Integer.toString(colorResource));
        createHistoryEntry(counter.getCounterNumber(), "Counter background color updated");

        this.notifyDataSetChanged();
    }

    private void removeCounter(int counterNumber) {

        SQLiteDatabase db = ((MainActivity) mMainActivity).mDbHelper.getWritableDatabase();
        String selection = CountersContract.CounterEntry.COUNTER_NUMBER + " = ?";
        String[] selectionArgs = {Integer.toString(counterNumber)};
        db.delete(CountersContract.CounterEntry.TABLE_NAME, selection, selectionArgs);
        db.delete(CountersContract.CounterHistory.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    @Override
    public int getItemCount() {
        return counterList.size();
    }

    public class MyViewHolderHeader extends RecyclerView.ViewHolder {

        public TextView header;

        public MyViewHolderHeader(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        CardView mainLayout;
        LinearLayout pinnedLayout;
        public EditText title;
        public TextView count;
        public ImageView reset, menu, delete;
        public ImageButton increment, decrement;


        public MyViewHolder(View view) {
            super(view);
            context = view.getContext();
            mainLayout = view.findViewById(R.id.main_layout);
            pinnedLayout = view.findViewById(R.id.pinned);
            title = view.findViewById(R.id.title);
            count = view.findViewById(R.id.counter);
            reset = view.findViewById(R.id.reset_counter);
            menu = view.findViewById(R.id.menu);
            delete = view.findViewById(R.id.delete);
            increment = view.findViewById(R.id.increment_counter);
            decrement = view.findViewById(R.id.decrement_counter);

        }
    }
    private void pinner(int positionFrom){
        int currentPosition = positionFrom;

        Counter temp;

        while (currentPosition - 1 >= 0){
            if (!counterList.get(currentPosition - 1).isPinned()){
                temp = new Counter(counterList.get(currentPosition - 1));
                counterList.set(currentPosition - 1, counterList.get(currentPosition));
                counterList.set(currentPosition, temp);
            }else{
                break;
            }
            currentPosition--;
        }
        this.changePositionOfOthers("Increment");

        Log.v("PositionOfCounterFrom", positionFrom+"");
        Log.v("PositionOfCounterTo", currentPosition+"");
        this.notifyDataSetChanged();
//        this.notifyItemMoved(positionFrom, currentPosition);
//        this.notifyItemMoved(positionFrom, currentPosition);

    }
    private void unPinner(int positionFrom){
        int currentPosition = positionFrom;

        Counter temp;

        while (currentPosition + 1 < counterList.size()){
            if (counterList.get(currentPosition + 1).isPinned()){
                temp = new Counter(counterList.get(currentPosition + 1));
                counterList.set(currentPosition + 1, counterList.get(currentPosition));
                counterList.set(currentPosition, temp);
            }else{
                break;
            }
            currentPosition++;
        }

        temp = new Counter(counterList.get(currentPosition + 1));
        counterList.set(currentPosition + 1, counterList.get(currentPosition));
        counterList.set(currentPosition, temp);

        this.changePositionOfOthers("Decrement");

        Log.v("PositionOfCounterFrom", positionFrom+"");
        Log.v("PositionOfCounterTo", currentPosition+"");
        this.notifyDataSetChanged();
//        this.notifyItemMoved(positionFrom, currentPosition);
    }

    private void updateDatabaseTuple(String counterNumber, String column, String columnValue) {
        SQLiteDatabase db = ((MainActivity) mMainActivity).mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        switch (column){
            case "Title":
                values.put(CountersContract.CounterEntry.TITLE, columnValue);
                break;
            case "Counter":
                values.put(CountersContract.CounterEntry.CURRENT_VALUE, columnValue);
                break;
            case "Increment":
                values.put(CountersContract.CounterEntry.INCREMENT_VALUE, columnValue);
                break;
            case "Pinned":
                boolean pinned = columnValue.compareTo("1") == 0;
                values.put(CountersContract.CounterEntry.PINNED, pinned);
                break;
            case "Color":
                values.put(CountersContract.CounterEntry.COLOR_RESOURCEID, columnValue);
                break;
        }

        String selection = CountersContract.CounterEntry.COUNTER_NUMBER + " = ?";
        String[] selectionArgs = {counterNumber};

        int count = db.update(
                CountersContract.CounterEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        db.close();

    }

    private void createHistoryEntry(int counterNumber, String action){
        SQLiteDatabase db = ((MainActivity) mMainActivity).mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(CountersContract.CounterHistory.COUNTER_NUMBER, counterNumber);
        values.put(CountersContract.CounterHistory.ACTION, action);
        values.put(CountersContract.CounterHistory.ACTION_DATETIME, (new Date()).toString());

        long tupleID = db.insert(CountersContract.CounterHistory.TABLE_NAME, null, values);

        db.close();
    }

    private void showWarningDialog(final Counter counter, final MyViewHolder holder, final int position, final String method){

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Are you sure?");

        String message = "Are you sure you want to " + method.toLowerCase() + " this counter?";

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
                if (method.compareTo("Reset") == 0){
                    resetCounter(counter, holder, position);
                }else if (method.compareTo("Delete") == 0){
                    deleteCounter(counter, position);
                }
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!tourComplete){
                    tourGuide.cleanUp();
                    tourGuide.setToolTip(new ToolTip()
                            .setDescription("Click to explore more options for this counter")
                            .setGravity(Gravity.START)
                            .setWidth(1000)
                            .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                    tourGuide.playOn(holder.menu);
                }
            }
        });

        builder.show();
    }

    private void showCountValueInputDialog(final Counter counter, final MyViewHolder holder){

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Set counter value");

        final LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(80,20,80,0);

        TextView inputTitle = new TextView(context);
        inputTitle.setText("Counter Value");
        mainLayout.addView(inputTitle);

        final EditText inputEditText = new EditText(context);
        inputEditText.setText(Integer.toString(counter.getCount()));
        inputEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        InputFilter[] FilterArrayCounterValue = new InputFilter[1];
        FilterArrayCounterValue[0] = new InputFilter.LengthFilter(8);
        inputEditText.setFilters(FilterArrayCounterValue);

        mainLayout.addView(inputEditText);

        TextView inputTitleIncrementValue = new TextView(context);
        inputTitleIncrementValue.setText("Increment Value");
        mainLayout.addView(inputTitleIncrementValue);

        final EditText inputEditTextIncrementValue = new EditText(context);
        inputEditTextIncrementValue.setText(Integer.toString(counter.getIncrementValue()));
        inputEditTextIncrementValue.setInputType(InputType.TYPE_CLASS_NUMBER);

        InputFilter[] filterArrayIncrementValue = new InputFilter[1];
        filterArrayIncrementValue[0] = new InputFilter.LengthFilter(5);
        inputEditTextIncrementValue.setFilters(filterArrayIncrementValue);
        mainLayout.addView(inputEditTextIncrementValue);

        builder.setView(mainLayout);

        builder.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newValue = inputEditText.getText().toString();
                String newValueIncrement = inputEditTextIncrementValue.getText().toString();

                if (newValue.compareTo("") != 0 && Integer.parseInt(newValue) != counter.getCount()){
                    counter.setCount(Integer.parseInt(newValue));
                    holder.count.setText(newValue);
                    updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Counter", newValue);
                    createHistoryEntry(counter.getCounterNumber(), "Counter value set to " + newValue);
                }
                if (newValueIncrement.compareTo("") != 0 && Integer.parseInt(newValueIncrement) != counter.getIncrementValue()){
                    counter.setIncrementValue(Integer.parseInt(newValueIncrement));
                    updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Increment", newValueIncrement);
                    createHistoryEntry(counter.getCounterNumber(), "Increment value updated to " + newValueIncrement);
                }

            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!tourComplete){
                    tourGuide.cleanUp();
                    tourGuide.setToolTip(new ToolTip()
                            .setDescription("Click to delete this counter")
                            .setGravity(Gravity.END)
                            .setWidth(1000)
                            .setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark)));
                    tourGuide.playOn(holder.delete);
                }
            }
        });

        builder.show();
    }

    private void resetCounter(Counter counter, MyViewHolder holder, int position){
        String defaultValue = Integer.toString(counter.getDefaultValue());
        counter.setCount(counter.getDefaultValue());
        holder.count.setText(defaultValue);

        updateDatabaseTuple(Integer.toString(counter.getCounterNumber()), "Counter", defaultValue);
        createHistoryEntry(counter.getCounterNumber(), "Reset counter to default value: " + counter.getDefaultValue());

//        this.notifyItemChanged(position);
    }

    private void deleteCounter(final Counter counter, final int position){

        final Counter backupCounter = new Counter(counter);
        counterList.remove(counter);
        notifyDataSetChanged();
//        notifyItemRemoved(position);

        if (counter.isPinned()){
            this.changePositionOfOthers("Decrement");
        }

        final Snackbar snackbar = Snackbar.make(((MainActivity)mMainActivity).drawerLayout, "Counter deleted", Snackbar.LENGTH_INDEFINITE);

        final Handler handler = new Handler();
        final Runnable timer = new Runnable() {
            @Override
            public void run() {
                //Do something after 3s
                removeCounter(counter.getCounterNumber());
                snackbar.dismiss();
            }
        };
        handler.postDelayed(timer, 3000);

        snackbar.setAction("UNDO", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterList.add(position, backupCounter);
                // notifyItemInserted(position);
                if (backupCounter.isPinned()) {
                    changePositionOfOthers("Increment");
                }
                notifyDataSetChanged();
                handler.removeCallbacks(timer);
                Snackbar.make(((MainActivity)mMainActivity).drawerLayout, "Undo successful", Snackbar.LENGTH_SHORT).show();
            }
        });
        snackbar.show();

    }

    @NonNull
    @Override
    public Filter getFilter(){
        if (mCounterFilter == null){
            mCounterFilter = new CounterFilter();
        }
        return mCounterFilter;
    }

    private class CounterFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            ArrayList<Counter> counters = new ArrayList<>(populateCountersList());

//            counterList.clear();
//            ((MainActivity)mMainActivity).populateCountersList();

            ArrayList<Counter> filteredCounters = new ArrayList<>();

            if (constraint == null || constraint.length() == 0){
                filteredCounters.addAll(counters);
            }else{
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Counter counter : counters){
                    if (counter.getTitle().toLowerCase().contains(filterPattern) || counter.getIncrementValue() == -1){
                        filteredCounters.add(new Counter(counter));
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredCounters;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            counterList = (ArrayList<Counter>) results.values;
            notifyDataSetChanged();
        }
    }

    private ArrayList<Counter> populateCountersList(){

        ArrayList<Counter> counters = new ArrayList<>();

//        addSectionHeaderItem(new Counter("PINNED", true));
        Counter item = new Counter("PINNED", true);
        counters.add(item);

        SQLiteDatabase db = ((MainActivity)mMainActivity).mDbHelper.getReadableDatabase();

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
            counters.add(counterItem);
//            addItem(counterItem);
            Log.v("CounterItemNumber", counterItem.toString() + "");
        }
        cursor.close();

        item = new Counter("OTHERS", false);
        counters.add(item);
        positionOfOthers = counters.size() - 1;

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
            counters.add(counterItem);
            Log.v("CounterItemNumber", counterItem.toString() + "");
        }
        cursor.close();
        db.close();

        return counters;
    }

    private void shareCounter(Counter counter){

        String counterInfo = String.format("Information about my counter\n" +
                                            "Title: %s\n" +
                                            "Value: %d\n" +
                                            "Increment: %d\n\n" +
                                            "Shared using Keep Counter", counter.getTitle(), counter.getCount(), counter.getIncrementValue());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, counterInfo);
        context.startActivity(Intent.createChooser(intent, "Share using"));
    }
}
