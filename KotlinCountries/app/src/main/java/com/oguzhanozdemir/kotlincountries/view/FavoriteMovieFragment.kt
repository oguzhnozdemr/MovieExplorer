package com.oguzhanozdemir.kotlincountries.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.oguzhanozdemir.kotlincountries.Adapter.MovieAdapter
import com.oguzhanozdemir.kotlincountries.R
import com.oguzhanozdemir.kotlincountries.databinding.FragmentFavoriteMovieBinding
import com.oguzhanozdemir.kotlincountries.model.Movie
import com.oguzhanozdemir.kotlincountries.viewmodel.FavoriteMovieViewModel
import com.oguzhanozdemir.kotlincountries.viewmodel.MovieViewModel
import kotlinx.coroutines.launch


class FavoriteMovieFragment : Fragment() {

    private var _binding: FragmentFavoriteMovieBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FavoriteMovieViewModel
    private val movieAdapter = MovieAdapter(arrayListOf())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_favorite_movie,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up toolbar
        setupToolbar()
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        ).get(FavoriteMovieViewModel::class.java)

        setupRecyclerView()
        setupEmptyStateView()

        // Show loading state
        showLoadingState()
        
        // Load favorite movies
        viewModel.getFavoriteMovies()

        // Observe changes in favorite movies list
        observeLiveData()
    }
    
    private fun setupToolbar() {
        binding.favoritesToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        // MovieAdapter için özel navigasyon ayarla
        movieAdapter.setMovieDetailNavigation { view, movieId ->
            val action = FavoriteMovieFragmentDirections.actionFavoriteMovieFragmentToCountryFragment(movieId)
            Navigation.findNavController(view).navigate(action)
        }

        binding.favoriteMoviesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = movieAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupEmptyStateView() {
        binding.browseMoviesButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun showLoadingState() {
        binding.favoriteMoviesRecyclerView.visibility = View.GONE
        binding.favoriteMoviesError.visibility = View.GONE
        binding.emptyFavoritesState.visibility = View.GONE
        binding.favoriteMoviesLoading.visibility = View.VISIBLE
    }
    
    private fun showEmptyState() {
        binding.favoriteMoviesRecyclerView.visibility = View.GONE
        binding.favoriteMoviesError.visibility = View.GONE
        binding.favoriteMoviesLoading.visibility = View.GONE
        binding.emptyFavoritesState.visibility = View.VISIBLE
    }
    
    private fun showErrorState(errorMessage: String) {
        binding.favoriteMoviesRecyclerView.visibility = View.GONE
        binding.emptyFavoritesState.visibility = View.GONE
        binding.favoriteMoviesLoading.visibility = View.GONE
        binding.favoriteMoviesError.visibility = View.VISIBLE
        binding.favoriteMoviesError.text = errorMessage
    }
    
    private fun showContentState(movies: List<Movie>) {
        binding.favoriteMoviesRecyclerView.visibility = View.VISIBLE
        binding.favoriteMoviesError.visibility = View.GONE
        binding.favoriteMoviesLoading.visibility = View.GONE
        binding.emptyFavoritesState.visibility = View.GONE
        
        // Update the adapter with movies
        movieAdapter.updateMovieList(movies)
    }

    private fun observeLiveData() {
        viewModel.favoriteMovies.observe(viewLifecycleOwner, Observer { favorites ->
            favorites?.let {
                if (favorites.isNotEmpty()) {
                    // Convert FavoriteMovie objects to Movie objects
                    val movieList = favorites.map { favorite ->
                        Movie(
                            id = favorite.movieId,
                            title = favorite.title,
                            posterPath = favorite.posterPath,
                            overview = favorite.overview,
                            releaseDate = favorite.releaseDate,
                            voteAverage = favorite.voteAverage,
                            genreNames = favorite.genreNames
                        )
                    }

                    // Show content with movies
                    showContentState(movieList)
                } else {
                    // Show empty state when no favorites
                    showEmptyState()
                }
            }
        })
        
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                if (it) {
                    showErrorState("An error occurred while loading favorites")
                }
            }
        })
        
        viewModel.loading.observe(viewLifecycleOwner, Observer { loading ->
            loading?.let {
                if (it) {
                    showLoadingState()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Refresh the favorite list when returning to this fragment
        showLoadingState()
        viewModel.getFavoriteMovies()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}