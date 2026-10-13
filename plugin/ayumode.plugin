__id__ = "ayumode"
__name__ = "AyuMode"
__description__ = "Полный функционал AyuGram в exteraGram: Призрак, сохранение удалённых сообщений, обход скриншотов и защиты"
__author__ = "wKey ( @exterawKey )"
__version__ = "1.0.0"
__icon__ = "msg_stealth"
__app_version__ = ">=12.5.1"
__sdk_version__ = ">=1.4.0"

import os
import threading
import urllib.request
from typing import Any, List

from base_plugin import BasePlugin, MethodHook
from hook_utils import find_class
from ui.bulletin import BulletinHelper
from ui.settings import Divider, Header, Switch, Text

DEX_RELEASE_URL = "https://github.com/exterawKey/AyuMode/releases/latest/download/classes.dex"


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
                    "TL_messages_readMentions",
                    "TL_messages_readReactions",
                ):
                    param.setResult(0)
                    return

            if plugin.get_setting("typing_ghost", True):
                if name in (
                    "TL_messages_setTyping",
                    "TL_messages_setEncryptedTyping",
                    "TL_channels_setTyping",
                ):
                    param.setResult(0)
                    return

            if plugin.get_setting("stories_ghost", True):
                if name in (
                    "TL_stories_readStories",
                    "TL_stories_incrementStoryViews",
                ):
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
        self._ensure_dex_loaded()

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

    def _ensure_dex_loaded(self):
        try:
            AppLoader = find_class("org.telegram.messenger.ApplicationLoader")
            if not AppLoader:
                return
            context = getattr(AppLoader, "applicationContext", None)
            if not context:
                return

            ayumode_dir = os.path.join(str(context.getFilesDir()), "ayumode")
            os.makedirs(ayumode_dir, exist_ok=True)
            dex_path = os.path.join(ayumode_dir, "classes.dex")

            if os.path.exists(dex_path) and os.path.getsize(dex_path) > 1024:
                self._load_dex(context, dex_path)
                return

            def download_dex_worker():
                try:
                    temp_path = dex_path + ".tmp"
                    req = urllib.request.Request(
                        DEX_RELEASE_URL,
                        headers={"User-Agent": "exteraGram-AyuMode"}
                    )
                    with urllib.request.urlopen(req, timeout=30) as resp:
                        data = resp.read()
                        if len(data) > 1024:
                            with open(temp_path, "wb") as f:
                                f.write(data)
                            os.replace(temp_path, dex_path)
                            self._load_dex(context, dex_path)
                            try:
                                BulletinHelper.show_success("AyuMode: DEX успешно загружен с GitHub")
                            except Exception:
                                pass
                except Exception:
                    pass

            threading.Thread(target=download_dex_worker, daemon=True).start()
        except Exception:
            pass

    def _load_dex(self, context: Any, dex_path: str):
        try:
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
            "send_offline_after_action",
            "mark_read_on_send",
            "save_deleted_messages",
            "save_edited_messages",
            "save_media_ttl",
            "save_for_bots",
            "save_exact_read_date",
            "save_local_online",
            "bypass_flag_secure",
            "force_download_media",
            "semi_transparent_deleted",
        ]
        for k in keys:
            try:
                val = self.get_setting(k, True)
                self._bridge.setConfigBool(k, bool(val))
            except Exception:
                pass

    def _on_message_deleted(self, dialog_id: int, message_id: int):
        self._deleted_messages.add(f"{dialog_id}_{message_id}")
        if self._bridge:
            try:
                self._bridge.onMessageDeleted(dialog_id, message_id, "", 0)
            except Exception:
                pass

    def create_settings(self) -> List[Any]:
        return [
            Header(text="👻 Режим призрака (AyuGhost)"),
            Switch(
                key="ghost_mode",
                text="Режим призрака",
                default=True,
                subtext="Главный тумблер скрытности в сети",
                icon="msg_stealth",
            ),
            Switch(
                key="read_messages_ghost",
                text="Не читать сообщения",
                default=True,
                subtext="Не отправлять статус прочтения при открытии чата",
                icon="msg_views",
            ),
            Switch(
                key="typing_ghost",
                text="Скрывать статус «печатает...»",
                default=True,
                subtext="Собеседник не видит набор текста, запись голосовых и отправку медиа",
                icon="msg_edit",
            ),
            Switch(
                key="online_ghost",
                text="Скрывать статус «в сети»",
                default=True,
                subtext="Не обновлять онлайн при открытии и использовании клиента",
                icon="msg_user",
            ),
            Switch(
                key="stories_ghost",
                text="Невидимка в историях",
                default=True,
                subtext="Просмотр историй без отображения в списке зрителей автора",
                icon="msg_stories",
            ),
            Switch(
                key="voice_video_ghost",
                text="Невидимка в голосовых и видео",
                default=True,
                subtext="Не отмечать прослушивание голосовых и просмотр кружочков",
                icon="msg_voice",
            ),
            Switch(
                key="send_offline_after_action",
                text="Оффлайн после действий",
                default=True,
                subtext="Сразу отправлять статус не в сети после отправки сообщений",
                icon="msg_retry",
            ),
            Switch(
                key="mark_read_on_send",
                text="Прочитать при ответе",
                default=False,
                subtext="Отмечать входящие сообщения прочитанными только когда вы сами отвечаете",
                icon="msg_send",
            ),
            Divider(),
            Header(text="🕵️‍♂️ Шпионские функции (AyuSpy)"),
            Switch(
                key="save_deleted_messages",
                text="Сохранять удалённые сообщения",
                default=True,
                subtext="Удалённые собеседником сообщения остаются в чате",
                icon="msg_delete",
            ),
            Switch(
                key="save_edited_messages",
                text="История изменений сообщений",
                default=True,
                subtext="Сохранять первоначальный текст при редактировании сообщений",
                icon="msg_history",
            ),
            Switch(
                key="save_media_ttl",
                text="Сохранять самоуничтожающиеся медиа",
                default=True,
                subtext="Таймерные фото и видео не исчезают после просмотра",
                icon="msg_secret",
            ),
            Switch(
                key="save_for_bots",
                text="Сохранять сообщения ботов",
                default=False,
                subtext="Не удалять сообщения и инлайн-клавиатуры от ботов",
                icon="msg_bot",
            ),
            Switch(
                key="save_exact_read_date",
                text="Фиксировать время прочтения",
                default=True,
                subtext="Запоминать точную дату и время прочтения сообщения собеседником",
                icon="msg_calendar",
            ),
            Switch(
                key="save_local_online",
                text="Локальный трекер онлайна",
                default=True,
                subtext="Сохранять в базу точное время последнего онлайна пользователей",
                icon="msg_log",
            ),
            Divider(),
            Header(text="🔓 Обход ограничений (Bypass)"),
            Switch(
                key="bypass_flag_secure",
                text="Скриншоты везде",
                default=True,
                subtext="Снятие FLAG_SECURE для скриншотов и записи экрана в защищённых чатах",
                icon="msg_permissions",
            ),
            Switch(
                key="force_download_media",
                text="Скачивание из закрытых каналов",
                default=True,
                subtext="Принудительное скачивание медиа при запрете сохранения контента",
                icon="msg_download",
            ),
            Divider(),
            Header(text="🎨 Кастомизация"),
            Switch(
                key="semi_transparent_deleted",
                text="Полупрозрачные удалённые сообщения",
                default=True,
                subtext="Отображать удалённые сообщения с лёгкой прозрачностью",
                icon="msg_customize",
            ),
            Divider(),
            Header(text="О плагине"),
            Text(text="AyuMode v1.0.0 (DEX Engine + Auto-Download)", icon="msg_info"),
            Text(text="Автор: wKey ( @exterawKey )", icon="msg_contacts"),
            Text(text="Релизы DEX: github.com/exterawKey/AyuMode", icon="msg_link"),
        ]
