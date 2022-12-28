package com.hilmarzech.mobileaat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is a superclass from which all tasks (e.g. questionnaire, aat) inherit.
 */
public class Task {
    private static final String TAG = "Task";
    public String type;
    public String id;
    public Boolean repeatable;
    public Boolean is_debriefing;

    public Task(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public Task(JSONObject jsonTask, String id) throws JSONException {

        if (!jsonTask.has("type")) {
            Log.e(TAG, "Task: Could not find type in jsonTask"+jsonTask);
        }
        this.id = id;
        this.type = jsonTask.getString("type");
        if (jsonTask.has("repeatable")) {
            this.repeatable = jsonTask.getBoolean("repeatable");
        } else {
            this.repeatable = true;
        }
        if (jsonTask.has("is_debriefing")) {
            this.is_debriefing = jsonTask.getBoolean("is_debriefing");
        } else {
            this.is_debriefing = false;
        }
    }
}
