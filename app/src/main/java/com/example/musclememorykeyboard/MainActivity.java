package com.example.musclememorykeyboard;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.Debug;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements SpellCheckerSession.SpellCheckerSessionListener {

    private BroadcastReceiver touchReceiver;
    private static final int ALPHABET_SIZE = 26;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    EditText textInput;
    TextView stringDistText;
    TextView gaussDistText;
    StringBuilder typedSentence = new StringBuilder();
    StringBuilder typedWord = new StringBuilder();
    boolean started = false;
    static double keyWidth = 0d;
    static float keyHeight = 0f;
    double dpi = 0;
    String[] wordList = new String[30000];
    Double[] valueList = new Double[30000];
    ArrayList<CustomKey> keyList;
    ArrayList<String> touchPoints = new ArrayList<>();
    double[][] keyDistances = new double[ALPHABET_SIZE][ALPHABET_SIZE];
    ArrayList<double[]> distanceMapList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);
        myToolbar.showOverflowMenu();

        TextServicesManager textServicesManager = (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);

        //Log.d("SPELLCHECKER SERVICE", String.valueOf(textServicesManager.isSpellCheckerEnabled()));
        SpellCheckerSession spellCheckerSession = textServicesManager.newSpellCheckerSession(null,Locale.ENGLISH,this ,false);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        double width = displayMetrics.widthPixels;
        dpi = displayMetrics.densityDpi;
        //Toast.makeText(this, "The dpi is: "+dpi, Toast.LENGTH_SHORT).show();
        keyWidth = width /10;
        String miss = "misspeled";

        //float dip = 55f;
        Resources r = getResources();
        keyHeight = r.getDimensionPixelSize(R.dimen.key_height);
        double height = 4*keyHeight;

        Button upload = findViewById(R.id.buttonUpload);
        Button start = findViewById(R.id.buttonStart);

        gaussDistText = findViewById(R.id.gaussText);
        stringDistText = findViewById(R.id.lichText);

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
            typedSentence.setLength(0);
            typedWord.setLength(0);
            try {
                Log.d("SPELLCHECK SUPPORTED:" ,String.valueOf(isSentenceSpellCheckSupported()));
                TextInfo[] txt = new TextInfo[1];
                txt[0] = new TextInfo("Dogs do not liek to siwm");
                //spellCheckerSession.getSentenceSuggestions(txt, 3);
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
                                    if (typedSentence.length() > 0) {
                                        typedSentence.deleteCharAt(typedSentence.length() - 1);
                                    }
                                    if (typedWord.length() > 0) {
                                        typedWord.deleteCharAt(typedWord.length() - 1);
                                    }
                                    break;
                                case CustomInputMethodService.KEY_DONE:
                                    if (touchPoints.size() > 0) {
                                        TextInfo[] txt = new TextInfo[1];
                                        txt[0] = new TextInfo(typedSentence.toString());
                                        spellCheckerSession.getSentenceSuggestions(txt, 3);
                                        closestWord();
                                    } else {
                                        typedSentence.setLength(0);
                                        typedWord.setLength(0);
                                        touchPoints.clear();
                                        distanceMapList.clear();
                                    }
                                    //CALL word detection
                                    break;
                                case CustomInputMethodService.KEY_SPACE:
                                    if(touchPoints.size() > 0){
                                        closestWord();
                                        touchPoints.add("SPACE");
                                    }
                                    break;
                                default:
                                    //DEFAULT CODE
                                    break;
                            }
                        }
                    }else if (intent.getAction().equals(CustomInputMethodService.KEYBOARD_OPENED)) {

                            keyList = intent.getParcelableArrayListExtra("CustomKeyList");
                            calculateKeyDistances(keyList);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String TAG = "TOOLBAR MENU";
        switch (item.getItemId()) {
            case R.id.logSettings:
                Log.d(TAG, "onOptionsItemSelected: LOG SETTINGS");
                return true;

            case R.id.sessionSettings:
                Intent myIntent = new Intent(MainActivity.this, SessionSettingsActivity.class);
                MainActivity.this.startActivity(myIntent);
                Log.d(TAG, "onOptionsItemSelected: SESSION SETTINGS");
                return true;

            case R.id.initTestSession:
                Log.d(TAG, "onOptionsItemSelected: SESSION INIT");
                return true;

            case R.id.bringUp:
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            default:
                return super.onOptionsItemSelected(item);
        }
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
        double[] tmpList = new double[ALPHABET_SIZE];
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
            Log.d("HIGHEST GAUSS VALUE", String.valueOf(val) + " " + key.getLabel());
            tmpList[key.getLabel().charAt(0) - 'a'] = val;
        }
        for(int i = 0; i < ALPHABET_SIZE; i++){
            tmpList[i] = tmpList[i]/sum;
        }
        distanceMapList.add(tmpList);
        typedSentence.append(letter);
        typedWord.append(letter);
        Log.d("STRING BUILDER SENTENCE APPENDED: ", typedSentence.toString());
        Log.d("STRING BUILDER WORD APPENDED: ", typedSentence.toString());
    }

    public void loadWordList(){
        BufferedReader reader;
        String filename = "30kFixed.txt";
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
        String filename = "30kValuesOnly.txt";
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
            Log.d("READING FREQLIST", "Failure");
            ioe.printStackTrace();
        }
        Log.d("READING FREQLIST", "Success");

    }

    public double gaussianDist(double dist){
        double mean = 0;
        double var_in_mm = 7;
        double variance = var_in_mm * 0.03937 * dpi;
        //Log.d("Variance", String.valueOf(variance));
        double result;
        result = (1/(variance*Math.sqrt(2*Math.PI)))*Math.pow(Math.E,-Math.pow((dist-mean),2)/(2*Math.pow(variance,2)));
        return result;
    }

    public void closestWord(){
        TreeMap<Double, String> wordsSorted= new TreeMap<>();
        Log.d("WORD LENGTH", String.valueOf(distanceMapList.size()));
        double maxVal = 23135851162D;
        double maxLog = 10.3642;
        double minLog = 5.901;
        double weight = 0.2;
        int counter = 0;
        double minLenDist = 9999;
        double wordFreq = 0;
        int wordLength = distanceMapList.size();
        String currentWord = typedWord.toString();
        String closestLenWord = "";

        typedWord.setLength(0);
        try {
            for (String tmpWord :
                    wordList) {

                double totalDist = 0;
                if(!(Math.abs(tmpWord.length() - wordLength) >= 1)) {
                    double tmpLevDist = calculateStringDistance(currentWord, tmpWord);
                    if (tmpLevDist < minLenDist) {
                        minLenDist = tmpLevDist;
                        closestLenWord = tmpWord;
                        wordFreq = valueList[counter];
                    }
                    //
                    /*else if (tmpLevDist == minLenDist) {
                        if (wordFreq < valueList[counter]) {
                            minLenDist = tmpLevDist;
                            closestLenWord = tmpWord;
                            wordFreq = valueList[counter];
                        }
                    }*/
                }
                if (tmpWord != null && tmpWord.length() == wordLength) {
                    //Log.d("WORDS", tmpWord);
                    for (int i = 0; i < distanceMapList.size(); i++) {
                        totalDist += distanceMapList.get(i)[(tmpWord.charAt(i))-'a'];
                    }
                    totalDist *= 1 + ((Math.log(valueList[counter]) - minLog)/(maxLog - minLog) * weight);
                    wordsSorted.put(totalDist, tmpWord);
                }
                counter++;
            }
            distanceMapList.clear();
            Log.d("CLOSEST WORD", wordsSorted.get(wordsSorted.lastKey()));
            gaussDistText.setText(wordsSorted.get(wordsSorted.lastKey()));
            Log.d("CLOSEST WORD LEV DIST", closestLenWord + " Distance: " + minLenDist);
            stringDistText.setText(closestLenWord);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        //return "Test";
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] suggestionsInfos) {
        Log.d("SUGGESTIONS","CALLED");
        /*typedSentence.setLength(0);
        if(suggestionsInfos != null){
            Log.d("SUGGESTIONS","NOT NULL");
            for (SuggestionsInfo info : suggestionsInfos){
                int sugCount = info.getSuggestionsCount();
                for (int i = 0; i<sugCount;i++){
                    String sug = info.getSuggestionAt(i);
                    Log.d("SUGGESTIONS: ", sug);
                }
            }
        }*/

    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfos) {
        Log.d("SENTENCE SUGGESTIONS","CALLED");
        typedSentence.setLength(0);
        if(sentenceSuggestionsInfos != null){
            Log.d("SENTENCE SUGGESTIONS","NOT NULL");
            for (SentenceSuggestionsInfo info : sentenceSuggestionsInfos){
                int sentenceCount = info.getSuggestionsCount();
                Log.d("SENTENCE SUGGESTIONS","COUNT " + String.valueOf(sentenceCount));
                for (int i = 0; i<sentenceCount;i++){
                    SuggestionsInfo sugInfo = info.getSuggestionsInfoAt(i);
                    int sugCount = sugInfo.getSuggestionsCount();
                    Log.d("SUGGESTIONS","COUNT " + String.valueOf(sugCount));
                    for(int j = 0; j < sugCount; j++){
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

    public int costOfSubstitutionSimple(char a, char b) {
        return a == b ? 0 : 1;
    }

    public double costOfSubstitutionComplex(char a, char b) {
        if(a == b) {
            return 0;
        }
        else {
            double min_val = 0.5;
            double max_val = 2;
            double keyDistance = keyDistances[a - 'a'][b - 'a'];
            double normalis = (max_val - min_val) / gaussianDist(0);
            //Log.d("normalizer", String.valueOf(normalis));
            double weight = min_val + (max_val - min_val) * (1 - gaussianDist(keyDistance) * normalis);
            //Log.d("Distance of substitution", String.valueOf(keyDistance));
            return weight;
        }
    }

    public double calculateStringDistance(String x, String y) {
        int xLen = x.length();
        int yLen = y.length();
        double[][] dp = new double[xLen + 1][yLen + 1];

        for (int i = 0; i <= xLen; i++) {
            for (int j = 0; j <= yLen; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1]
                                    + this.costOfSubstitutionComplex(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1),
                            dp[i][j - 1] + 1);
                    }
                /*if(dp[i][j] > 5){
                    return 9999;
                }*/
            }
        }

        return dp[xLen][yLen];
    }

    public void calculateKeyDistances(ArrayList<CustomKey> keys){
        Log.d("ALLKEYDISTANCES", "Method called");
        int keyNumber = keys.size();
        for(int i = 0; i < keyNumber; i++)
        {
            CustomKey firstKey = keys.get(i);
            for (int j = 0; j<keyNumber; j++){
                CustomKey secondKey = keys.get(j);
                double dist = firstKey.distanceFrom(secondKey.getX(), secondKey.getY());
                keyDistances[firstKey.getLabel().charAt(0) - 'a'][secondKey.getLabel().charAt(0) - 'a'] = dist;
            }
        }

        Log.d("ALLKEYDISTANCES", Arrays.deepToString(keyDistances));

    }
}