package com.example.assignment4;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class SketchTaggerActivity extends AppCompatActivity {
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sketch_tagger);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = this.openOrCreateDatabase("db", Context.MODE_PRIVATE, null);
//        db.execSQL("DROP TABLE IF EXISTS DRAWINGS");
        db.execSQL("CREATE TABLE IF NOT EXISTS DRAWINGS (ID INTEGER PRIMARY KEY AUTOINCREMENT, IMAGE BLOB, TIME DATETIME DEFAULT CURRENT_TIMESTAMP, TAGS TEXT)");
        displayRecents();

        EditText setTags = findViewById(R.id.setTags);
        DrawingView drawingView = findViewById(R.id.drawingArea);
        drawingView.setTagsEditText(setTags);
    }

    public void displayRecents() {
        Cursor c;
        c = db.rawQuery("SELECT * FROM DRAWINGS ORDER BY TIME DESC", null);
        displayThumbnail(c);
    }

    public void saveDrawing(View view) {
        DrawingView dv = findViewById(R.id.drawingArea);
        Bitmap b = dv.getBitmap();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] ba = stream.toByteArray();

        EditText et = findViewById(R.id.setTags);
        String tagsInput = et.getText().toString().trim();
        String[] tagsArray = tagsInput.split("\\s*,\\s*");  // Split by comma and trim whitespace around

        // Join the array back into a single comma-separated string
        String tags = String.join(", ", tagsArray);

        ContentValues cv = new ContentValues();
        cv.put("IMAGE", ba);
        cv.put("TAGS", tags);
        db.insert("DRAWINGS", null, cv);

        clearDrawing(findViewById(R.id.drawingArea));
    }

    public void findDrawings(View view) {
        EditText et = findViewById(R.id.tagsQuery);
        String tagInput = et.getText().toString().trim();

        if (tagInput.contains(",")) {
            et.setError("Please enter only one tag");
            return;
        }

        Cursor c;
        if (tagInput.isEmpty()) {
            c = db.rawQuery("SELECT * FROM DRAWINGS ORDER BY TIME DESC", null);
        } else {
            c = db.rawQuery(
                    "SELECT * FROM DRAWINGS WHERE TAGS LIKE ? OR TAGS LIKE ? OR TAGS LIKE ? OR TAGS = ? ORDER BY TIME DESC",
                    new String[]{tagInput + ",%", "%, " + tagInput + ",%", "%, " + tagInput, tagInput}
            );
        }

        displayThumbnail(c);
    }

    public void displayThumbnail(Cursor c) {
        ArrayList<ImgItem>data = new ArrayList<>();
        ImgListAdapter adapter = new ImgListAdapter(this, R.layout.thumbnail, data);
        ListView lv = findViewById(R.id.thumbnail);
        lv.setAdapter(adapter);

        if (c.moveToFirst()) {
            for (int i = 0; i < c.getCount(); i++) {
                data.add(new ImgItem(c.getBlob(1), c.getString(3), c.getString(2)));
                c.moveToNext();
            }

            if (c.getCount() < 3) {
                int placeholderCount = 3 - c.getCount();
                for (int i = 0; i < placeholderCount; i++) {
                    data.add(new ImgItem(getPlaceholderImage(), "Unavailable", ""));
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                data.add(new ImgItem(getPlaceholderImage(), "Unavailable", ""));
            }
        }
    }

    private byte[] getPlaceholderImage() {
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder);

        int cropStartX = 0;
        int cropStartY = 0;
        int cropWidth = 117;
        int cropHeight = 78;

        Bitmap croppedPlaceholder = Bitmap.createBitmap(b, cropStartX, cropStartY, cropWidth, cropHeight);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        croppedPlaceholder.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] ba = stream.toByteArray();
        return ba;
    }

    public void clearDrawing(View view) {
        DrawingView dv = findViewById(R.id.drawingArea);
        dv.clearDrawing();
    }

    public void goBack(View view) {
        Intent intent = new Intent(SketchTaggerActivity.this, MainActivity.class);
        startActivity(intent);
    }
}