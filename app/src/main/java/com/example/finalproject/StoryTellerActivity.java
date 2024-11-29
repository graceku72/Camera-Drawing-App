package com.example.finalproject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StoryTellerActivity extends AppCompatActivity {
    SQLiteDatabase db;
    String url = "https://api.textcortex.com/v1/texts/social-media-posts";
    String API_KEY = ""; // Removed for security purposes
    private Map<CheckBox, List<String>> checkBoxTagMap = new HashMap<>();
    int checkboxCount = 0;
    boolean displayMsg = false;
    TextToSpeech tts = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_story_teller);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = this.openOrCreateDatabase("db", Context.MODE_PRIVATE, null);
        displayRecents();

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }

    public void displayRecents() {
        Cursor c;
        c = db.rawQuery("SELECT * FROM IMAGES UNION SELECT * FROM DRAWINGS ORDER BY TIME DESC", null);
        displayThumbnail(c);
    }

    public void goBack(View view) {
        if (tts != null) {
            tts.stop();
        }
        Intent intent = new Intent(StoryTellerActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void findImages(View view) {
        EditText et = findViewById(R.id.tagsQuery);
        String tagsInput = et.getText().toString().trim();
        Cursor c;
        CheckBox sketchesCheckbox = findViewById(R.id.sketchesCheckbox);

        if (tagsInput.isEmpty()) {
            if (sketchesCheckbox.isChecked()) {
                c = db.rawQuery("SELECT * FROM IMAGES UNION SELECT * FROM DRAWINGS ORDER BY TIME DESC", null);
            } else {
                c = db.rawQuery("SELECT * FROM IMAGES ORDER BY TIME DESC", null);
            }
        } else {
            String query;
            String[] queryArgs;

            if (tagsInput.contains(",")) {
                String[] tags = tagsInput.split("\\s*,\\s*");
                StringBuilder queryBuilder = new StringBuilder();
                ArrayList<String> args = new ArrayList<>();

                if (sketchesCheckbox.isChecked()) {
                    queryBuilder.append("SELECT * FROM IMAGES WHERE ");
                    appendTagsQuery(tags, queryBuilder, args);
                    queryBuilder.append(" UNION SELECT * FROM DRAWINGS WHERE ");
                    appendTagsQuery(tags, queryBuilder, args);
                } else {
                    queryBuilder.append("SELECT * FROM IMAGES WHERE ");
                    appendTagsQuery(tags, queryBuilder, args);
                }

                queryBuilder.append(" ORDER BY TIME DESC");
                query = queryBuilder.toString();
                queryArgs = args.toArray(new String[0]);
                c = db.rawQuery(queryBuilder.toString(), queryArgs);
            } else {
                String tag = "%" + tagsInput + "%";
                if (sketchesCheckbox.isChecked()) {
                    query = "SELECT * FROM IMAGES WHERE TAGS LIKE ? UNION SELECT * FROM DRAWINGS WHERE TAGS LIKE ? ORDER BY TIME DESC";
                    queryArgs = new String[]{tag, tag};
                } else {
                    query = "SELECT * FROM IMAGES WHERE TAGS LIKE ? ORDER BY TIME DESC";
                    queryArgs = new String[]{tag};
                }
                c = db.rawQuery(query, queryArgs);
            }
        }
        displayThumbnail(c);
    }

    private void appendTagsQuery(String[] tags, StringBuilder queryBuilder, ArrayList<String> args) {
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) {
                queryBuilder.append(" OR ");
            }
            queryBuilder.append("TAGS LIKE ?");
            args.add("%" + tags[i] + "%");
        }
    }

    public void toggleCheckbox(View view) {
        findImages(view);
        checkBoxTagMap.clear();
        TextView selectionText = findViewById(R.id.selectionText);
        selectionText.setText("");
        displayMsg = false;
        checkboxCount = 0;
    }

    public void displayThumbnail(Cursor c) {
        ArrayList<ImgItem> data = new ArrayList<>();
        StoryTellerImgListAdapter adapter = new StoryTellerImgListAdapter(this, R.layout.story_teller_thumbnail, data);
        ListView lv = findViewById(R.id.thumbnail);
        lv.setAdapter(adapter);

        if (c.moveToFirst()) {
            for (int i = 0; i < c.getCount(); i++) {
                data.add(new ImgItem(c.getBlob(1), c.getString(3), c.getString(2), c.getInt(4)));
                c.moveToNext();
            }
        }
    }

    public void getStory(View view) {
        if (checkBoxTagMap.isEmpty()) {
            Toast.makeText(this, "No images are selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            makeHttpRequest();
        } catch (JSONException e) {
            Log.e("error", e.toString());
        }
    }

    void makeHttpRequest() throws JSONException {
        JSONObject data = new JSONObject();

        String context = "context";
        data.put("context", context);

        data.put("max_tokens", "10");
        data.put("mode", "twitter");
        data.put("model", "claude-3-haiku");

        Set<String> keywordsSet = new HashSet<>();
        for (List<String> checkboxTags : checkBoxTagMap.values()) {
            keywordsSet.addAll(checkboxTags);
        }
        String[] keywordsArray = keywordsSet.toArray(new String[0]);
        data.put("keywords", new JSONArray(keywordsArray));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Log.d("success", response.toString());
                TextView story = findViewById(R.id.story);
                String responseText = "";
                try {
                    responseText = response.getJSONObject("data")
                            .getJSONArray("outputs")
                            .getJSONObject(0)
                            .getString("text");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                story.setText(responseText);

                if (tts != null) {
                    tts.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", new String(error.networkResponse.data));
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + API_KEY);
                return headers;
            }
        };

        RequestQueue requestQueue  = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    public void toggleItemCheckbox(View view) {
        CheckBox checkBox = (CheckBox) view;
        TextView tagsTV = (TextView) ((View) view.getParent()).findViewById(R.id.tags);
        String tags = tagsTV.getText().toString();
        String[] tagsSplit = tags.split("\\s*,\\s*");
        List<String> tagList = Arrays.asList(tagsSplit);

        if (checkBox.isChecked()) {
            if (checkboxCount >= 3) {
                checkBox.setChecked(false);
                Toast.makeText(this, "Select up to 3 items only", Toast.LENGTH_SHORT).show();
                return;
            }

            checkBoxTagMap.put(checkBox, tagList);
            checkboxCount++;
        } else {
            checkBoxTagMap.remove(checkBox);
            checkboxCount--;
        }

        Set<String> allKeywords = new HashSet<>();
        for (List<String> tagsForCheckbox : checkBoxTagMap.values()) {
            allKeywords.addAll(tagsForCheckbox);
        }

        TextView selectionText = findViewById(R.id.selectionText);
        if (!allKeywords.isEmpty()) {
            String keywordsStr = String.join(", ", allKeywords);
            selectionText.setText("You selected: " + keywordsStr);
        } else {
            selectionText.setText("");
        }
    }
}