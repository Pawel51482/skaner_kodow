package com.example.skaner_kodow

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.skaner_kodow.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.setLanguageCode("pl")
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        setSupportActionBar(binding.appBarMain.toolbar)

        //        binding.appBarMain.fab.setOnClickListener { view ->
        //            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //                .setAction("Action", null)
        //                .setAnchorView(R.id.fab).show()
        //        }

        //wyświetlenie emaila użytkownika w menu
        val user = firebaseAuth.currentUser
        val userEmail = user?.email
        if (userEmail != null) {
            val navView: NavigationView = binding.navView
            val headerView = navView.getHeaderView(0)

            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarH = if (resId > 0) resources.getDimensionPixelSize(resId) else 0
            headerView.setPadding(
                headerView.paddingLeft,
                statusBarH + headerView.paddingTop,
                headerView.paddingRight,
                headerView.paddingBottom
            )

            val textViewEmail: TextView = headerView.findViewById(R.id.textViewEmail)
            textViewEmail.text = userEmail
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_products, R.id.nav_promotions, R.id.nav_account, R.id.nav_favorites
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //Wylogowyywanie użytkownika
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    firebaseAuth.signOut()

                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> {
                    findNavController(R.id.nav_host_fragment_content_main).navigate(menuItem.itemId)
                    binding.drawerLayout.closeDrawers()
                    true
                }
            }
        }

        setupUserHeader(navView)
    }

    // funkcja do ładowania username + avatara w menu
    private fun setupUserHeader(navView: NavigationView) {
        val headerView = navView.getHeaderView(0)
        val ivProfile = headerView.findViewById<ImageView>(R.id.imageViewProfile)
        val tvUsername = headerView.findViewById<TextView>(R.id.textViewUsername)
        val tvEmail = headerView.findViewById<TextView>(R.id.textViewEmail)

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) return

        val uid = currentUser.uid
        val userRef = dbRef.child(uid)

        // Pierwsze wczytanie danych z bazy
        userRef.get().addOnSuccessListener { snap ->
            val username = snap.child("username").getValue(String::class.java).orEmpty()
            val photoUrl = snap.child("photoUrl").getValue(String::class.java).orEmpty()
            val email = snap.child("email").getValue(String::class.java) ?: currentUser.email

            tvUsername.text = if (username.isNotBlank()) username else "Anonim"
            tvEmail.text = email ?: ""

            if (photoUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(ivProfile)
            } else {
                ivProfile.setImageResource(R.drawable.ic_person)
            }
        }

        // Nasłuchiwanie zmian w czasie rzeczywistym
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java).orEmpty()
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java).orEmpty()

                tvUsername.text = if (username.isNotBlank()) username else "Anonim"
                if (photoUrl.isNotEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(photoUrl)
                        .circleCrop()
                        .into(ivProfile)
                } else {
                    ivProfile.setImageResource(R.drawable.ic_person)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        navHostFragment?.childFragmentManager?.fragments?.forEach { fragment ->
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }
}
