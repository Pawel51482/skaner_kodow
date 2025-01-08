package com.example.skaner_kodow.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skaner_kodow.BarcodeScannerActivity
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentHomeBinding
import com.example.skaner_kodow.ui.products.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val SCAN_REQUEST_CODE = 1

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        homeViewModel.text.observe(viewLifecycleOwner) { text ->
            binding.tvResult.text = text
        }

        binding.btnOpenScanner.setOnClickListener {
            val intent = Intent(requireContext(), BarcodeScannerActivity::class.java)
            startActivityForResult(intent, SCAN_REQUEST_CODE)
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCAN_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val scannedCode = data.getStringExtra("scanned_code")
            if (scannedCode != null) {
                checkBarcodeInDatabase(scannedCode)
            } else {
                Toast.makeText(context, "Nie zeskanowano kodu", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun checkBarcodeInDatabase(barcode: String) {
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("products")

        productsRef.orderByChild("barcode").equalTo(barcode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val productSnapshot = snapshot.children.first()
                        val product = productSnapshot.getValue(Product::class.java)

                        if (product != null) {
                            homeViewModel.updateText("Znaleziono: ${product.name}\nKod: ${product.barcode}")

                            binding.buttonDetails.apply {
                                visibility = View.VISIBLE
                                setOnClickListener {
                                    val bundle = Bundle().apply {
                                        putString("name", product.name)
                                        putString("barcode", product.barcode)
                                        putString("description", product.description)
                                        putString("imageUrl", product.imageUrl)
                                    }
                                    findNavController().navigate(
                                        R.id.action_homeFragment_to_productDetailsFragment,
                                        bundle
                                    )
                                }
                            }
                        }
                    } else {
                        homeViewModel.updateText("Nie znaleziono kodu w bazie: $barcode")
                        binding.buttonDetails.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
