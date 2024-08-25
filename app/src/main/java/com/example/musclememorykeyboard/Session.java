package com.example.musclememorykeyboard;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class Session {


    public static String getKeyboardName() {
        return keyboardName;
    }

    public static void setKeyboardName(String keyboardName) {
        Session.keyboardName = keyboardName;
    }
    private static String user = "username", sessionID = "session1", keyboardName = "Invisible keys", targetPhrase = "",
    rawPhrase = "", distancePhrase = "", stringDistancePhrase = "";

    public static String getTargetPhrase() {
        return targetPhrase;
    }

    public static void setTargetPhrase(String targetPhrase) {
        Session.targetPhrase = targetPhrase;
    }

    public static String getRawPhrase() {
        return rawPhrase;
    }

    public static void setRawPhrase(String rawPhrase) {
        Session.rawPhrase = rawPhrase;
    }

    public static String getDistancePhrase() {
        return distancePhrase;
    }

    public static void setDistancePhrase(String distancePhrase) {
        Session.distancePhrase = distancePhrase;
    }

    public static String getStringDistancePhrase() {
        return stringDistancePhrase;
    }

    public static void setStringDistancePhrase(String stringDistancePhrase) {
        Session.stringDistancePhrase = stringDistancePhrase;
    }
    public static void clearStats(){
        stringDistanceStats.clear();
        distanceStats.clear();
        plainStats.clear();
    }

    private static int phraseCount = 50, keyboard = 0;
    private static long startTime = 0, currentTime = 0;
    private static Orientation orientation = Orientation.PORTRAIT;
    private static TypingMode typingMode = TypingMode.TWO_THUMBS;
    private static KeyboardLayout keyboardLayout = KeyboardLayout.QWERTZ;
    private static boolean set = false;
    private static boolean started = false;

    public static KeyboardLayout getKeyboardLayout() {
        return keyboardLayout;
    }

    public static void setKeyboardLayout(KeyboardLayout keyboardLayout) {
        Session.keyboardLayout = keyboardLayout;
    }

    private static ArrayList<HashMap<String, Double>> distanceStats = new ArrayList<HashMap<String, Double>>();
    private static ArrayList<HashMap<String, Double>> stringDistanceStats = new ArrayList<HashMap<String, Double>>();
    private static ArrayList<HashMap<String, Double>> plainStats = new ArrayList<HashMap<String, Double>>();

    public static ArrayList<HashMap<String, Double>> getDistanceStats() {
        return distanceStats;
    }

    public static ArrayList<HashMap<String, Double>> getStringDistanceStats() {
        return stringDistanceStats;
    }

    public static ArrayList<HashMap<String, Double>> getPlainStats() {
        return plainStats;
    }

    public static HashMap<String, Double> getCurrentPlainStats(int index) {
        return plainStats.get(index);
    }

    public static HashMap<String, Double> getCurrentDistanceStats(int index) {
        return distanceStats.get(index);
    }

    public static HashMap<String, Double> getCurrentStringDistanceStats(int index) {
        return stringDistanceStats.get(index);
    }

    public static void calculateErrors(String targetPhrase, String writtenPhrase, String alg) {
        Log.d("ERRORS:", "|" + writtenPhrase + "|" + "targetPhrase:|" + targetPhrase +"|");

        HashMap<String, Double> currentStats = new HashMap<>();

        int m = targetPhrase.length();
        int n = writtenPhrase.length();

        double distance = calculateStringDistance(targetPhrase, writtenPhrase);

        double NCER = distance/targetPhrase.length() * 100;
        double TER = NCER;



        double WPM = (m / 5.0) / ((double) (currentTime-startTime) / 60000.0);
        double accuracy = (100 - TER) / 100.0;
        double AWPM = WPM * accuracy;
        currentStats.put("NCER", NCER);
        currentStats.put("TER", TER);
        currentStats.put("WPM", WPM);
        currentStats.put("AWPM", AWPM);
        switch (alg){
            case "distance":
                distanceStats.add(currentStats);
                break;
            case "stringDistance":
                stringDistanceStats.add(currentStats);
                break;
            case "none":
                plainStats.add(currentStats);
                break;
        }
    }

    public static double calculateStringDistance(String x, String y) {
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
                                    + Session.costOfSubstitutionSimple(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1),
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[xLen][yLen];
    }
    public static int costOfSubstitutionSimple(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static long getCurrentTime() {
        return currentTime;
    }

    public static void setCurrentTime(long currentTime) {
        Session.currentTime = currentTime;
    }

    public static long getStartTime() {
        return startTime;
    }

    public static void setStartTime(long startTime) {
        Session.startTime = startTime;
    }

    public static String getTime()
    {
        long time = currentTime - startTime;
        return " " + time / 1000  + "." + time % 100 + "s";
    }
    public static int getCurrentPhraseCount() {
        return currentPhraseCount;
    }

    public static void setCurrentPhraseCount(int currentPhraseCount) {
        Session.currentPhraseCount = currentPhraseCount;
        if(currentPhraseCount >= phraseCount){
            started = false;
        }
    }

    private static int currentPhraseCount = 0;

    public static boolean isStarted() {
        return started;
    }

    public static void setStarted(boolean started) {
        Session.started = started;
    }

    public static boolean isSet() {
        return set;
    }

    public static void setSet(boolean set) {
        Session.set = set;
    }

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        Session.user = user;
    }

    public static String getSessionID() {
        return sessionID;
    }

    public static void setSessionID(String sessionID) {
        Session.sessionID = sessionID;
    }

    public static int getKeyboard() {
        return keyboard;
    }

    public static void setKeyboard(int keyboard) {
        Session.keyboard = keyboard;
    }

    public static int getPhraseCount() {
        return phraseCount;
    }

    public static void setPhraseCount(int phraseCount) {
        Session.phraseCount = phraseCount;
    }

    public static Orientation getOrientation() {
        return orientation;
    }

    public static void setOrientation(Orientation orientation) {
        Session.orientation = orientation;
    }

    public static TypingMode getTypingMode() {
        return typingMode;
    }

    public static void setTypingMode(TypingMode typingMode) {
        Session.typingMode = typingMode;
    }

}
