package com.webitel.chat.sdk.internal.transport.parser

import com.webitel.chat.sdk.ChatButtonAction
import com.webitel.chat.sdk.ChatKeyboard
import com.webitel.chat.sdk.ChatKeyboardButton
import com.webitel.chat.sdk.ChatKeyboardRow
import com.webitel.chat.sdk.ChatKeyboardSection
import com.webitel.chat.sdk.MessageAttachment
import com.webitel.chat.sdk.MessageContent
import com.webitel.chat.sdk.internal.transport.dto.ContactDto
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.ParticipantDto
import org.json.JSONArray
import org.json.JSONObject


internal class Parser {
    fun parseMessage(messageObj: JSONObject?): MessageDto? {
        messageObj ?: return null
        val sender = parseParticipant(
            messageObj.optJSONObject("sender")
        ) ?: return null

        val id = messageObj.optString("id")
        if (id.isNullOrEmpty()) return null

        val dialogId = messageObj.optString("thread_id")
        val createdAt = messageObj.optLong("created_at")
        val updatedAt = messageObj.optLong("edited_at")
        val sendId = messageObj.optString("send_id").takeIf { it.isNotEmpty() }

        val content = parseContent(messageObj) ?: return null
        return MessageDto(
            id = id,
            dialogId = dialogId,
            createdAt = createdAt,
            editedAt = updatedAt,
            from = sender,
            content = content,
            sendId = sendId
        )
    }


    fun parseContact(obj: JSONObject?): ContactDto? {
        obj ?: return null

        val id = obj.optString("sub")
        val iss = obj.optString("iss")

        if (id.isNullOrEmpty() || iss.isNullOrEmpty()) return null

        val name = obj.optString("name", "unknown")
        val source = obj.optString("type", iss)
        val isBot = obj.optBoolean("is_bot")

        return ContactDto(
            iss = iss,
            name = name,
            id = id,
            source = source,
            isBot = isBot
        )
    }


    fun parseDialog(obj: JSONObject?): DialogDto? {
        obj ?: return null
        val id = obj.optString("id")
        if (id.isNullOrEmpty()) return null

        val subject = obj.optString("subject")
        val type = obj.optString("type")

        val members = parseParticipantArray(obj.optJSONArray("members"))

        val lastMsgObj = obj.optJSONObject("last_msg")
        val lastMessage = parseMessage(lastMsgObj)

        return DialogDto(
            id = id,
            subject = subject,
            type = type,
            members = members,
            lastMessage = lastMessage
        )
    }


    private fun parseContent(obj: JSONObject): MessageContent? {
        obj.optJSONObject("contact")?.let { contact ->
            return MessageContent.Contact(
                name = contact.optString("name"),
                phone = contact.optString("phone"),
                email = contact.optString("email").takeIf { it.isNotEmpty() }
            )
        }

        obj.optJSONObject("location")?.let { location ->
            return MessageContent.Location(
                name = location.optString("name"),
                address = location.optString("address"),
                latitude = location.optDouble("latitude"),
                longitude = location.optDouble("longitude")
            )
        }

        obj.optJSONObject("system")?.let { system ->
            val metadataMap = if (system.has("metadata") && !system.isNull("metadata")) {
                val metadataObj = system.optJSONObject("metadata")
                metadataObj?.toMap()
            } else {
                null
            }
            return MessageContent.System(
                type = system.optString("type"),
                text = obj.optString("body"),
                metadata = metadataMap
            )
        }

        val text = obj.optString("body")
            .takeIf { it.isNotBlank() }

        val attachments = parseAttachments(obj)

        val keyboard = obj
            .optJSONObject("interactive")
            ?.let { parseKeyboard(it) }

        if (
            listOf(
                text != null,
                attachments.isNotEmpty(),
                keyboard != null
            ).count { it } >= 2
        ) {
            return MessageContent.Composite(
                text = text,
                attachments = attachments,
                keyboard = keyboard
            )
        }

        if (text != null) {
            return MessageContent.Text(text)
        }

        if (keyboard != null) {
            return MessageContent.KeyboardOnly(keyboard)
        }

        if (attachments.isNotEmpty()) {
            return MessageContent.Attachments(attachments)
        }

        return null
    }


