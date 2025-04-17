package com.oguzhanozdemir.kotlincountries.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.oguzhanozdemir.kotlincountries.R
import com.oguzhanozdemir.kotlincountries.databinding.FragmentCountryBinding
import com.oguzhanozdemir.kotlincountries.model.Movie
import com.oguzhanozdemir.kotlincountries.util.downloadFromUrl
import com.oguzhanozdemir.kotlincountries.util.formatDate
import com.oguzhanozdemir.kotlincountries.util.placeholderProgressBar
import com.oguzhanozdemir.kotlincountries.viewmodel.MovieViewModel
//import android.graphics.ColorStateList
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.oguzhanozdemir.kotlincountries.Adapter.NotesAdapter
import kotlinx.coroutines.launch

class MovieFragment : Fragment() {

    private lateinit var viewModel: MovieViewModel
    private var currentMovie: Movie? = null
    private lateinit var notesAdapter: NotesAdapter

    // ViewBinding değişkenleri
    private var _binding: FragmentCountryBinding? = null
    private val binding get() = _binding!!
    private var movieId = 0
    private var note: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Binding'i inflate et
        _binding = FragmentCountryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get movie ID from arguments
        arguments?.let {
            movieId = MovieFragmentArgs.fromBundle(it).countryUuid
            Log.d("MovieFragment", "Film ID: $movieId")
        }

        // Setup ViewModel
        viewModel = ViewModelProvider(this).get(MovieViewModel::class.java)
        viewModel.getDataFromRoom(movieId)

        // Setup UI interactions
        setupBackButton()
        // setupSubmitButton() - Remove this line since we're handling it in setupUI()
        setupFavoriteButton()
        setupUI()
        setupObservers()
        viewModel.loadMovieNotes(movieId.toString())
        // Observe movie data
        observeLiveData()
    }

    private fun updateFavoriteButtonState(isFavorite: Boolean) {
        binding.favoriteButton.apply {
            if (isFavorite) {
                // Set colored icon when favorite
                setImageResource(R.drawable.favorite_icon)
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorFavoriteActive))
            } else {
                // Set gray icon when not favorite
                setImageResource(R.drawable.favorite_icon)
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorFavoriteInactive))
            }
        }
    }

    private fun checkMovieFavoriteStatus() {
        viewModel.viewModelScope.launch {
            currentMovie?.let {
                try {
                    val isFavorite = viewModel.isMovieFavorite(it.id)
                    updateFavoriteButtonState(isFavorite)
                } catch (e: Exception) {
                    Log.e("MovieFragment", "Error checking favorite status", e)
                    // Default to not favorite in case of error
                    updateFavoriteButtonState(false)
                }
            }
        }
    }

    private fun setupBackButton() {
        // Handle back button click
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Handle system back press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )
    }

