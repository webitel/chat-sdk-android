package com.webitel.chat.sdk.internal.api

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.ContactRequest
import com.webitel.chat.sdk.DialogRequest
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
        const val REGISTER_DEVICE_PATH = "api/v1/auth/device"
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
                .put(body)
                .build()

            logger.debug(TAG, "registerDevice: $httpRequest")

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


    private fun buildDialogsUrl(request: DialogRequest): HttpUrl =
        HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .addPathSegments(DIALOGS_PATH)
            .addQueryParameter("page", request.page.toString())
            .addQueryParameter("size", request.size.toString())
            .addQueryParameter("fields", "members")
            .addQueryParameter("fields", "id")
            .addQueryParameter("fields", "subject")
            .addQueryParameter("fields", "kind")
            .apply {
                if (clientContext.port > 0)
                    port(clientContext.port)
            }
            .build()


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
            val items = parseDialogsArray(root.optJSONArray("threads"))

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

            val items = parseMessagesArray(root.optJSONArray("messages")).let {
                if (request.cursor?.direction == MoveDirection.AFTER) it else it.reversed()
            }
            val paging = parsePaging(root.optJSONObject("paging"))

            HistorySlice(
                items = items,
                beforeCursor = paging.component1(),
                afterCursor = paging.component2()
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toChatError()) }
        )
    }


    private fun parsePaging(obj: JSONObject?): Pair<HistoryCursor?, HistoryCursor?> {
        if (obj == null) return Pair(null, null)
        val cursors = obj.optJSONObject("cursors")
        val aftObj = cursors?.optJSONObject("after")
        val befObj = cursors?.optJSONObject("before")

        val before = parseCursor(befObj)
        val after = parseCursor(aftObj)
        return Pair(before, after)
    }


    private fun parseCursor(obj: JSONObject?): HistoryCursor? {
        if (obj == null) return null
        val createdAt = obj.optLong("created_at", 0)
        val direction = obj.optBoolean("id", false)
        val id = obj.optString("id")
        if (id.isEmpty()) return null
        return HistoryCursor(
            id,
            createdAt,
            if (direction) MoveDirection.BEFORE
            else MoveDirection.AFTER
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
                val obj = array.optJSONObject(i) ?: continue
                mapContact(obj)?.let(::add)
            }
        }

    
    private fun parseMessagesArray(array: JSONArray?): List<MessageDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val senderObj = obj.optJSONObject("sender")
                mapMessage(obj, senderObj)?.let(::add)
            }
        }


    private fun parseContactsInDialogArray(array: JSONArray?): List<ContactDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                mapContactInDialog(obj)?.let(::add)
            }
        }


    private fun parseDialogsArray(array: JSONArray?): List<DialogDto> =
        buildList {
            if (array == null) return@buildList

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                mapDialog(obj)?.let(::add)
            }
        }


    private fun mapDialog(obj: JSONObject): DialogDto? {
        val id = obj.optString("id")
        if (id.isNullOrEmpty()) return null

        val subject = obj.optString("subject")
        val type = obj.optString("kind")

        val members = parseContactsInDialogArray(obj.optJSONArray("members"))

        return DialogDto(
            id = id,
            subject = subject,
            type = type,
            members = members,
            lastMessage = null
        )
    }


    private fun mapContact(obj: JSONObject): ContactDto? {
        val id = obj.optString("subject")
        val iss = obj.optString("iss_id")

        if (id.isNullOrEmpty() || iss.isNullOrEmpty()) return null

        val source = obj.optString("type", iss)
        val isBot = obj.optBoolean("is_bot")

        return ContactDto(
            iss = iss,
            name = obj.optString("name", "unknown"),
            id = id,
            source = source,
            isBot = isBot
        )
    }

    
    private fun mapMessage(messageObj: JSONObject?, fromObj: JSONObject?): MessageDto? {
        messageObj ?: return null
        fromObj ?: return null

        val id = messageObj.optString("id")

        if (id.isNullOrEmpty()) return null

        val dialogId = messageObj.optString("thread_id")
        val createdAt = messageObj.optLong("created_at")
        val updatedAt = messageObj.optLong("updated_at")
        val text = messageObj.optString("body")
        val sendId = messageObj.optString("send_id").takeIf { it.isNotEmpty() }

        val contactId = fromObj.optString("subject")
        val iss = fromObj.optString("issuer")
        val name = fromObj.optString("username").ifBlank { "unknown" }

        val source = fromObj.optString("type", iss)
        val isBot = fromObj.optBoolean("is_bot")

        val from = ContactDto(
            id = contactId,
            iss = iss,
            name = name,
            source = source,
            isBot = isBot
        )

        return MessageDto(
            id = id,
            dialogId = dialogId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            from = from,
            text = text,
            sendId = sendId
        )
    }


    private fun mapContactInDialog(obj: JSONObject): ContactDto? {
        val memberObj = obj.optJSONObject("member") ?: return null

        val id = memberObj.optString("subject")
        val iss = memberObj.optString("issuer")
        val username = memberObj.optString("username")

        if (id.isNullOrEmpty() || iss.isNullOrEmpty()) return null

        val source = memberObj.optString("type", iss)
        val isBot = memberObj.optBoolean("is_bot")

        return ContactDto(
            iss = iss,
            source = source,
            name = username,
            id = id,
            isBot = isBot
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
                    addQueryParameter("cursor.created_at", cursor.createdAt.toString())
                    addQueryParameter(
                        "cursor.direction",
                        if (cursor.direction == MoveDirection.BEFORE) "true" else "false"
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