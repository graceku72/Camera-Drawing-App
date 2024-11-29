package com.example.finalproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class StoryTellerImgListAdapter extends ArrayAdapter<ImgItem> {
    private HashMap<Integer, Boolean> checkedStates;

    StoryTellerImgListAdapter(Context context, int resource, ArrayList<ImgItem> objects) {
        super(context, resource, objects);
        this.checkedStates = new HashMap<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.story_teller_thumbnail, parent, false);
        }
        ImgItem currentItem = getItem(position);

        ImageView thumbnailImg = convertView.findViewById(R.id.thumbnailImg);
        TextView tags = convertView.findViewById(R.id.tags);
        TextView dateTime = convertView.findViewById(R.id.dateTime);
        CheckBox checkbox = convertView.findViewById(R.id.itemCheckbox);

        byte[] ba = currentItem.img;
        Bitmap b = BitmapFactory.decodeByteArray(ba, 0, ba.length);
        thumbnailImg.setImageBitmap(b);

        tags.setText(currentItem.tags);
        dateTime.setText(currentItem.dateTime);

        if ("Unavailable".equals(currentItem.tags)) {
            checkbox.setVisibility(View.GONE);
        } else {
            checkbox.setVisibility(View.VISIBLE);
        }

        checkbox.setOnCheckedChangeListener(null);
        checkbox.setChecked(checkedStates.getOrDefault(position, false));
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> checkedStates.put(position, isChecked));

        return convertView;
    }
}