package com.exterawkey.ayumode;

import java.lang.reflect.Field;

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
                    || "TL_messages_readEncryptedHistory".equals(className)) {
                return true;
            }
        }

        if (AyuModeConfig.typingGhost) {
            if ("TL_messages_setTyping".equals(className)) {
                return true;
            }
        }

        if (AyuModeConfig.storiesGhost) {
            if ("TL_stories_readStories".equals(className)) {
                return true;
            }
        }

        if (AyuModeConfig.voiceVideoGhost) {
            if ("TL_messages_readMessageContents".equals(className)) {
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
}
