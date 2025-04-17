package com.oguzhanozdemir.kotlincountries.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.oguzhanozdemir.kotlincountries.R
import com.oguzhanozdemir.kotlincountries.databinding.ItemMovieBinding
import com.oguzhanozdemir.kotlincountries.model.Movie
import com.oguzhanozdemir.kotlincountries.util.MovieClickListener
import com.oguzhanozdemir.kotlincountries.view.FeedFragmentDirections
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.coroutineContext

class MovieAdapter(
    private val movieList: ArrayList<Movie>
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>(), MovieClickListener {

    private var onNavigateToMovieDetail: ((View, Int) -> Unit)? = null

    init {
        // Varsayılan olarak FeedFragment navigasyonunu kullan
        setMovieDetailNavigation { view, movieId ->
            val action = com.oguzhanozdemir.kotlincountries.view.FeedFragmentDirections
                .actionFeedFragmentToCountryFragment(movieId)
            Navigation.findNavController(view).navigate(action)
        }
    }

    class MovieViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ItemMovieBinding>(
            inflater,
            R.layout.item_movie,
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun getItemCount(): Int = movieList.size

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.binding.movie = movieList[position]
        holder.binding.listener = this
    }

    fun updateMovieList(newMovieList: List<Movie>) {
        movieList.clear()
        movieList.addAll(newMovieList)
        notifyDataSetChanged()
    }

    // Fragment'e göre navigasyon işlemini ayarlamak için yeni metod
    fun setMovieDetailNavigation(navigationCallback: (View, Int) -> Unit) {
        onNavigateToMovieDetail = navigationCallback
    }

    override fun onMovieClicked(view: View, movie: Movie) {
        // Ayarlanmış callback'i kullan
        onNavigateToMovieDetail?.invoke(view, movie.id)
    }
}