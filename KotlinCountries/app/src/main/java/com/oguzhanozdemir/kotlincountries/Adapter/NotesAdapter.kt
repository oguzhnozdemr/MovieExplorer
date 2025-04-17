package com.oguzhanozdemir.kotlincountries.Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.oguzhanozdemir.kotlincountries.R
import com.oguzhanozdemir.kotlincountries.databinding.ItemNoteBinding
import com.oguzhanozdemir.kotlincountries.model.Note
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(private val onLikeClick: (String) -> Unit,private val onDeleteClick: (String) -> Unit) :
    ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.apply {
                userName.text = note.userName
                noteContent.text = note.content
                noteDate.text = getTimeAgo(note.timestamp)
                // Kullanıcı fotoğrafını yükle
                if (note.userPhotoUrl.isNotEmpty()) {
                    Glide.with(userAvatar.context)
                        .load(note.userPhotoUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.man)
                        .error(R.drawable.error_background)
                        .into(userAvatar)
                } else {
                    // Varsayılan avatar göster
                    userAvatar.setImageResource(R.drawable.user_placeholder)
                }

                // Kullanıcı bu notu beğenmiş mi kontrol et
                val hasUserLiked = note.likedUserIds.contains(currentUserId)

                // Beğeni görünümünü güncelle
                if (hasUserLiked) {
                    // Kullanıcı zaten beğenmiş
                    likeButton.text = "Liked (${note.likes})"
                    likeButton.setTextColor(ContextCompat.getColor(likeButton.context, R.color.colorPrimary))
                    likeButton.isEnabled = false // Tekrar beğenmeyi engelle
                } else {
                    // Kullanıcı henüz beğenmemiş
                    likeButton.text = "Like (${note.likes})"
                    likeButton.setTextColor(ContextCompat.getColor(likeButton.context, R.color.colorHintText))
                    likeButton.isEnabled = currentUserId.isNotEmpty() // Kullanıcı giriş yapmışsa etkinleştir
                }

                // Set up like button click
                likeButton.setOnClickListener {
                    onLikeClick(note.id)
                }

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId == note.userId) {
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener {
                        onDeleteClick(note.id)
                    }
                } else {
                    deleteButton.visibility = View.GONE
                }
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} mins ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.likes == newItem.likes &&
                    oldItem.content == newItem.content &&
                    oldItem.likedUserIds == newItem.likedUserIds
        }
    }
}