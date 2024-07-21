package com.example.musclememorykeyboard;

public class Session {
    private static String user,sessionID;
    private static int phraseCount, keyboard;
    private static Orientation orientation;
    private static TypingMode typingMode;
    private static boolean set = false;

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
