__id__ = "ayumode"
__name__ = "AyuMode"
__description__ = "Функционал AyuGram в exteraGram: Призрак, сохранение удалённых сообщений, обход скриншотов и защиты"
__author__ = "wKey ( @exterawKey )"
__version__ = "1.1.0"
__icon__ = "msg_stealth"
__app_version__ = ">=12.5.1"
__sdk_version__ = ">=1.4.0"

import json
import os
import threading
import time
import urllib.request
from typing import Any, List

from base_plugin import BasePlugin, HookResult, HookStrategy, MenuItemData, MenuItemType, MethodHook
from client_utils import get_last_fragment, get_messages_controller, get_messages_storage, get_connections_manager
from hook_utils import find_class
from ui.alert import AlertDialogBuilder
from ui.bulletin import BulletinHelper
from ui.settings import Divider, Header, Switch, Text

DEX_RELEASE_URL = "https://github.com/exterawKey/AyuMode/releases/latest/download/classes.dex"

READ_REQUESTS = (
    "TL_messages_readHistory",
    "TL_channels_readHistory",
    "TL_messages_readDiscussion",
    "TL_messages_readSavedHistory",
    "TL_messages_readEncryptedHistory",
    "TL_messages_readMentions",
    "TL_messages_readReactions",
    "TL_messages_markDialogUnread",
)

VOICE_VIDEO_READ_REQUESTS = (
    "TL_messages_readMessageContents",
    "TL_channels_readMessageContents",
)

TYPING_REQUESTS = (
    "TL_messages_setTyping",
    "TL_messages_setEncryptedTyping",
    "TL_channels_setTyping",
)

STORIES_REQUESTS = (
    "TL_stories_readStories",
    "TL_stories_incrementStoryViews",
)

ONLINE_REQUESTS = (
    "TL_account_updateStatus",
)

ACTION_REQUESTS = (
    "TL_messages_sendMessage",
    "TL_messages_sendMedia",
    "TL_messages_sendMultiMedia",
    "TL_messages_sendReaction",
    "TL_messages_sendVote",
)

EDIT_UPDATES = (
    "TL_updateEditMessage",
    "TL_updateEditChannelMessage",
)


class SendRequestFallbackHook(MethodHook):
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
                if name in READ_REQUESTS:
                    param.setResult(0)
                    return

            if plugin.get_setting("typing_ghost", True):
                if name in TYPING_REQUESTS:
                    param.setResult(0)
                    return

            if plugin.get_setting("stories_ghost", True):
                if name in STORIES_REQUESTS:
                    param.setResult(0)
                    return

            if plugin.get_setting("voice_video_ghost", True):
                if name in VOICE_VIDEO_READ_REQUESTS:
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


class DeleteMessagesHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def before_hooked_method(self, param):
        if not self._plugin or not self._plugin.get_setting("save_deleted_messages", True):
            return
        try:
            if not param.args or len(param.args) == 0:
                return

            is_user_delete = False
            if len(param.args) >= 6 and isinstance(param.args[5], int) and param.args[5] == 1:
                is_user_delete = True
            elif len(param.args) >= 5 and isinstance(param.args[4], int) and param.args[4] == 1:
                is_user_delete = True

            if is_user_delete:
                return

            dialog_id = 0
            if len(param.args) > 3 and param.args[3] is not None:
                try:
                    dialog_id = int(param.args[3])
                except Exception:
                    dialog_id = 0

            msg_ids = param.args[0]
            if msg_ids and hasattr(msg_ids, "size"):
                for i in range(msg_ids.size()):
                    mid = int(msg_ids.get(i))
                    self._plugin._on_message_deleted(dialog_id, mid)
                if hasattr(msg_ids, "clear"):
                    msg_ids.clear()
            param.setResult(None)
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
            if param.args and len(param.args) >= 2:
                dialog_id = 0
                try:
                    dialog_id = int(param.args[0])
                except Exception:
                    pass
                msg_ids = param.args[1]
                if msg_ids and hasattr(msg_ids, "size"):
                    for i in range(msg_ids.size()):
                        mid = int(msg_ids.get(i))
                        self._plugin._on_message_deleted(dialog_id, mid)
                    if hasattr(msg_ids, "clear"):
                        msg_ids.clear()
            param.setResult(None)
        except Exception:
            pass


class MarkDialogAsReadHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def before_hooked_method(self, param):
        if not self._plugin:
            return
        if not self._plugin.get_setting("ghost_mode", True) or not self._plugin.get_setting("read_messages_ghost", True):
            return
        try:
            if param.args and len(param.args) > 0 and param.args[0] is not None:
                dialog_id = int(param.args[0])
                if self._plugin._allow_read_dialog != dialog_id:
                    param.setResult(None)
                    return
        except Exception:
            param.setResult(None)


class ChatMessageCellHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def after_hooked_method(self, param):
        if not self._plugin or not self._plugin.get_setting("save_deleted_messages", True):
            return
        cell = getattr(param, "thisObject", None)
        if not cell or not param.args:
            return
        msg_obj = param.args[0]
        if not msg_obj:
            return
        try:
            mid = int(msg_obj.getId())
            dialog_id = int(msg_obj.getDialogId())
            if self._plugin.is_deleted(dialog_id, mid):
                if self._plugin.get_setting("semi_transparent_deleted", True):
                    cell.setAlpha(0.65)
                else:
                    cell.setAlpha(1.0)
            else:
                cell.setAlpha(1.0)
        except Exception:
            pass


class WindowFlagsHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def before_hooked_method(self, param):
        if not self._plugin or not self._plugin.get_setting("bypass_flag_secure", True):
            return
        try:
            if not param.args or len(param.args) == 0:
                return
            FLAG_SECURE = 8192
            if len(param.args) >= 2:
                flags = int(param.args[0])
                mask = int(param.args[1])
                if (flags & FLAG_SECURE) != 0 or (mask & FLAG_SECURE) != 0:
                    param.args[0] = flags & ~FLAG_SECURE
                    param.args[1] = mask & ~FLAG_SECURE
            elif len(param.args) == 1:
                flags = int(param.args[0])
                if (flags & FLAG_SECURE) != 0:
                    param.args[0] = flags & ~FLAG_SECURE
        except Exception:
            pass


class SendTypingHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def before_hooked_method(self, param):
        if self._plugin and self._plugin.get_setting("ghost_mode", True) and self._plugin.get_setting("typing_ghost", True):
            param.setResult(None)


class BypassProtectedContentHook(MethodHook):
    def __init__(self, plugin, return_value):
        super().__init__()
        self._plugin = plugin
        self._return_value = return_value

    def after_hooked_method(self, param):
        if self._plugin and self._plugin.get_setting("force_download_media", True):
            param.setResult(self._return_value)


class SecretMediaHook(MethodHook):
    def __init__(self, plugin):
        super().__init__()
        self._plugin = plugin

    def after_hooked_method(self, param):
        if self._plugin and self._plugin.get_setting("save_media_ttl", True):
            param.setResult(False)


