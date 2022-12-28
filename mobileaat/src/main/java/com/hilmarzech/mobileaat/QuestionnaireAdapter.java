package com.hilmarzech.mobileaat;

import android.content.Context;
import androidx.core.util.LogWriter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * The purpose of this class is
 * to take JSON questionnaires with multiple questions and display them. The difficulty is
 * that each question type (e.g. multiple choice question, open question) has to be displayed in
 * a different way.
 *
 * getViewTypeCount() and getItemViewType(int position) are used to display list rows in different
 * formats. In getView() we check which format the question has. Convertview is than created based
 * on the specific question format.
 *
 * While all formats have different display and interaction characteristics, all formats have to implement
 * callback.questionAnswered and callback.allQuestionsAnswered, as these callbacks are used in the activity
 * to save the answers.
 *
 * To implement a new type:
 *  1) Add new type to Type class
 *  2) Increment Type.MAX_ID in Type class
 *  3) Add if statement to getView() to handle type
 *  4) Create type item layout file
 *  5) Create type View Holder
 *  6) Create getConvertView() method for type to inflate and populate layout
 *  7) Make sure getConvertView() implements questionAnswered()
 *  8) If the view needs a keyboard and is not an EditText, add this to the onClick() callback in getView()
 *  9) Add position tag when view is created
 */
public class QuestionnaireAdapter extends ArrayAdapter<Question> {
    private Questionnaire questionnaire;
    private QuestionnaireCallback callback;
    private static String TAG = "QuestionnaireAdapter";

    /**
     * A factory method.
     * @param context
     * @param questionnaire
     */
    public QuestionnaireAdapter(Context context, Questionnaire questionnaire) {
        super(context, 0, questionnaire.questions);
        this.questionnaire = questionnaire;
    }

    /**
     * A function that checks whether all questions are answered.
     */
    private void checkAllQuestionsAnswered() {
        boolean allQuestionsAnswered = true;
        for (String key: questionnaire.answers.keySet()) {
            if (questionnaire.answers.get(key).answer.equals(Answer.MISSING_VALUE)) {
                allQuestionsAnswered = false;
            }
        }
        if (allQuestionsAnswered) {
            callback.allQuestionsAnswered();
        }
    }

