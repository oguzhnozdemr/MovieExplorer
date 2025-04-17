package com.oguzhanozdemir.kotlincountries.view

import android.os.Bundle
import android.util.Log
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
import com.oguzhanozdemir.kotlincountries.databinding.FragmentSignUpBinding


class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth

        binding.buttonBack.setOnClickListener {
            val action = SignUpFragmentDirections.actionSignUpFragmentToLoginFragment()
            Navigation.findNavController(view).navigate(action)
        }

        binding.textViewLogin.setOnClickListener {
            val action = SignUpFragmentDirections.actionSignUpFragmentToLoginFragment()
            Navigation.findNavController(view).navigate(action)
        }

        binding.buttonSignUp.setOnClickListener {
            createUserWithEmailAndPassword(view)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }

    private fun createUserWithEmailAndPassword(view: View) {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()
        val name = binding.editTextName.text.toString().trim()

        // Tüm hataları kontrol öncesi sıfırlayalım
        resetErrors()

        var hasError = false

        // Boşluk kontrolü
        if (name.isEmpty()) {
            binding.editTextName.error = "Name is required"
            hasError = true
        }

        if (email.isEmpty()) {
            binding.editTextEmail.error = "Email is required"
            hasError = true
        } else if (!isValidEmail(email)) {
            // Email format kontrolü
            binding.editTextEmail.error = "Invalid email format"
            hasError = true
        }

        if (password.isEmpty()) {
            binding.editTextPassword.error = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            // Şifre uzunluk kontrolü
            binding.editTextPassword.error = "Password must be at least 6 characters"
            hasError = true
        }

        if (confirmPassword.isEmpty()) {
            binding.editTextConfirmPassword.error = "Confirm password is required"
            hasError = true
        } else if (password != confirmPassword) {
            // Şifre eşleşmeme durumunda hata ikonu yerine kırmızı hint kullanımı
            binding.editTextConfirmPassword.hint = "Passwords do not match"
            binding.editTextConfirmPassword.setHintTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            hasError = true
        }

        // Terms kontrolü
        if (!binding.checkBoxTerms.isChecked) {
            Toast.makeText(context, "You must agree to the Terms of Service and Privacy Policy", Toast.LENGTH_LONG).show()
            hasError = true
        }

        // Hata varsa fonksiyondan çık
        if (hasError) {
            return
        }

        // Tüm kontroller başarılı ise kullanıcı oluştur
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            // Email doğrulama linki gönderme
            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Verification email sent. Please verify your email before logging in.", Toast.LENGTH_LONG).show()
                    val action = SignUpFragmentDirections.actionSignUpFragmentToLoginFragment()
                    Navigation.findNavController(view).navigate(action)
                } else {
                    Toast.makeText(context, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Hataları sıfırlama
    private fun resetErrors() {
        // Hata mesajlarını temizle
        binding.editTextEmail.error = null
        binding.editTextPassword.error = null
        binding.editTextConfirmPassword.error = null
        binding.editTextName.error = null

        // Hint renklerini ve metinlerini orijinal haline getir
        binding.editTextConfirmPassword.hint = "Confirm Password"
        binding.editTextConfirmPassword.setHintTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
    }

    // Email format doğrulama fonksiyonu
    private fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}