package com.universidad.streamzone.ui.category

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.repository.ServiceRepository
import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.ui.components.NavbarManager
import com.universidad.streamzone.ui.home.PurchaseDialogFragment
import com.universidad.streamzone.ui.home.adapter.GridSpacingItemDecoration
import com.universidad.streamzone.ui.home.adapter.ServiceAdapter
import com.universidad.streamzone.util.toUIServiceList
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var rvServices: RecyclerView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvCategorySubtitle: TextView
    private lateinit var tvCategoryIcon: TextView
    private lateinit var btnBack: MaterialButton

    // Firebase Repository
    private val serviceRepository = ServiceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usar el layout base con navbar inferior
        setContentView(R.layout.activity_category)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Inflar el contenido específico de CategoryActivity
        val contentContainer = findViewById<FrameLayout>(R.id.category_container)
        val categoryView = LayoutInflater.from(this).inflate(R.layout.activity_category_content, contentContainer, true)

        // Aplicar padding superior para evitar el notch
        categoryView.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        initViews()
        loadCategoryData()
        setupRecyclerView()
        setupBottomNavbar()
    }

    private fun initViews() {
        rvServices = findViewById(R.id.rv_category_services)
        tvCategoryTitle = findViewById(R.id.tv_category_title)
        tvCategorySubtitle = findViewById(R.id.tv_category_subtitle)
        tvCategoryIcon = findViewById(R.id.tv_category_icon)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCategoryData() {
        // Obtener datos de la categoría desde el Intent
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Categoría"
        val categoryIcon = intent.getStringExtra("CATEGORY_ICON") ?: "📦"

        // Actualizar UI
        tvCategoryTitle.text = categoryName
        tvCategoryIcon.text = categoryIcon
    }

    private fun setupRecyclerView() {
        // Obtener el ID de la categoría desde el Intent
        val categoryId = intent.getStringExtra("CATEGORY_ID") ?: ""

        if (categoryId.isEmpty()) {
            showToast("Error: Categoría no encontrada")
            finish()
            return
        }

        // Configurar RecyclerView
        rvServices.layoutManager = GridLayoutManager(this, 2)
        val spacingPx = (resources.displayMetrics.density * 12).toInt()
        rvServices.addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))

        // Cargar servicios desde Firebase
        lifecycleScope.launch {
            try {
                // Obtener servicios de la categoría desde Firebase
                val firebaseServices = serviceRepository.getServicesByCategory(categoryId)
                val services = firebaseServices.toUIServiceList()

                // Actualizar UI en el hilo principal
                runOnUiThread {
                    tvCategorySubtitle.text = "${services.size} servicios disponibles"

                    val adapter = ServiceAdapter(services) { service ->
                        onServiceClick(service)
                    }
                    rvServices.adapter = adapter
                }

            } catch (e: Exception) {
                runOnUiThread {
                    showToast("Error al cargar servicios: ${e.message}")
                }
            }
        }
    }

    private lateinit var navbarManager: NavbarManager

    private fun setupBottomNavbar() {
        navbarManager = NavbarManager(this, NavbarManager.Screen.HOME)
    }

    private fun onServiceClick(service: Service) {
        // Obtener el nombre del usuario desde SharedPreferences
        val sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("logged_in_user_name", "") ?: ""

        // Mostrar el diálogo de compra
        val dlg = PurchaseDialogFragment.newInstance(
            service.id,
            service.title,
            service.price,
            service.desc,
            currentUser,
            null,
            service.iconRes
        )
        dlg.show(supportFragmentManager, "purchaseDialog")
    }

    private fun cerrarSesion() {
        val sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove("logged_in_user_email")
            remove("logged_in_user_name")
            remove("session_start_time")
            apply()
        }

        Toast.makeText(this, "👋 Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}