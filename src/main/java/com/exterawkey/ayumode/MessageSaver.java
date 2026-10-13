package com.exterawkey.ayumode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageSaver {
    private static final Map<String, DeletedMessageInfo> deletedMessages = new ConcurrentHashMap<String, DeletedMessageInfo>();
    private static final Map<String, List<EditedMessageEntry>> editedHistory = new ConcurrentHashMap<String, List<EditedMessageEntry>>();

    public static class DeletedMessageInfo {
        public final long dialogId;
        public final int messageId;
        public final long deletedTime;
        public final String text;
        public final long fromId;

        public DeletedMessageInfo(long dialogId, int messageId, String text, long fromId) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.deletedTime = System.currentTimeMillis();
            this.text = text != null ? text : "";
            this.fromId = fromId;
        }
    }

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

    public static void onMessageDeleted(long dialogId, int messageId, String text, long fromId) {
        if (!AyuModeConfig.saveDeletedMessages) return;
        deletedMessages.put(makeKey(dialogId, messageId), new DeletedMessageInfo(dialogId, messageId, text, fromId));
    }

    public static boolean isDeleted(long dialogId, int messageId) {
        if (!AyuModeConfig.saveDeletedMessages) return false;
        return deletedMessages.containsKey(makeKey(dialogId, messageId));
    }

    public static DeletedMessageInfo getDeletedInfo(long dialogId, int messageId) {
        return deletedMessages.get(makeKey(dialogId, messageId));
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

    public static List<EditedMessageEntry> getEditHistory(long dialogId, int messageId) {
        List<EditedMessageEntry> list = editedHistory.get(makeKey(dialogId, messageId));
        return list != null ? new ArrayList<EditedMessageEntry>(list) : Collections.<EditedMessageEntry>emptyList();
    }

    public static void clear() {
        deletedMessages.clear();
        editedHistory.clear();
    }
}
