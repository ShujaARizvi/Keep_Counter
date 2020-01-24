package com.syncbros.keepcounter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class HistoryAdapter extends ArrayAdapter<CounterHistory> {

    ArrayList<CounterHistory> counterHistories;
    Context context;

    public HistoryAdapter(Activity context, ArrayList<CounterHistory> counterHistories) {
        super(context, 0, counterHistories);
        this.counterHistories = counterHistories;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.counter_history_list_item, parent, false);
        }

        CounterHistory history = counterHistories.get(position);

        TextView actionTextView = listItemView.findViewById(R.id.history_action);
        actionTextView.setText(history.getActionTaken());

        TextView dateTextView = listItemView.findViewById(R.id.history_date);
        dateTextView.setText(history.getActionDate().toString());

        return listItemView;
    }
}
