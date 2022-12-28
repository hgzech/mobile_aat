package com.hilmarzech.mobileaat;


import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;

/*
 * This activity handles sessions.  It loops through tasks, specified in the session and decides which activity to load to display the task.  Therefore this activity, does not have it's own view.
 */
public class SessionActivity extends AppCompatActivity {
    public static final String TASK_ID_EXTRA_NAME = "json_task";
    public static final String SESSION_ID_EXTRA_NAME = "session_id";
    public static final String SESSION_OUTDATED_EXTRA_NAME = "session_outdated";
    static final String TAG = "SessionActivity";
    private String session_id;
    Session session;


    /**
     * This function loads the session, specified in the intent.  If the activity gets opened by a task, it also sets the tasks completion to true (and stores the completion to the database helper).  Finally, it also checks whether the session is outdated in moves the participant back home if this is the case.
     * @param savedInstanceState
     */
    // TODO: Should this be in on create (does it always get called when coming with intent)?
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        Intent intent = getIntent();
        session_id = intent.getStringExtra(SESSION_ID_EXTRA_NAME);
        try {
            Log.d(TAG, "onCreate: Get session");
            this.session = DatabaseHelper.getSession(session_id, this);
            Log.d(TAG, "onCreate: Got session");

            // If we are coming from a task and the session is not repeatable, save task completion
            if (intent.hasExtra(TASK_ID_EXTRA_NAME)) {
                Log.d(TAG, "Coming from task " + intent.getStringExtra(TASK_ID_EXTRA_NAME));

                DatabaseHelper.saveTaskCompletion(session_id, intent.getStringExtra(TASK_ID_EXTRA_NAME), true, this);
            }
            // If the session is outdated, we log it as outdated and move home
            if (intent.hasExtra(SESSION_OUTDATED_EXTRA_NAME)) {
                Log.w(TAG, "onCreate: Coming from outdated AAT");
                DatabaseHelper.saveSessionTimeOut(session_id, this);
                // Session has to be marked as completed so next session can start
                DatabaseHelper.saveSessionCompletion(session_id, this);
                this.startActivity(new Intent(this, HomeActivity.class));
            } else {
                moveToNextTask();
            }
        } catch (JSONException e) {
            Log.w(TAG, "onCreate: " + "Could not load session.");
            e.printStackTrace();
        }
    }

    /**
     * This function moves to the appropriate activity for the next task (or to home if there's no task).  It opens the appropriate activity and tells it which task (by id) it is supposed to display.
     */
    private void moveToNextTask() {
        Intent nextActivityIntent;
        Task nextTask = null;

        for (String taskId : this.session.tasks) {
            Boolean task_completed = DatabaseHelper.checkTaskCompletion(session_id, taskId,this);
            if (!task_completed) {
                Log.d(TAG, "moveToNextTask: Found uncompleted task.");
                try {
                    nextTask = DatabaseHelper.getTask(taskId, this);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        // if not mark session as complete and move to home activity.
        if (nextTask == null) {
            Log.d(TAG, "moveToNextTask: Saving session completion.");
            DatabaseHelper.saveSessionCompletion(session_id, this);
            //if (this.session.repeatable==1) {
            for (String taskId : this.session.tasks) {
                Log.d(TAG, "moveToNextTask: resetting task completion");
                Boolean set_completed = true;
                try {
                    set_completed = !DatabaseHelper.getTask(taskId, this).repeatable;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                DatabaseHelper.saveTaskCompletion(session_id, taskId, set_completed, this);
            }
            //}
            Log.d(TAG, "moveToNextTask: Moving home.");
            // This should add an extra so you can not go back to the completed session.
            nextActivityIntent = new Intent(this, HomeActivity.class);
        // If there are tasks left, we choose the appropriate next activity
        } else {
            // We're saving completion before debriefing
            if (nextTask.is_debriefing) {
                DatabaseHelper.saveCompletion(true, this);
            }
            Class nextActivity = HomeActivity.class;
            switch (nextTask.type) {
                case "questionnaire":
                    nextActivity = QuestionnaireActivity.class;
                    break;
                case "aat":
                    nextActivity = AATActivity.class;
                    break;
                case "picture_rating":
                    nextActivity = PictureRatingActivity.class;
                    break;
                // If we can't handle the task type we log a warning and move to Home.
                default:
                    Log.w(TAG, "moveToNextTask: " + "Task type " + nextTask.type + " can't be handled.");
            }
            // Here we build the intent
            nextActivityIntent = new Intent(this, nextActivity);
            // We append the json encoded task description to the next activity.
            nextActivityIntent.putExtra(SESSION_ID_EXTRA_NAME, session_id);
            nextActivityIntent.putExtra(TASK_ID_EXTRA_NAME, nextTask.id);
        }
        this.startActivity(nextActivityIntent);
        finish();
    }
}
