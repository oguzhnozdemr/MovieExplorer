package com.oguzhanozdemir.kotlincountries.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class Movie(
    @PrimaryKey var id: Int,
    val title: String,
    @ColumnInfo("posterPath")
    @SerializedName("poster_path")
    val posterPath: String?,
    @ColumnInfo("overview")
    @SerializedName("overview")
    val overview: String?,
    @ColumnInfo("releaseDate")
    @SerializedName("release_date")
    val releaseDate: String?,
    @ColumnInfo("voteAverage")
    @SerializedName("vote_average")
    val voteAverage: Float,
    // Genre bilgilerini sadece API'den çekerken kullanacağız, SQLite'a kaydetmeyeceğiz
    @Ignore
    @SerializedName("genres")
    val genres: List<Genre> = emptyList(),
    // Tür isimlerini virgülle ayrılmış şekilde saklayacağız
    @ColumnInfo("genreNames")
    var genreNames: String? = ""
) {
    // Room için boş constructor gerekli (Ignore edilmiş alanlar için)
    constructor(
        id: Int, 
        title: String, 
        posterPath: String?, 
        overview: String?, 
        releaseDate: String?, 
        voteAverage: Float,
        genreNames: String?
    ) : this(id, title, posterPath, overview, releaseDate, voteAverage, emptyList(), genreNames)
}

data class MovieResponse(
    val results: List<Movie>
) 