package com.example.musclememorykeyboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver touchReceiver;
    private static final int ALPHABET_SIZE = 26;
    private FirebaseStorage storage;
    private ArrayList<String> phrases;
    private StorageReference storageReference;
    private MaterialButton phraseBtn;
    EditText textInput;
    private int phraseIndex;
    private TextView timeTV, userTV, phraseCountTV, testKeyboardTV, handlingTV, phraseResultTV, sessionTV;
    StringBuilder typedSentence = new StringBuilder();
    StringBuilder typedWord = new StringBuilder();
    static double keyWidth = 0d;
    static float keyHeight = 0f;
    double dpi = 0;
    String[] wordList = new String[29994];
    Double[] valueList = new Double[29994];
    ArrayList<CustomKey> keyList;
    double[][] keyDistances = new double[ALPHABET_SIZE][ALPHABET_SIZE];
    ArrayList<double[]> distanceMapList = new ArrayList<>();
    private String distanceSentence="", stringDistanceSentence="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phraseBtn = findViewById(R.id.phraseGenerateBtn);
        userTV = findViewById(R.id.userTV);
        testKeyboardTV = findViewById(R.id.keyboardTV);
        handlingTV = findViewById(R.id.handlingTV);
        timeTV = findViewById(R.id.timeTV);
        sessionTV = findViewById(R.id.sessionNameTV);
        phraseCountTV = findViewById(R.id.countTV);
        phraseResultTV = findViewById(R.id.phraseResultsTV);
        textInput = findViewById(R.id.transcribeET);
        textInput.setEnabled(false);

        Toolbar myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);
        myToolbar.showOverflowMenu();

        TextServicesManager textServicesManager = (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        double width = displayMetrics.widthPixels;
        dpi = displayMetrics.densityDpi;
        Toast.makeText(this, "The dpi is: "+dpi, Toast.LENGTH_SHORT).show();
        keyWidth = width /10;

        //float dip = 55f;
        Resources r = getResources();
        keyHeight = r.getDimensionPixelSize(R.dimen.key_height);
        double spaceHeight = keyHeight * 1.3;
        double height = 4*keyHeight;

        Context main = this;

        loadWordList();
        loadFrequencyList();
        phrases = loadPhraseList();

        textInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b && Session.isStarted()){
                    //Set phrases here
                    distanceMapList.clear();
                    Log.d("EDIT TEXT", "SELECTED");
                    Session.setStartTime(SystemClock.elapsedRealtime());
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
                        if (Session.isStarted()) {
                            String type = intent.getStringExtra("KeyType");
                            double x = intent.getDoubleExtra("x", 0);
                            double y = intent.getDoubleExtra("y", 0);
                            Log.d("TOUCH_BROADCAST", "TOUCH RECEIVED! X: " + x + " Y: " + y + "TYPE: " + type.toString());
                            switch (type) {
                                case CustomInputMethodService.KEY_OTHER:
                                    if (y >= height - keyHeight) {
                                        Log.d("TOUCH_BROADCAST", "BOTTOM ROW PRESSED");
                                        if(x> 2*keyWidth && x < width-2*keyWidth){
                                            Log.d("SPACEPRESSED", "SPACE PRESSED");
                                            if(distanceMapList.size() > 0){
                                                closestWord(false);
                                            }
                                            else{
                                                distanceSentence += " ";
                                                stringDistanceSentence += " ";
                                            }
                                            textInput.append(" ");

                                            Log.d("TOUCH_BROADCAST", "SPACE CUSTOM PRESSED");
                                        }
                                    } else if (y >= height - 2 * keyHeight && x <= 1.5 * keyWidth) {
                                        Log.d("TOUCH_BROADCAST", "CAPS PRESSED");
                                    } else if (y >= height - 2 * keyHeight && x >= width - 1.5 * keyWidth) {
                                        Log.d("TOUCH_BROADCAST", "DELETE PRESSED");
                                    } else {
                                        if (keyList != null)
                                            closestKey(keyList, x, y);
                                    }
                                    break;
                                case CustomInputMethodService.KEY_DELETE:
                                    //TODO think about what to do with delete
                                    /*if (!distanceMapList.isEmpty()) {
                                        distanceMapList.remove(distanceMapList.size() - 1);
                                    }
                                    if (typedSentence.length() > 0) {
                                        typedSentence.deleteCharAt(typedSentence.length() - 1);
                                    }
                                    if (typedWord.length() > 0) {
                                        typedWord.deleteCharAt(typedWord.length() - 1);
                                    }*/
                                    break;
                                case CustomInputMethodService.KEY_DONE:
                                    Session.setCurrentTime(SystemClock.elapsedRealtime());
                                    if(distanceMapList.size() > 0){
                                        closestWord(true);
                                    }
                                        nextPhrase();
                                        textInput.clearFocus();
                                        typedSentence.setLength(0);
                                        typedWord.setLength(0);
                                        distanceMapList.clear();
                                    //CALL word detection
                                    break;
                                case CustomInputMethodService.KEY_SPACE:
                                    break;
                                default:
                                    //DEFAULT CODE
                                    break;
                            }
                        }
                    }else if (intent.getAction().equals(CustomInputMethodService.KEYBOARD_OPENED)) {

                            keyList = intent.getParcelableArrayListExtra("CustomKeyList");
                            if(Session.getKeyboardLayout().equals(KeyboardLayout.QWERTZ)){
                                calculateKeyDistances(keyList);
                            }
                            else{
                                CustomKey zKey = null, yKey = null;
                                for (CustomKey key:
                                     keyList) {
                                    if(key.getLabel().toLowerCase().equals("y")){
                                        yKey = key;
                                    }
                                    else if(key.getLabel().toLowerCase().equals("z")){
                                        zKey = key;
                                    }
                                }
                                double tmpx = yKey.getX();
                                double tmpy = yKey.getY();
                                yKey.setY(zKey.getY());
                                yKey.setX(zKey.getX());
                                zKey.setY(tmpy);
                                zKey.setX(tmpx);
                            }
                            Log.d("KEYBOARD_OPEN_BROADCAST", keyList.toString());
                            Log.d("KEYBOARD_OPEN_BROADCAST", "RECEIVED!");
                        }
                    }
                }

        };

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    public void nextPhrase() {
        Session.setCurrentPhraseCount(Session.getCurrentPhraseCount()+1);
        Session.setTargetPhrase(phraseBtn.getText().toString());
        Session.setRawPhrase(textInput.getText().toString());
        Session.setDistancePhrase(distanceSentence);
        Session.setStringDistancePhrase(stringDistanceSentence);

        Session.calculateErrors(phraseBtn.getText().toString(), textInput.getText().toString(), "none");
        Session.calculateErrors(phraseBtn.getText().toString(), distanceSentence, "distance");
        Session.calculateErrors(phraseBtn.getText().toString(), stringDistanceSentence, "stringDistance");
        Logger.writeToCSV(this);
        phraseCountTV.setText(R.string.phrase_count);
        phraseCountTV.append(" " + Session.getCurrentPhraseCount() + "/" + Session.getPhraseCount());

        timeTV.setText(R.string.time);
        timeTV.append(" " + Session.getTime());


        //TODO: give this to session class to handle

        String currentInfo = "Time: " + Session.getTime() + "\n"
                + "Phrase given: " + phraseBtn.getText() + "\n"
                + "Raw transcribed: " + textInput.getText() + "\n"
                + statsToString(Session.getCurrentPlainStats(Session.getCurrentPhraseCount()-1)).toString() + "\n"
                + "Distance transcribed: " + distanceSentence + "\n"
                + statsToString(Session.getCurrentDistanceStats(Session.getCurrentPhraseCount()-1)).toString() +"\n"
                + "String distance transcribed: " + stringDistanceSentence + "\n"
                + statsToString(Session.getCurrentStringDistanceStats(Session.getCurrentPhraseCount()-1)).toString();

        phraseResultTV.setText(currentInfo);

        Log.d("INPUTEDPHRASE", "Plain: |" + textInput.getText().toString() +"|");
        Log.d("INPUTEDPHRASE", "Distance: |" + distanceSentence+"|");
        Log.d("INPUTEDPHRASE", "String distance: " + stringDistanceSentence+"|");

        textInput.setText("");
        distanceSentence = "";
        stringDistanceSentence = "";

        Random random = new Random();
        phraseIndex = random.nextInt(phrases.size());
        phraseBtn.setText(phrases.get(phraseIndex));
        phrases.remove(phrases.get(phraseIndex));

        //Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = MainActivity.this.getCurrentFocus();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        //Check if done
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!Session.isStarted()) {
                //KeyboardLogger.readTest(getApplicationContext(), session);
                textInput.setEnabled(false);
                phraseBtn.setText("New session not yet started");
                Toast.makeText(this, "You have successfully finished with this session!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public String statsToString(HashMap<String, Double> stats){
        DecimalFormat df = new DecimalFormat("0.00");

        StringBuilder rawBuilder = new StringBuilder();
        for (Map.Entry<String,Double> entry: stats.entrySet()
        ) {rawBuilder.append(entry.getKey()).append(" = ").append(df.format(entry.getValue())).append(", ");
        }
        if (rawBuilder.length() > 2){
            rawBuilder.delete(rawBuilder.length()-2, rawBuilder.length());
        }
        return rawBuilder.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String TAG = "TOOLBAR MENU";
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        switch (item.getItemId()) {
            case R.id.logSettings:
                Log.d(TAG, "onOptionsItemSelected: LOG SETTINGS");
                if(!Session.isStarted()){
                    Logger.uploadLog(getApplicationContext(), storageReference, this);
                }
                else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Session in progress")
                            .setMessage("Cannot upload log while session is in progress")
                            .setPositiveButton("OK", null);
                    builder.show();
                    }
                return true;

            case R.id.sessionSettings:
                if(!Session.isStarted()) {
                    Intent myIntent = new Intent(MainActivity.this, SessionSettingsActivity.class);
                    MainActivity.this.startActivity(myIntent);
                    Log.d(TAG, "onOptionsItemSelected: SESSION SETTINGS");
                }
                else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Session in progress")
                            .setMessage("Cannot open session settings while session is in progress")
                            .setPositiveButton("OK", null);
                    builder.show();
                }
                return true;

            case R.id.initTestSession:
                Log.d(TAG, "onOptionsItemSelected: SESSION INIT");
                initSessionConfirm();
                return true;

            case R.id.bringUp:
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startNewSession(){
        phrases = loadPhraseList();
        Session.setStarted(true);
        textInput.clearFocus();
        //Resetting everything
        textInput.setText("");

        userTV.setText("User: "+Session.getUser());
        sessionTV.setText("Session Name: "+Session.getSessionID());
        phraseCountTV.setText("Phrase count: " + "0/" + Session.getPhraseCount());
        handlingTV.setText("Handling: " + Session.getTypingMode().toString());
        testKeyboardTV.setText("Tested keyboard: " + Session.getKeyboardName());

        Random random = new Random();
        phraseIndex = random.nextInt(phrases.size());
        phraseBtn.setText(phrases.get(phraseIndex));
        phrases.remove(phrases.get(phraseIndex));

        Session.setCurrentPhraseCount(0);
        distanceMapList.clear();
        typedSentence.setLength(0);
        typedWord.setLength(0);
        textInput.setEnabled(true);
        phraseResultTV.setText("");

        Logger.setFirst(true);
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
        textInput.append(letter);
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

    public ArrayList<String> loadPhraseList(){
        BufferedReader reader;
        String filename = "phrases.txt";
        ArrayList<String> phraseList = new ArrayList<>();
        try{
            InputStream file = getAssets().open(filename);
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while(line != null){
                phraseList.add(line.toLowerCase());
                line = reader.readLine();
            }
        } catch (IOException ioe){
            Log.d("READING WORDLIST", "Failure");
            ioe.printStackTrace();
        }
        Log.d("READING WORDLIST", "Success");
    return phraseList;
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

    public void closestWord(boolean last){
        TreeMap<Double, String> wordsSorted= new TreeMap<>();
        Log.d("WORD LENGTH", String.valueOf(distanceMapList.size()));
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
            distanceSentence += wordsSorted.get(wordsSorted.lastKey());
            stringDistanceSentence += closestLenWord;
            //If the function was called by space press, add a space, otherwise it was called by done and no space is needed
            if(!last){
                distanceSentence += " ";
                stringDistanceSentence += " ";
            }


            //textInput.append(wordsSorted.get(wordsSorted.lastKey()));
            //textInput.append(closestLenWord);
            Log.d("CLOSEST WORD LEV DIST", closestLenWord + " Distance: " + minLenDist);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        //return "Test";
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
            //Setting the value to a interval between 0.5 and 2
            double weight = min_val + (max_val - min_val) * (1 - gaussianDist(keyDistance) * normalis);
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

    public void initSessionConfirm() {
        //Alert box to confirm new session since phrase count, and such are reset
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.init_message)
                .setTitle(R.string.init_title)
                .setCancelable(false);

        builder.setPositiveButton(R.string.OK, (dialogInterface, i) -> {
        startNewSession();
        });

        builder.setNegativeButton(R.string.cancel, ((dialogInterface, i) -> {
        }));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}