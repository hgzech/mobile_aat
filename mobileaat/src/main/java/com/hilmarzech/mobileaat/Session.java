package com.hilmarzech.mobileaat;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Session objects hold task objects which can be handled by task specific activities (e.g. AATActivity)
 * Session ids can be saved locally to indicate completion and used for saving data to the online store
 * Session also has some display related fields (e.g. name)
 */
public class Session {
    static final String TAG = "Session";
    public final String id;
    //public ArrayList<Task> tasks;
    public ArrayList<String> tasks;
    public String name;
    public boolean is_introduction;
    public int start = -1;
    public int end = -1;
    public int duration;
    // TODO: This should be a boolean
    public boolean repeatable;
    public int days_before_next_session;
    public String subtitle;
    //public Boolean completed;
    public Long completedAt;
    //public Boolean last_session_completed;
    public String waiting_for_session;
    public Boolean timed_out;
    public Boolean missed;
    public DateTime expected_starting_date;
    public DateTime expected_ending_date;
    public Boolean complete_all;
    public Boolean all_completed;
    public boolean reminder_scheduled;
    public Integer reminder;
    public String reminder_text;
    public String reminder_when;
    public String reminder_schedule;
    public Long reminderUnix;
    public Boolean is_food_replication = false;
    private DateTime specified_day;


    public Session(String sessionId, JSONObject jsonSession) throws JSONException {
        this.id = sessionId;
        // On date defines the date on which the session is active
        if (jsonSession.has("year") & jsonSession.has("month") & jsonSession.has("day")) {
            this.specified_day = new DateTime().withYear(jsonSession.getInt("year")).withMonthOfYear(jsonSession.getInt("month")).withDayOfMonth(jsonSession.getInt("day"));
            Log.d(TAG, "Session: setting starting day: " + this.specified_day.toString());

        }
        // TODO: Integrate the specified day in the is_active call

        // Start and end defines hours between which the session is active
        if (jsonSession.has("food_replication")) {
            this.is_food_replication = jsonSession.getBoolean("food_replication");
        }
        if (jsonSession.has("start")) {
            this.start = jsonSession.getInt("start");
        }
        if (jsonSession.has("end")) {
            this.end = jsonSession.getInt("end");
        }
        if (this.start != -1) {
            this.duration = this.start < this.end ? this.end - this.start : this.end - this.start + 24;
        } else {
            this.duration = 24;
        }


        if (jsonSession.has("repeatable")) {
            this.repeatable = jsonSession.getBoolean("repeatable");
        } else {
            this.repeatable = false;
        }

        if (jsonSession.has("is_introduction")) {
            this.is_introduction = jsonSession.getBoolean("is_introduction");
        } else {
            this.is_introduction = false;
        }
        if (jsonSession.has("complete_all")) {
            this.complete_all = jsonSession.getBoolean("complete_all");
        } else {
            this.complete_all = false;
        }
        this.all_completed = false;

        if (jsonSession.has("reminder")) {
            this.reminder = jsonSession.getInt("reminder");
        } else {
            this.reminder = null;
        }
        if (jsonSession.has("reminder_text")) {
            this.reminder_text = jsonSession.getString("reminder_text");
        }
        if (jsonSession.has("reminder_when")) {
            this.reminder_when = jsonSession.getString("reminder_when");
        }
        this.reminder_scheduled = false;
        if (jsonSession.has("days_before_next_session")) {
            this.days_before_next_session = jsonSession.getInt("days_before_next_session");
        } else {
            this.days_before_next_session = 0;
        }
        // Getting the tasks
        this.tasks = new ArrayList<>();
        this.name = jsonSession.getString("name");
        this.subtitle = jsonSession.getString("subtitle");
        this.reminder_schedule = "";
        if (jsonSession.has("reminder_schedule")) {
            this.reminder_schedule = jsonSession.getString("reminder_schedule");
        }

        //this.completed = false;
        //this.last_session_completed = true;
        this.waiting_for_session = null;
        this.completedAt = null;
        //this.introduction_completed = true;
        this.timed_out = false;
        this.missed = false;
        if (jsonSession.has("tasks")) {
            JSONArray jsonTasks = jsonSession.getJSONArray("tasks");
            for (int i = 0; i < jsonTasks.length(); i++) {
                String task = jsonTasks.getString(i);
                //Task task = new Task(jsonTask);
                // TODO: This should happen during setting session completed
                this.tasks.add(task);
            }
        }
    }

    public void checkAllSessionsCompleted(ArrayList<Session> sessions) {
        this.all_completed = true;
        for (Session session : sessions) {
            if (!session.name.equals(this.name) && session.completedAt == null && !session.timed_out) {
                this.all_completed = false;
            }
        }
    }

