package com.exterawkey.ayumode;

import android.content.Context;
import android.view.Window;
import java.util.List;

public class AyuModeBridge {
    public static final String VERSION = "1.0.0";
    private static volatile boolean initialized = false;

    public static synchronized void init(Context context) {
        if (initialized) return;
        if (context != null) {
            AyuModeConfig.init(context);
        }
        initialized = true;
    }

    public static boolean shouldCancelRequest(Object tlObject) {
        return GhostController.shouldCancelSendRequest(tlObject);
    }

    public static int sanitizeWindowFlags(int flags) {
        return BypassController.sanitizeWindowFlags(flags);
    }

    public static void cleanWindow(Window window) {
        BypassController.cleanWindow(window);
    }

    public static void onMessageDeleted(long dialogId, int messageId, String text, long fromId) {
        MessageSaver.onMessageDeleted(dialogId, messageId, text, fromId);
    }

    public static boolean isMessageDeleted(long dialogId, int messageId) {
        return MessageSaver.isDeleted(dialogId, messageId);
    }

    public static void onMessageEdited(long dialogId, int messageId, String previousText, long timestamp) {
        MessageSaver.onMessageEdited(dialogId, messageId, previousText, timestamp);
    }

    public static List<MessageSaver.EditedMessageEntry> getEditHistory(long dialogId, int messageId) {
        return MessageSaver.getEditHistory(dialogId, messageId);
    }

    public static void setConfigBool(String key, boolean val) {
        AyuModeConfig.setBool(key, val);
    }

    public static void setConfigInt(String key, int val) {
        AyuModeConfig.setInt(key, val);
    }

    public static String getVersion() {
        return VERSION;
    }
}
