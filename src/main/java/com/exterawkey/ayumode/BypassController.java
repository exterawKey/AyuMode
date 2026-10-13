package com.exterawkey.ayumode;

import android.view.Window;
import android.view.WindowManager;

public class BypassController {
    public static final int FLAG_SECURE = WindowManager.LayoutParams.FLAG_SECURE; // 8192

    public static int sanitizeWindowFlags(int flags) {
        if (!AyuModeConfig.bypassFlagSecure) {
            return flags;
        }
        return flags & ~FLAG_SECURE;
    }

    public static void cleanWindow(Window window) {
        if (window == null || !AyuModeConfig.bypassFlagSecure) {
            return;
        }
        try {
            window.clearFlags(FLAG_SECURE);
        } catch (Throwable ignored) {
        }
    }

    public static boolean shouldAllowDownload(boolean originalHasProtectedContent) {
        if (AyuModeConfig.forceDownloadMedia) {
            return false; // Tells Telegram that content is NOT protected, allowing download/forward
        }
        return originalHasProtectedContent;
    }
}
