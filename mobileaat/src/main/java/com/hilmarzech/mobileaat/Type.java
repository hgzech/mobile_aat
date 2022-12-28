package com.hilmarzech.mobileaat;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This class holds the different possible question types and their required information.
 */
public class Type {
    static final String TAG = "Type";
    static final String LIKERT_TYPE = "likert";
    static final int LIKERT_ID = 1;
    static final String MULTIPLE_TYPE = "multiple";
    static final int MULTIPLE_ID = 2;
    static final String INSTRUCTION_TYPE = "instruction";
    static final int INSTRUCTION_ID = 3;
    static final String TEXT_INPUT_TYPE = "text_input";
    static final int TEXT_INPUT_ID = 4;
    static final String CHECK_TYPE = "check";
    static final int CHECK_ID = 5;
    static final int MAX_ID = 5;
    public String format = null;
    public int format_id = 0;
    // Likert variables
    public String min = null;
    public String max = null;
    // Multiple choice variables
    public ArrayList<String> options = null;

    public Type() {
        this.format = TEXT_INPUT_TYPE;
        this.format_id = TEXT_INPUT_ID;
    }

    public Type(JSONObject json) {
        try {
            this.format = json.getString("format");
            if (this.format.equals(LIKERT_TYPE)) {
                this.min = json.getString("min");
                this.max = json.getString("max");
                this.format_id = LIKERT_ID;
            } else if (this.format.equals(MULTIPLE_TYPE)) {
                this.options = new ArrayList<>();
                JSONArray options = json.getJSONArray("options");
                for (int i=0; i<options.length(); i++) {
                    this.options.add(options.getString(i));
                }
                this.format_id = MULTIPLE_ID;
            } else if (this.format.equals(CHECK_TYPE)) {
                this.options = new ArrayList<>();
                JSONArray options = json.getJSONArray("options");
                for (int i=0; i<options.length(); i++) {
                    this.options.add(options.getString(i));
                }
                this.format_id = CHECK_ID;
            } else if (this.format.equals(INSTRUCTION_TYPE)) {
                this.format_id = INSTRUCTION_ID;
            } else if (this.format.equals(TEXT_INPUT_TYPE)) {
                this.format_id = TEXT_INPUT_ID;
            } else {
                Log.w(TAG, "Type: Could not handle format "+this.format);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
