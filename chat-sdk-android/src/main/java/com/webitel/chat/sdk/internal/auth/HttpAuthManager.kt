package com.webitel.chat.sdk.internal.auth

import com.webitel.chat.sdk.AuthMethod
import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.ContactIdentity
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.client.ClientContext
import com.webitel.chat.sdk.internal.client.ExecutionContext
import com.webitel.chat.sdk.internal.transport.http.HeaderProvider
import com.webitel.chat.sdk.internal.extensions.toJsonObject
import com.webitel.chat.sdk.internal.transport.dto.ContactDto
import com.webitel.chat.sdk.internal.transport.http.safeCall
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


internal class HttpAuthManager(
    private val clientContext: ClientContext,
    private val execution: ExecutionContext,
    private val httpClient: OkHttpClient,
    private val headerProvider: HeaderProvider
)
    : AuthManager {

    private val listeners = mutableSetOf<(String) -> Unit>()
    override var currentContact: ContactDto? = null
    private val lock = Any()
    private var refreshing = false
    private val pending = mutableListOf<(Result<Unit>) -> Unit>()

    private companion object {
        const val TAG = "HttpAuthManager"
        const val LOGIN_PATH = "api/v1/auth/token"
        const val LOGOUT_PATH = "api/v1/auth/logout"
    }


    override fun refresh(onComplete: (Result<Unit>) -> Unit) {
        synchronized(lock) {
            if (refreshing) {
                logger.debug(TAG,
                    "refresh: auth is refreshing. Added to queue"
                )
                pending += onComplete
                return
            }

            refreshing = true
            pending += onComplete
        }

        execution.api {
            val result = performRefresh()

            synchronized(lock) {
                refreshing = false

                val callbacks = pending.toList()
                pending.clear()

                callbacks.forEach { cb ->
                    cb(result)
                }
            }
        }
    }


    override fun endSession(onComplete: (Result<Unit>) -> Unit) {
        execution.api {
            onComplete(userLogout())
        }
    }


    fun addTokenListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }


    private fun userLogout(): Result<Unit> {
        return safeCall(logger, TAG) {
            logger.debug(TAG, "userLogout()")

            if (!headerProvider.hasAuth()) {
                logger.debug(TAG, "userLogout: no auth session, skipping API call")
                return Result.success(Unit)
            }

            val json = JSONObject()
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url(buildLogoutUrl())
                .post(body)
                .build()

            logger.debug(TAG, "userLogout: $httpRequest")

            httpClient.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()

                if (!response.isSuccessful) {
                    logger.error(TAG, "userLogout: ${response.code} $bodyString")
                    throw ChatError.Companion.fromCode(response.code, bodyString)
                }

                logger.debug(TAG, "userLogout: $bodyString")

                return@safeCall
            }
        }
    }


    private fun buildLogoutUrl(): HttpUrl =
        HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .addPathSegments(LOGOUT_PATH)
            .apply {
                if (clientContext.port > 0)
                    port(clientContext.port)
            }
            .build()


    private fun inspect(): Result<Unit> {
        return safeCall(logger, TAG) {
            val httpRequest = Request.Builder()
                .url(buildLoginUrl())
                .get()
                .build()

            logger.debug(TAG, "inspect: $httpRequest")

            httpClient.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()

                if (!response.isSuccessful) {
                    logger.error(TAG, "inspect: ${response.code} $bodyString")
                    throw ChatError.Companion.fromCode(response.code, bodyString)
                }

                if (bodyString == null) {
                    logger.error(TAG,
                        "inspect: Empty response body. Code=${response.code}"
                    )
                    throw ChatError.Companion.fromCode(
                        ChatError.Companion.UNKNOWN_CODE,
                        "Empty response body"
                    )
                }

                logger.debug(TAG, "inspect: $bodyString")

                val json = JSONObject(bodyString)

                val user = json.optJSONObject("contact")
                val contact = createContactFromResponse(user)

                if (contact == null) {
                    throw ChatError.Companion.fromCode(
                        ChatError.Companion.UNKNOWN_CODE,
                        "contact.sub or contact.iss not found"
                    )
                }

                currentContact = contact
                return@safeCall
            }
        }
    }


    private fun userLogin(contact: ContactIdentity): Result<String> {
        return safeCall(logger, TAG) {
            logger.debug(TAG, "userLogin: $contact")
            val json = JSONObject().apply {
                put("client_id", clientContext.clientToken)
                put("identity", (contact.toJsonObject()))
            }

            val body = json.toString()
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url(buildLoginUrl())
                .post(body)
                .build()

            logger.debug(TAG, "userLogin: $httpRequest")

            httpClient.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()

                if (!response.isSuccessful) {
                    logger.error(TAG, "userLogin: ${response.code} $bodyString")
                    throw ChatError.Companion.fromCode(response.code, bodyString)
                }

                if (bodyString == null) {
                    logger.error(TAG,
                        "userLogin: Empty response body. Code=${response.code}"
                    )
                    throw ChatError.Companion.fromCode(
                        ChatError.Companion.UNKNOWN_CODE, "Empty response body"
                    )
                }
                logger.debug(TAG, "userLogin: $bodyString")

                val json = JSONObject(bodyString)
                val token = json.optJSONObject("token")
                val access = token?.optString("access_token")

                if (access.isNullOrEmpty()) {
                    logger.error(
                        TAG,
                        "userLogin: access_token not found in response $json"
                    )
                    throw ChatError.Companion.fromCode(
                        ChatError.Companion.UNKNOWN_CODE,
                        "access_token not found in response"
                    )
                }

                val user = json.optJSONObject("contact")
                val contact = createContactFromResponse(user)
                    ?: ContactDto(
                        contact.sub,
                        contact.iss,
                        contact.name,
                        contact.iss,
                        false
                    )

                currentContact = contact

                notifyUpdate(access)
                return@safeCall access
            }
        }
    }


    private fun createContactFromResponse(user: JSONObject?): ContactDto? {
        if (user == null) return null
        val id = user.optString("sub")
        val iss = user.optString("iss")
        val name = user.optString("name").ifBlank { "unknown" }

        if (id.isEmpty() || iss.isEmpty()) return null

        val source = user.optString("type", iss)
        val isBot = user.optBoolean("is_bot")

        return ContactDto(id, iss, name, source, isBot)
    }


    private fun buildLoginUrl(): HttpUrl =
        HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .addPathSegments(LOGIN_PATH)
            .apply {
                if (clientContext.port > 0)
                    port(clientContext.port)
            }
            .build()


    private fun performRefresh(): Result<Unit> {
        logger.debug(TAG,
            "performRefresh: start refresh auth with ${clientContext.authMethod}"
        )

        return when (val method = clientContext.authMethod) {
            is AuthMethod.Contact -> {
                refreshWithContact(method.identity)
            }

            is AuthMethod.Token -> {
                val token = method.tokenProvider.invoke()
                refreshWithToken(token)
            }
        }
    }


    private fun refreshWithContact(identity: ContactIdentity): Result<Unit> {
        headerProvider.updateAccessToken(null)
        val loginResult = userLogin(identity)

        return if (loginResult.isSuccess) {
            headerProvider.updateAccessToken(loginResult.getOrThrow())
            Result.success(Unit)
        } else {
            Result.failure(loginResult.exceptionOrNull()!!)
        }
    }


    private fun refreshWithToken(token: String): Result<Unit> {
        headerProvider.updateAccessToken(token)
        val inspect = inspect()
        return if (inspect.isSuccess) {
            notifyUpdate(token)
            Result.success(Unit)
        } else {
            Result.failure(inspect.exceptionOrNull()!!)
        }
    }


    private fun notifyUpdate(newToken: String) {
        listeners.forEach {
            try {
                it.invoke(newToken)
            } catch (e: Exception) {
                logger.error(TAG,
                    "Error notifying token listener: ${e.message}"
                )
            }
        }
    }
}