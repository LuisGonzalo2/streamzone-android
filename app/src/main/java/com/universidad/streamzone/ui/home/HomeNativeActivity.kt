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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.Category
import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.ui.category.CategoryActivity
import com.universidad.streamzone.ui.home.adapter.CategoryCardAdapter
import com.universidad.streamzone.ui.home.adapter.GridSpacingItemDecoration
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.ServicioPopular
import kotlinx.coroutines.launch
class HomeNativeActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var tvGreeting: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private var currentUser: String = ""

    // Definir las categorías
    private val categories = listOf(
        Category(
            id = "streaming",
            name = "Streaming",
            icon = "📺",
            description = "Netflix, Disney+, Max y más",
            serviceCount = 8,
            gradientStart = R.color.category_streaming_start,
            gradientEnd = R.color.category_streaming_end,
            serviceIds = listOf("netflix", "disney_plus_premium", "disney_plus_standard", "max", "vix", "prime", "paramount", "appletv", "crunchyroll")
        ),
        Category(
            id = "music",
            name = "Música",
            icon = "🎵",
            description = "Spotify, Deezer, YouTube Music",
            serviceCount = 3,
            gradientStart = R.color.category_music_start,
            gradientEnd = R.color.category_music_end,
            serviceIds = listOf("spotify", "deezer", "youtube_premium")
        ),
        Category(
            id = "design",
            name = "Diseño",
            icon = "🎨",
            description = "Canva, Office, Autodesk",
            serviceCount = 5,
            gradientStart = R.color.category_design_start,
            gradientEnd = R.color.category_design_end,
            serviceIds = listOf("canva", "canva_year", "m365_year", "office365_year", "autodesk_year")
        ),
        Category(
            id = "ai",
            name = "IA",
            icon = "🤖",
            description = "ChatGPT y más",
            serviceCount = 1,
            gradientStart = R.color.category_ai_start,
            gradientEnd = R.color.category_ai_end,
            serviceIds = listOf("chatgpt")
        )
    )

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
    }

    private fun setupViews() {
        tvGreeting = findViewById(R.id.tvGreeting)

        currentUser = intent.getStringExtra("USER_FULLNAME") ?: ""
        tvGreeting.text = if (currentUser.isNotEmpty()) "Bienvenido, $currentUser" else "Bienvenido"

        // Configurar el botón de oferta
        findViewById<Button>(R.id.btnViewOffer).setOnClickListener {
            showToast("Próximamente: Ofertas especiales")
        }
    }

    private fun setupCategoriesRecyclerView() {
        rvCategories = findViewById(R.id.rvCategories)

        // Grid de 2 columnas con aspect ratio dinámico
        val gridLayoutManager = GridLayoutManager(this, 2)
        rvCategories.layoutManager = gridLayoutManager

        // Ajustar espaciado
        val spacingPx = (resources.displayMetrics.density * 8).toInt()
        rvCategories.addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))

        val adapter = CategoryCardAdapter(categories) { category ->
            onCategoryClick(category)
        }
        rvCategories.adapter = adapter
    }
    private fun setupPopularServices() {
        // Servicios por defecto
        val netflix = Service("netflix", "Netflix", "US$ 4,00 /mes", "Acceso inmediato", R.drawable.rounded_square_netflix)
        val spotify = Service("spotify", "Spotify", "US$ 3,50 /mes", "Acceso inmediato", R.drawable.rounded_square_spotify)
        val disney = Service("disney_plus_premium", "Disney+ Premium", "US$ 3,75 /mes", "Acceso inmediato", R.drawable.rounded_square_disney_premium)

        // Configurar clicks directamente
        findViewById<View>(R.id.card_popular_netflix).setOnClickListener {
            Log.d("HomeNative", "Click en Netflix")
            openPurchaseDialog(netflix)
        }

        findViewById<View>(R.id.card_popular_spotify).setOnClickListener {
            Log.d("HomeNative", "Click en Spotify")
            openPurchaseDialog(spotify)
        }

        findViewById<View>(R.id.card_popular_disney).setOnClickListener {
            Log.d("HomeNative", "Click en Disney")
            openPurchaseDialog(disney)
        }

        Log.d("HomeNative", "Listeners configurados para servicios populares")
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



    private fun setupBottomNavbar() {
        // Botón Home
        findViewById<View>(R.id.btn_home).setOnClickListener {
            showToast("Estás en el inicio")
        }

        // Botón Regalos
        findViewById<View>(R.id.btn_gift).setOnClickListener {
            showToast("Próximamente: Sección de Regalos")
        }

        // Botón Perfil
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        // Botón Cerrar Sesión
        findViewById<View>(R.id.btn_logout_nav).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun onCategoryClick(category: Category) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("CATEGORY_NAME", category.name)
        intent.putExtra("CATEGORY_ICON", category.icon)
        intent.putStringArrayListExtra("SERVICE_IDS", ArrayList(category.serviceIds))
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
}