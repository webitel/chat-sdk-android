package com.webitel.chat.sdk


/**
 * Represents contact identity information used for session creation.
 *
 * This model is provided by the SDK client and sent to backend
 * to create or restore a chat session.
 *
 * The structure is compatible with common OpenID Connect identity claims.
 */
data class ContactIdentity(

    /**
     * Unique subject identifier (stable contact ID).
     *
     * This is the primary identity field.
     * Example: external CRM ID, UUID, or internal user ID.
     */
    val sub: String,

    /** Issuer identifier */
    val iss: String,

    /** Full display name */
    val name: String,

    /** First name */
    val givenName: String? = null,

    /** Middle name */
    val middleName: String? = null,

    /** Last name */
    val familyName: String? = null,

    /** Email address */
    val email: String? = null,

    /** Whether email was verified */
    val emailVerified: Boolean? = null,

    /** Phone number in E.164 format */
    val phoneNumber: String? = null,

    /** Whether phone number was verified */
    val phoneNumberVerified: Boolean? = null,

    /** Birthdate (ISO 8601: YYYY-MM-DD) */
    val birthdate: String? = null,

    /** Gender (free text, e.g. "male", "female", "other") */
    val gender: String? = null,

    /** Locale (e.g. "en-US", "uk-UA") */
    val locale: String? = null,

    /** Timezone (e.g. "Europe/Kyiv") */
    val zoneinfo: String? = null,

    /**
     * Additional custom metadata associated with this identity.
     *
     * Used to pass arbitrary key–value attributes to the backend
     *
     * Supported value types:
     *  - String
     *  - Number
     *  - Boolean
     *  - null
     *
     * Any other type will be serialized using `toString()`.
     *
     * Example:
     * ```
     * metadata = mapOf(
     *     "crm_id" to "12345",
     *     "vip" to true,
     *     "age" to 30
     * )
     * ```
     */
    val metadata: Map<String, Any?>? = null
)