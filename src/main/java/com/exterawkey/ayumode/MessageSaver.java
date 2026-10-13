package com.exterawkey.ayumode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessageSaver {
    private static final Set<String> deletedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final ConcurrentHashMap<String, List<EditedMessageEntry>> editedHistory = new ConcurrentHashMap<String, List<EditedMessageEntry>>();

    public static class EditedMessageEntry {
        public final String text;
        public final long timestamp;

        public EditedMessageEntry(String text, long timestamp) {
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    private static String makeKey(long dialogId, int messageId) {
        return dialogId + "_" + messageId;
    }

    public static void onMessageDeleted(long dialogId, int messageId) {
        if (!AyuModeConfig.saveDeletedMessages) return;
        deletedMessageKeys.add(makeKey(dialogId, messageId));
    }

    public static boolean isDeleted(long dialogId, int messageId) {
        if (!AyuModeConfig.saveDeletedMessages) return false;
        return deletedMessageKeys.contains(makeKey(dialogId, messageId));
    }

    public static void onMessageEdited(long dialogId, int messageId, String previousText, long timestamp) {
        if (!AyuModeConfig.saveEditedMessages || previousText == null || previousText.isEmpty()) return;
        String key = makeKey(dialogId, messageId);
        List<EditedMessageEntry> history = editedHistory.get(key);
        if (history == null) {
            history = Collections.synchronizedList(new ArrayList<EditedMessageEntry>());
            editedHistory.put(key, history);
        }
        history.add(new EditedMessageEntry(previousText, timestamp));
    }

    public static List<EditedMessageEntry> getHistory(long dialogId, int messageId) {
        List<EditedMessageEntry> list = editedHistory.get(makeKey(dialogId, messageId));
        return list != null ? new ArrayList<EditedMessageEntry>(list) : Collections.<EditedMessageEntry>emptyList();
    }

    public static void clear() {
        deletedMessageKeys.clear();
        editedHistory.clear();
    }
}
