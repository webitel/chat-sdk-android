# Message Search

Messages can be searched by text — either across all dialogs of the current
user, or within a single dialog. Results are paginated with cursors, the same
way as [Message History](messages.md#message-history).


## Searching Messages

Search is available via the client or via a dialog instance:
- via the client: `chatClient.searchMessages(request)` — searches across all dialogs
- via the client with a dialog id: `chatClient.searchMessages(request, dialogId)` — searches within that dialog
- via a dialog instance: `dialog.searchMessages(request)` — searches within that dialog

```kotlin
fun searchMessages(
    request: MessageSearchRequest,
    dialogId: String? = null,
    onComplete: (Result<MessageSearchSlice>) -> Unit
)
```

```kotlin
val request = MessageSearchRequest(query = "invoice")

chatClient.searchMessages(request) { result ->
    result
        .onSuccess { slice ->
            println("Found: ${slice.items.size}")
        }
        .onFailure { error ->
            println("Failed to search messages: $error")
        }
}
```

```kotlin
dialog.searchMessages(MessageSearchRequest(query = "invoice")) { result ->
    result
        .onSuccess { slice -> }
        .onFailure { error -> }
}
```


### Request parameters

```kotlin
data class MessageSearchRequest(

    /** Search text, matched as a case-insensitive substring. Expected to be 1...256 characters, server-validated. */
    val query: String,

    /** Optional filter by message sender. Expected to be up to 50 identifiers, server-validated. */
    val senderIds: Set<String> = emptySet(),

    /** Optional filter by message content type. Expected to be up to 8 values, server-validated. */
    val contentTypes: Set<MessageSearchContentType> = emptySet(),

    /** Maximum number of items to return. Clamped to the 1...100 range. */
    val limit: Int = 20,

    /** Optional cursor used to continue search navigation. */
    val cursor: MessageSearchCursor? = null,
)
```

```kotlin
enum class MessageSearchContentType {
    TEXT, DOCUMENT, IMAGE, SYSTEM, INTERACTIVE, LOCATION, CONTACT
}
```

Filters are combined: for example, only images sent by a specific contact:

```kotlin
val request = MessageSearchRequest(
    query = "report",
    senderIds = setOf(contactId),
    contentTypes = setOf(MessageSearchContentType.IMAGE)
)
```


### Result

```kotlin
data class MessageSearchSlice(

    /** Messages matching the search request. */
    val items: List<Message>,

    /** Cursor used to load newer search results. */
    val newerCursor: MessageSearchCursor?,

    /** Cursor used to load older search results. */
    val olderCursor: MessageSearchCursor?
) {
    val isEmpty: Boolean
}
```

- `olderCursor` — used to load older results, `null` when there are no more
- `newerCursor` — used to load newer results, `null` when there are no more


### Pagination

To load the next page, repeat the request with the cursor from the previous
result. Keep the other parameters unchanged:

```kotlin
slice.olderCursor?.let { cursor ->
    chatClient.searchMessages(request.copy(cursor = cursor)) { result -> }
}
```

```kotlin
data class MessageSearchCursor(

    /** Identifier of the reference message. */
    val messageId: String,

    /** Direction in which search results should be loaded. */
    val direction: SearchDirection = SearchDirection.OLDER
)
```