class AyuModePlugin(BasePlugin):
    def __init__(self):
        super().__init__()
        self._bridge = None
        self._deleted_messages = set()
        self._deleted_info = {}
        self._edit_history = {}
        self._allow_read_dialog = 0
        self._data_dir = ""

    def on_plugin_load(self):
        self._init_storage()
        self._ensure_dex_loaded()

        for req in READ_REQUESTS:
            self.add_hook(req)

        for req in VOICE_VIDEO_READ_REQUESTS:
            self.add_hook(req)

        for req in TYPING_REQUESTS:
            self.add_hook(req)

        for req in STORIES_REQUESTS:
            self.add_hook(req)

        for req in ONLINE_REQUESTS:
            self.add_hook(req)

        for req in ACTION_REQUESTS:
            self.add_hook(req)

        for upd in EDIT_UPDATES:
            self.add_hook(upd)

        controller_cls = find_class("org.telegram.messenger.MessagesController")
        if controller_cls:
            self.hook_all_methods(controller_cls, "deleteMessages", DeleteMessagesHook(self))
            self.hook_all_methods(controller_cls, "markDialogAsRead", MarkDialogAsReadHook(self))
            self.hook_all_methods(controller_cls, "markDialogAsReadNow", MarkDialogAsReadHook(self))
            self.hook_all_methods(controller_cls, "isPeerNoForwards", BypassProtectedContentHook(self, False))

        storage_cls = find_class("org.telegram.messenger.MessagesStorage")
        if storage_cls:
            self.hook_all_methods(storage_cls, "markMessagesAsDeleted", MessagesStorageDeleteHook(self))

        cell_cls = find_class("org.telegram.ui.Cells.ChatMessageCell")
        if cell_cls:
            self.hook_all_methods(cell_cls, "setMessageObject", ChatMessageCellHook(self))

        helper_cls = find_class("org.telegram.messenger.SendMessagesHelper")
        if helper_cls:
            self.hook_all_methods(helper_cls, "sendTyping", SendTypingHook(self))

        window_cls = find_class("android.view.Window")
        if window_cls:
            self.hook_all_methods(window_cls, "setFlags", WindowFlagsHook(self))
            self.hook_all_methods(window_cls, "addFlags", WindowFlagsHook(self))

        msg_obj_cls = find_class("org.telegram.messenger.MessageObject")
        if msg_obj_cls:
            self.hook_all_methods(msg_obj_cls, "canForwardMessage", BypassProtectedContentHook(self, True))
            self.hook_all_methods(msg_obj_cls, "isSecretMedia", SecretMediaHook(self))

        conn_mgr = find_class("org.telegram.tgnet.ConnectionsManager")
        if conn_mgr:
            self.hook_all_methods(conn_mgr, "sendRequest", SendRequestFallbackHook(self))

        try:
            self.add_menu_item(
                MenuItemData(
                    menu_type=MenuItemType.MESSAGE_CONTEXT_MENU,
                    text="AyuMode: Инфо / Правки",
                    on_click=self.handle_message_info_click,
                    icon="msg_info",
                    priority=15,
                )
            )
            self.add_menu_item(
                MenuItemData(
                    menu_type=MenuItemType.CHAT_ACTION_MENU,
                    text="AyuMode: Прочитать диалог",
                    on_click=self.handle_mark_chat_read,
                    icon="msg_views",
                    priority=15,
                )
            )
        except Exception:
            pass

        try:
            BulletinHelper.show_success("AyuMode v1.1.0 активирован")
        except Exception:
            pass

    def pre_request_hook(self, request_name: str, account: int, request: Any) -> HookResult:
        if not self.get_setting("ghost_mode", True):
            return HookResult()

        if request_name in READ_REQUESTS:
            if self.get_setting("read_messages_ghost", True):
                return HookResult(strategy=HookStrategy.CANCEL)

        if request_name in VOICE_VIDEO_READ_REQUESTS:
            if self.get_setting("voice_video_ghost", True):
                return HookResult(strategy=HookStrategy.CANCEL)

        if request_name in TYPING_REQUESTS:
            if self.get_setting("typing_ghost", True):
                return HookResult(strategy=HookStrategy.CANCEL)

        if request_name in STORIES_REQUESTS:
            if self.get_setting("stories_ghost", True):
                return HookResult(strategy=HookStrategy.CANCEL)

        if request_name in ONLINE_REQUESTS:
            if self.get_setting("online_ghost", True):
                try:
                    request.offline = True
                    return HookResult(strategy=HookStrategy.MODIFY, request=request)
                except Exception:
                    return HookResult(strategy=HookStrategy.CANCEL)

        return HookResult()

    def post_request_hook(self, request_name: str, account: int, response: Any, error: Any) -> HookResult:
        if not error and request_name in ACTION_REQUESTS:
            if self.get_setting("send_offline_after_action", True):
                self._send_offline_packet(account)
        return HookResult()

    def on_update_hook(self, update_name: str, account: int, update: Any) -> HookResult:
        if update_name in EDIT_UPDATES:
            self._handle_edit_update(account, update)
        return HookResult()

    def _send_offline_packet(self, account: int):
        def worker():
            try:
                TLRPC = find_class("org.telegram.tgnet.TLRPC")
                if TLRPC:
                    req = TLRPC.TL_account_updateStatus()
                    req.offline = True
                    mgr = get_connections_manager()
                    if mgr:
                        mgr.sendRequest(req, None)
            except Exception:
                pass
        threading.Thread(target=worker, daemon=True).start()

    def _handle_edit_update(self, account: int, update: Any):
        if not self.get_setting("save_edited_messages", True):
            return
        try:
            msg = getattr(update, "message", None)
            if not msg:
                return
            mid = int(getattr(msg, "id", 0))
            peer = getattr(msg, "peer_id", None)
            dialog_id = 0
            if peer:
                uid = getattr(peer, "user_id", 0)
                cid = getattr(peer, "chat_id", 0)
                chid = getattr(peer, "channel_id", 0)
                if uid:
                    dialog_id = int(uid)
                elif cid:
                    dialog_id = -int(cid)
                elif chid:
                    dialog_id = -int(chid)

            if not mid or not dialog_id:
                return

            storage = get_messages_storage()
            if storage:
                old_msg = storage.getMessageLegit(dialog_id, mid)
                if old_msg:
                    old_text = getattr(old_msg, "message", "")
                    new_text = getattr(msg, "message", "")
                    if old_text and old_text != new_text:
                        self._on_message_edited(dialog_id, mid, str(old_text))
        except Exception:
            pass

    def _init_storage(self):
        try:
            AppLoader = find_class("org.telegram.messenger.ApplicationLoader")
            if not AppLoader:
                return
            context = getattr(AppLoader, "applicationContext", None)
            if not context:
                return
            self._data_dir = os.path.join(str(context.getFilesDir()), "ayumode")
            os.makedirs(self._data_dir, exist_ok=True)

            del_file = os.path.join(self._data_dir, "deleted.json")
            if os.path.exists(del_file):
                with open(del_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self._deleted_info = data
                    self._deleted_messages = set(data.keys())

            edit_file = os.path.join(self._data_dir, "edits.json")
            if os.path.exists(edit_file):
                with open(edit_file, "r", encoding="utf-8") as f:
                    self._edit_history = json.load(f)
        except Exception:
            pass

    def _save_data_async(self):
        def worker():
            try:
                if not self._data_dir:
                    return
                del_file = os.path.join(self._data_dir, "deleted.json")
                with open(del_file + ".tmp", "w", encoding="utf-8") as f:
                    json.dump(self._deleted_info, f, ensure_ascii=False)
                os.replace(del_file + ".tmp", del_file)

                edit_file = os.path.join(self._data_dir, "edits.json")
                with open(edit_file + ".tmp", "w", encoding="utf-8") as f:
                    json.dump(self._edit_history, f, ensure_ascii=False)
                os.replace(edit_file + ".tmp", edit_file)
            except Exception:
                pass
        threading.Thread(target=worker, daemon=True).start()

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
        key = f"{dialog_id}_{message_id}"
        self._deleted_messages.add(key)

        text = ""
        try:
            storage = get_messages_storage()
            if storage:
                legit = storage.getMessageLegit(dialog_id, message_id)
                if legit:
                    text = str(getattr(legit, "message", ""))
        except Exception:
            pass

        self._deleted_info[key] = {
            "dialog_id": dialog_id,
            "message_id": message_id,
            "text": text,
            "time": int(time.time()),
        }
        self._save_data_async()

        if self._bridge:
            try:
                self._bridge.onMessageDeleted(dialog_id, message_id, text, 0)
            except Exception:
                pass

    def _on_message_edited(self, dialog_id: int, message_id: int, old_text: str):
        key = f"{dialog_id}_{message_id}"
        if key not in self._edit_history:
            self._edit_history[key] = []
        self._edit_history[key].append({
            "text": old_text,
            "time": int(time.time()),
        })
        self._save_data_async()

        if self._bridge:
            try:
                self._bridge.onMessageEdited(dialog_id, message_id, old_text, int(time.time()))
            except Exception:
                pass

    def is_deleted(self, dialog_id: int, message_id: int) -> bool:
        key = f"{dialog_id}_{message_id}"
        if key in self._deleted_messages:
            return True
        if self._bridge:
            try:
                return bool(self._bridge.isMessageDeleted(dialog_id, message_id))
            except Exception:
                pass
        return False

    def handle_message_info_click(self, context: dict):
        msg = context.get("message")
        if not msg:
            return
        try:
            mid = int(msg.getId())
            dialog_id = int(msg.getDialogId())
            key = f"{dialog_id}_{mid}"
            is_del = self.is_deleted(dialog_id, mid)
            edits = self._edit_history.get(key, [])

            info_lines = []
            if is_del:
                del_data = self._deleted_info.get(key, {})
                t_str = time.strftime("%H:%M:%S %d.%m.%Y", time.localtime(del_data.get("time", time.time())))
                info_lines.append(f"🗑️ СООБЩЕНИЕ УДАЛЕНО\nВремя фиксации: {t_str}")
                orig = del_data.get("text")
                if orig:
                    info_lines.append(f"\nИсходный текст:\n{orig}")
            else:
                info_lines.append("✅ Сообщение активно (не удалялось)")

            if edits:
                info_lines.append(f"\n✏️ ИСТОРИЯ ПРАВОК ({len(edits)}):")
                for idx, entry in enumerate(edits, 1):
                    t_str = time.strftime("%H:%M:%S %d.%m", time.localtime(entry.get("time", 0)))
                    info_lines.append(f"{idx}. [{t_str}] {entry.get('text')}")
            else:
                info_lines.append("\nПравки не зафиксированы.")

            fragment = get_last_fragment()
            activity = fragment.getParentActivity() if fragment else None
            if activity:
                builder = AlertDialogBuilder(activity)
                builder.set_title("AyuMode: Анализ сообщения")
                builder.set_message("\n".join(info_lines))
                builder.set_positive_button("Закрыть", lambda dialog, which: dialog.dismiss())
                builder.show()
        except Exception as e:
            try:
                BulletinHelper.show_error(f"Ошибка: {e}")
            except Exception:
                pass

    def handle_mark_chat_read(self, context: dict):
        dialog_id = context.get("dialog_id")
        if not dialog_id:
            chat = context.get("chat")
            user = context.get("user")
            if chat:
                dialog_id = -int(chat.id)
            elif user:
                dialog_id = int(user.id)
        if not dialog_id:
            try:
                BulletinHelper.show_error("Не удалось определить ID чата")
            except Exception:
                pass
            return
        try:
            self._allow_read_dialog = int(dialog_id)
            controller = get_messages_controller()
            if controller:
                controller.markDialogAsRead(int(dialog_id), 0, 0, 0, False, 0, 0, True, 0)
            self._allow_read_dialog = 0
            BulletinHelper.show_success("Чат отмечен прочитанным")
        except Exception as e:
            self._allow_read_dialog = 0
            try:
                BulletinHelper.show_error(f"Ошибка: {e}")
            except Exception:
                pass

    def create_settings(self) -> List[Any]:
        return [
            Header(text="👻 Режим призрака (AyuGhost)"),
            Switch(
                key="ghost_mode",
                text="Режим призрака",
                default=True,
                subtext="Главный переключатель скрытности",
                icon="msg_stealth",
            ),
            Switch(
                key="read_messages_ghost",
                text="Не читать сообщения",
                default=True,
                subtext="Не отправлять статус прочтения при открытии чатов",
                icon="msg_views",
            ),
            Switch(
                key="typing_ghost",
                text="Скрывать статус «печатает...»",
                default=True,
                subtext="Собеседник не видит набор текста, аудио и медиа",
                icon="msg_edit",
            ),
            Switch(
                key="online_ghost",
                text="Скрывать статус «в сети»",
                default=True,
                subtext="Принудительный оффлайн при использовании Telegram",
                icon="msg_user",
            ),
            Switch(
                key="stories_ghost",
                text="Невидимка в историях",
                default=True,
                subtext="Анонимный просмотр историй без отметки в списке зрителей",
                icon="msg_stories",
            ),
            Switch(
                key="voice_video_ghost",
                text="Невидимка в голосовых и кружочках",
                default=True,
                subtext="Не отмечать прослушивание войсов и кружков",
                icon="msg_voice",
            ),
            Switch(
                key="send_offline_after_action",
                text="Оффлайн после действий",
                default=True,
                subtext="Сразу возвращать оффлайн статус после отправки сообщений",
                icon="msg_retry",
            ),
            Switch(
                key="mark_read_on_send",
                text="Прочитать при ответе",
                default=False,
                subtext="Отмечать входящие прочитанными только при вашем ответе",
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
                subtext="Сохранять первоначальный текст при редактировании",
                icon="msg_history",
            ),
            Switch(
                key="save_media_ttl",
                text="Сохранять самоуничтожающиеся медиа",
                default=True,
                subtext="Таймерные фото и видео не исчезают после просмотра",
                icon="msg_secret",
            ),
            Divider(),
            Header(text="🔓 Обход ограничений (Bypass)"),
            Switch(
                key="bypass_flag_secure",
                text="Скриншоты везде",
                default=True,
                subtext="Снятие FLAG_SECURE для скриншотов и записи экрана",
                icon="msg_permissions",
            ),
            Switch(
                key="force_download_media",
                text="Скачивание из закрытых каналов",
                default=True,
                subtext="Принудительное сохранение и пересылка закрытого контента",
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
            Text(text="AyuMode v1.1.0", icon="msg_info"),
            Text(text="Автор: wKey ( @exterawKey )", icon="msg_contacts"),
            Text(text="Репозиторий: github.com/exterawKey/AyuMode", icon="msg_link"),
        ]