    public void checkLastSessionCompleted(Session lastSession) {
        if (lastSession != null) {
            if (lastSession.completedAt == null && !lastSession.timed_out && !lastSession.missed) {
                this.waiting_for_session = lastSession.name;
            }
        }
    }

    public void setExpectedDateRange(Session lastSession) {

        DateTime now = new DateTime();
        //DateTime today = now.withTimeAtStartOfDay();
        // If there is no last session set the starting date to now
        DateTime startingDate = now;
        if ((lastSession != null) & (startingDate == null)) {
            Log.d(TAG, "setExpectedDateRange: starting date 0");
            // If this we don't have starting time set the time to midnight today
            startingDate = lastSession.expected_starting_date.withTimeAtStartOfDay();
            if (this.start != -1) {
                startingDate = startingDate.plusHours(this.start);
            }
            // If the last session enforces a break apply it if still necessary
            if (lastSession.days_before_next_session > 0) {
                // The default expected starting date is the amount of days after the last session is scheduled...
                if (lastSession.completedAt == null) {
                    startingDate = startingDate.plusDays(lastSession.days_before_next_session);
                    // ...However, if the last session has been completed...
                } else {
                    // ... The earliest expected date becomes the amount of days after the completion day
                    startingDate = new DateTime(lastSession.completedAt).withTimeAtStartOfDay().plusDays(lastSession.days_before_next_session);
                    Log.d(TAG, "setExpectedDateRange: starting date 1 " + startingDate);
                    // Next we check if this starting date is in the past, if it is we set the starting date to today
                    if (startingDate.isBefore(now)) {
                        startingDate = now.withTimeAtStartOfDay();
                        Log.d(TAG, "setExpectedDateRange: starting date 2 " + startingDate);
                    }

                    if (this.start != -1) {
                        startingDate = startingDate.plusHours(this.start);
                    }
                }
                // If it does not apply a break check if the this sessions start is before this session -> increase one day (but only if the last session has been completed today)
            } else {
                if (this.start < lastSession.start) {
                    if (lastSession.completedAt != null) {
                        boolean completedToday = new DateTime(lastSession.completedAt).withTimeAtStartOfDay().isEqual(now.withTimeAtStartOfDay());
                        if (completedToday) {
                            startingDate = startingDate.plusDays(1);
                        }
                        Log.d(TAG, "setExpectedDateRange: starting date 3 " + startingDate);
                    }
                    // TODO: Check if starting date is in the past and adapt day
                }
            }
        }
        // We set expected date range
        this.expected_starting_date = startingDate;
        Log.d(TAG, "setExpectedDateRange: starting" + this.name + this.expected_starting_date);
        if (this.reminder != null) {
            int reminderHoursAfterStart;
            if (this.start <= this.reminder) {
                reminderHoursAfterStart = this.reminder - this.start; // 6 - 8 = -2
            } else {
                reminderHoursAfterStart = this.reminder - this.start + 24;
            }
            this.reminderUnix = this.expected_starting_date.plusHours(reminderHoursAfterStart).getMillis();
        }
        this.expected_ending_date = startingDate.plusHours(this.duration);
        // We check if the session was missed today
        Log.d(TAG, "setExpectedDateRange: ending" + this.name + this.expected_ending_date);

        if (this.expected_ending_date.isBefore(now)) {
            this.missed = true;
        }

        if (this.specified_day != null) {
            if (this.specified_day.isBefore(now)) {
                this.missed = true;
            }
        }
    }

    public void setExpectedDateRangeLegacy(Session lastSession) {
        DateTime now = new DateTime();
        DateTime startingDate = now;
        if (lastSession != null) {
            startingDate = lastSession.expected_starting_date.withTimeAtStartOfDay();
            if (this.start != -1) {
                startingDate = startingDate.plusHours(this.start);
            }
            if (lastSession.days_before_next_session>0) {
                if (lastSession.completedAt == null) {
                    startingDate = startingDate.plusDays(lastSession.days_before_next_session);
                } else {
                    startingDate = new DateTime(lastSession.completedAt).withTimeAtStartOfDay().plusDays(lastSession.days_before_next_session);
                    Log.d(TAG, "setExpectedDateRange: starting date 1 "+startingDate);
                    if (startingDate.isBefore(now)) {
                        startingDate = now.withTimeAtStartOfDay();
                        Log.d(TAG, "setExpectedDateRange: starting date 2 "+startingDate);
                    }

                    if (this.start != -1) {
                        startingDate = startingDate.plusHours(this.start);
                    }
                }
            } else {
                if (this.start < lastSession.start) {
                    if (lastSession.completedAt != null) {
                        boolean completedToday = new DateTime(lastSession.completedAt).withTimeAtStartOfDay().isEqual(now.withTimeAtStartOfDay());
                        if (completedToday) {
                            startingDate = startingDate.plusDays(1);
                        }
                        Log.d(TAG, "setExpectedDateRange: starting date 3 " + startingDate);
                    }
                }
            }
        }
        this.expected_starting_date = startingDate;
        Log.d(TAG, "setExpectedDateRange: starting"+this.name+this.expected_starting_date);
        if (this.reminder != null) {
            int reminderHoursAfterStart;
            if (this.start <= this.reminder) {
                reminderHoursAfterStart = this.reminder - this.start; // 6 - 8 = -2
            } else {
                reminderHoursAfterStart = this.reminder - this.start + 24;
            }
            this.reminderUnix = this.expected_starting_date.plusHours(reminderHoursAfterStart).getMillis();
        }
        this.expected_ending_date = startingDate.plusHours(this.duration);
        Log.d(TAG, "setExpectedDateRange: ending"+this.name+this.expected_ending_date);

        if (this.expected_ending_date.isBefore(now)) {
            this.missed = true;
        }
    }

