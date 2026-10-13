__id__ = "ayumode"
__name__ = "AyuMode"
__description__ = "Функционал AyuGram в exteraGram: Призрак, сохранение сообщений, обход скриншотов и защиты контента"
__author__ = "wKey ( @exterawKey )"
__version__ = "1.0.0"
__icon__ = "msg_stealth"
__app_version__ = ">=12.5.1"
__sdk_version__ = ">=1.4.0"

import os
from typing import Any, List

from base_plugin import BasePlugin, MethodHook
from hook_utils import find_class
from ui.bulletin import BulletinHelper
from ui.settings import Divider, Header, Switch, Text


class SendRequestHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def before_hooked_method(self, param):
        if not param.args or len(param.args) == 0:
            return

        req = param.args[0]
        if not req:
            return

        plugin = self._plugin
        if not plugin or not plugin.get_setting("ghost_mode", True):
            return

        if plugin._bridge:
            try:
                if plugin._bridge.shouldCancelRequest(req):
                    param.setResult(0)
                    return
            except Exception:
                pass

        try:
            name = req.getClass().getSimpleName()

            if plugin.get_setting("read_messages_ghost", True):
                if name in (
                    "TL_messages_readHistory",
                    "TL_channels_readHistory",
                    "TL_messages_readDiscussion",
                    "TL_messages_readSavedHistory",
                    "TL_messages_readEncryptedHistory",
                ):
                    param.setResult(0)
                    return

            if plugin.get_setting("typing_ghost", True):
                if name == "TL_messages_setTyping":
                    param.setResult(0)
                    return

            if plugin.get_setting("stories_ghost", True):
                if name == "TL_stories_readStories":
                    param.setResult(0)
                    return

            if plugin.get_setting("voice_video_ghost", True):
                if name == "TL_messages_readMessageContents":
                    param.setResult(0)
                    return

            if plugin.get_setting("online_ghost", True):
                if name == "TL_account_updateStatus":
                    try:
                        offline = getattr(req, "offline", False)
                        if not offline:
                            param.setResult(0)
                            return
                    except Exception:
                        param.setResult(0)
                        return
        except Exception:
            pass


class WindowSetFlagsHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def before_hooked_method(self, param):
        if not self._plugin or not self._plugin.get_setting("bypass_flag_secure", True):
            return
        try:
            if len(param.args) >= 2:
                flags = int(param.args[0])
                mask = int(param.args[1])
                FLAG_SECURE = 8192
                if (flags & FLAG_SECURE) != 0 or (mask & FLAG_SECURE) != 0:
                    param.args[0] = flags & ~FLAG_SECURE
                    param.args[1] = mask & ~FLAG_SECURE
        except Exception:
            pass


class MessagesStorageDeleteHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def before_hooked_method(self, param):
        if not self._plugin or not self._plugin.get_setting("save_deleted_messages", True):
            return
        try:
            if len(param.args) >= 2:
                dialog_id = int(param.args[0])
                msg_ids = param.args[1]
                if msg_ids and hasattr(msg_ids, "size"):
                    for i in range(msg_ids.size()):
                        mid = int(msg_ids.get(i))
                        self._plugin._on_message_deleted(dialog_id, mid)
        except Exception:
            pass


