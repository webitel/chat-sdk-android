# Dialogs

Dialogs represent conversations between users.


## Types
- DIRECT — One-to-one private dialog between two users.  
- GROUP — Dialog with multiple participants.  
- CHANNEL — Broadcast or topic-based dialog.  


## DIRECT behavior
- Created automatically on first message  
- Only one dialog exists per pair of users  


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
    val members: List<Participant>

    /** Last message sent in the dialog, if available. */
    val lastMessage: Message?
}
```


## Participants

Each dialog contains a list of participants (`members`).

A participant represents a user **within the context of a specific dialog** and includes:

- underlying contact information (`Contact`)
- role in the dialog (e.g. OWNER, ADMIN, MEMBER)
- a dialog-scoped identifier

Important:

- the same contact may have different roles in different dialogs
- participant data should be used for permissions and UI logic
- do not confuse `Participant` with `Contact` — they serve different purposes

```kotlin

data class Participant(
  val id: String,
  val contact: Contact,
  val role: ParticipantRole
)
```


## Fetch dialogs

```kotlin
val request = DialogRequest(
    page = 1,
    size = 50,
    filter = DialogFilter(
        query = "support",
        types = listOf(DialogType.GROUP, DialogType.CHANNEL)
    )
)

chatClient.getDialogs(request) { result -> }
```

Returns: `Result<Page<Dialog>>`


## Filtering

DialogFilter allows narrowing the result set:  

- query — full-text search query (up to 256 characters).  
Performs a case-insensitive partial match (`ilike`) against:  
    * `subject` for group/channel dialogs  
    * `title` for direct dialogs  
Matches both exact values and substrings anywhere in the text.  

- ids — include only specific dialogs by ID  
- types — filter by dialog types  
    * UNKNOWN is ignored  

If multiple filter fields are provided, they are combined (AND).  


## Pagination

```kotlin
data class Page<T>(
    val page: Int,
    val items: List<T>,
    val hasNext: Boolean
)
```

* page — current page number
* items — list of dialogs
* hasNext — indicates if more pages are available