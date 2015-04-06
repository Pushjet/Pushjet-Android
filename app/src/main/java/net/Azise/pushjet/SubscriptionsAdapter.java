package net.Azise.pushjet;


import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.Azise.pushjet.PushjetApi.PushjetService;

import java.util.ArrayList;
import java.util.Collections;

public class SubscriptionsAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private ArrayList<PushjetService> entries = new ArrayList<PushjetService>();

    public SubscriptionsAdapter(Context context) {
        this.context = context;
        this.layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int i) {
        return entries.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout itemView;
        if (convertView == null) {
            itemView = (RelativeLayout) layoutInflater.inflate(
                    R.layout.fragment_servicelist, parent, false
            );
        } else {
            itemView = (RelativeLayout) convertView;
        }

        TextView titleText = (TextView) itemView.findViewById(R.id.service_name);
        TextView tokenText = (TextView) itemView.findViewById(R.id.service_token);
        ImageView iconImage = (ImageView) itemView.findViewById(R.id.service_icon_image);

        String title = entries.get(position).getName();
        String token = entries.get(position).getToken();
        Bitmap icon = entries.get(position).getIconBitmapOrDefault(context);

        titleText.setText(title);
        tokenText.setText(token);
        iconImage.setImageBitmap(icon);

        return itemView;
    }

    public void addEntries(ArrayList<PushjetService> entries) {
        Collections.reverse(entries);
        for (PushjetService entry : entries)
            this.entries.add(0, entry);
        notifyDataSetChanged();
    }

    public void addEntry(PushjetService entry) {
        this.entries.add(0, entry);
        notifyDataSetChanged();
    }

    public void upDateEntries(ArrayList<PushjetService> entries) {
        Collections.reverse(entries);
        this.entries = entries;
        notifyDataSetChanged();
    }
}