//    private fun setupSubmitButton() {
//        binding.submitNoteButton.setOnClickListener {
//            val noteText = binding.notesInput.text.toString().trim()
//            if (noteText.isNotEmpty()) {
//                saveNote(noteText)
//            } else {
//                Snackbar.make(binding.root, "Please enter a note", Snackbar.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun setupFavoriteButton() {
        binding.favoriteButton.setOnClickListener {
            viewModel.viewModelScope.launch {
                currentMovie?.let {
                    viewModel.addFavoriteMovie(it)
                    
                    // Check the new favorite status after toggle
                    val isFavorite = viewModel.isMovieFavorite(it.id)
                    updateFavoriteButtonState(isFavorite)
                }
            }
        }
    }

    private fun observeLiveData() {
        viewModel.movieLiveData.observe(viewLifecycleOwner, Observer { movie ->
            if (movie != null) {
                currentMovie = movie
                Log.d("MovieFragment", "Film verisi alındı: ${movie.title}")
                try {
                    // Set movie details
                    binding.movieTitle.text = movie.title
                    binding.movieOverview.text = movie.overview
                    binding.movieRelaseDate.text = "Released: ${movie.releaseDate?.let {
                        formatDate(
                            it
                        )
                    }}"
                    binding.movieVoteAvarage.text = String.format("%.1f", movie.voteAverage)

                    // Apply rating color based on vote average
                    val ratingColor = when {
                        movie.voteAverage >= 7.0 -> R.color.ratingHigh
                        movie.voteAverage >= 5.0 -> R.color.ratingMedium
                        else -> R.color.ratingLow
                    }
                    binding.ratingCard.setCardBackgroundColor(resources.getColor(ratingColor, null))

                    // Display genres as text
                    displayGenres(movie)

                    context?.let { context ->
                        val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
                        binding.movieImage.downloadFromUrl(posterUrl, placeholderProgressBar(context))
                        binding.movieBackdrop.downloadFromUrl(posterUrl, placeholderProgressBar(context))
                    }

                    // Check if the movie is already in favorites and update button state
                    checkMovieFavoriteStatus()

                } catch (e: Exception) {
                    Toast.makeText(context, "Film detayları yüklenirken bir sorun oluştu", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            } else {
                Toast.makeText(context, "Film bulunamadı", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        })


    }
    private fun setupObservers() {
        // Not listesini gözlemle
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            notesAdapter.submitList(notes)

            // Eğer notlar boşsa, boş durum mesajını göster
            if (notes.isEmpty()) {
                binding.emptyNotesMessage.visibility = View.VISIBLE
                binding.previousNotesRecyclerView.visibility = View.GONE
            } else {
                binding.emptyNotesMessage.visibility = View.GONE
                binding.previousNotesRecyclerView.visibility = View.VISIBLE
            }
        }

        // Yükleme durumunu gözlemle
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Burada isteğe bağlı olarak bir progress bar gösterebilirsiniz
        }

        // Hata durumunu gözlemle
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupUI() {
        // Geri butonu
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        notesAdapter = NotesAdapter(
            onLikeClick = { noteId ->
                viewModel.toggleLikeNote(noteId)  // Changed from likeNote to toggleLikeNote
            },
            onDeleteClick = { noteId ->
                // Show confirmation dialog before deleting
                showDeleteConfirmationDialog(noteId)
            }
        )

        binding.previousNotesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notesAdapter
        }

        // Not gönderme butonu
        binding.submitNoteButton.setOnClickListener {
            val noteContent = binding.notesInput.text.toString().trim()

            // Kullanıcı giriş yapmış mı kontrol et
            if (FirebaseAuth.getInstance().currentUser == null) {
                Toast.makeText(context, "Please sign in to share your notes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Not içeriği boş mu kontrol et
            if (noteContent.isEmpty()) {
                binding.notesInputLayout.error = "Note cannot be empty"
                return@setOnClickListener
            }

            // Notu ekle
            viewModel.addNote(movieId.toString(), noteContent) {
                // Başarı durumunda input'u temizle
                binding.notesInput.text?.clear()
                binding.notesInputLayout.error = null

                // Firestore'dan güncel notları tekrar yükle
                viewModel.loadMovieNotes(movieId.toString())
            }
        }
    }

    private fun displayGenres(movie: Movie) {
        val genreList = if (movie.genres.isNotEmpty()) {
            movie.genres.map { it.name ?: "Unknown" }
        } else if (movie.genreNames?.isNotEmpty() == true) {
            movie.genreNames!!.split(",").map { it.trim() }
        } else {
            listOf("No genre information")
        }
        
        val genreText = genreList.joinToString(" • ")
        binding.genreText.apply {
            text = genreText
            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGenreText))
        }
    }
    
//    private fun saveNote(noteText: String) {
//        // Here we would normally save the note to a database
//        // For this example, we'll just clear the input and show a confirmation
//        this.note = noteText
//        binding.notesInput.text?.clear()
//        Snackbar.make(binding.root, "Note saved successfully!", Snackbar.LENGTH_SHORT).show()
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun showDeleteConfirmationDialog(noteId: String) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteNot(noteId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }
}