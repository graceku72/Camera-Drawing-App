package com.example.assignment4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

public class DrawingView extends View {
    Path path = new Path();
    Bitmap bmp;
    private final String API_KEY = "";
    // API_KEY value removed for security purposes, and was only temporarily available
    private EditText setTagsEditText;

    public DrawingView(Context context) {
        super(context);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5f);

        canvas.drawPath(path, p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            path.moveTo(x, y);
        } else if (action == MotionEvent.ACTION_MOVE) {
            path.lineTo(x, y);
        }
        invalidate();

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

        return true;
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

        post(() -> {
            if (setTagsEditText != null) {
                if (!labels.isEmpty()) {
                    String labelsConcat = String.join(", ", labels);
                    setTagsEditText.setText(labelsConcat);
                } else {
                    setTagsEditText.setText(label);
                }
            }
        });

    }

    public void setTagsEditText(EditText editText) {
        this.setTagsEditText = editText;
    }

    public Bitmap getBitmap() {
        bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.WHITE);
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setStrokeWidth(5f);
        c.drawPath(path, p);
        return bmp;
    }

    public void clearDrawing() {
        path.reset();
        invalidate();
    }
}
