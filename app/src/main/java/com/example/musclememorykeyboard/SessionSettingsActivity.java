package com.example.musclememorykeyboard;

import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.musclememorykeyboard.databinding.ActivitySessionSettingsBinding;

public class SessionSettingsActivity extends AppCompatActivity {
    private EditText username, sessionName, phraseNumber;
    private RadioGroup typingGroup, orientationGroup;
    private RadioButton oneHand, twoThumb, cradle, portrait, landscape;

    private Spinner keyboard;
    private TypingMode interaction;
    private Orientation orientation;

    private boolean somethingChanged = false;
    private String currentUsername, currentSession;
    private Orientation currentOrientation;
    private TypingMode currentTypingMode;
    private int currentPhraseNumber, currentKeyboard;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_session_settings);
        username = findViewById(R.id.txtUsername);
        sessionName = findViewById(R.id.txtSessionName);
        keyboard = findViewById(R.id.spinner_Keyboard);
        phraseNumber = findViewById(R.id.txtNumberOfPhrases);

        typingGroup = findViewById(R.id.radioGroupInteraction);
        orientationGroup = findViewById(R.id.radioGroupOrientation);

        oneHand = findViewById(R.id.radioOneHand);
        twoThumb = findViewById(R.id.radioTwoThumbs);
        cradle = findViewById(R.id.radioCradle);
        portrait = findViewById(R.id.radioPortrait);
        landscape = findViewById(R.id.radioLandscape);

        interaction = TypingMode.TWO_THUMBS;
        orientation = Orientation.PORTRAIT;

        if(Session.isSet()){
            currentUsername = Session.getUser();
            currentSession = Session.getSessionID();
            currentKeyboard = Session.getKeyboard();
            currentPhraseNumber = Session.getPhraseCount();
            currentOrientation = Session.getOrientation();
            currentTypingMode = Session.getTypingMode();

            username.setText(currentUsername);
            sessionName.setText(currentSession);

            keyboard.setSelection(currentKeyboard);

            phraseNumber.setText(String.valueOf(currentPhraseNumber));

            switch(currentTypingMode.name()){
                case("TWO_THUMBS"): twoThumb.setChecked(true);
                    break;
                case("ONE_HAND"): oneHand.setChecked(true);
                    break;
                case("CRADLING"): cradle.setChecked(true);
                    break;
            }

            switch(currentOrientation.name()){
                case("PORTRAIT"): portrait.setChecked(true);
                    break;
                case("LANDSCAPE"): landscape.setChecked(true);
                    break;
            }
        }

        typingGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.radioOneHand:
                        interaction = TypingMode.ONE_HAND;
                        break;
                    case R.id.radioTwoThumbs:
                        interaction = TypingMode.TWO_THUMBS;
                        break;
                    case R.id.radioCradle:
                        interaction = TypingMode.CRADLING;
                        break;
                }
            }
        });

        orientationGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.radioPortrait:
                        orientation = Orientation.PORTRAIT;
                        break;
                    case R.id.radioLandscape:
                        orientation = Orientation.LANDSCAPE;
                        break;
                }
            }
        });

    }
    @Override
    public void onBackPressed() {
        String usernameString = username.getText().toString();
        String sessionNameString = sessionName.getText().toString();
        int numberOfPhrases;
        try{
            numberOfPhrases = Integer.valueOf(phraseNumber.getText().toString());
        }
        catch (Exception ex){
            numberOfPhrases = 30;
        }

        if(usernameString.isEmpty()){
            usernameString = "username";
        }
        if(sessionNameString.isEmpty()){
            sessionNameString = "session1";
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

        String finalUsernameString = usernameString;
        String finalSessionNameString = sessionNameString;
        int finalNumberOfPhrases = numberOfPhrases;

        builder.setMessage("Do you want to save your changes?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Session.setUser(finalUsernameString);
                Session.setSessionID(finalSessionNameString);
                Session.setKeyboard(keyboard.getSelectedItemPosition());
                Session.setPhraseCount(finalNumberOfPhrases);
                Session.setOrientation(orientation);
                Session.setTypingMode(interaction);

                Session.setSet(true);
                finish();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        }).setCancelable(false).show();

    }

}