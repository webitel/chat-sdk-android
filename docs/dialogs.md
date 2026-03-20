# Dialogs

Dialogs represent conversations.

## Types
- DIRECT  
- GROUP  


## DIRECT behavior
- Created automatically on first message  
- One dialog per pair  


## MessageTarget

```kotlin
/**
 * Represents the destination where a message should be sent.
 */
sealed interface MessageTarget {

    /** Target an existing dialog by its unique identifier. */
    data class Dialog(val id: String) : MessageTarget

    /** Target a contact directly (a dialog may be created automatically). */
    data class Contact(val contactId: ContactId) : MessageTarget
}
```


## Model

```kotlin
interface Dialog {

    /** Unique identifier of the dialog. */
    val id: String

    /** Display name or subject of the dialog. */
    val subject: String

    /** Type of the dialog (direct, group, etc.). */
    val type: DialogType

    /** List of dialog participants. */
    val members: List<Contact>

    /** Last message sent in the dialog, if available. */
    val lastMessage: Message?
}
```


## Fetch dialogs

```kotlin
val request = DialogRequest(page = 1, size = 50)

chatClient.getDialogs(request) { result -> }
```

Returns: `Result<Page<Dialog>>`


## Pagination

```kotlin
data class Page<T>(
    val page: Int,
    val items: List<T>,
    val hasNext: Boolean
)
```