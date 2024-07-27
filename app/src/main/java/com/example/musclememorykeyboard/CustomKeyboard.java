package com.example.musclememorykeyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.util.Log;

import java.util.List;

public class CustomKeyboard extends Keyboard {
    public CustomKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    public void changeKeyHeight() {
        int height = (getKeys().get(0).height);
        List<Key> keys = getKeys();
        for (Key key : keys) {
            key.height = height;// Example modification
            // Ensure to log and verify changes if needed
            Log.d("KEY HEIGHT", "Key code: " + key.codes[0] + ", Height: " + key.height);
        }
            this.setVerticalGap(0);
            Log.d("CHANGE KEY HEIGHT", "CALLED");

        /*somehow adding this fixed a weird bug where bottom row keys could not be pressed if keyboard height is too tall..
        from the Keyboard source code seems like calling this will recalculate some values used in keypress detection calculation*/
            getNearestKeys(0, 0);
    }

    @Override
    public int[] getNearestKeys(int x, int y) {
        Log.d("GET NEAREST KEY", "CALLED");
        List<Key> keys = getKeys();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).isInside(x, y+50)) return new int[]{i};
        }
        return new int[0];
    }

}
