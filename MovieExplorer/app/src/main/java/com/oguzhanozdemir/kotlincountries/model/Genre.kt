package com.oguzhanozdemir.kotlincountries.model

import com.google.gson.annotations.SerializedName

data class Genre(
    val id: Int,
    val name: String
)

data class GenreList(
    @SerializedName("genres")
    val genres: List<Genre>
) 