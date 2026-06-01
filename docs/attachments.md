# Files

## Overview

The SDK supports:

- uploading files to remote storage
- downloading files from storage
- attaching uploaded files to messages

Files are uploaded separately before sending a message.
After a successful upload, the server returns a file identifier that can later be attached to outgoing messages.


## Uploading Files
File uploads are asynchronous and support:

- progress tracking
- cancellation 
- resumable uploads

Example
```kotlin
val request = UploadRequest(
    source = FileSource.Stream(inputStream),
    fileName = "image.png",
    mimeType = "image/png",
    totalSize = fileSize
)

val task = chatClient.upload(
    request,
    object : UploadListener {
        override fun onCreated(uploadId: String) {
            // Save uploadId if resumable uploads are needed
        }

        override fun onProgress(
            uploaded: Long,
            total: Long?
        ) {}

        override fun onCompleted(result: UploadResult) {
            val uploadedFile = result.file
        }

        override fun onError(error: ChatError) {}
    }
)
```


### Upload Sources

The SDK supports multiple upload sources.


#### ByteArray
```kotlin
FileSource.Bytes(bytes)
```


#### InputStream
```kotlin
FileSource.Stream(inputStream)
```


### Resuming Uploads

Uploads may be resumed using resumeId.

The upload identifier is provided via:

```kotlin
UploadListener.onCreated(uploadId)
```

Example:
```kotlin
val request = UploadRequest(
    source = FileSource.Stream(stream),
    fileName = "video.mp4",
    mimeType = "video/mp4",
    resumeId = savedUploadId
)
```


### Upload Result

After a successful upload, the SDK returns file metadata.

```kotlin
data class UploadResult(
    val file: UploadedFile,
    val hash: Map<String, String>
)
```

Example:

```kotlin
val fileId = result.file.id
```


### Sending Attachments

Uploaded files can be attached to messages using SendAttachment.File.

```kotlin
val content = SendContent.Composite(
    text = "See attached file",
    attachments = listOf(
        SendAttachment.File(
            fileId = uploadedFile.id
        )
    )
)
```

Or attachment-only message:

```kotlin
val content = SendContent.Attachments(
    attachments = listOf(
        SendAttachment.File(
            fileId = uploadedFile.id
        )
    )
)
```

External URLs are also supported:

```kotlin
SendAttachment.Url(
    url = "https://example.com/image.jpg",
    fileName = "image.jpg"
)
```


## Downloading Files

File downloads are asynchronous and support:

- streaming downloads
- resumable downloads
- cancellation

Example:

```kotlin
val task = chatClient.download(
    DownloadRequest(
        fileId = fileId
    ),
    object : DownloadListener {

        override fun onChunk(chunk: ByteArray) {
            output.write(chunk)
        }

        override fun onCompleted(result: DownloadResult) {}

        override fun onError(error: ChatError) {}
    }
)
```


### Resuming Downloads

Downloads can resume from a specific byte offset.

Example:

```kotlin
val request = DownloadRequest(
    fileId = fileId,
    offset = alreadyDownloadedBytes
)
```


## Cancelling Transfers

Both uploads and downloads return a Cancellable.

Example:

```kotlin
val task = chatClient.download(...)

task.cancel()
```
Cancellation interrupts the active transfer operation.

The listener will receive:

```kotlin
ChatError.Canceled
```
via onError(...).