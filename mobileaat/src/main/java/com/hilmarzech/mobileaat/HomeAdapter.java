package com.hilmarzech.mobileaat;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * The home adapter handles the display of individual sessions.
 */
public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.MyViewHolder> implements View.OnClickListener {
    static final String TAG = "HomeAdapter";
    public Context context;
    public ArrayList<Session> sessions;
    SessionClickListener listener;
    final static String COMPLETION_MARK = "\u2713";
    final static String TIMED_OUT_MARK = "\u2717";

    public HomeAdapter(ArrayList<Session> sessions, SessionClickListener listener, Context contex){
        this.context = contex;
        this.sessions= sessions;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(com.hilmarzech.mobileaat.R.layout.session_card, parent, false);
        return new MyViewHolder(listItem);
    }


    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Session session = this.sessions.get(position);
        // TODO: Add cross for timed out session
        if (session.completedAt != null && !session.timed_out) {
            holder.sessionCompletedText.setText(COMPLETION_MARK);
        } else if (session.timed_out) {
            holder.sessionCompletedText.setText(TIMED_OUT_MARK);
            holder.sessionCompletedText.setTextColor(ContextCompat.getColor(this.context, com.hilmarzech.mobileaat.R.color.colorSecondary));
        } else if (session.reminder != null && !session.reminder_scheduled){
            holder.sessionCompletedText.setText("\uD83D\uDCC5");
        } else {
            holder.sessionCompletedText.setText("");
        }
        boolean active = false;
        Log.d(TAG, "onBindViewHolder: replication: "+session.is_food_replication);

        if (session.is_food_replication) {
            active = session.isActiveLegacy();
        } else {
            active = session.isActive();
        }
        Log.d(TAG, "onBindViewHolder: Active "+active);

        if (active) {
            holder.titleText.setTextColor(ContextCompat.getColor(this.context, com.hilmarzech.mobileaat.R.color.colorPrimaryDark));
            holder.subtitleText.setTextColor(ContextCompat.getColor(this.context, com.hilmarzech.mobileaat.R.color.colorPrimaryDark));
            holder.positionTextView.setTextColor(ContextCompat.getColor(this.context, com.hilmarzech.mobileaat.R.color.colorPrimaryDark));

        } else {
            holder.titleText.setTextColor(ContextCompat.getColor(this.context, com.hilmarzech.mobileaat.R.color.colorSecondary));
            holder.subtitleText.setTextColor(ContextCompat.getColor(this.context, com.hilmarzech.mobileaat.R.color.colorSecondary));
            holder.positionTextView.setTextColor(ContextCompat.getColor(this.context, com.hilmarzech.mobileaat.R.color.colorSecondary));
        }
        holder.titleText.setText(session.name);
        holder.subtitleText.setText(session.subtitle);
        holder.positionTextView.setText(String.format("%d. ", position+1));
        holder.cardView.setTag(position);
        holder.cardView.setOnClickListener(this);
    }


    @Override
    public int getItemCount() {
        return sessions.size();
    }

    @Override
    public void onClick(View v) {
        listener.sessionClicked((Integer)v.getTag());
    }

    public interface SessionClickListener
    {
        void sessionClicked(int position);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView titleText;
        private TextView positionTextView;
        private TextView subtitleText;
        private TextView sessionCompletedText;
        public MyViewHolder(View itemView) {
            super(itemView);
            titleText = (TextView)itemView.findViewById(com.hilmarzech.mobileaat.R.id.session_title_text);
            subtitleText = (TextView)itemView.findViewById(com.hilmarzech.mobileaat.R.id.session_subtitle_text);
            positionTextView = (TextView)itemView.findViewById(com.hilmarzech.mobileaat.R.id.session_number_text);
            sessionCompletedText = (TextView)itemView.findViewById(com.hilmarzech.mobileaat.R.id.session_completed_text);
            cardView = (CardView)itemView.findViewById(com.hilmarzech.mobileaat.R.id.card_view);
        }
    }
}
