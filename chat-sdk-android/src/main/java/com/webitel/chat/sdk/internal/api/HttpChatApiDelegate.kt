package com.webitel.chat.sdk.internal.api

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.ContactRequest
import com.webitel.chat.sdk.DialogFilter
import com.webitel.chat.sdk.DialogRequest
import com.webitel.chat.sdk.DialogType
import com.webitel.chat.sdk.DownloadListener
import com.webitel.chat.sdk.HistoryCursor
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.MoveDirection
import com.webitel.chat.sdk.Page
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.client.ClientContext
import com.webitel.chat.sdk.internal.client.CompositeCancellable
import com.webitel.chat.sdk.internal.client.ExecutionContext
import com.webitel.chat.sdk.internal.extensions.toChatError
import com.webitel.chat.sdk.internal.transport.dto.ContactDto
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.ParticipantDto
import com.webitel.chat.sdk.internal.transport.http.OkHttpCancellable
import com.webitel.chat.sdk.internal.transport.http.safeCall
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


internal class HttpChatApiDelegate(
    private val clientContext: ClientContext,
    private val execution: ExecutionContext,
    private val httpClient: OkHttpClient
): ChatApiDelegate {

    private companion object {
        const val TAG = "HttpChatApiDelegate"
        const val DIALOGS_PATH = "api/v1/threads"
        const val CONTACTS_PATH = "api/v1/contacts"
        const val SEND_TEXT_PATH = "api/v1/messages/text"
        const val REGISTER_DEVICE_PATH = "api/v1/auth/devices"
    }


    override fun sendMessage(
        target: MessageTarget,
        options: MessageOptions,
        onComplete: (Result<String>) -> Unit
    ): Cancellable {
        val composite = CompositeCancellable()

        execution.api {
            if (composite.isCanceled()) return@api

            val json = buildMessageJson(target, options)
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(buildSendMessageUrl())
                .post(body)
                .build()

            logger.debug(TAG, request.toString())
            logger.debug(TAG, json.toString())

            val call = httpClient.newCall(request)
            composite.add(OkHttpCancellable(call))
            try {
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (call.isCanceled()) return
                        logger.error(TAG,
                            "sendMessage: onFailure ${e.message.toString()}"
                        )
                        onComplete(Result.failure(ChatError.fromCode(
                            ChatError.UNKNOWN_CODE,e.message))
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            val result = parseSendMessageResponse(it)
                            onComplete(result)
                        }
                    }
                })
            } catch (e: Exception) {
                onComplete(Result.failure(ChatError.fromCode(
                    ChatError.UNKNOWN_CODE,e.message))
                )
            }

        }
        return composite
    }


    override fun getDialogs(
        request: DialogRequest,
        onComplete: (Result<Page<DialogDto>>) -> Unit
    ) {
        execution.api {
            val call = runCatching {
                val httpRequest = Request.Builder()
                    .url(buildDialogsUrl(request))
                    .get()
                    .build()

                logger.debug(TAG, "getDialogs: $httpRequest")

                httpClient.newCall(httpRequest)
            }.getOrElse { error ->
                onComplete(Result.failure(error.toChatError()))
                return@api
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logger.error(TAG,
                        "getDialogs: onFailure ${e.message.toString()}"
                    )
                    onComplete(Result.failure(e.toChatError()))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        onComplete(parseDialogsResponse(res, request))
                    }
                }
            })
        }
    }


    override fun getContacts(
        request: ContactRequest,
        onComplete: (Result<Page<ContactDto>>) -> Unit
    ) {
        execution.api {
            val call = runCatching {
                val httpRequest = Request.Builder()
                    .url(buildContactsUrl(request))
                    .get()
                    .build()

                logger.debug(TAG, "getContacts: $httpRequest")
                httpClient.newCall(httpRequest)
            }.getOrElse { error ->
                onComplete(Result.failure(error.toChatError()))
                return@api
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logger.error(TAG,
                        "getContacts: onFailure ${e.message.toString()}"
                    )
                    onComplete(Result.failure(e.toChatError()))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        onComplete(parseContactsResponse(res, request))
                    }
                }
            })
        }
    }


    override fun getHistory(
        dialogId: String,
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<MessageDto>>) -> Unit
    ) {
        execution.api {
            val call = runCatching {
                val httpRequest = Request.Builder()
                    .url(buildHistoryUrl(dialogId,request))
                    .get()
                    .build()

                logger.debug(TAG, "getHistory: $httpRequest")

                httpClient.newCall(httpRequest)
            }.getOrElse { error ->
                onComplete(Result.failure(error.toChatError()))
                return@api
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logger.error(TAG,
                        "getHistory: onFailure ${e.message.toString()}"
                    )
                    onComplete(Result.failure(e.toChatError()))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        onComplete(parseHistoryResponse(res, request))
                    }
                }
            })
        }
    }


    override fun registerDevice(
        pushToken: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        execution.api {
            onComplete(registerDevice(pushToken))
        }
    }


    override fun downloadFile(
        dialogId: String,
        fileId: String,
        offset: Long?,
        listener: DownloadListener
    ): Cancellable {
        TODO("Not yet implemented")
    }


    private fun registerDevice(pushToken: String): Result<Unit> {
        return safeCall(logger, TAG) {
            logger.debug(TAG, "registerDevice: $pushToken")
            val json = JSONObject().apply {
                put("fcm", pushToken)
            }

            val body = json.toString()
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url(buildRegisterDeviceUrl())
                .post(body)
                .build()
            logger.debug(TAG, "registerDevice: $httpRequest")
            logger.debug(TAG, "registerDevice: $json")

            httpClient.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()

                if (!response.isSuccessful) {
                    logger.error(TAG,
                        "registerDevice: ${response.code} $bodyString"
                    )
                    throw ChatError.Companion.fromCode(
                        response.code, bodyString
                    )
                }

                logger.debug(TAG, "registerDevice: success - $bodyString")

                return@safeCall
            }
        }
    }


    private fun buildContactsUrl(request: ContactRequest): HttpUrl =
        HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .addPathSegments(CONTACTS_PATH)
            .addQueryParameter("page", request.page.toString())
            .addQueryParameter("size", request.size.toString())
            .apply {
                if (clientContext.port > 0)
                    port(clientContext.port)
            }
            .build()


    private fun buildDialogsUrl(request: DialogRequest): HttpUrl {
        return HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .apply {
                if (clientContext.port > 0) {
                    port(clientContext.port)
                }
            }
            .addPathSegments(DIALOGS_PATH)
            .addQueryParameter("page", request.page.toString())
            .addQueryParameter("size", request.size.toString())
            .apply {
                addFields()
                request.filter?.let { addFilter(it) }
            }
            .build()
    }


    private fun HttpUrl.Builder.addFields() {
        listOf(
            "members",
            "id",
            "subject",
            "kind",
            "last_msg"
        ).forEach {
            addQueryParameter("fields", it)
        }
    }


    private fun HttpUrl.Builder.addFilter(filter: DialogFilter) {
        filter.query?.let {
            addQueryParameter("q", it)
        }

        addArray("ids", filter.ids)

        val types = filter.types
            ?.mapNotNull { it.toApiKind() }
            ?.map { it.toString() }

        addArray("types", types)
    }


    private fun HttpUrl.Builder.addArray(
        name: String,
        values: List<String>?
    ) {
        values?.forEach {
            addQueryParameter(name, it)
        }
    }


    private fun DialogType.toApiKind(): Int? = when (this) {
        DialogType.DIRECT -> 1
        DialogType.GROUP -> 2
        DialogType.CHANNEL -> 3
        DialogType.UNKNOWN -> null
    }


    private fun buildRegisterDeviceUrl(): HttpUrl =
        HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .addPathSegments(REGISTER_DEVICE_PATH)
            .apply {
                if (clientContext.port > 0)
                    port(clientContext.port)
            }
            .build()


    private fun buildSendMessageUrl(): HttpUrl =
         HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .addPathSegments(SEND_TEXT_PATH)
            .apply {
                if (clientContext.port > 0)
                    port(clientContext.port)
            }
            .build()


    private fun parseDialogsResponse(
        res: Response,
        request: DialogRequest
    ): Result<Page<DialogDto>> {
        val bodyString = res.body?.string()
        logger.debug(TAG, "parseDialogsResponse: $bodyString")

        if (!res.isSuccessful) {
            logger.error(
                TAG,
                "parseDialogsResponse: Code=${res.code} $bodyString"
            )
            return Result.failure(
                ChatError.fromCode(res.code, bodyString)
            )
        }

        if (bodyString.isNullOrEmpty()) {
            logger.error(
                TAG,
                "parseDialogsResponse: Empty response body. Code=${res.code}"
            )
            return Result.failure(
                ChatError.fromCode(ChatError.UNKNOWN_CODE,
                    "Empty response body")
            )
        }

        return runCatching {
            val root = JSONObject(bodyString)

            val page = root.optInt("page", request.page)
            val hasNext = root.optBoolean("next", false)
            val items = parseDialogsArray(root.optJSONArray("items"))

            Page(
                page = page,
                items = items,
                hasNext = hasNext
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toChatError()) }
        )
    }


    private fun parseHistoryResponse(
        res: Response,
        request: HistoryRequest
    ): Result<HistorySlice<MessageDto>> {
        val bodyString = res.body?.string()
        logger.debug(TAG, "parseHistoryResponse: $bodyString")

        if (!res.isSuccessful) {
            logger.error(
                TAG,
                "parseHistoryResponse: Code=${res.code} $bodyString"
            )
            return Result.failure(
                ChatError.fromCode(res.code, bodyString)
            )
        }

        if (bodyString.isNullOrEmpty()) {
            logger.error(
                TAG,
                "parseHistoryResponse: Empty response body. Code=${res.code}"
            )
            return Result.failure(
                ChatError.fromCode(ChatError.UNKNOWN_CODE,
                    "Empty response body")
            )
        }

        return runCatching {
            val root = JSONObject(bodyString)

            val items = parseMessagesArray(root.optJSONArray("items"))
                .reversed()
            val paging = parsePaging(root)

            HistorySlice(
                items = items,
                newerCursor = paging.component1(),
                olderCursor = paging.component2()
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toChatError()) }
        )
    }


    private fun parsePaging(obj: JSONObject?): Pair<HistoryCursor?, HistoryCursor?> {
        if (obj == null) return Pair(null, null)

        val oldObj = obj.optJSONObject("next_cursor")
        val newObj = obj.optJSONObject("prev_cursor")

        val newer = parseCursor(newObj, MoveDirection.NEWER)
        val older = parseCursor(oldObj, MoveDirection.OLDER)
        return Pair(newer, older)
    }


    private fun parseCursor(obj: JSONObject?, direction: MoveDirection): HistoryCursor? {
        if (obj == null) return null
        val id = obj.optString("id")
        if (id.isEmpty()) return null
        return HistoryCursor(
            id,
            direction
        )
    }


    private fun parseContactsResponse(
        res: Response,
        request: ContactRequest
    ): Result<Page<ContactDto>> {
        val bodyString = res.body?.string()
        logger.debug(TAG, "parseContactsResponse: $bodyString")

        if (!res.isSuccessful) {
            logger.error(
                TAG,
                "parseContactsResponse: Code=${res.code} $bodyString"
            )
            return Result.failure(
                ChatError.fromCode(res.code, bodyString)
            )
        }

        if (bodyString.isNullOrEmpty()) {
            logger.error(
                TAG,
                "parseContactsResponse: Empty response body. Code=${res.code}"
            )
            return Result.failure(
                ChatError.fromCode(ChatError.UNKNOWN_CODE,
                    "Empty response body")
            )
        }

        return runCatching {
            val root = JSONObject(bodyString)

            val page = root.optInt("page", request.page)
            val hasNext = root.optBoolean("next", false)
            val items = parseContactsArray(root.optJSONArray("items"))

            Page(
                page = page,
                items = items,
                hasNext = hasNext
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toChatError()) }
        )
    }


    private fun parseContactsArray(array: JSONArray?): List<ContactDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                parseContact(array.optJSONObject(i))?.let(::add)
            }
        }

    
    private fun parseMessagesArray(array: JSONArray?): List<MessageDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseMessage(obj)?.let(::add)
            }
        }


    private fun parseParticipantArray(array: JSONArray?): List<ParticipantDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseParticipant(obj)?.let(::add)
            }
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


    private fun parseDialogsArray(array: JSONArray?): List<DialogDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseDialog(obj)?.let(::add)
            }
        }


    private fun parseDialog(obj: JSONObject): DialogDto? {
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


    private fun parseContact(obj: JSONObject?): ContactDto? {
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

    
    private fun parseMessage(messageObj: JSONObject?): MessageDto? {
        messageObj ?: return null
        val sender = parseParticipant(
            messageObj.optJSONObject("sender")
        ) ?: return null

        val id = messageObj.optString("id")
        if (id.isNullOrEmpty()) return null

        val dialogId = messageObj.optString("thread_id")
        val createdAt = messageObj.optLong("created_at")
        val updatedAt = messageObj.optLong("edited_at")
        val text = messageObj.optString("body")
        val sendId = messageObj.optString("send_id").takeIf { it.isNotEmpty() }

        return MessageDto(
            id = id,
            dialogId = dialogId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            from = sender,
            text = text,
            sendId = sendId
        )
    }


    private fun buildHistoryUrl(
        dialogId: String,
        request: HistoryRequest
    ): HttpUrl =
        HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .addPathSegment("api")
            .addPathSegment("v1")
            .addPathSegment(dialogId)
            .addPathSegment("messages")
            .addQueryParameter("size", request.limit.toString())
            .apply {
                request.cursor?.let { cursor ->
                    addQueryParameter("cursor.id", cursor.messageId)
                    addQueryParameter(
                        "cursor.before",
                        if (cursor.direction == MoveDirection.NEWER) "true" else "false"
                    )
                }
                if (clientContext.port > 0)
                    port(clientContext.port)
            }
            .build()


    private fun parseSendMessageResponse(response: Response): Result<String> {
        return safeCall(logger, TAG) {
            val bodyString = response.body?.string()
            logger.debug(TAG, "parseSendMessageResponse: $bodyString")

            if (!response.isSuccessful) {
                logger.error(TAG,
                    "sendMessage: ${response.code} $bodyString"
                )
                throw ChatError.Companion.fromCode(
                    response.code, bodyString
                )
            }

            if (bodyString == null) {
                logger.error(
                    TAG,
                    "parseSendMessageResponse: Empty response body. Code=${response.code}"
                )
                throw ChatError.Companion.fromCode(
                    ChatError.Companion.UNKNOWN_CODE, "Empty response body"
                )
            }

            val json = JSONObject(bodyString)
            val newMessageId = json.optString("id")

            if (newMessageId.isNullOrEmpty()) {
                logger.error(
                    TAG,
                    "sendMessage: new_message_id not found in response $json"
                )
                throw ChatError.Companion.fromCode(
                    ChatError.Companion.UNKNOWN_CODE,
                    "new_message_id not found in response"
                )
            } else {
                return@safeCall newMessageId
            }
        }
    }


    private fun buildMessageJson(target: MessageTarget,
                          options: MessageOptions): JSONObject {
        return JSONObject().apply {
            options.text?.let { put("body", it) }
            put("send_id", options.sendId)

            put("to", JSONObject().apply {
                when (target) {
                    is MessageTarget.Contact -> {
                        put("contact", JSONObject().apply {
                            put("iss", target.contactId.iss)
                            put("sub", target.contactId.sub)
                        })
                    }

                    is MessageTarget.Dialog -> {
                        put("thread_id", target.id)
                    }
                }
            })
        }
    }
}