package com.universidad.streamzone.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.repository.CategoryRepository
import com.universidad.streamzone.data.firebase.repository.ServiceRepository
import com.universidad.streamzone.data.firebase.repository.OfferRepository
import com.universidad.streamzone.data.firebase.repository.UserRepository
import com.universidad.streamzone.data.model.Category
import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.ui.category.CategoryActivity
import com.universidad.streamzone.ui.components.NavbarManager
import com.universidad.streamzone.ui.home.adapter.CategoryCardAdapter
import com.universidad.streamzone.ui.home.adapter.GridSpacingItemDecoration
import com.universidad.streamzone.util.NotificationPermissionHelper
import com.universidad.streamzone.services.NotificationListenerService
import kotlinx.coroutines.launch

class HomeNativeActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var tvGreeting: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var fabAdminMenu: FloatingActionButton
    private var currentUser: String = ""

    private lateinit var cardHeroOffer: View
    private lateinit var tvOfferTitle: TextView
    private lateinit var tvOfferPrice: TextView

    // Firebase Repositories
    private val userRepository = UserRepository()
    private val categoryRepository = CategoryRepository()
    private val serviceRepository = ServiceRepository()
    private val offerRepository = OfferRepository()

    // Servicio de notificaciones en tiempo real
    private var notificationService: NotificationListenerService? = null

    // Mapa para guardar categoryId (Firebase ID -> Local display)
    private val categoryIdMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_base)
        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        // Configurar edge-to-edge
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Inflar contenido del home
        val contentContainer = findViewById<FrameLayout>(R.id.content_container)
        val homeView = LayoutInflater.from(this).inflate(R.layout.activity_home_native, contentContainer, true)

        // Aplicar padding superior para el notch
        homeView.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupViews()
        setupCategoriesRecyclerView()
        setupPopularServices()
        setupBottomNavbar()
        checkAdminStatus()

        // Solicitar permisos de notificaciones
        NotificationPermissionHelper.requestNotificationPermission(this)

        // Iniciar servicio de notificaciones
        startNotificationService()
    }

    private fun startNotificationService() {
        Log.d(TAG, "🔔 Inicializando NotificationListenerService...")
        notificationService = NotificationListenerService(applicationContext)
        notificationService?.startListening()
        Log.d(TAG, "✅ NotificationListenerService iniciado")
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationService?.stopListening()
        notificationService = null
        Log.d(TAG, "🛑 NotificationListenerService detenido")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        NotificationPermissionHelper.handlePermissionResult(
            requestCode,
            grantResults,
            onGranted = { Log.d(TAG, "Permisos de notificaciones concedidos") },
            onDenied = { Log.d(TAG, "Permisos de notificaciones denegados") }
        )
    }

    private fun setupViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        fabAdminMenu = findViewById(R.id.fab_admin_menu)
        cardHeroOffer = findViewById(R.id.cardHeroOffer)
        tvOfferTitle = findViewById(R.id.tvOfferTitle)
        tvOfferPrice = findViewById(R.id.tvOfferPrice)

        currentUser = intent.getStringExtra("USER_FULLNAME") ?: ""
        tvGreeting.text = if (currentUser.isNotEmpty()) "Bienvenido, $currentUser" else "Bienvenido"

        loadActiveOfferCard()

        fabAdminMenu.setOnClickListener {
            openAdminMenu()
        }
    }

    private fun checkAdminStatus() {
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) {
            Log.w(TAG, "❌ No hay usuario en sesión")
            fabAdminMenu.visibility = View.GONE
            return
        }

        Log.d(TAG, "🔍 Verificando permisos para: $userEmail")

        lifecycleScope.launch {
            try {
                val user = userRepository.findByEmail(userEmail)

                if (user == null) {
                    Log.e(TAG, "❌ Usuario no encontrado: $userEmail")
                    runOnUiThread { fabAdminMenu.visibility = View.GONE }
                    return@launch
                }

                Log.d(TAG, "👤 Usuario encontrado - isAdmin: ${user.isAdmin}")

                // Verificar si es admin o tiene permisos
                val hasPermissions = user.isAdmin || user.roleIds.isNotEmpty()

                runOnUiThread {
                    fabAdminMenu.visibility = if (hasPermissions) {
                        Log.d(TAG, "✅ FAB Admin VISIBLE")
                        View.VISIBLE
                    } else {
                        Log.d(TAG, "❌ FAB Admin OCULTO")
                        View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al verificar estado admin", e)
                runOnUiThread { fabAdminMenu.visibility = View.GONE }
            }
        }
    }

    private fun openAdminMenu() {
        val intent = Intent(this, com.universidad.streamzone.ui.admin.AdminMenuActivity::class.java)
        startActivity(intent)
    }

    private fun setupCategoriesRecyclerView() {
        rvCategories = findViewById(R.id.rvCategories)

        val gridLayoutManager = GridLayoutManager(this, 2)
        rvCategories.layoutManager = gridLayoutManager

        val spacingPx = (resources.displayMetrics.density * 8).toInt()
        rvCategories.addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))

        lifecycleScope.launch {
            try {
                // Obtener categorías activas desde Firebase
                val firebaseCategories = categoryRepository.getActiveCategoriesSync()

                // Contar servicios por categoría
                val serviceCounts = mutableMapOf<String, Int>()
                firebaseCategories.forEach { fbCategory ->
                    val services = serviceRepository.getAllSync()
                        .filter { it.categoryId == fbCategory.id && it.isActive }
                    serviceCounts[fbCategory.id] = services.size
                }

                // Convertir a modelo de UI
                val categories = firebaseCategories.map { fbCategory ->
                    categoryIdMap[fbCategory.id] = fbCategory.categoryId
                    Category(
                        id = fbCategory.categoryId,
                        name = fbCategory.name,
                        icon = fbCategory.icon,
                        serviceCount = serviceCounts[fbCategory.id] ?: 0,
                        gradientStart = fbCategory.gradientStart,
                        gradientEnd = fbCategory.gradientEnd
                    )
                }

                runOnUiThread {
                    val adapter = CategoryCardAdapter(categories) { category ->
                        onCategoryClick(category)
                    }
                    rvCategories.adapter = adapter
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar categorías", e)
                runOnUiThread {
                    showToast("Error al cargar categorías")
                }
            }
        }
    }

    private fun setupPopularServices() {
        lifecycleScope.launch {
            try {
                // Obtener servicios populares desde Firebase
                val popularServices = serviceRepository.getPopularServices()

                if (popularServices.isEmpty()) {
                    Log.w(TAG, "No hay servicios populares")
                    return@launch
                }

                runOnUiThread {
                    // Asignar listeners basados en serviceId
                    popularServices.forEach { service ->
                        val cardView = when (service.serviceId) {
                            "netflix" -> findViewById<View>(R.id.card_popular_netflix)
                            "spotify" -> findViewById<View>(R.id.card_popular_spotify)
                            "disney_plus_premium" -> findViewById<View>(R.id.card_popular_disney)
                            "chatgpt" -> findViewById<View>(R.id.card_popular_disney)
                            else -> null
                        }

                        cardView?.setOnClickListener {
                            Log.d(TAG, "Click en ${service.name}")
                            openPurchaseDialog(service)
                        }
                    }

                    Log.d(TAG, "Listeners configurados para ${popularServices.size} servicios")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar servicios populares", e)
            }
        }
    }

    private fun openPurchaseDialog(service: com.universidad.streamzone.data.firebase.models.Service) {
        val dlg = PurchaseDialogFragment.newInstance(
            service.serviceId,
            service.name,
            service.price,
            service.description,
            currentUser,
            null,
            null // iconRes - Firebase usa URLs
        )
        dlg.show(supportFragmentManager, "purchaseDialog")
    }

    private lateinit var navbarManager: NavbarManager

    private fun setupBottomNavbar() {
        navbarManager = NavbarManager(this, NavbarManager.Screen.HOME)
    }

    private fun onCategoryClick(category: Category) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("CATEGORY_NAME", category.name)
        intent.putExtra("CATEGORY_ICON", category.icon)
        intent.putExtra("CATEGORY_ID", category.id) // Ahora usamos el String ID
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun loadActiveOfferCard() {
        lifecycleScope.launch {
            try {
                // Obtener oferta activa desde Firebase
                val activeOfferData = offerRepository.getActiveOfferWithServices()

                runOnUiThread {
                    if (activeOfferData != null && activeOfferData.isValid()) {
                        cardHeroOffer.visibility = View.VISIBLE
                        tvOfferTitle.text = activeOfferData.offer.title
                        tvOfferPrice.text = "US$ %.2f/mes (ahorra ${activeOfferData.offer.discountPercent}%%)"
                            .format(activeOfferData.offer.comboPrice)

                        findViewById<Button>(R.id.btnViewOffer).setOnClickListener {
                            openActiveOffer(activeOfferData.offer.id)
                        }

                        Log.d(TAG, "✅ Oferta cargada: ${activeOfferData.offer.title}")
                    } else {
                        cardHeroOffer.visibility = View.GONE
                        Log.d(TAG, "⚠️ No hay ofertas activas")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar oferta", e)
                runOnUiThread {
                    cardHeroOffer.visibility = View.GONE
                }
            }
        }
    }

    private fun openActiveOffer(offerId: String) {
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) {
            showToast("Debes iniciar sesión para ver las ofertas")
            return
        }

        val dialog = PurchaseOfferDialogFragment.newInstance(
            offerId = offerId,
            userEmail = userEmail,
            userName = currentUser
        )
        dialog.show(supportFragmentManager, "offerDialog")
    }

    companion object {
        private const val TAG = "HomeNativeActivity"
    }
}
