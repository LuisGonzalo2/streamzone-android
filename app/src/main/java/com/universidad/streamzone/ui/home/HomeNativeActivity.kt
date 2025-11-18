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
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.Category
import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.ui.category.CategoryActivity
import com.universidad.streamzone.ui.components.NavbarManager
import com.universidad.streamzone.ui.home.adapter.CategoryCardAdapter
import com.universidad.streamzone.ui.home.adapter.GridSpacingItemDecoration
import com.universidad.streamzone.util.NotificationPermissionHelper
import com.universidad.streamzone.util.toCategory
import com.universidad.streamzone.util.toServiceList
import kotlinx.coroutines.launch

class HomeNativeActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var tvGreeting: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var fabAdminMenu: FloatingActionButton
    private var currentUser: String = ""
    private var currentUserId: Int = 0

    private lateinit var cardHeroOffer: View
    private lateinit var tvOfferTitle: TextView
    private lateinit var tvOfferPrice: TextView

    // Mapa para guardar categoryId (String -> Int) para pasarlo al Intent
    private val categoryIdMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usar el layout base con navbar inferior
        setContentView(R.layout.activity_base)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        // Configurar edge-to-edge con padding dinámico
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Inflar el contenido específico del home
        val contentContainer = findViewById<FrameLayout>(R.id.content_container)
        val homeView = LayoutInflater.from(this).inflate(R.layout.activity_home_native, contentContainer, true)

        // Aplicar padding superior para evitar el notch
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

        // DEBUG: Verificar que los views existen
        Log.d("HomeNative", "Netflix card: ${findViewById<View>(R.id.card_popular_netflix) != null}")
        Log.d("HomeNative", "Spotify card: ${findViewById<View>(R.id.card_popular_spotify) != null}")
        Log.d("HomeNative", "Disney card: ${findViewById<View>(R.id.card_popular_disney) != null}")

        setupViews()
        setupCategoriesRecyclerView()
        setupPopularServices()
        setupBottomNavbar()
        checkAdminStatus()

        // Solicitar permisos de notificaciones (Android 13+)
        NotificationPermissionHelper.requestNotificationPermission(this)
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
            onGranted = {
                Log.d("HomeNative", "Permisos de notificaciones concedidos")
            },
            onDenied = {
                Log.d("HomeNative", "Permisos de notificaciones denegados")
            }
        )
    }

    private fun setupViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        fabAdminMenu = findViewById(R.id.fab_admin_menu)

        //Referencias al Hero Card
        cardHeroOffer = findViewById(R.id.cardHeroOffer)
        tvOfferTitle = findViewById(R.id.tvOfferTitle)
        tvOfferPrice = findViewById(R.id.tvOfferPrice)

        currentUser = intent.getStringExtra("USER_FULLNAME") ?: ""
        tvGreeting.text = if (currentUser.isNotEmpty()) "Bienvenido, $currentUser" else "Bienvenido"

        // Cargar oferta dinámica
        loadActiveOfferCard()

        // Configurar FAB de admin
        fabAdminMenu.setOnClickListener {
            openAdminMenu()
        }
    }

    //  Verificar si el usuario es admin
    private fun checkAdminStatus() {
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) return

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@HomeNativeActivity)
                val usuarioDao = db.usuarioDao()
                val usuario = usuarioDao.buscarPorEmail(userEmail)

                usuario?.let { user ->
                    currentUserId = user.id

                    // Verificar si el usuario es admin O tiene algún permiso
                    val hasAnyPermission = checkIfUserHasAnyPermission(user.id)

                    if (user.isAdmin || hasAnyPermission) {
                        runOnUiThread {
                            fabAdminMenu.visibility = View.VISIBLE
                            Log.d("HomeNative", "Usuario tiene acceso al panel admin")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeNative", "Error al verificar estado admin", e)
            }
        }
    }

    // Verificar si el usuario tiene algún permiso asignado
    private suspend fun checkIfUserHasAnyPermission(userId: Int): Boolean {
        return try {
            val db = AppDatabase.getInstance(this@HomeNativeActivity)
            val userRoleDao = db.userRoleDao()
            val rolePermissionDao = db.rolePermissionDao()

            // Obtener roles del usuario
            val userRoleIds = userRoleDao.getRolesByUserId(userId)

            // Si tiene roles, verificar si alguno tiene permisos
            if (userRoleIds.isNotEmpty()) {
                // Verificar si algún rol tiene permisos asignados
                userRoleIds.any { roleId ->
                    val permissions = rolePermissionDao.obtenerPermisosPorRol(roleId.toInt())
                    permissions.isNotEmpty()
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("HomeNative", "Error al verificar permisos", e)
            false
        }
    }

    // Abrir oferta activa
    private fun openActiveOffer() {
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        // ✅ AGREGAR ESTE LOG PARA DEBUG
        android.util.Log.d("HomeNative", "Email: '$userEmail', User: '$currentUser'")

        if (userEmail.isEmpty()) {
            showToast("Debes iniciar sesión para ver las ofertas")
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@HomeNativeActivity)
                val offerDao = db.offerDao()

                val activeOffer = offerDao.getActiveOffer()
                if (activeOffer == null) {
                    runOnUiThread {
                        showToast("No hay ofertas disponibles en este momento")
                    }
                    return@launch
                }

                runOnUiThread {
                    val dialog = PurchaseOfferDialogFragment.newInstance(
                        offerId = activeOffer.id,
                        userEmail = userEmail,
                        userName = currentUser
                    )
                    dialog.show(supportFragmentManager, "offerDialog")
                }

            } catch (e: Exception) {
                Log.e("HomeNative", "Error al cargar oferta", e)
                runOnUiThread {
                    showToast("Error al cargar la oferta")
                }
            }
        }
    }


    // NUEVA FUNCIÓN: Abrir menú de admin
    private fun openAdminMenu() {
        val intent = Intent(this, com.universidad.streamzone.ui.admin.AdminMenuActivity::class.java)
        startActivity(intent)
    }

    private fun setupCategoriesRecyclerView() {
        rvCategories = findViewById(R.id.rvCategories)

        // Grid de 2 columnas con aspect ratio dinámico
        val gridLayoutManager = GridLayoutManager(this, 2)
        rvCategories.layoutManager = gridLayoutManager

        // Ajustar espaciado
        val spacingPx = (resources.displayMetrics.density * 8).toInt()
        rvCategories.addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))

        // Cargar categorías desde la BD
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@HomeNativeActivity)
                val categoryDao = db.categoryDao()
                val serviceDao = db.serviceDao()

                // Obtener categorías activas
                val categoryEntities = categoryDao.obtenerCategoriasActivasSync()

                // Calcular el conteo de servicios por categoría
                val serviceCounts = mutableMapOf<Int, Int>()
                categoryEntities.forEach { categoryEntity ->
                    val count = serviceDao.obtenerServiciosPorCategoriaSync(categoryEntity.id).size
                    serviceCounts[categoryEntity.id] = count
                }

                // Convertir a modelo de UI y guardar mapeo de IDs
                val categories = categoryEntities.map { categoryEntity ->
                    categoryIdMap[categoryEntity.categoryId] = categoryEntity.id
                    categoryEntity.toCategory(serviceCounts[categoryEntity.id] ?: 0)
                }

                // Actualizar UI en el hilo principal
                runOnUiThread {
                    val adapter = CategoryCardAdapter(categories) { category ->
                        onCategoryClick(category)
                    }
                    rvCategories.adapter = adapter
                }

            } catch (e: Exception) {
                Log.e("HomeNative", "Error al cargar categorías", e)
                runOnUiThread {
                    showToast("Error al cargar categorías")
                }
            }
        }
    }

    private fun setupPopularServices() {
        // Cargar servicios populares desde la BD
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@HomeNativeActivity)
                val serviceDao = db.serviceDao()

                // Obtener servicios marcados como populares
                val popularServiceEntities = serviceDao.obtenerServiciosPopulares()
                val popularServices = popularServiceEntities.toServiceList()

                // Si no hay servicios populares en la BD, no hacer nada
                if (popularServices.isEmpty()) {
                    Log.w("HomeNative", "No hay servicios populares en la BD")
                    return@launch
                }

                // Actualizar UI en el hilo principal
                runOnUiThread {
                    // Asignar listeners dinámicamente según los servicios populares
                    // Nota: Los IDs de las cards están hardcodeados en el XML (card_popular_netflix, etc.)
                    // Para hacerlo completamente dinámico, sería mejor usar un RecyclerView

                    // Por ahora, asignar basado en el serviceId
                    popularServices.forEachIndexed { index, service ->
                        val cardView = when (service.id) {
                            "netflix" -> findViewById<View>(R.id.card_popular_netflix)
                            "spotify" -> findViewById<View>(R.id.card_popular_spotify)
                            "disney_plus_premium" -> findViewById<View>(R.id.card_popular_disney)
                            "chatgpt" -> findViewById<View>(R.id.card_popular_disney) // Reusa una card existente
                            else -> null
                        }

                        cardView?.setOnClickListener {
                            Log.d("HomeNative", "Click en ${service.title}")
                            openPurchaseDialog(service)
                        }
                    }

                    Log.d("HomeNative", "Listeners configurados para ${popularServices.size} servicios populares")
                }

            } catch (e: Exception) {
                Log.e("HomeNative", "Error al cargar servicios populares", e)
            }
        }
    }

    private fun openPurchaseDialog(service: Service) {
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

    private lateinit var navbarManager: NavbarManager

    private fun setupBottomNavbar() {
        navbarManager = NavbarManager(this, NavbarManager.Screen.HOME)
    }

    private fun onCategoryClick(category: Category) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("CATEGORY_NAME", category.name)
        intent.putExtra("CATEGORY_ICON", category.icon)

        // Obtener el ID numérico de la categoría desde el mapa
        val categoryId = categoryIdMap[category.id] ?: -1
        intent.putExtra("CATEGORY_ID", categoryId)

        startActivity(intent)
    }

    private fun cerrarSesion() {
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
    /**
     * Cargar oferta activa y mostrarla en el Hero Card
     */
    private fun loadActiveOfferCard() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@HomeNativeActivity)
                val offerDao = db.offerDao()

                // ✅ NUEVO: Sincronizar ofertas desde Firebase
                if (isNetworkAvailable()) {
                    syncOffersFromFirebase()
                }

                val activeOffer = offerDao.getActiveOffer()

                runOnUiThread {
                    if (activeOffer != null) {
                        // Mostrar oferta en el Hero Card
                        cardHeroOffer.visibility = View.VISIBLE
                        tvOfferTitle.text = activeOffer.title
                        tvOfferPrice.text = "US$ %.2f/mes (ahorra ${activeOffer.discountPercent}%%)"
                            .format(activeOffer.comboPrice)

                        findViewById<Button>(R.id.btnViewOffer).setOnClickListener {
                            openActiveOffer()
                        }

                        Log.d("HomeNative", "✅ Oferta cargada: ${activeOffer.title}")
                    } else {
                        // No hay oferta activa, ocultar el Hero Card
                        cardHeroOffer.visibility = View.GONE
                        Log.d("HomeNative", "⚠️ No hay ofertas activas")
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeNative", "❌ Error al cargar oferta", e)
                runOnUiThread {
                    cardHeroOffer.visibility = View.GONE
                }
            }
        }
    }
    /**
     * ✅ NUEVA FUNCIÓN: Sincronizar ofertas desde Firebase
     */
    private suspend fun syncOffersFromFirebase() {
        com.universidad.streamzone.data.remote.FirebaseService.obtenerTodasLasOfertas { firebaseOffers ->
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@HomeNativeActivity)
                    val offerDao = db.offerDao()

                    firebaseOffers.forEach { firebaseOffer ->
                        // Buscar si ya existe localmente
                        val localOffer = offerDao.getAll()
                            .find { it.firebaseId == firebaseOffer.firebaseId }

                        if (localOffer == null) {
                            // Nueva oferta → Insertar
                            offerDao.insert(firebaseOffer)
                            Log.d("HomeNative", "➕ Oferta insertada: ${firebaseOffer.title}")
                        } else {
                            // Oferta existe → Actualizar
                            val updated = firebaseOffer.copy(id = localOffer.id)
                            offerDao.update(updated)
                            Log.d("HomeNative", "🔄 Oferta actualizada: ${firebaseOffer.title}")
                        }
                    }

                    // Recargar oferta activa
                    runOnUiThread {
                        loadActiveOfferCard()
                    }

                } catch (e: Exception) {
                    Log.e("HomeNative", "❌ Error al guardar ofertas de Firebase", e)
                }
            }
        }
    }

    /**
     * Verificar conectividad
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false

            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            false
        }
    }
}