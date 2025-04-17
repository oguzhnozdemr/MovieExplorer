package com.oguzhanozdemir.kotlincountries.view

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.oguzhanozdemir.kotlincountries.R
import com.oguzhanozdemir.kotlincountries.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {



        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        val currentUser = auth.currentUser
        binding.textViewSignUp.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToSignUpFragment()
            Navigation.findNavController(view).navigate(action)
        }
        if(currentUser!=null){
            val action = LoginFragmentDirections.actionLoginFragmentToFeedFragment()
            Navigation.findNavController(view).navigate(action)
        }
        binding.buttonLogin.setOnClickListener {

            loginWithEmailandPassword(view)
        }

        binding.textViewForgotPassword.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(context,"Email is required",Toast.LENGTH_LONG).show()
            } else if (!isValidEmail(email)) {
                Toast.makeText(context,"Invalid email format",Toast.LENGTH_LONG).show()
            } else {
                sendPasswordReset(email)
            }
        }
    }

    private fun loginWithEmailandPassword(view: View){
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        resetErrors()
        var hasError = false

        if(email.isEmpty() ){
            binding.editTextEmail.error = "Email is required"
            hasError = true
        }

        if (password.isEmpty()) {
            binding.editTextPassword.error = "Password is required"
            hasError = true
        }
        // Hata varsa fonksiyondan çık
        if (hasError) {
            return
        }
        auth.signInWithEmailAndPassword(email,password).addOnSuccessListener {
            val action = LoginFragmentDirections.actionLoginFragmentToFeedFragment()
            Navigation.findNavController(view).navigate(action)
        }.addOnFailureListener {exception ->
            Toast.makeText(context,"${exception.message}",Toast.LENGTH_LONG).show()

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
    private fun resetErrors() {
        // Hata mesajlarını temizle
        binding.editTextEmail.error = null
        binding.editTextPassword.error = null

    }

    private fun sendPasswordReset(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Başarıyla gönderildi
                    Toast.makeText(context, "We've sent you an email to reset your password.", Toast.LENGTH_LONG).show()
                } else {
                    // Hata oluştu
                    Toast.makeText(context, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }
    // Email format doğrulama fonksiyonu
    private fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}