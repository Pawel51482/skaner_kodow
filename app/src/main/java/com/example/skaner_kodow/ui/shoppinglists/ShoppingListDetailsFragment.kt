package com.example.skaner_kodow.ui.shoppinglists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skaner_kodow.BarcodeScannerActivity
import com.example.skaner_kodow.R
import com.example.skaner_kodow.databinding.FragmentShoppingListDetailsBinding
import com.example.skaner_kodow.ui.products.ProductsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import android.content.ContentResolver
import android.provider.OpenableColumns

class ShoppingListDetailsFragment : Fragment() {

    private lateinit var binding: FragmentShoppingListDetailsBinding
    private lateinit var viewModel: ShoppingListDetailsViewModel
    private lateinit var productsViewModel: ProductsViewModel
    private lateinit var adapter: ShoppingListItemsAdapter

    // ID bieżącej listy
    private var currentListId: String = ""

    private var pendingImageItemId: String? = null

    // launcher skanera
    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val code = result.data?.getStringExtra("scanned_code")
                if (code != null) {
                    handleScannedBarcode(code)
                }
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val itemId = pendingImageItemId ?: return@registerForActivityResult
            if (uri == null) return@registerForActivityResult

            val ctx = requireContext()
            val file = createTempFileFromUri(uri)
            if (file == null) {
                Toast.makeText(ctx, "Nie udało się odczytać pliku", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            // upload do Imgur
            com.example.skaner_kodow.utils.ImgurUploader.uploadImage(
                file,
                onSuccess = { url ->
                    requireActivity().runOnUiThread {
                        viewModel.updateItemImage(itemId, url)
                        Toast.makeText(ctx, "Zdjęcie zaktualizowane", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = {
                    requireActivity().runOnUiThread {
                        Toast.makeText(ctx, "Błąd uploadu zdjęcia", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingListDetailsBinding.inflate(inflater, container, false)

        val listId = arguments?.getString("listId") ?: ""
        currentListId = listId

        viewModel = ShoppingListDetailsViewModel(listId)
        productsViewModel = ViewModelProvider(requireActivity()).get(ProductsViewModel::class.java)

        // załaduj produkty z bazy
        productsViewModel.fetchProducts()

        setupRecycler()
        setupFab()

        viewModel.observeItems()

        viewModel.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        return binding.root
    }

    private fun setupRecycler() {
        adapter = ShoppingListItemsAdapter(
            onCheckClick = { itemId, checked ->
                viewModel.toggleChecked(itemId, checked)
            },
            onLongClick = { item ->
                showItemOptionsDialog(item)
            }
        )

        binding.recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewItems.adapter = adapter
    }

    private fun setupFab() {
        // po kliknięciu + pokazujemy menu
        binding.fabAddItem.setOnClickListener {
            showAddItemMenu()
        }
    }

    private fun showAddItemMenu() {
        val options = arrayOf(
            "Dodaj skanerem",
            "Dodaj z produktów",
            "Dodaj własny produkt"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj do listy")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addItemByScanner()
                    1 -> addItemFromProducts()
                    2 -> addCustomItem()
                }
            }
            .show()
    }

    // otwarcie skanera
    private fun addItemByScanner() {
        val intent = android.content.Intent(requireContext(), BarcodeScannerActivity::class.java)
        scanLauncher.launch(intent)
    }

    // logika po zeskanowaniu kodu
    private fun handleScannedBarcode(barcodeRaw: String) {
        val barcode = barcodeRaw.trim()
        if (barcode.isEmpty()) {
            Toast.makeText(requireContext(), "Pusty kod kreskowy", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) najpierw próbujemy w już wczytanej liście produktów
        val localProducts = productsViewModel.filteredProducts.value ?: emptyList()
        val localMatch = localProducts.firstOrNull { it.barcode == barcode }

        if (localMatch != null) {
            askQuantityAndAdd(localMatch.id, localMatch.name)
            return
        }

        // 2) jak w pamięci nie ma, szukamy bezpośrednio w firebase /products po polu barcode
        val ref = FirebaseDatabase.getInstance()
            .getReference("products")

        ref.orderByChild("barcode").equalTo(barcode).limitToFirst(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Toast.makeText(
                        requireContext(),
                        "Nie znaleziono produktu dla kodu: $barcode",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                val child = snap.children.first()
                val productId = child.key ?: return@addOnSuccessListener
                val productName = child.child("name").getValue(String::class.java) ?: "produkt"

                askQuantityAndAdd(productId, productName)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Błąd wyszukiwania produktu", Toast.LENGTH_SHORT).show()
            }
    }

    // zapytaj o ilość i dodaj
    private fun askQuantityAndAdd(productId: String, productName: String) {
        val context = requireContext()

        val qtyEdit = EditText(context).apply {
            hint = "Ilość"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(qtyEdit)
        }

        AlertDialog.Builder(context)
            .setTitle("Ile sztuk dodać?")
            .setView(layout)
            .setPositiveButton("Dodaj") { _, _ ->
                val qtyText = qtyEdit.text.toString().trim()
                val qty = qtyText.toIntOrNull() ?: 1
                addOrIncrementProduct(productId, productName, qty)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    // jeśli item z tym productId istnieje - zwiększ ilość jeśli nie, to stwórz
    private fun addOrIncrementProduct(productId: String, productName: String, quantityToAdd: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val listId = currentListId.ifEmpty { arguments?.getString("listId") ?: return }

        val itemsRef = FirebaseDatabase.getInstance()
            .getReference("users/$uid/shoppingLists/$listId/items")

        itemsRef.orderByChild("productId").equalTo(productId).limitToFirst(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    // jest już taki produkt - zwiększamy ilość
                    val child = snap.children.first()
                    val currentQty = child.child("quantity").getValue(Int::class.java) ?: 1
                    child.ref.child("quantity").setValue(currentQty + quantityToAdd)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Dodano ${quantityToAdd} szt. $productName (razem ${currentQty + quantityToAdd})",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Błąd przy aktualizacji ilości",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // nie ma jeszcze tego produktu na liście - tworzymy nowy
                    val ref = itemsRef.push()
                    val item = ShoppingListItem(
                        id = ref.key ?: "",
                        productId = productId,
                        quantity = quantityToAdd,
                        checked = false
                    )
                    ref.setValue(item)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Dodano ${quantityToAdd} szt. $productName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Błąd przy dodawaniu do listy",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Błąd odczytu listy", Toast.LENGTH_SHORT).show()
            }
    }

    // „Dodaj z produktów” – tylko nawigacja z listId
    private fun addItemFromProducts() {
        val navController = findNavController()
        val bundle = Bundle().apply {
            putString("listId", currentListId)
        }
        navController.navigate(R.id.selectProductFragment, bundle)
    }

    // dodawanie własnego produktu (nazwa + ilość)
    private fun addCustomItem() {
        val context = requireContext()

        val nameEdit = EditText(context).apply {
            hint = "Nazwa produktu"
        }

        val qtyEdit = EditText(context).apply {
            hint = "Ilość"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(nameEdit)
            addView(qtyEdit)
        }

        AlertDialog.Builder(context)
            .setTitle("Dodaj własny produkt")
            .setView(layout)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = nameEdit.text.toString().trim()
                val qtyText = qtyEdit.text.toString().trim()
                val qty = qtyText.toIntOrNull() ?: 1

                if (name.isEmpty()) {
                    Toast.makeText(context, "Podaj nazwę produktu", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addCustomItem(name, qty)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showItemOptionsDialog(item: ShoppingListItem) {
        val options = arrayOf(
            "Zmień ilość",
            "Ustaw / zmień zdjęcie",   // 🆕
            "Usuń z listy"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Opcje pozycji")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditQuantityDialog(item)
                    1 -> {
                        pendingImageItemId = item.id
                        pickImageLauncher.launch("image/*")
                    }
                    2 -> deleteItem(item)
                }
            }
            .show()
    }

    private fun showEditQuantityDialog(item: ShoppingListItem) {
        val context = requireContext()

        val qtyEdit = android.widget.EditText(context).apply {
            hint = "Ilość"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(item.quantity.toString())
            setSelection(text.length)
        }

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(qtyEdit)
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Zmień ilość")
            .setView(layout)
            .setPositiveButton("Zapisz") { _, _ ->
                val txt = qtyEdit.text.toString().trim()
                val newQty = txt.toIntOrNull()

                if (newQty == null || newQty <= 0) {
                    android.widget.Toast.makeText(
                        context,
                        "Podaj poprawną ilość (> 0)",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.updateQuantity(item.id, newQty)
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }



    private fun deleteItem(item: ShoppingListItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val listId = currentListId.ifEmpty { arguments?.getString("listId") ?: return }

        FirebaseDatabase.getInstance()
            .getReference("users/$uid/shoppingLists/$listId/items/${item.id}")
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Usunięto produkt", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Błąd przy usuwaniu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val resolver: ContentResolver = requireContext().contentResolver
            val name = queryName(resolver, uri) ?: "image.jpg"
            val file = File(requireContext().cacheDir, name)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? {
        val returnCursor = resolver.query(uri, null, null, null, null) ?: return null
        returnCursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
    }
}
