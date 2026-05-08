# Interactive Messages

## Overview

Messages may contain interactive keyboards that allow users to perform actions directly from the chat UI.

Keyboards are delivered by the server as part of MessageContent.

Supported keyboard styles:

- inline button grid
- list-style menu with sections

## Keyboard Types

```kotlin
sealed class ChatKeyboard
```

### Buttons Keyboard

Grid-style keyboard where buttons are arranged into rows.

```kotlin
ChatKeyboard.Buttons(
    rows = listOf(
        ChatKeyboardRow(
            buttons = listOf(
                ChatKeyboardButton(...),
                ChatKeyboardButton(...)
            )
        )
    )
)
```

### List Menu Keyboard

List-style keyboard with sections.

Typically rendered as a single trigger button that opens a menu.

```kotlin
ChatKeyboard.ListMenu(
    title = "Open menu",
    sections = listOf(
        ChatKeyboardSection(
            title = "Main",
            buttons = listOf(...)
        )
    )
)
```

## Keyboard Structure

### Row

```kotlin
data class ChatKeyboardRow(
    val buttons: List<ChatKeyboardButton>
)
```

### Section

```kotlin
data class ChatKeyboardSection(
    val title: String,
    val buttons: List<ChatKeyboardButton>
)
```

### Button

```kotlin
data class ChatKeyboardButton(
    val id: String,
    val label: String,
    val action: ChatButtonAction,
    val metadata: Map<String, Any>? = null
)
```

Fields:
- id — unique button identifier
- label — text shown to the user
- action — action executed on click
- metadata — optional UI-related data


Example metadata:

```kotlin
metadata = mapOf(
    "color" to "primary",
    "size" to "large"
)
```

## Button Actions

```kotlin
sealed class ChatButtonAction
```

### Open URL

Opens an external link.

```kotlin
ChatButtonAction.OpenUrl(
    url = "https://example.com"
)
```

The client application is responsible for handling navigation.

### Send Callback

Sends a callback event to the backend.

```kotlin
ChatButtonAction.SendCallback(
    data = "confirm_order"
)
```

This action is typically used for bots or interactive workflows.

### Request Data

Requests information from the client device.

```kotlin
ChatButtonAction.RequestData(
    type = "location"
)
```

Common types:
- location
- contact


## Receiving Interactive Messages

Keyboards may be included in:

- MessageContent.KeyboardOnly
- MessageContent.Composite

Example:

```kotlin
when (val content = message.content) {
    is MessageContent.KeyboardOnly -> {
        renderKeyboard(content.keyboard)
    }
    is MessageContent.Composite -> {
        content.keyboard?.let {
            renderKeyboard(it)
        }
    }
    else -> Unit
}
```

## Sending Button Actions

When the user presses a button, the client should send a corresponding MessageAction.

```kotlin
chatClient.sendAction(
    messageId = message.id,
    action = MessageAction.ButtonClick(
        id = button.id,
        data = "confirm_order"
    )
) { result -> }
```

### MessageAction

```kotlin
sealed interface MessageAction
```
### Button Click

Represents a button press event.

```kotlin
MessageAction.ButtonClick(
    id = button.id,
    data = data
)
```

Fields:

- id — identifier of the clicked button
- data — callback payload associated with the button
