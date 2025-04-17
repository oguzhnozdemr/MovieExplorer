package com.oguzhanozdemir.kotlincountries.services
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.oguzhanozdemir.kotlincountries.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MovieNotesService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notesCollection = db.collection("movie_notes")

    /**
     * Bir filme ait tüm notları getirir
     */
    suspend fun getNotesForMovie(movieId: String): List<Note> = withContext(Dispatchers.IO) {
        try {
            Log.d("MovieNotesService", "Fetching notes for movie ID: $movieId")

            val snapshot = notesCollection
                .whereEqualTo("movieId", movieId)
                .get()
                .await()

            val notes = snapshot.toObjects(Note::class.java)

            // Sort the notes in memory
            val sortedNotes = notes.sortedByDescending { it.timestamp }

            Log.d("MovieNotesService", "Found ${sortedNotes.size} notes for movie $movieId")

            return@withContext sortedNotes
        } catch (e: Exception) {
            Log.e("MovieNotesService", "Error fetching notes for movie $movieId: ${e.message}", e)
            return@withContext emptyList<Note>()
        }
    }

    /**
     * Yeni bir not ekler
     */
    suspend fun addNote(movieId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext false
            Log.d("MovieNotesService", "Current user: $currentUser")
            val noteId = notesCollection.document().id
            val note = Note(
                id = noteId,
                movieId = movieId,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonymous",
                userPhotoUrl = currentUser.photoUrl?.toString() ?: "",
                content = content,
                timestamp = System.currentTimeMillis()
            )
            Log.d("NoteLog", "Note to be added: $note")
            notesCollection.document(noteId).set(note).await()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("NoteError", "Failed to add note: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Bir notu beğenir veya beğeniyi geri alır - Toggle işlemi
     */
    suspend fun toggleLikeNote(noteId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext false
            val currentUserId = currentUser.uid
            val noteRef = notesCollection.document(noteId)

            // İşlemi transaction içinde gerçekleştir
            db.runTransaction { transaction ->
                val snapshot = transaction.get(noteRef)
                val note = snapshot.toObject(Note::class.java) ?: return@runTransaction

                if (note.likedUserIds.contains(currentUserId)) {
                    // Kullanıcı zaten beğenmiş, beğeniyi geri al
                    transaction.update(noteRef, "likedUserIds", FieldValue.arrayRemove(currentUserId))
                    transaction.update(noteRef, "likes", note.likes - 1)
                    Log.d("MovieNotesService", "User $currentUserId unliked note $noteId")
                } else {
                    // Kullanıcı henüz beğenmemiş, beğeni ekle
                    transaction.update(noteRef, "likedUserIds", FieldValue.arrayUnion(currentUserId))
                    transaction.update(noteRef, "likes", note.likes + 1)
                    Log.d("MovieNotesService", "User $currentUserId liked note $noteId")
                }
            }.await()

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MovieNotesService", "Error toggling note like: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Kullanıcının bir notu beğenip beğenmediğini kontrol eder
     */
    suspend fun hasUserLikedNote(noteId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext false
            val noteDoc = notesCollection.document(noteId).get().await()
            val note = noteDoc.toObject(Note::class.java) ?: return@withContext false

            return@withContext note.likedUserIds.contains(currentUser.uid)
        } catch (e: Exception) {
            Log.e("MovieNotesService", "Error checking if user liked note: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Bir notu siler - Sadece not sahibi silebilir
     */
    suspend fun deleteNote(noteId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext false
            val currentUserId = currentUser.uid

            // Note'u al ve sahibini kontrol et
            val noteDoc = notesCollection.document(noteId).get().await()
            val note = noteDoc.toObject(Note::class.java)

            if (note == null) {
                Log.e("MovieNotesService", "Note not found for deletion: $noteId")
                return@withContext false
            }

            // Sadece not sahibi silebilir
            if (note.userId != currentUserId) {
                Log.e("MovieNotesService", "User $currentUserId tried to delete note $noteId but is not the owner")
                return@withContext false
            }

            // Notu sil
            notesCollection.document(noteId).delete().await()
            Log.d("MovieNotesService", "Note $noteId deleted successfully")
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MovieNotesService", "Error deleting note: ${e.message}", e)
            return@withContext false
        }
    }
}