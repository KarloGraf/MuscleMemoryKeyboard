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
    StringBuilder sb = new StringBuilder();
    boolean started = false;
    static double keyWidth = 0d;
    static float keyHeight = 0f;
    double dpi = 0;
    String[] wordList = new String[10000];
    Double[] valueList = new Double[10000];
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
        dpi = displayMetrics.densityDpi;
        Toast.makeText(this, "The dpi is: "+dpi, Toast.LENGTH_SHORT).show();
        keyWidth = width /10;
        String miss = "misspeled";

        //float dip = 55f;
        Resources r = getResources();
        keyHeight = r.getDimensionPixelSize(R.dimen.key_height);
        double height = 4*keyHeight;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button upload = findViewById(R.id.buttonUpload);
        Button start = findViewById(R.id.buttonStart);
        start.setOnClickListener(view -> {
            if(!started){
                start.setText("STOP");
            }
            else{
                start.setText("START");
            }
            started = !started;

            //Resetting everything
            touchPoints.clear();
            distanceMapList.clear();
            sb.setLength(0);
            try {
                Log.d("SPELLCHECK SUPPORTED:" ,String.valueOf(isSentenceSpellCheckSupported()));
                TextInfo[] txt = new TextInfo[1];
                txt[0] = new TextInfo("Dogs do not liek to siwm");
                spellCheckerSession.getSentenceSuggestions(txt, 3);
            }
            catch (Exception ex){
                ex.printStackTrace();
            }

        });
        Context main = this;

        loadWordList();
        loadFrequencyList();

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
                        if (started) {
                            String type = intent.getStringExtra("KeyType");
                            double x = intent.getDoubleExtra("x", 0);
                            double y = intent.getDoubleExtra("y", 0);
                            Log.d("TOUCH_BROADCAST", "TOUCH RECEIVED! X: " + x + " Y: " + y + "TYPE: " + type.toString());
                            switch (type) {
                                case CustomInputMethodService.KEY_OTHER:
                                    if (y >= height - keyHeight) {
                                        Log.d("TOUCH_BROADCAST", "BOTTOM ROW PRESSED");
                                    } else if (y >= height - 2 * keyHeight && x <= 1.5 * keyWidth) {
                                        Log.d("TOUCH_BROADCAST", "CAPS PRESSED");
                                    } else if (y >= height - 2 * keyHeight && x >= width - 1.5 * keyWidth) {
                                        Log.d("TOUCH_BROADCAST", "DELETE PRESSED");
                                    } else {
                                        touchPoints.add(String.valueOf(x) + " , " + String.valueOf(y));
                                        if (keyList != null)
                                            closestKey(keyList, x, y);
                                    }
                                    break;
                                case CustomInputMethodService.KEY_DELETE:
                                    if (!touchPoints.isEmpty()) {
                                        touchPoints.remove(touchPoints.size() - 1);
                                    }
                                    if (!distanceMapList.isEmpty()) {
                                        distanceMapList.remove(distanceMapList.size() - 1);
                                    }
                                    if (sb.length() > 0) {
                                        sb.deleteCharAt(sb.length() - 1);
                                    }
                                    break;
                                case CustomInputMethodService.KEY_DONE:
                                    if (touchPoints.size() > 0) {
                                        TextInfo[] txt = new TextInfo[1];
                                        txt[0] = new TextInfo(sb.toString());
                                        spellCheckerSession.getSentenceSuggestions(txt, 3);
                                        closestWord();
                                    } else {
                                        sb.setLength(0);
                                        touchPoints.clear();
                                        distanceMapList.clear();
                                    }
                                    //CALL word detection
                                    break;
                                case CustomInputMethodService.KEY_SPACE:
                                    closestWord();
                                    touchPoints.add("SPACE");
                                    break;
                                default:
                                    //DEFAULT CODE
                                    break;
                            }
                        }
                    }else if (intent.getAction().equals(CustomInputMethodService.KEYBOARD_OPENED)) {

                            keyList = intent.getParcelableArrayListExtra("CustomKeyList");
                            Log.d("KEYBOARD_OPEN_BROADCAST", keyList.toString());
                            Log.d("KEYBOARD_OPEN_BROADCAST", "RECEIVED!");
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
        String letter = "";
        double highest = 0;
        double sum = 0;
        for (CustomKey key:
             keys) {
            double val = gaussianDist(key.distanceFrom(x,y));
            sum += val;
            if(val > highest){
                highest = val;
                letter = key.getLabel();
            }
            distanceLabelMap.put(key.getLabel(), val);
        }
        //Normalization
        HashMap <String, Double> normalizedMap = new HashMap<>();
        for (Map.Entry<String,Double> key:
             distanceLabelMap.entrySet()) {
            double normal = key.getValue()/sum;
            normalizedMap.put(key.getKey(), normal);
        }
        Log.d("NORMALIZED VALUES:", normalizedMap.toString());
        distanceMapList.add(normalizedMap);
        sb.append(letter);
        Log.d("STRING BUILDER APPENDED: ", sb.toString());
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

    public void loadFrequencyList(){
        BufferedReader reader;
        String filename = "valuesOnly.txt";
        int i = 0;
        try{
            InputStream file = getAssets().open(filename);
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while(line != null){
                Double value = Double.valueOf(line);
                valueList[i] = value;
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
        double var_in_mm = 6;
        double variance = var_in_mm * 0.03937 * dpi;
        double result;
        result = 1/(variance*Math.sqrt(2*Math.PI))*Math.pow(Math.E,-0.5*Math.pow((dist-mean)/variance,2));
        return result;
    }

    public void closestWord(){
        TreeMap<Double, String> wordsSorted= new TreeMap<>();
        Log.d("WORD LENGTH", String.valueOf(distanceMapList.size()));
        double maxVal = 23135851162D;
        double weight = 0.2;
        int counter = 0;
        int wordLength = distanceMapList.size();
        try {
            for (String tmpWord :
                    wordList) {

                double totalDist = 0;
                if (tmpWord != null && tmpWord.length() == wordLength) {
                    //Log.d("WORDS", tmpWord);
                    for (int i = 0; i < distanceMapList.size(); i++) {
                        totalDist += distanceMapList.get(i).get(String.valueOf(tmpWord.charAt(i)));
                    }
                    totalDist += (valueList[counter]/maxVal) * weight * wordLength;
                    wordsSorted.put(totalDist, tmpWord);
                }
                counter++;
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
        Log.d("SUGGESTIONS","CALLED");
        sb.setLength(0);
        if(suggestionsInfos != null){
            Log.d("SUGGESTIONS","NOT NULL");
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
        Log.d("SENTENCE SUGGESTIONS","CALLED");
        sb.setLength(0);
        if(sentenceSuggestionsInfos != null){
            Log.d("SENTENCE SUGGESTIONS","NOT NULL");
            for (SentenceSuggestionsInfo info : sentenceSuggestionsInfos){
                int sugCount = info.getSuggestionsCount();
                Log.d("SENTENCE SUGGESTIONS","COUNT " + String.valueOf(sugCount));
                for (int i = 0; i<sugCount;i++){
                    SuggestionsInfo sugInfo = info.getSuggestionsInfoAt(i);
                    Log.d("SUGGESTIONS","COUNT " + String.valueOf(sugInfo.getSuggestionsCount()));
                    for(int j = 0; j < sugInfo.getSuggestionsCount(); j++){
                        String sug = sugInfo.getSuggestionAt(j);
                        Log.d("SUGGESTIONS: ", sug);
                    }
                }
            }
        }
    }

    private boolean isSentenceSpellCheckSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
}