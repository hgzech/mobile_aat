package com.hilmarzech.mobileaat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ListView;

import org.json.JSONException;


/**
 * This activity handles display of and use input to questionnaires.  Note that the display of individual questions is handled by QuestionnaireAdapter.
 */
// TODO: Add some space between questions (move to CardView/RecyclerView as in home screen eventually)
// TODO: Multiple choice does not actually allow multiple choices at this point
// TODO: Multiple choice options are a bit small
public class QuestionnaireActivity extends AppCompatActivity  implements QuestionnaireAdapter.QuestionnaireCallback, AbsListView.OnScrollListener {
    private static final String TAG = "QuestionnaireActivity";
    //private DatabaseHelper dbHelper;
    private Questionnaire questionnaire;
    private FloatingActionButton actionButton;
    private ListView listView;
    private String session_id;
    private String task_id;
    private boolean scrolled_down;
    private boolean safety_time_up;
    private boolean action_button_visible;
    private boolean all_questions_answered;


    /**
     * This function creates the basic layout (e.g. action button).  It also starts a timer of 3 seconds which makes sure that participants cannot just click through questionnaires.  In other words, each questionnaire has to be displayed for at least 3 seconds before the participant can move on.
     * TODO: The timer is not strictly necessary.  Perhaps I can remove it.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.hilmarzech.mobileaat.R.layout.activity_questionnaire);
        actionButton = (FloatingActionButton) findViewById(com.hilmarzech.mobileaat.R.id.finished_button);
        listView = (ListView) findViewById(com.hilmarzech.mobileaat.R.id.list_view);

        Intent intent = getIntent();
        if (intent != null) {
            session_id = getIntent().getStringExtra(SessionActivity.SESSION_ID_EXTRA_NAME);
            this.task_id = getIntent().getStringExtra(SessionActivity.TASK_ID_EXTRA_NAME);
            try {
                this.questionnaire = DatabaseHelper.getQuestionnaire(this.task_id, this);
            } catch (JSONException e) {
                Log.w(TAG, "onCreate: "+"Could not find questionnaire "+ this.task_id );
                e.printStackTrace();
            }
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                safetyTimeUp();
            }
        }, 3000);

        displayQuestionnaire(this.questionnaire);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * This function displays a questionnaire adapter which handle's the exact display of each questionnaire.
     * @param questionnaire
     */
    private void displayQuestionnaire(Questionnaire questionnaire) {
        // Get reference to listview
        //ListView listView = (ListView) findViewById(R.id.list_view);
        this.listView.setOnScrollListener(this);
        // Create adapter with questionnaire
        QuestionnaireAdapter jsonAdapter = new QuestionnaireAdapter(this, questionnaire);
        jsonAdapter.setCallback(this);
        this.listView.setAdapter(jsonAdapter);
    }

    /**
     * This function makes the action button available and sets all_questions_answered to true.
     */
    @Override
    public void allQuestionsAnswered() {
        this.all_questions_answered = true;
        toggleActionButton();
    }

    /**
     * This function saves answers to the database once a question was answered.
     * @param answer
     */
    @Override
    public void questionAnswered(Answer answer) {
        DatabaseHelper.saveAnswer(this.session_id, answer, this);
    }

    /**
     * this function hides the keyboard.
     */
    @Override
    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * This function moves back to the session activity, passing on the session id and the task id.
     */
    public void actionButtonClick(View view) {
        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra(SessionActivity.SESSION_ID_EXTRA_NAME, session_id);
        intent.putExtra(SessionActivity.TASK_ID_EXTRA_NAME, this.task_id);
        startActivity(intent);
        finish();
    }

    /**
     * This function toggles the action button (if the safety time has passed, all questions are answered and the user has scrolled all the way down.
     */
    private void toggleActionButton() {
        if (this.scrolled_down && this.all_questions_answered && safety_time_up) {
            actionButton.setVisibility(View.VISIBLE);
            action_button_visible = true;
        }
    }

    /**
     * This function toggles the action button and sets safty_time_up to true
     * TODO: This could be combined with toggleActionButton
     */
    private void safetyTimeUp() {
        this.safety_time_up = true;
        toggleActionButton();
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    /**
     * This function handles scrolling.
     * @param view
     * @param firstVisibleItem
     * @param visibleItemCount
     * @param totalItemCount
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (!action_button_visible) {
            if (this.listView.getLastVisiblePosition() != -1) {
                if (this.listView.getLastVisiblePosition() == this.questionnaire.questions.size() - 1 &&
                        (this.listView.getChildAt(this.listView.getChildCount() - 1).getBottom()-100) <= this.listView.getHeight()) {
                    this.scrolled_down = true;
                    toggleActionButton();
                } else {
                    this.scrolled_down = false;
                }
            }
        }
    }
}
