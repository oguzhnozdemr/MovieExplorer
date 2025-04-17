package com.oguzhanozdemir.kotlincountries.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.oguzhanozdemir.kotlincountries.databinding.FragmentFeedBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.oguzhanozdemir.kotlincountries.Adapter.MovieAdapter
import com.oguzhanozdemir.kotlincountries.R
import com.oguzhanozdemir.kotlincountries.model.Movie
import com.oguzhanozdemir.kotlincountries.services.MovieDatabase
import com.oguzhanozdemir.kotlincountries.viewmodel.FeedViewModel
import kotlinx.coroutines.launch


class FeedFragment : Fragment(R.layout.fragment_feed) {
    private lateinit var auth: FirebaseAuth
    // ViewBinding için değişken
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FeedViewModel
    private val movieAdapter = MovieAdapter(arrayListOf())
    private lateinit var spinner: Spinner

    private val genres = listOf(
        "Drama", "Action", "Comedy", "Sci-Fi", "Horror", "Romance", "Adventure", "Animation", 
        "Thriller", "Crime", "Documentary", "Fantasy", "Musical", "History", "Western", "War", "Mystery"
    )

    private val genreIds = listOf(
        18, 28, 35, 878, 27, 10749, 12, 16, 53, 80,
        99, 14, 10402, 36, 37, 10752, 9648
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewBinding ile layout bağlama
        _binding = FragmentFeedBinding.bind(view)
        auth = Firebase.auth
        val currentUser = auth.currentUser
        // Set up toolbar
        setupToolbar()
        
        // Set up components
        setupViewModel()
        setupRecyclerView()
        setupGenreSpinner()
        setupSwipeRefresh()
        setupSearch()
        // Observe data
        observeLiveData()

        binding.favoritesButton.setOnClickListener {
            setupFavoriteButton()
            //showFavoriteMovies()
        }

        // Geri tuşuna özel popup dialog göster
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(requireContext())
                    .setTitle("Are you sure you want to log out?")
                    .setMessage("You are about to exit the application.")
                    .setPositiveButton("Logout") { _, _ ->


                            auth.signOut()
                            val action = FeedFragmentDirections.actionFeedFragmentToLoginFragment()
                            Navigation.findNavController(view).navigate(action)


                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        })

    }

    private fun showFavoriteMovies(){

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = context?.let { MovieDatabase(it).favoriteMovieDao() }
        lifecycleScope.launch {
            val favorites = userId?.let { database?.getFavoritesByUser(it) }
            if (favorites != null) {
                movieAdapter.updateMovieList(favorites.map {
                    Movie(
                        id = it.movieId,
                        title = it.title,
                        posterPath = it.posterPath,
                        overview = it.overview,
                        releaseDate = it.releaseDate,
                        voteAverage = it.voteAverage,
                        genreNames = it.genreNames
                    )
                }
                )
            }
        }

    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this).get(FeedViewModel::class.java)
        // Initial data load with cache check
        viewModel.refhreshData()
    }

    private fun setupRecyclerView() {
        binding.recyclerView2.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = movieAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupGenreSpinner() {
        // Spinner setup with modern styling
        spinner = binding.spinnerGenre
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            genres
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Get selected genre ID and update ViewModel
                val selectedGenreId = genreIds[position]
                showLoadingState()
                
                // Update genre and load data
                viewModel.setGenreId(selectedGenreId)
                
                // Show selected genre in a snackbar
                Snackbar.make(
                    binding.root,
                    "Showing ${genres[position]} movies",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Default to Drama (18) if nothing selected
                viewModel.setGenreId(18)
            }
        }
    }
    
    private fun setupSearch() {
        // Arama butonu tıklandığında
        binding.searchButton.setOnClickListener {
            binding.searchCard.visibility = View.VISIBLE
            binding.searchEditText.requestFocus()
        }
        
        // Temizle butonu tıklandığında
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text.clear()
            binding.searchCard.visibility = View.GONE
            
            // Normal türe göre listeye dön
            val selectedGenreId = genreIds[spinner.selectedItemPosition]
            viewModel.setGenreId(selectedGenreId)
        }
        
        // Arama yapıldığında
        binding.searchEditText.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchQuery = textView.text.toString().trim()
                if (searchQuery.isNotEmpty()) {
                    showLoadingState()
                    viewModel.searchMovies(searchQuery)
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        
        // Arama durumunu gözlemle
        viewModel.isSearchActive.observe(viewLifecycleOwner) { isSearchActive ->
            if (!isSearchActive) {
                binding.searchCard.visibility = View.GONE
                binding.searchEditText.text.clear()
            }
        }
    }

    private fun setupSwipeRefresh() {
        // SwipeRefreshLayout configuration
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent)
            setOnRefreshListener {
                showLoadingState()
                
                // Get selected genre ID and refresh
                val selectedGenreId = genreIds[spinner.selectedItemPosition]
                viewModel.setGenreId(selectedGenreId)
                
                // Hide refresh indicator
                isRefreshing = false
            }
        }
    }

    private fun showLoadingState() {
        binding.apply {
            recyclerView2.visibility = View.GONE
            textView.visibility = View.GONE
            emptyState.visibility = View.GONE
            countryLoading.visibility = View.VISIBLE
        }
    }

    private fun observeLiveData() {
        // Observe movie list
        viewModel.movies.observe(viewLifecycleOwner, Observer { movies ->
            movies?.let {
                if (movies.isEmpty()) {
                    showEmptyState()
                } else {
                    showMovieList(movies)
                }
            }
        })

        // Observe error state
        viewModel.countryError.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                if (it) {
                    showErrorState()
                }
            }
        })

        // Observe loading state
        viewModel.countryLoading.observe(viewLifecycleOwner, Observer { loading ->
            loading?.let {
                binding.countryLoading.visibility = if (it) View.VISIBLE else View.GONE
            }
        })
    }
    
    private fun showMovieList(movies: List<com.oguzhanozdemir.kotlincountries.model.Movie>) {
        binding.apply {
            recyclerView2.visibility = View.VISIBLE
            movieAdapter.updateMovieList(newMovieList = movies)
            countryLoading.visibility = View.GONE
            textView.visibility = View.GONE
            emptyState.visibility = View.GONE
        }
    }
    
    private fun showErrorState() {
        binding.apply {
            textView.visibility = View.VISIBLE
            recyclerView2.visibility = View.GONE
            countryLoading.visibility = View.GONE
            emptyState.visibility = View.GONE
        }
    }
    private fun setupFavoriteButton() {
        binding.favoritesButton.setOnClickListener {
            val action = FeedFragmentDirections.actionFeedFragmentToFavoriteMovieFragment()
            Navigation.findNavController(it).navigate(action)
        }
    }
    private fun showEmptyState() {
        binding.apply {
            emptyState.visibility = View.VISIBLE
            recyclerView2.visibility = View.GONE
            countryLoading.visibility = View.GONE
            textView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}