    public boolean isActive() {
        DateTime now = new DateTime();
        // By default it is not active
        boolean active = false;
        // Check if it is within time frame
        boolean in_time_frame = false;
        if (this.start == -1) {
            in_time_frame = true;
        } else {

            if (now.isAfter(this.expected_starting_date) && now.isBefore(this.expected_ending_date)) {
                in_time_frame = true;
            } else {
                int daysUntilOpen = Days.daysBetween(now.toLocalDate(), this.expected_starting_date.toLocalDate()).getDays();
            }
        }
        if (this.specified_day != null) {
            if (!(now.isAfter(this.specified_day) && now.isBefore(this.specified_day.plusHours(24)))) {
                Log.d(TAG, "setExpectedDateRange: not the correct day " + this.specified_day.toString());
                in_time_frame = false;
            }
        }

        //if (this.missed && this.completedAt == null && !this.timed_out) {
        //    this.subtitle = "You missed this session, today. Please repeat it tomorrow.";
        //}
        // If it is in time
        if (in_time_frame) {
            // Set active if it is not completed or repeatable
            //if (repeatable==1 || !completed) {
            if (repeatable || completedAt == null) {
                active = true;
            }
        }

        if (this.waiting_for_session != null) {
            //this.subtitle = String.format("First complete \"%s\"", this.waiting_for_session);
            active = false;
        }
        if (this.complete_all && !this.all_completed) {
            //this.subtitle = String.format("First complete all other sessions", this.waiting_for_session);
            active = false;
        }

        if (this.timed_out && repeatable == false) {
            active = false;
            //this.subtitle = "You took too long to complete this session.";
        } else if (!this.timed_out && this.completedAt != null && repeatable == false) {
            //this.subtitle = "You completed this session.";
        } else if (this.completedAt != null && repeatable) {
            //this.subtitle = "You can always repeat this session.";
        }
        if (reminder != null && !reminder_scheduled) {
            active = true;
            this.subtitle = this.reminder_schedule;
        }

        return active;
    }

    public boolean isActiveLegacy() {
        boolean active = false;
        boolean in_time_frame = false;
        if (this.start==-1) {
            in_time_frame = true;
        } else {

            DateTime now = new DateTime();
            if (now.isAfter(this.expected_starting_date) && now.isBefore(this.expected_ending_date)) {
                in_time_frame = true;
            } else {
                int daysUntilOpen = Days.daysBetween(now.toLocalDate(), this.expected_starting_date.toLocalDate()).getDays();
                switch (daysUntilOpen) {
                    case 0:
                        break;
                    case 1:
                        this.subtitle = "You can start this session tomorrow";
                        break;
                    case 2:
                        this.subtitle = "You can start this session the day after tomorrow.";
                        break;
                    default:
                        this.subtitle = String.format("You can start this session in %d days", daysUntilOpen);
                }
            }

        }
        if (this.missed && this.completedAt == null && !this.timed_out) {
            this.subtitle = "You missed this session, today. Please repeat it tomorrow.";
        }
        if (in_time_frame) {
            if (repeatable || completedAt == null) {
                active = true;
            }
        }

        if (this.waiting_for_session != null) {
            this.subtitle = String.format("First complete \"%s\"", this.waiting_for_session);
            active = false;
        }
        if (this.complete_all && !this.all_completed) {
            this.subtitle = String.format("First complete all other sessions", this.waiting_for_session);
            active = false;
        }

        if (this.timed_out && repeatable==false) {
            active = false;
            this.subtitle = "You took too long to complete this session.";
        } else if (!this.timed_out && this.completedAt != null && repeatable==false) {
            this.subtitle = "You completed this session.";
        } else if (this.completedAt != null && repeatable) {
            this.subtitle = "You can always repeat this session.";
        }
        if (reminder!=null && !reminder_scheduled) {
            active = true;
            this.subtitle = "Click to schedule a reminder.";
        }
        return active;
    }
}
