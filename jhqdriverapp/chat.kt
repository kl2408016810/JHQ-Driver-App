package com.example.jhqdriverapp

data class Chat(
    val chatId: String = "",
    val participants: List<String> = listOf(),
    val participantNames: Map<String, String> = mapOf(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Map<String, Int> = mapOf()
)