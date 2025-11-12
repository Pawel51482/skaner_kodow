package com.example.skaner_kodow.ui.account

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.skaner_kodow.databinding.FragmentAccountBinding
import com.example.skaner_kodow.utils.ImgurUploader
import java.io.File

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private lateinit var viewModel: AccountViewModel
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(AccountViewModel::class.java)

        setupObservers()
        viewModel.loadUserData()

        binding.btnChangePhoto.setOnClickListener { openGallery() }

        binding.btnSave.setOnClickListener { saveChanges() }

        binding.btnResetPassword.setOnClickListener {
            viewModel.resetPassword {
                Toast.makeText(
                    requireContext(),
                    if (it) "Wysłano link do resetu hasła" else "Nie udało się wysłać linku",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            Toast.makeText(requireContext(), "Wylogowano", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }

        return binding.root
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.etUsername.setText(user.username)
            binding.tvEmail.text = user.email
            binding.tvRole.text = user.role

            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(user.photoUrl)
                    .circleCrop()
                    .into(binding.ivProfile)
            } else {
                binding.ivProfile.setImageResource(com.example.skaner_kodow.R.drawable.ic_person)
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun saveChanges() {
        val name = binding.etUsername.text.toString().trim()
        val finalName = if (name.isBlank()) "anonim" else name
        val localUri = imageUri

        // jeśli wybrano zdjęcie, wgrywamy do Imgur
        if (localUri != null) {
            val file = fileFromUri(localUri)
            if (file == null) {
                Toast.makeText(requireContext(), "Błąd odczytu pliku", Toast.LENGTH_SHORT).show()
                return
            }
            binding.progressBar.visibility = View.VISIBLE

            ImgurUploader.uploadImage(
                file,
                onSuccess = { url ->
                    activity?.runOnUiThread {
                        viewModel.updateProfile(finalName, url) { success ->
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                if (success) "Profil zaktualizowany" else "Błąd aktualizacji",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (success) imageUri = null
                        }
                    }
                },
                onError = { ex ->
                    activity?.runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Błąd wgrywania: ${ex.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        } else {
            // bez zmiany zdjęcia
            viewModel.updateProfile(finalName, null) { success ->
                Toast.makeText(
                    requireContext(),
                    if (success) "Profil zaktualizowany" else "Błąd aktualizacji",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            Glide.with(this)
                .load(imageUri)
                .circleCrop()
                .into(binding.ivProfile)
        }
    }

    // Konwersja Uri -> File
    private fun fileFromUri(uri: Uri): File? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri)
            val tmp = File.createTempFile("avatar_", ".jpg", requireContext().cacheDir)
            tmp.outputStream().use { out -> input?.copyTo(out) }
            tmp
        } catch (e: Exception) {
            null
        }
    }
}
