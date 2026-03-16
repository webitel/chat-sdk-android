package com.webitel.chat.sdk


data class Page<T>(
    val page: Int,
    val items: List<T>,
    val hasNext: Boolean
)