    private fun parseAttachments(obj: JSONObject): List<MessageAttachment> {
        val documents = obj.optJSONArray("documents") ?: return emptyList()
        return buildList {
            for (i in 0 until documents.length()) {
                val item = documents.optJSONObject(i) ?: continue
                add(
                    MessageAttachment(
                        fileId = item.optString("id"),
                        fileName = item.optString("name"),
                        mimeType = item.optString("mime"),
                        size = item.optString("size")
                            .toLongOrNull() ?: 0,
                        url = item.optString("url")
                            .takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }


    private fun parseKeyboard(obj: JSONObject): ChatKeyboard? {
        obj.optJSONObject("list_reply")?.let { list ->
            return ChatKeyboard.ListMenu(
                title = list.optString("main_button_title"),
                sections = parseSections(list.optJSONArray("sections"))
            )
        }

        obj.optJSONObject("markup")?.let { markdown ->
            return ChatKeyboard.Buttons(
                rows = parseRows(markdown.optJSONArray("rows"))
            )
        }

        return null
    }


    private fun parseSections(array: JSONArray?): List<ChatKeyboardSection> =
        buildList {
            if (array == null) return@buildList
            for (i in 0 until array.length()) {

                val obj = array.optJSONObject(i) ?: continue
                val title = obj.optString("section")

                val buttons = buildList {
                    val btnArray = obj.optJSONArray("buttons") ?: return@buildList
                    for (j in 0 until btnArray.length()) {
                        val btnObj = btnArray.optJSONObject(j) ?: continue
                        parseButton(btnObj)?.let(::add)
                    }
                }
                add(ChatKeyboardSection(title, buttons))
            }
        }


    private fun parseRows(array: JSONArray?): List<ChatKeyboardRow> =
        buildList {
            if (array == null) return@buildList
            for (i in 0 until array.length()) {
                val rowObj = array.optJSONObject(i) ?: continue
                val buttons = buildList {
                    val btnArray = rowObj.optJSONArray("buttons") ?: return@buildList
                    for (j in 0 until btnArray.length()) {
                        val btnObj = btnArray.optJSONObject(j) ?: continue
                        parseButton(btnObj)?.let(::add)
                    }
                }
                if (buttons.isNotEmpty()) {
                    add(ChatKeyboardRow(buttons))
                }
            }
        }


    private fun parseButton(obj: JSONObject): ChatKeyboardButton? {
        val id = obj.optString("id")
        if (id.isEmpty()) return null

        val label = obj.optString("label")
        val action = parseAction(obj) ?: return null

        val metadataMap = if (obj.has("metadata") && !obj.isNull("metadata")) {
            val metadataObj = obj.optJSONObject("metadata")
            metadataObj?.toMap()
        } else {
            null
        }

        return ChatKeyboardButton(
            id = id,
            label = label,
            action = action,
            metadata = metadataMap
        )
    }


    fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            var value = this.get(key)
            if (value == JSONObject.NULL) {
                continue
            }
            if (value is JSONObject) {
                value = value.toMap()
            } else if (value is JSONArray) {
                value = value.toList()
            }
            map[key] = value
        }
        return map
    }


    fun JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until this.length()) {
            var value = this.get(i)
            if (value is JSONObject) {
                value = value.toMap()
            } else if (value is JSONArray) {
                value = value.toList()
            }
            list.add(value)
        }
        return list
    }


    private fun parseAction(obj: JSONObject): ChatButtonAction? {
        obj.optJSONObject("callback")?.let {
            val data = it.optString("data")
            if (data.isNotEmpty()) {
                return ChatButtonAction.SendCallback(data)
            }
        }

        obj.optJSONObject("url")?.let {
            val url = it.optString("url")
            if (url.isNotEmpty()) {
                return ChatButtonAction.OpenUrl(url)
            }
        }

        obj.optJSONObject("request")?.let {
            val action = it.optString("action")
            if (action.isNotEmpty()) {
                return ChatButtonAction.RequestData(action)
            }
        }
        return null
    }


    private fun parseParticipant(obj: JSONObject?): ParticipantDto? {
        obj ?: return null

        val id = obj.optString("id")
        if (id.isNullOrEmpty() ) return null

        val contact = parseContact(
            obj.optJSONObject("contact")
        ) ?: return null

        val role = obj.optString("role", "ROLE_UNSPECIFIED")

        return ParticipantDto(
            id = id,
            contact = contact,
            role = role
        )
    }


    private fun parseParticipantArray(array: JSONArray?): List<ParticipantDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseParticipant(obj)?.let(::add)
            }
        }
}