    /**
     * A function that creates an Answer object once a question is answered.
     * @param view
     * @param answerText
     * TODO: This function would better be called create Answer.
     */
    private void questionAnswered(View view, String answerText) {
        if (!answerText.isEmpty()) {
            // Send answer to callback
            int position = (Integer) view.getTag();
            Answer answer = questionnaire.answers.get(questionnaire.questions.get(position).id);
            // CheckBoxes are handled specifically, as they can have several answers
            if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                if (checkBox.isChecked()) {
                    answer.answers.add(answerText);
                } else {
                    answer.answers.remove(answerText);
                }
                // TODO: implement optional checkboxes (answer should switch back to -1 or optional if list is empty)
                answer.answer = "clicked";
                callback.questionAnswered(answer);

            } else {
                answer.answer = answerText;
            }
            callback.questionAnswered(answer);
            // Check if all questions are answered
            checkAllQuestionsAnswered();

        }
        // Remove focus from edittexts
        if (!(view instanceof EditText)) {
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
            callback.hideSoftKeyboard();
        }
    }


    /**
     * Finds the type of the current question.
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
        Question question = questionnaire.questions.get(position);
        return question.type.format_id;
    }

    /**
     * Gets the number of implemented types.
     * @return
     */
    @Override
    public int getViewTypeCount() {
        return Type.MAX_ID + 1;
    }

    /**
     * This function chooses the correct view based on the question type.
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Checking which format the question has (i.e., how it has to be displayed)
        int type = getItemViewType(position);
        Question question = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (type == Type.LIKERT_ID) {
            convertView = getLikertConvertView(position, convertView, parent, question);
        } else if (type == Type.MULTIPLE_ID) {
            convertView = getMultipleConvertView(position, convertView, parent, question);
        } else if (type == Type.INSTRUCTION_ID) {
            convertView = getInstructionConvertView(convertView, parent, question);
        } else if (type == Type.TEXT_INPUT_ID) {
            convertView = getTextInputConvertView(position, convertView, parent, question);
        } else if (type == Type.CHECK_ID) {
            convertView = getCheckConvertView(position, convertView, parent, question);
        }
        convertView.setClickable(true);
        convertView.setFocusable(true);
        convertView.setFocusableInTouchMode(true);
        // This makes sure that EditTexts loose focus when clicked outside
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.requestFocus();
                if (!(v instanceof EditText)) {
                    callback.hideSoftKeyboard();
                }
            }
        });
        return convertView;
    }

    /**
     * A listener used for Likert scale and multiple choice questions.
     * @param for_type
     * @return
     */
    private RadioGroup.OnCheckedChangeListener getRadiogroupListener(final int for_type) {
        return new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i != -1) {
                    RadioButton button = (RadioButton) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
                    if (button.isPressed()) {
                        int index = radioGroup.indexOfChild(button);
                        //Integer position = (Integer) radioGroup.getTag();
                        String answerText = null;
                        if (for_type == Type.LIKERT_ID) {
                            answerText = Integer.toString(index + 1);
                        } else if (for_type == Type.MULTIPLE_ID) {
                            Question question = questionnaire.questions.get((int) radioGroup.getTag());
                            answerText = question.type.options.get(index);
                        }
                        questionAnswered(radioGroup, answerText);
                    }
                }
            }
        };
    }

    /**
     * This function creates a Likert question.
     * @param position
     * @param convertView
     * @param parent
     * @param question
     * @return
     */
    private View getLikertConvertView(int position, View convertView, ViewGroup parent, Question question) {
        LikertViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new LikertViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(com.hilmarzech.mobileaat.R.layout.item_likert, parent, false);
            viewHolder.likert_text = (TextView) convertView.findViewById(com.hilmarzech.mobileaat.R.id.likert_text);
            viewHolder.likert_min_text = (TextView) convertView.findViewById(com.hilmarzech.mobileaat.R.id.likert_min_text);
            viewHolder.likert_max_text = (TextView) convertView.findViewById(com.hilmarzech.mobileaat.R.id.likert_max_text);
            viewHolder.likert_radio_group = (RadioGroup) convertView.findViewById(com.hilmarzech.mobileaat.R.id.likert_radio_group);
            // Attach listeners to the radio buttons
            viewHolder.likert_radio_group.setOnCheckedChangeListener(getRadiogroupListener(Type.LIKERT_ID));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (LikertViewHolder) convertView.getTag();
        }
        viewHolder.likert_text.setText(question.text);
        viewHolder.likert_radio_group.setTag(position);
        viewHolder.likert_min_text.setText(question.type.min);
        viewHolder.likert_max_text.setText(question.type.max);
        viewHolder.likert_text.invalidate();
        viewHolder.likert_min_text.invalidate();
        viewHolder.likert_max_text.invalidate();
        // Also remember already chosen answers so user does not have to click again
        Answer answer = questionnaire.answers.get(questionnaire.questions.get(position).id);
        if (answer.answer.equals("-1")) {
            viewHolder.likert_radio_group.check(-1);
        } else {
            int answerIndex = Integer.parseInt(answer.answer) - 1;
            viewHolder.likert_radio_group.check(viewHolder.likert_radio_group.getChildAt(answerIndex).getId());
        }
        return convertView;
    }

    /**
     * This function creates a multiple choice question.
     * @param position
     * @param convertView
     * @param parent
     * @param question
     * @return
     */
    private View getCheckConvertView(int position, View convertView, ViewGroup parent, Question question) {
        CheckViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new CheckViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_checkbox, parent, false);
            viewHolder.check_text = (TextView) convertView.findViewById(com.hilmarzech.mobileaat.R.id.check_text);
            viewHolder.check_group = (LinearLayout) convertView.findViewById(com.hilmarzech.mobileaat.R.id.check_group);
            //viewHolder.multiple_radio_group.setOnCheckedChangeListener(getRadiogroupListener(Type.MULTIPLE_ID));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (CheckViewHolder) convertView.getTag();
        }
        viewHolder.check_text.setText(question.text);
        Answer answer = questionnaire.answers.get(questionnaire.questions.get(position).id);
        //viewHolder.check_group.setTag(position);
        for (int i=0; i<viewHolder.check_group.getChildCount();i++) {
            CheckBox child = (CheckBox) viewHolder.check_group.getChildAt(i);
            if (i<question.type.options.size()) {
                child.setVisibility(View.VISIBLE);
                child.setText(question.type.options.get(i));
                child.setTag(position);
                child.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CheckBox checkBox = (CheckBox) view;
                        int position = (int) checkBox.getTag();
                        boolean checked = checkBox.isChecked();
                        Log.d(TAG, "onClick checked: " + position + checked);
                        questionAnswered(checkBox, checkBox.getText().toString());
                    }
                });
                boolean checked = answer.answers.contains(child.getText().toString());
                child.setChecked(checked);
                Log.d(TAG, "getCheckConvertView: rechecked:" + checked);
            } else {
                child.setVisibility(View.GONE);
            }
        }


