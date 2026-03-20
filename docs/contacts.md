# Contacts

Contacts are core entities used in dialogs and messages.

A contact is created automatically on first authentication.

## Model

```kotlin
data class Contact(
    /** Unique identifier of the contact. */
    val id: ContactId,

    /** Display name of the contact. */
    val name: String,

    /**
     * Source of the contact (e.g. telegram, facebook, webitel, custom).
     * Corresponds to the `iss` field configured on the server.
     */
    val source: String,

    /**
     * Indicates whether the contact represents a bot.
     */
    val isBot: Boolean
)


/**
 * Unique identity of a contact.
 */
data class ContactId(
    /** Subject identifier of the contact. */
    val sub: String,

    /** Issuer of the contact identity. */
    val iss: String
)
```


## Fetch contacts

```kotlin
val request = ContactRequest(page = 1, size = 50)

chatClient.getContacts(request) { result -> }
```

Returns: `Result<Page<Contact>>`


## Pagination

```kotlin
data class Page<T>(
    val page: Int,
    val items: List<T>,
    val hasNext: Boolean
)
```
