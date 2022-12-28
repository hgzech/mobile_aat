package com.hilmarzech.mobileaat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.util.LogWriter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.installreferrer.api.InstallReferrerClient;

import org.joda.time.DateTime;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.TimeZone;


/**
 * This activity handles the home screen.  It checks whether a participant ID exists (otherwise creating one); checks whether an experiment has been downloaded (otherwise displaying a text entry for the participant to choose one; checks whether a condition has been allocated (otherwise allocating one); Finally, it displays sessions and determines which sessions are active (i.e. clickable by the participant).
 * TODO: When an experiment is chosen, the action button should go inactive to give participants feedback that the click was registered and prevent other clicks (alternatively, the app could move directly to the progress layout).
 * TODO: This should really be broken up into several activities
 */
public class HomeActivity extends AppCompatActivity implements FirebaseHelper.ConditionChosenListener, FirebaseHelper.ResourcesDownloadedListener, HomeAdapter.SessionClickListener, TextView.OnEditorActionListener {
    private static final String TAG = "Homeactivity";
    private ArrayList<Session> sessions;
    private RecyclerView recyclerView;
    private LinearLayout progressLayout;
    private WebView progressBar;
    private TextView progressText;
    private EditText experiment_edit_text = null;
    private LinearLayout experiment_edit_layout = null;
    private String condition_name;
    private HomeAdapter adapter = null;
    private InstallReferrerClient referrerClient;

