package com.example.skaner_kodow.ui.home

import androidx.lifecycle.ViewModelProvider

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.skaner_kodow.databinding.FragmentHomeBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tvResult: TextView
    private lateinit var btnScanBarcode: Button

    private val homeViewModel: HomeViewModel by activityViewModels()

    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        tvResult = binding.tvResult
        btnScanBarcode = binding.btnScanBarcode

        // Observing ViewModel's text live data
        homeViewModel.text.observe(viewLifecycleOwner) { text ->
            tvResult.text = text
        }

        if (!hasCameraPermission()) {
            requestCameraPermission()
        } else {
            enableScanButton()
        }

        return root
    }

    private fun hasCameraPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        )
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableScanButton() {
        btnScanBarcode.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val activities = requireActivity().packageManager.queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (activities.isNotEmpty()) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            tvResult.text = "Nie znaleziono aplikacji kamery"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                processBarcodeFromImage(imageBitmap)
            } else {
                tvResult.text = "Nie udało się uzyskać zdjęcia"
            }
        }
    }

    private fun processBarcodeFromImage(image: Bitmap) {
        val inputImage = InputImage.fromBitmap(image, 0)
        val scanner = BarcodeScanning.getClient()

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val barcodeValue = barcodes.first().rawValue ?: "Nie rozpoznano kodu"
                    tvResult.text = "Zeskanowany kod: $barcodeValue"
                } else {
                    tvResult.text = "Nie znaleziono żadnego kodu kreskowego."
                }
            }
            .addOnFailureListener {
                tvResult.text = "Błąd podczas skanowania: ${it.message}"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
