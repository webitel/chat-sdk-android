# Authentication

Authentication is configured via `auth()` during initialization.

The SDK performs authentication automatically using provided data.


## Supported methods

- Token-based (JWT, Opaque, etc.)  
- ContactIdentity  

```kotlin
sealed class AuthMethod {
    data class Token(val tokenProvider: () -> String) : AuthMethod()
    data class Contact(val identity: ContactIdentity) : AuthMethod()
}
```


## ContactIdentity

```kotlin
data class ContactIdentity(
    /**
     * Unique subject identifier (stable contact ID).
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
     */
    val metadata: Map<String, Any?>? = null
)
```


## Error handling

- On 401 → re-authentication  
- Request is retried automatically  
