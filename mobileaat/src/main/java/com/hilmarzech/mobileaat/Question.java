package com.hilmarzech.mobileaat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class holds information about questions.
 */
public class Question {
    static final String TAG = "Question";
    public String id;
    public String text;
    public Type type;
    public boolean optional;

    // Constructor create a Question class from a json object
    public Question(JSONObject jsonQuestion) throws JSONException {
        this.id = jsonQuestion.getString("id");
        this.text = jsonQuestion.getString("text");
        // Checking if question has type, otherwise using default type
        if (jsonQuestion.has("optional")) {
            this.optional = jsonQuestion.getBoolean("optional");
        } else {
            this.optional = false;
        }

        if (jsonQuestion.has("type")) {
            this.type = new Type(jsonQuestion.getJSONObject("type"));
        } else {
            this.type = new Type();
        }
    }
}
