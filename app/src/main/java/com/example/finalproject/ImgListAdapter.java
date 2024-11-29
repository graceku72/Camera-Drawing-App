package com.example.finalproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ImgListAdapter extends ArrayAdapter<ImgItem> {
    ImgListAdapter(Context context, int resource, ArrayList<ImgItem> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.thumbnail, parent, false);
        }
        ImgItem currentItem = getItem(position);

        ImageView thumbnailImg = convertView.findViewById(R.id.thumbnailImg);
        TextView tags = convertView.findViewById(R.id.tags);
        TextView dateTime = convertView.findViewById(R.id.dateTime);

        byte[] ba = currentItem.img;
        Bitmap b = BitmapFactory.decodeByteArray(ba, 0, ba.length);
        thumbnailImg.setImageBitmap(b);

        tags.setText(currentItem.tags);
        dateTime.setText(currentItem.dateTime);

        return convertView;
    }
}