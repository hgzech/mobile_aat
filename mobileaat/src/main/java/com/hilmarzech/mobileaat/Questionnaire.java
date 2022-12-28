package com.hilmarzech.mobileaat;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * This class holds information about questionnaires.  It is responsible for turning json questionnaire definitions into Questionnaire java objects, which can be displayed during the experiment.
 */
public class Questionnaire extends Task {
    private static final String TAG = "QuestionnaireObject";
    private static final String TYPE = "questionnaire";
    public ArrayList<Question> questions;
    public Map<String, Answer> answers;

    public Questionnaire(JSONObject jsonQuestionnaire, String id) throws JSONException {
        // Creating Task
        super(id, TYPE);
        Log.d(TAG, "Questionnaire: "+id);
        // Get questions from json object and create Question objects
        ArrayList<Question> questions = new ArrayList<>();
        JSONArray jsonQuestions = jsonQuestionnaire.getJSONArray("questions");
        for (int i = 0; i < jsonQuestions.length(); i++) {
            Object jsonObject = jsonQuestions.get(i);
            JSONObject jsonQuestion;
            if (jsonObject instanceof String) {
                jsonQuestion = new JSONObject();
                jsonQuestion.put("text", (String)jsonObject);
            } else {
                jsonQuestion = (JSONObject)jsonObject;
            }
            if (jsonQuestionnaire.has("default_type") & (!jsonQuestion.has("type"))) {
                jsonQuestion.put("type", jsonQuestionnaire.getJSONObject("default_type"));
            }
            if (!jsonQuestion.has("id")) {
                jsonQuestion.put("id", String.format("%s_%02d",id, i+1));
            }
            questions.add(new Question(jsonQuestion));
        }
        this.questions = questions;
        this.answers = Answer.fromQuestionnaire(this);
    }
}