//        if (!answer.has_been_answered()) {//.answer.equals("-1")) {
//            viewHolder.multiple_radio_group.check(-1);
//        } else {
//            int answerIndex = questionnaire.questions.get(position).type.options.indexOf(answer.answer);
//            viewHolder.multiple_radio_group.check(viewHolder.multiple_radio_group.getChildAt(answerIndex).getId());

        return convertView;
    }

    /**
     * This function creates a multiple choice question.
     * @param position
     * @param convertView
     * @param parent
     * @param question
     * @return
     */
    private View getMultipleConvertView(int position, View convertView, ViewGroup parent, Question question) {
        MultipleViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new MultipleViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(com.hilmarzech.mobileaat.R.layout.item_multiple, parent, false);
            viewHolder.multiple_text = (TextView) convertView.findViewById(com.hilmarzech.mobileaat.R.id.multiple_text);
            viewHolder.multiple_radio_group = (RadioGroup) convertView.findViewById(com.hilmarzech.mobileaat.R.id.multiple_radio_group);
            viewHolder.multiple_radio_group.setOnCheckedChangeListener(getRadiogroupListener(Type.MULTIPLE_ID));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MultipleViewHolder) convertView.getTag();
        }
        viewHolder.multiple_text.setText(question.text);
        viewHolder.multiple_radio_group.setTag(position);
        for (int i=0; i<viewHolder.multiple_radio_group.getChildCount();i++) {
            RadioButton child = (RadioButton) viewHolder.multiple_radio_group.getChildAt(i);
            if (i<question.type.options.size()) {
                child.setVisibility(View.VISIBLE);
                child.setText(question.type.options.get(i));
            } else {
                child.setVisibility(View.GONE);
            }
        }
        Answer answer = questionnaire.answers.get(questionnaire.questions.get(position).id);
        if (!answer.has_been_answered()) {//.answer.equals("-1")) {
            viewHolder.multiple_radio_group.check(-1);
        } else {
            int answerIndex = questionnaire.questions.get(position).type.options.indexOf(answer.answer);
            viewHolder.multiple_radio_group.check(viewHolder.multiple_radio_group.getChildAt(answerIndex).getId());
        }
        return convertView;
    }

    /**
     * This function creates an instruction.
     * @param convertView
     * @param parent
     * @param question
     * @return
     */
    private View getInstructionConvertView(View convertView, ViewGroup parent, Question question) {
        InstructionViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new InstructionViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(com.hilmarzech.mobileaat.R.layout.item_instruction, parent, false);
            viewHolder.instruction_text = (WebView) convertView.findViewById(com.hilmarzech.mobileaat.R.id.instruction_text);
            viewHolder.instruction_text.setBackgroundColor(0x00000000);
            // Setting the font size
            WebSettings webSettings = viewHolder.instruction_text.getSettings();
            int font_size = (int) getContext().getResources().getDimension(com.hilmarzech.mobileaat.R.dimen.text_size);
            font_size = font_size/(int)getContext().getResources().getDisplayMetrics().scaledDensity;
            webSettings.setDefaultFontSize(font_size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (InstructionViewHolder) convertView.getTag();
        }

        String participantText = question.text.replace("PRIVATE_PARTICIPANT_ID",DatabaseHelper.getParticipantID(getContext()));
        participantText = participantText.replace("PARTICIPANT_ID",DatabaseHelper.getPublicParticipantID(getContext()));

        viewHolder.instruction_text.loadDataWithBaseURL("file:///android_res/raw/", participantText, "text/html", "UTF-8","");
        return convertView;
    }

    /**
     * This function creates a text input question.
     * @param position
     * @param convertView
     * @param parent
     * @param question
     * @return
     */
    private View getTextInputConvertView(int position, View convertView, ViewGroup parent, Question question) {
        TextInputViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new TextInputViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(com.hilmarzech.mobileaat.R.layout.item_text_input, parent, false);
            viewHolder.text_input_text = (TextView) convertView.findViewById(com.hilmarzech.mobileaat.R.id.text_input_text);
            viewHolder.text_input_edit_text = (EditText) convertView.findViewById(com.hilmarzech.mobileaat.R.id.text_input_edit_text);
            // When done is clicked, hide the keyboard and save the answer
            viewHolder.text_input_edit_text.setOnEditorActionListener( new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        callback.hideSoftKeyboard();
                        String answer_text = v.getText().toString();
                        questionAnswered(v,answer_text);
                        handled = true;
                    }
                    return handled;}});
            // When focus is lost, save the answer
            viewHolder.text_input_edit_text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        EditText editText = (EditText) v;
                        String answer_text = editText.getText().toString();
                        questionAnswered(editText,answer_text);}}});

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (TextInputViewHolder) convertView.getTag();
        }
        viewHolder.text_input_edit_text.setTag(position);
        viewHolder.text_input_text.setText(question.text);
        Log.w("Hilmar", "getTextInputConvertView: Here");
        toggleTextViewVisibility(viewHolder.text_input_text);
        return convertView;
    }

    /**
     * This function is a bit of a design hack.  It moves optional EditTexts closer to multiple choice questions when "other" option is used.
     * @param textView
     */
    private void toggleTextViewVisibility(TextView textView) {
        if (textView.getText().equals("")) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * A view holder to store information of the Likert view.
     */
    private static class LikertViewHolder {
        TextView likert_text;
        TextView likert_min_text;
        TextView likert_max_text;
        RadioGroup likert_radio_group;
    }

    /**
     * A view holder to store information of the MultipleChoice view.
     */
    private static class MultipleViewHolder {
        TextView multiple_text;
        RadioGroup multiple_radio_group;
    }

    /**
     * A view holder to store information of the MultipleChoice view.
     */
    private static class CheckViewHolder {
        TextView check_text;
        LinearLayout check_group;
    }


    /**
     * A view holder to store information of the Instruction view.
     */
    private static class InstructionViewHolder {
        WebView instruction_text;
    }

    /**
     * A view holder to store information of the TextInput view.
     */
    private static class TextInputViewHolder {
        TextView text_input_text;
        EditText text_input_edit_text;
    }

    /**
     * A callback to pass information to the QuestionnaireActivity.
     */
    public interface QuestionnaireCallback {
        void allQuestionsAnswered();
        void questionAnswered(Answer answer);
        void hideSoftKeyboard();
    }

    /**
     * A setter function for the callback that also checks whether all questions are answered.
     * @param callback
     */
    public void setCallback(QuestionnaireCallback callback) {
        this.callback = callback;
        checkAllQuestionsAnswered();
    }
}
