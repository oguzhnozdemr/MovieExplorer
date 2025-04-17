package com.oguzhanozdemir.kotlincountries.model

data class Note(
    val id: String = "",
    val movieId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Long = 0L,
    val likedUserIds: MutableList<String> = mutableListOf()
) {
    constructor() : this("", "", "", "", "", "", 0L, 0L, mutableListOf())
}