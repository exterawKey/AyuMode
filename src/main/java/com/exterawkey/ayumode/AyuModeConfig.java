package com.exterawkey.ayumode;

import android.content.Context;
import android.content.SharedPreferences;

public class AyuModeConfig {
    private static final String PREFS_NAME = "ayumode_config";

    public static volatile boolean ghostMode = true;
    public static volatile boolean readMessagesGhost = true;
    public static volatile boolean typingGhost = true;
    public static volatile boolean onlineGhost = true;
    public static volatile boolean storiesGhost = true;
    public static volatile boolean voiceVideoGhost = true;
    public static volatile boolean markReadOnSend = false;

    public static volatile boolean bypassFlagSecure = true;
    public static volatile boolean forceDownloadMedia = true;

    public static volatile boolean saveDeletedMessages = true;
    public static volatile boolean saveEditedMessages = true;

    private static SharedPreferences preferences;

    public static void init(Context context) {
        if (context == null) return;
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static void load() {
        if (preferences == null) return;
        ghostMode = preferences.getBoolean("ghostMode", true);
        readMessagesGhost = preferences.getBoolean("readMessagesGhost", true);
        typingGhost = preferences.getBoolean("typingGhost", true);
        onlineGhost = preferences.getBoolean("onlineGhost", true);
        storiesGhost = preferences.getBoolean("storiesGhost", true);
        voiceVideoGhost = preferences.getBoolean("voiceVideoGhost", true);
        markReadOnSend = preferences.getBoolean("markReadOnSend", false);

        bypassFlagSecure = preferences.getBoolean("bypassFlagSecure", true);
        forceDownloadMedia = preferences.getBoolean("forceDownloadMedia", true);

        saveDeletedMessages = preferences.getBoolean("saveDeletedMessages", true);
        saveEditedMessages = preferences.getBoolean("saveEditedMessages", true);
    }

    public static void save() {
        if (preferences == null) return;
        preferences.edit()
            .putBoolean("ghostMode", ghostMode)
            .putBoolean("readMessagesGhost", readMessagesGhost)
            .putBoolean("typingGhost", typingGhost)
            .putBoolean("onlineGhost", onlineGhost)
            .putBoolean("storiesGhost", storiesGhost)
            .putBoolean("voiceVideoGhost", voiceVideoGhost)
            .putBoolean("markReadOnSend", markReadOnSend)
            .putBoolean("bypassFlagSecure", bypassFlagSecure)
            .putBoolean("forceDownloadMedia", forceDownloadMedia)
            .putBoolean("saveDeletedMessages", saveDeletedMessages)
            .putBoolean("saveEditedMessages", saveEditedMessages)
            .apply();
    }

    public static void setBool(String key, boolean val) {
        if (preferences == null) return;
        preferences.edit().putBoolean(key, val).apply();
        load();
    }
}
