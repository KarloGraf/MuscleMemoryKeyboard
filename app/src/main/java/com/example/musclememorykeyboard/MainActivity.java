package com.example.musclememorykeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.inputmethod.TextSnapshot;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements SpellCheckerSession.SpellCheckerSessionListener {

    private BroadcastReceiver touchReceiver;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    EditText textInput;
    boolean started = false;
    static double keyWidth = 0d;
    static float keyHeight = 0f;
    String[] wordList = new String[10000];
    ArrayList<CustomKey> keyList;
    ArrayList<String> touchPoints = new ArrayList<>();
    ArrayList<HashMap<String,Double>> distanceMapList= new ArrayList<>();
    String[] fourLetter = {"frog", "cake", "make", "lake", "rake", "read", "glow", "book", "rock", "dock", "four", "play", "game", "star", "door"};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TextServicesManager textServicesManager = (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);

        Log.d("SPELLCHECKER SERVICE", String.valueOf(textServicesManager.isSpellCheckerEnabled()));
        SpellCheckerSession spellCheckerSession = textServicesManager.newSpellCheckerSession(null,Locale.ENGLISH,this ,false);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        double width = displayMetrics.widthPixels;
        double dpi = displayMetrics.densityDpi;
        Toast.makeText(this, "The dpi is: "+dpi, Toast.LENGTH_SHORT).show();
        keyWidth = width /10;
        String miss = "misspeled";

        //float dip = 55f;
        Resources r = getResources();
        keyHeight = r.getDimensionPixelSize(R.dimen.key_height);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button upload = findViewById(R.id.buttonUpload);
        Button start = findViewById(R.id.buttonStart);
        start.setOnClickListener(view -> {
            if(started){
                start.setText("STOP");
            }
            else{
                start.setText("START");
            }
            started = !started;
            try {
                Log.d("SPELLCHECK SUPPORTED: " ,String.valueOf(isSentenceSpellCheckSupported()));
                spellCheckerSession.getSuggestions(new TextInfo(miss), 3);
            }
            catch (Exception ex){
                ex.printStackTrace();
            }

        });
        Context main = this;

        loadWordList();

        textInput = findViewById(R.id.editTextPhrase);
        textInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    distanceMapList.clear();
                    touchPoints.clear();
                    Log.d("EDIT TEXT", "SELECTED");
                }
            }
        });

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        touchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null) {
                    if (intent.getAction().equals(CustomInputMethodService.KEYBOARD_TOUCH)) {
                        TouchTypes type = TouchTypes.valueOf(intent.getStringExtra("KeyType"));
                        double x = intent.getDoubleExtra("x", 0);
                        double y = intent.getDoubleExtra("y", 0);
                        Log.d("TOUCH_BROADCAST", "TOUCH RECEIVED! X: " + x + " Y: " + y + "TYPE: " + type.toString());
                        if (type == TouchTypes.DEFAULT) {
                            touchPoints.add(String.valueOf(x) + " , " + String.valueOf(y));
                            if (keyList != null)
                                closestKey(keyList,x,y);
                        } else if (type == TouchTypes.DELETE) {
                            if (!touchPoints.isEmpty()) {
                                touchPoints.remove(touchPoints.size() - 1);
                                touchPoints.remove(touchPoints.size() - 1);
                            }
                        } else if(type == TouchTypes.OTHER){
                            touchPoints.remove(touchPoints.size() - 1);
                            distanceMapList.remove(distanceMapList.size()-1);
                            if(touchPoints.size() == 4){
                                closestWord();
                            }
                            touchPoints.clear();
                            //CALL word detection
                        }
                        else {
                            touchPoints.remove(touchPoints.size() - 1);
                            touchPoints.add("SPACE");
                        }
                    }
                    else if(intent.getAction().equals(CustomInputMethodService.KEYBOARD_OPENED)){

                        keyList = intent.getParcelableArrayListExtra("CustomKeyList");
                        Log.d("KEYBOARD_OPEN_BROADCAST", keyList.toString());
                        Log.d("KEYBOARD_OPEN_BROADCAST","RECEIVED!");
                    }
                }

            }
        };


        upload.setOnClickListener(view -> {
            Logger.writeLog(getApplicationContext(), touchPoints);
            Logger.uploadLog(getApplicationContext(), storageReference, this);
            touchPoints.clear();

        });

    }

    public static double getKeyWidth(){
        return keyWidth;
    }

    public static float getKeyHeight(){
        return keyHeight;
    }

    @Override
    protected void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter(CustomInputMethodService.KEYBOARD_TOUCH);
        filter.addAction(CustomInputMethodService.KEYBOARD_OPENED);
        registerReceiver(touchReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(touchReceiver);
    }

    public void closestKey(List<CustomKey> keys, double x, double y){
        HashMap <String, Double> distanceLabelMap = new HashMap<>();
        for (CustomKey key:
             keys) {
            distanceLabelMap.put(key.getLabel(), gaussianDist(key.distanceFrom(x,y)));
        }
        distanceMapList.add(distanceLabelMap);
        /*Log.d("CLOSEST KEY: ", distanceLabelMap.get(distanceLabelMap.lastKey()) + "  VALUE: " + distanceLabelMap.lastKey().toString());
        Log.d("CLOSEST KEY:", distanceLabelMap.toString());*/

    }

    public void loadWordList(){
        BufferedReader reader;
        String filename = "google-10000-english.txt";
        int i = 0;
        try{
            InputStream file = getAssets().open(filename);
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while(line != null){
                wordList[i] = line;
                i++;
                line = reader.readLine();
            }
        } catch (IOException ioe){
            Log.d("READING WORDLIST", "Failure");
            ioe.printStackTrace();
        }
        Log.d("READING WORDLIST", "Success");

    }

    public double gaussianDist(double dist){
        double mean = 0;
        double var_in_mm = 8;
        double variance = var_in_mm * 0.03937 * keyHeight;
        double result;
        result = 1/(variance*Math.sqrt(2*Math.PI))*Math.pow(Math.E,-0.5*Math.pow((dist-mean)/variance,2));
        return result;
    }

    public void closestWord(){
        TreeMap<Double, String> wordsSorted= new TreeMap<>();
        try {
            for (String tmpWord :
                    wordList) {

                double totalDist = 0;
                if (tmpWord != null && tmpWord.length() == distanceMapList.size()) {
                    Log.d("WORDS", tmpWord);
                    for (int i = 0; i < distanceMapList.size(); i++) {
                        totalDist += distanceMapList.get(i).get(String.valueOf(tmpWord.charAt(i)));
                    }
                    wordsSorted.put(totalDist, tmpWord);
                }
            }
            distanceMapList.clear();
            Log.d("CLOSEST WORD", wordsSorted.get(wordsSorted.lastKey()));
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        //return "Test";
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] suggestionsInfos) {
        if(suggestionsInfos != null){
            for (SuggestionsInfo info : suggestionsInfos){
                int sugCount = info.getSuggestionsCount();
                for (int i = 0; i<sugCount;i++){
                    String sug = info.getSuggestionAt(i);
                    Log.d("SUGGESTIONS: ", sug);
                }
            }
        }

    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfos) {

    }

    private boolean isSentenceSpellCheckSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
}