package com.example.finalproject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
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

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SketchTaggerActivity extends AppCompatActivity {
    SQLiteDatabase db;
    private final String API_KEY = ""; // Removed for security purposes

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
        db.execSQL("CREATE TABLE IF NOT EXISTS DRAWINGS (ID INTEGER PRIMARY KEY AUTOINCREMENT, IMAGE BLOB, TIME DATETIME DEFAULT CURRENT_TIMESTAMP, TAGS TEXT, FROMDRAWINGS INT)");
        displayRecents();
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
        String[] tagsArray = tagsInput.split("\\s*,\\s*");
        String tags = String.join(", ", tagsArray);

        ContentValues cv = new ContentValues();
        cv.put("IMAGE", ba);
        cv.put("TAGS", tags);
        cv.put("FROMDRAWINGS", 1);
        db.insert("DRAWINGS", null, cv);

        clearDrawing(findViewById(R.id.drawingArea));

        displayRecents();
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
                data.add(new ImgItem(c.getBlob(1), c.getString(3), c.getString(2), c.getInt(4)));
                c.moveToNext();
            }
        }
    }

    public void clearDrawing(View view) {
        DrawingView dv = findViewById(R.id.drawingArea);
        dv.clearDrawing();
    }

    public void goBack(View view) {
        Intent intent = new Intent(SketchTaggerActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void getTags(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DrawingView dv = findViewById(R.id.drawingArea);
                    Bitmap b = dv.getBitmap();
                    visionAPI(b);
                } catch (IOException e) {
                    Log.e("vision", e.toString());
                }
            }
        }).start();
    }

    private void visionAPI(Bitmap b) throws IOException {
        //1. ENCODE image.
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image im = new Image();
        im.encodeContent(bout.toByteArray());

        //2. PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(im);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        List<Feature> lf = new ArrayList<Feature>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        //3.BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer(API_KEY));
        Vision vision = builder.build();

        //4. CALL Vision.Images.Annotate
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<AnnotateImageRequest>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        BatchAnnotateImagesResponse response = task.execute();
        Log.v("MYTAG", response.toPrettyString());

        String label = response.getResponses().get(0).getLabelAnnotations().get(0).getDescription();
        ArrayList<String> labels = new ArrayList<>();
        for (int j = 0; j < response.getResponses().get(0).getLabelAnnotations().size(); j++) {
            if (response.getResponses().get(0).getLabelAnnotations().get(j).getScore() >= 0.85) {
                labels.add(response.getResponses().get(0).getLabelAnnotations().get(j).getDescription());
            }
        }

        runOnUiThread(() -> {
            EditText et = findViewById(R.id.setTags);
            if (!labels.isEmpty()) {
                String labelsConcat = String.join(", ", labels);
                et.setText(labelsConcat);
            } else {
                et.setText(label);
            }
        });
    }
}