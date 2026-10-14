package com.exterawkey.ayumode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GhostController {

    public static boolean shouldCancelSendRequest(Object tlObject) {
        if (tlObject == null || !AyuModeConfig.ghostMode) {
            return false;
        }

        String className = tlObject.getClass().getSimpleName();

        if (AyuModeConfig.readMessagesGhost) {
            if ("TL_messages_readHistory".equals(className)
                    || "TL_channels_readHistory".equals(className)
                    || "TL_messages_readDiscussion".equals(className)
                    || "TL_messages_readSavedHistory".equals(className)
                    || "TL_messages_readEncryptedHistory".equals(className)
                    || "TL_messages_readMentions".equals(className)
                    || "TL_messages_readReactions".equals(className)
                    || "TL_messages_markDialogUnread".equals(className)) {
                return true;
            }
        }

        if (AyuModeConfig.typingGhost) {
            if ("TL_messages_setTyping".equals(className)
                    || "TL_messages_setEncryptedTyping".equals(className)
                    || "TL_channels_setTyping".equals(className)) {
                return true;
            }
        }

        if (AyuModeConfig.storiesGhost) {
            if ("TL_stories_readStories".equals(className)
                    || "TL_stories_incrementStoryViews".equals(className)) {
                return true;
            }
        }

        if (AyuModeConfig.voiceVideoGhost) {
            if ("TL_messages_readMessageContents".equals(className)
                    || "TL_channels_readMessageContents".equals(className)) {
                return true;
            }
        }

        if (AyuModeConfig.onlineGhost) {
            if ("TL_account_updateStatus".equals(className)) {
                try {
                    Field offlineField = tlObject.getClass().getField("offline");
                    boolean offline = offlineField.getBoolean(tlObject);
                    if (!offline) {
                        return true;
                    }
                } catch (Throwable ignored) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void onMessageSent(long dialogId, int maxId, Object connectionsManagerInstance) {
        if (!AyuModeConfig.markReadOnSend || connectionsManagerInstance == null) {
            return;
        }
        try {
            Class<?> tlClass = Class.forName("org.telegram.tgnet.TLRPC$TL_messages_readHistory");
            Object readReq = tlClass.newInstance();
            Field maxIdField = tlClass.getField("max_id");
            maxIdField.setInt(readReq, maxId);

            Method sendReqMethod = connectionsManagerInstance.getClass().getMethod(
                    "sendRequest",
                    Class.forName("org.telegram.tgnet.TLObject"),
                    Class.forName("org.telegram.tgnet.RequestDelegate")
            );
            sendReqMethod.invoke(connectionsManagerInstance, readReq, null);
        } catch (Throwable ignored) {
        }
    }
}