    /**
     * Setting up the progress screen.
     */
    private void setupProgressBar() {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progress_bar);
            progressBar.loadDataWithBaseURL("file:///android_res/raw/", "<img src='progress.gif' style='max-width:100%' align='middle'>", "text/html", "UTF-8", "");
        }
    }

    /**
     * Getter for progressLayout.
     * @return
     */
    private LinearLayout getProgressLayout() {
        progressLayout = (progressLayout == null) ? (LinearLayout)findViewById(R.id.progress_layout) : progressLayout;
        return progressLayout;
    }

    /**
     * Getter for progressText.
     * @return
     */
    private TextView getProgressText() {
        progressText = (progressText == null) ? (TextView)findViewById(R.id.progress_text) : progressText;
        return progressText;
    }

    private LinearLayout getExperimentEditLayout(){
        if (experiment_edit_layout == null) {
            experiment_edit_layout = findViewById(R.id.experiment_edit_layout);
        }
        return experiment_edit_layout;
    }

    /**
     * Getter for experiment_edit_text.
     * @return
     */
    private EditText getExperimentEditText() {
        if (experiment_edit_text == null) {
            experiment_edit_text = findViewById(R.id.experiment_edit_text);
            experiment_edit_text.setOnEditorActionListener(this);
        }
        return experiment_edit_text;
    }

    /**
     * Function that hides recyclerView.
     */
    private void hideRecyclerView() {
        recyclerView.setVisibility(View.GONE);
    }

    /**
     * Function that hides experiment_edit_layout.
     */
    private void hideExperimentEditLayout() {
        getExperimentEditLayout().setVisibility(View.GONE);
    }

    /**
     * function that hies progress text.
     */
    private void hideProgress() {
        if (progressLayout != null) {
            progressLayout.setVisibility(View.GONE);
        }
    }

    /**
     * This function displays the progress bar if an experiment has been found online.
     */
    private void showProgress() {
        hideRecyclerView();
        hideExperimentEditLayout();
        setupProgressBar();
        getProgressLayout().setVisibility(View.VISIBLE);
    }

    /**
     * This function displays a text entry field in which participants can specify the experiment.
     */
    private void showExperimentEditText() {
        hideRecyclerView();
        hideProgress();
        Log.w(TAG, "showExperimentEditText: showing edit text");
        getExperimentEditLayout().setVisibility(View.VISIBLE);
        getExperimentEditText();
    }

    /**
     * This function shows the recyclerView which handles sessions.
     */
    private void showRecyclerView() {
        hideProgress();
        hideExperimentEditLayout();
        recyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * A function that allows participants to reset the AAT app.
     * TODO: It should be possible to toggle this using JSON resources.
     */
    public void resetApp() {
        DatabaseHelper.deletePreferences(this);
        finish();
        startActivity(getIntent());
    }

    /**
     * Creates basic layout.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        // This should only run on first app start
        // TODO: Add referrer checking
        //checkReferrer();
        setContentView(com.hilmarzech.mobileaat.R.layout.activity_home);
        this.recyclerView = findViewById(com.hilmarzech.mobileaat.R.id.recyclerview);
        this.recyclerView.setHasFixedSize(true);
    }

    /**
     * Updates the layout.
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }

    /**
     * A function which allows participants to easily add session appointments to their calendar.
     * TODO: This should be depreciated to allow for easier maintenance of the AAT app.  Participant can, instead, simply be asked to add appointments to their calendar of choice.
     * @param session
     */
    private void showCalendar(Session session) {
        TimeZone timeZone = TimeZone.getDefault();
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setType("vnd.android.cursor.item/event")
                .putExtra(CalendarContract.Events._ID, new DateTime().getMillis())
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, session.reminderUnix)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, session.reminderUnix + (30 * 60 * 1000))
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false) // just included for completeness
                .putExtra(CalendarContract.Events.TITLE, String.format(session.reminder_when))
                .putExtra(CalendarContract.Events.DESCRIPTION, String.format("%s. " + session.reminder_text, session.reminder_when))
                .putExtra(CalendarContract.Events.HAS_ALARM, 1);
        startActivity(intent);
    }

    /**
     * This function interacts with Session objects to decide whether a session should be active or not.  It also checks whether the introduction session was completed.
     * TODO: This function needs cleaning up.  Its name does not seem to reflect its full behaviour.
     */
    private void setExpectedStartingDates() {
        for (int i = 0; i < this.sessions.size(); i++) {
            Session currentSession = this.sessions.get(i);
            Session lastSession = i == 0 ? null : this.sessions.get(i - 1);
            currentSession.checkLastSessionCompleted(lastSession);
            if (currentSession.is_food_replication) {
                currentSession.setExpectedDateRangeLegacy(lastSession);
            } else {
                currentSession.setExpectedDateRange(lastSession);
            }
            if (currentSession.complete_all) {
                currentSession.checkAllSessionsCompleted(this.sessions);
            }
        }
        for (Session session : this.sessions) {
            Log.d(TAG, "setExpectedStartingDates: " + session.id + session.completedAt);
        }
        checkIntroductionCompleted();
    }

    /**
     * This function checks whether the introduction has been completed.  I think this function was mainly designed to prevent participants from scheduling reminders before the introduction was completed.
     */
    private void checkIntroductionCompleted() {
        // Check if introduction is completed
        String waiting_for_introduction = null;
        for (int i = 0; i < this.sessions.size(); i++) {
            if (this.sessions.get(i).is_introduction && (this.sessions.get(i).completedAt == null)) {
                waiting_for_introduction = this.sessions.get(i).name;
            }
        }
        // If it is not completed adjust all other sessions
        if (waiting_for_introduction != null) {
            for (int i = 0; i < this.sessions.size(); i++) {
                if (!sessions.get(i).is_introduction) {
                    sessions.get(i).reminder = null;
                }
            }
        }
    }

    /**
     * This function checks whether a participant ID has been created (otherwise it creates one); It checks whether an experiment has been downloaded (otherwise it displays the experiment edit text); It checks if a condition has been chosen (otherwise it chooses one); Finally, it displays sessions and decides which sessions are active.
     * TODO: This function should be broken down into several functions.
     */
    private void updateView() {
        // Check if participant id exists (otherwise create one)
        String participant_id = DatabaseHelper.getParticipantID(this);
        if (participant_id == null) {
            DatabaseHelper.addParticipant(this);
        }
        // Check if experiment exists (otherwise download it)
        final String experiment = DatabaseHelper.getExperiment(this);
        if (experiment == null) {
            showExperimentEditText();
        }
        // Check if condition exists (otherwise choose one)
        this.condition_name = DatabaseHelper.getConditionName(this);
        if (this.condition_name == null & experiment != null) {
            getProgressText().setText("Setting up experiment");
            showProgress();
            DatabaseHelper.chooseCondition(this, this);
        }

        if (this.condition_name != null & experiment != null) {
            try {
                this.sessions = DatabaseHelper.getSessions(condition_name, this);
                showRecyclerView();
                //checkExperimentCompletion();
                setExpectedStartingDates();
                if (this.adapter == null) {
                    this.adapter = new HomeAdapter(this.sessions, this, this);
                    this.recyclerView.setVisibility(View.VISIBLE);
                    this.recyclerView.setAdapter(adapter);
                    LinearLayoutManager llm = new LinearLayoutManager(this);
                    llm.setOrientation(LinearLayoutManager.VERTICAL);
                    this.recyclerView.setLayoutManager(llm);
                } else {
                    this.adapter.sessions = this.sessions;
                    this.adapter.notifyDataSetChanged();
                }
            } catch (JSONException e) {
                Log.w(TAG, "onConditionChosen: Could not get sessions for condition:" + condition_name);
                e.printStackTrace();
            }
        }
    }

    /**
     * This is an interface with FirebaseHelper which handle's the message that a condition has been chosen.
     * @param conditionName
     * TODO: This should interact with DatabaseHelper not FirebaseHelper.
     */
    @Override
    public void onConditionChosen(String conditionName) {
        this.condition_name = conditionName;
        DatabaseHelper.saveConditionName(conditionName, this);
        String model = Build.MODEL;
        DatabaseHelper.saveModel(this, model);
        updateView();
    }

    /**
     * An interface with FirebaseHelper handling that no condition could be chosen.
     * TODO: This should interact with DatabaseHelper not FirebaseHelper.
     */
    @Override
    public void onFailed() {
        Log.w(TAG, "onFailed: Could not choose condition");
        Toast.makeText(this, "Could not choose condition", Toast.LENGTH_LONG).show();
    }

    /**
     * This function allows participants to email data.
     */
    // TODO: It should be possible to specify whether this function is accessible via JSON resources.  Theses resources should also specify the email address (which is currently hard coded)
    public void sendData() {
        OfflineHelper.makeBackupFile(this);
        try {
            File filePath = new File(this.getExternalFilesDir(null), "Backup");
            File newFile = new File(filePath, String.format("%s.aat", DatabaseHelper.getParticipantID(this)));
            Uri URI = FileProvider.getUriForFile(this, "com.hilmarzech.picturegame.fileprovider", newFile);
            String email = "h.g.zech@fsw.leidenuniv.nl";
            String subject = "data " + DatabaseHelper.getPublicParticipantID(this);
            String message = "";
            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
            if (URI != null) {
                emailIntent.putExtra(Intent.EXTRA_STREAM, URI);
            }
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            this.startActivity(Intent.createChooser(emailIntent, "Sending email..."));
        } catch (Throwable t) {
            Toast.makeText(this, "Request failed try again: " + t.toString(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This function handle's clicks on sessions.  If a session is active it opens SessionActivity with the session.id.  Otherwise it allows participants to schedule reminders, if specified in the sessions JSON resource.
     * @param position
     * TODO: Part of this function might become redundant if the calendar function is depreciated.
     */
    @Override
    public void sessionClicked(int position) {
        Session session = this.sessions.get(position);
        boolean active = false;
        if (session.is_food_replication) {
            active = session.isActiveLegacy();
        } else {
            active = session.isActive();
        }
        if (active) {
            // If the reminder is scheduled open the session
            if (session.reminder == null || session.reminder_scheduled) {
                Intent intent = new Intent(this, SessionActivity.class);
                intent.putExtra("session_id", session.id);
                startActivity(intent);
            } else {
                session.reminder_scheduled = true;
                DatabaseHelper.saveSessionReminderSet(session.id, this);
                showCalendar(session);
            }
        } else
            Toast.makeText(this, session.subtitle, Toast.LENGTH_SHORT).show();
    }

    /**
     * This function handles the checkmark click after a participant filled in the experiment name.
     * @param textView
     * @param actionId
     * @param keyEvent
     * @return
     */
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            Log.w(TAG, "onEditorAction: IME ACTION");
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            String experiment_name = experiment_edit_text.getText().toString();
            showProgress();
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            Log.e("Width", "" + width);
            Log.e("height", "" + height);
            DatabaseHelper.downloadResources(experiment_name, this, this);
            getProgressText().setText("Fetching experiment resources.");
        }
        return false;
    }

    /**
     * An interface with FirebaseHelper.  Once resources are downloaded this function saves them to the offline storage.
     * TODO: This should all be handled by DatabaseHelper
     * @param experiment_name
     */
    @Override
    public void onResourcesDownloaded(String experiment_name) {
        DatabaseHelper.saveExperiment(experiment_name, this);
        updateView();
    }

    /**
     * An interface with FirebaseHelper displaying a notification if the chose experiment does not exist.
     * TODO: This should be handled by DatabaseHelper
     **/
    @Override
    public void onExperimentDoesNotExist() {
        Toast.makeText(this, "Experiment does not exist.", Toast.LENGTH_LONG).show();
        hideProgress();
        //getExperimentEditText().setText("");
        showExperimentEditText();
    }

    /**
     * An interface with FirebaseHelper displaying a notification if the resources could not be downloaded.
     * TODO: This should be handled by DatabaseHelper
     **/
    @Override
    public void onResourceDownloadFailed() {
        Toast.makeText(this, "Could not download experiment.", Toast.LENGTH_LONG).show();
        hideProgress();
        //getExperimentEditText().setText("");
        showExperimentEditText();
    }


    /**
     * A function creating the menu which participants can click to access extra functions.
     * TODO: It would be a nice feature if this menu would include an "email experimenter" button.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        return true;
    }

    /**
     * This function handles menu button clicks.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_settings) {
//
//            Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT)
//                    .show();
//            sendData();
//        } else if
        if (item.getItemId() == R.id.reset_app) {
            resetApp();
        }
        return true;
    }
}

