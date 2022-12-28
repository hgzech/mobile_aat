package com.hilmarzech.mobileaat;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class holds information about answers given in questionnaires.
 */
public class Answer {
    public String questionID;
    public String answer;
    public ArrayList<String> answers;
    public String questionnaireID;
    static final String MISSING_VALUE = "-1";
    static final String OPTIONAL_VALUE = "";

    public Answer(String questionID, String questionnaireID) {
        this.questionID = questionID;
        this.questionnaireID = questionnaireID;
        this.answer = MISSING_VALUE;
        this.answers = new ArrayList<>();
    }

    public boolean has_been_answered() {
        boolean answered = true;
        if (this.answer.equals(MISSING_VALUE) | this.answer.equals(OPTIONAL_VALUE)) {
            answered = false;
        }
        return answered;
    }

    public static HashMap<String, Answer> fromQuestionnaire(Questionnaire questionnaire) {
        //ArrayList<Answer> answers = new ArrayList<>();
        HashMap<String, Answer> answers = new HashMap<>();
        for (Question question : questionnaire.questions) {
            if (question.type.format_id != Type.INSTRUCTION_ID) {
                Answer answer = new Answer(question.id, questionnaire.id);
                if (question.optional) {
                    answer.answer = OPTIONAL_VALUE;
                }
                answers.put(question.id, answer);
            }
        }
        return answers;
    }
}