class AyuModePlugin(BasePlugin):
    def __init__(self):
        super().__init__()
        self._bridge = None
        self._deleted_messages = set()

    def on_plugin_load(self):
        self._init_dex_bridge()

        conn_mgr = find_class("org.telegram.tgnet.ConnectionsManager")
        if conn_mgr:
            self.hook_all_methods(conn_mgr, "sendRequest", SendRequestHook(self))

        window_cls = find_class("android.view.Window")
        if window_cls:
            self.hook_all_methods(window_cls, "setFlags", WindowSetFlagsHook(self))

        storage_cls = find_class("org.telegram.messenger.MessagesStorage")
        if storage_cls:
            self.hook_all_methods(storage_cls, "markMessagesAsDeleted", MessagesStorageDeleteHook(self))

        try:
            BulletinHelper.show_success("AyuMode успешно активирован")
        except Exception:
            pass

    def _init_dex_bridge(self):
        try:
            AppLoader = find_class("org.telegram.messenger.ApplicationLoader")
            if not AppLoader:
                return
            context = getattr(AppLoader, "applicationContext", None)
            if not context:
                return

            candidates = [
                os.path.join(os.path.dirname(__file__), "classes.dex"),
                os.path.join(str(context.getFilesDir()), "ayumode", "classes.dex"),
                "/sdcard/Download/classes.dex",
            ]

            dex_path = None
            for p in candidates:
                if os.path.exists(p):
                    dex_path = p
                    break

            if not dex_path:
                return

            DexClassLoader = find_class("dalvik.system.DexClassLoader")
            File = find_class("java.io.File")
            if not DexClassLoader or not File:
                return

            opt_dir = context.getDir("dex", 0)
            loader = DexClassLoader(
                dex_path,
                opt_dir.getAbsolutePath(),
                None,
                context.getClassLoader(),
            )
            bridge_class = loader.loadClass("com.exterawkey.ayumode.AyuModeBridge")
            if bridge_class:
                bridge_class.init(context)
                self._bridge = bridge_class
                self._sync_all_to_bridge()
        except Exception:
            self._bridge = None

    def _sync_all_to_bridge(self):
        if not self._bridge:
            return
        keys = [
            "ghost_mode",
            "read_messages_ghost",
            "typing_ghost",
            "online_ghost",
            "stories_ghost",
            "voice_video_ghost",
            "bypass_flag_secure",
            "force_download_media",
            "save_deleted_messages",
            "save_edited_messages",
        ]
        for k in keys:
            try:
                val = self.get_setting(k, True)
                self._bridge.setConfig(k, bool(val))
            except Exception:
                pass

    def _on_message_deleted(self, dialog_id: int, message_id: int):
        self._deleted_messages.add(f"{dialog_id}_{message_id}")
        if self._bridge:
            try:
                self._bridge.onMessageDeleted(dialog_id, message_id)
            except Exception:
                pass

    def create_settings(self) -> List[Any]:
        return [
            Header(text="AyuMode — Функции AyuGram"),
            Switch(
                key="ghost_mode",
                text="Режим призрака",
                default=True,
                subtext="Главный тумблер скрытности в сети",
                icon="msg_stealth",
            ),
            Switch(
                key="read_messages_ghost",
                text="Нечиталка сообщений",
                default=True,
                subtext="Не отправлять статус прочтения при открытии чата",
                icon="msg_views",
            ),
            Switch(
                key="typing_ghost",
                text="Скрывать статус печатает",
                default=True,
                subtext="Собеседник не видит когда вы набираете текст",
                icon="msg_edit",
            ),
            Switch(
                key="online_ghost",
                text="Скрывать статус онлайн",
                default=True,
                subtext="Не обновлять время в сети при использовании клиента",
                icon="msg_user",
            ),
            Switch(
                key="stories_ghost",
                text="Невидимка в историях",
                default=True,
                subtext="Просмотр историй без отображения в списке зрителей",
                icon="msg_stories",
            ),
            Switch(
                key="voice_video_ghost",
                text="Невидимка в голосовых и видео",
                default=True,
                subtext="Не отмечать прослушивание голосовых и просмотр видеосообщений",
                icon="msg_voice",
            ),
            Divider(),
            Header(text="Обход ограничений"),
            Switch(
                key="bypass_flag_secure",
                text="Скриншоты везде",
                default=True,
                subtext="Разрешить скриншоты и запись экрана в защищённых чатах",
                icon="msg_permissions",
            ),
            Switch(
                key="force_download_media",
                text="Скачивание из закрытых каналов",
                default=True,
                subtext="Разрешить сохранение медиа при запрете сохранения контента",
                icon="msg_download",
            ),
            Divider(),
            Header(text="Шпионские функции"),
            Switch(
                key="save_deleted_messages",
                text="Сохранять удалённые сообщения",
                default=True,
                subtext="Не удалять сообщения из чата если собеседник их стёр",
                icon="msg_delete",
            ),
            Switch(
                key="save_edited_messages",
                text="История изменений сообщений",
                default=True,
                subtext="Сохранять исходный текст если сообщение отредактировали",
                icon="msg_history",
            ),
            Divider(),
            Header(text="О плагине"),
            Text(text="AyuMode v1.0.0 (DEX Engine + Python Bridge)", icon="msg_info"),
            Text(text="Автор: wKey ( @exterawKey )", icon="msg_contacts"),
            Text(text="GitHub: github.com/exterawKey/AyuMode", icon="msg_link"),
        ]
