package io.Pushjet.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.Pushjet.api.PushjetApi.PushjetMessage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class PushListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mLayoutInflater;
    private ArrayList<PushjetMessage> entries = new ArrayList<PushjetMessage>();
    private DateFormat df;
    private int selected = -1;

    public PushListAdapter(Context context) {
        this.context = context;
        this.mLayoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.df = new SimpleDateFormat("d MMM HH:mm"); // 7 jul 15:30
    }

    @Override
    public int getCount() {
        return this.entries.size();
    }

    @Override
    public Object getItem(int position) {
        return this.entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout itemView;
        if (convertView == null) {
            itemView = (RelativeLayout) mLayoutInflater.inflate(
                    R.layout.fragment_pushlist, parent, false
            );
        } else {
            itemView = (RelativeLayout) convertView;
        }

        TextView dateText = (TextView) itemView.findViewById(R.id.push_date);
        TextView titleText = (TextView) itemView.findViewById(R.id.push_title);
        TextView descriptionText = (TextView) itemView.findViewById(R.id.push_description);
        ImageView iconImage = (ImageView) itemView.findViewById(R.id.push_icon_image);

        String title = entries.get(position).getTitle();
        if (title.equals(""))
            title = entries.get(position).getService().getName();
        String description = entries.get(position).getMessage();
        Date pushDate = entries.get(position).getTimestamp();
        Bitmap icon = entries.get(position).getService().getIconBitmapOrDefault(context);

        dateText.setText(this.df.format(pushDate));
        titleText.setText(title);
        descriptionText.setText(description);
        iconImage.setImageBitmap(icon);

        TypedValue value = new TypedValue();
        DisplayMetrics metrics = new DisplayMetrics();

        context.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
        ((WindowManager) (context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getMetrics(metrics);

        int lineCount = selected == position ? descriptionText.getLineCount() : 0;
        int minHeight = (int) TypedValue.complexToDimension(value.data, metrics);
        int prefHeight = (lineCount + 2) * descriptionText.getLineHeight();
        itemView.getLayoutParams().height = prefHeight > minHeight ? prefHeight : minHeight;

        return itemView;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public void clearSelected() {
        setSelected(-1);
    }

    public void addEntries(ArrayList<PushjetMessage> entries) {
        Collections.reverse(entries);
        for (PushjetMessage entry : entries)
            this.entries.add(0, entry);
        notifyDataSetChanged();
    }

    public void addEntry(PushjetMessage entry) {
        this.entries.add(0, entry);
        notifyDataSetChanged();
    }

    public void upDateEntries(ArrayList<PushjetMessage> entries) {
        Collections.reverse(entries);
        this.entries = entries;
        notifyDataSetChanged();
    }
}