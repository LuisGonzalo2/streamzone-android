package com.universidad.streamzone.ui.category

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.ui.home.HomeNativeActivity
import com.universidad.streamzone.ui.home.PurchaseDialogFragment
import com.universidad.streamzone.ui.home.UserProfileActivity
import com.universidad.streamzone.ui.home.adapter.GridSpacingItemDecoration
import com.universidad.streamzone.ui.home.adapter.ServiceAdapter

class CategoryActivity : AppCompatActivity() {

    private lateinit var rvServices: RecyclerView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvCategorySubtitle: TextView
    private lateinit var tvCategoryIcon: TextView
    private lateinit var btnBack: MaterialButton

    // Lista completa de todos los servicios
    private val allServices = listOf(
        // Streaming
        Service("netflix", "Netflix", "US$ 4,00 /mes", "Acceso inmediato", R.drawable.rounded_square_netflix),
        Service("disney_plus_premium", "Disney+ Premium", "US$ 3,75 /mes", "Acceso inmediato", R.drawable.rounded_square_disney_premium),
        Service("disney_plus_standard", "Disney+ Standard", "US$ 3,25 /mes", "Acceso inmediato", R.drawable.rounded_square_disney_standard),
        Service("max", "Max", "US$ 3,00 /mes", "Acceso inmediato", R.drawable.rounded_square_max),
        Service("vix", "ViX", "US$ 2,50 /mes", "Acceso inmediato", R.drawable.rounded_square_vix),
        Service("prime", "Prime Video", "US$ 3,00 /mes", "Acceso inmediato", R.drawable.rounded_square_prime),
        Service("paramount", "Paramount+", "US$ 2,75 /mes", "Acceso inmediato", R.drawable.rounded_square_paramount),
        Service("appletv", "Apple TV+", "US$ 3,50 /mes", "Acceso inmediato", R.drawable.rounded_square_appletv),
        Service("crunchyroll", "Crunchyroll", "US$ 2,50 /mes", "Acceso inmediato", R.drawable.rounded_square_crunchyroll),

        // Música
        Service("spotify", "Spotify", "US$ 3,50 /mes", "Acceso inmediato", R.drawable.rounded_square_spotify),
        Service("deezer", "Deezer", "US$ 3,00 /mes", "Acceso inmediato", R.drawable.rounded_square_deezer),
        Service("youtube_premium", "YouTube Premium", "US$ 3,35 /mes", "Acceso inmediato", R.drawable.rounded_square_yt),

        // Diseño
        Service("canva", "Canva Pro", "US$ 2,00 /mes", "Acceso inmediato", R.drawable.rounded_square_canva),
        Service("canva_year", "Canva Pro (1 año)", "US$ 17,50 /año", "Licencia anual", R.drawable.rounded_square_canva),
        Service("m365_year", "Microsoft 365 (M365)", "US$ 15,00 /año", "Licencia anual", R.drawable.rounded_square_m365),
        Service("office365_year", "Office 365 (O365)", "US$ 15,00 /año", "Licencia anual", R.drawable.rounded_square_office365),
        Service("autodesk_year", "Autodesk (AD)", "US$ 12,50 /año", "Licencia anual", R.drawable.rounded_square_autodesk),

        // IA
        Service("chatgpt", "ChatGPT", "US$ 4,00 /mes", "Acceso inmediato", R.drawable.rounded_square_chatgpt)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usar el layout base con navbar inferior
        setContentView(R.layout.activity_base)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Inflar el contenido específico de CategoryActivity
        val contentContainer = findViewById<FrameLayout>(R.id.content_container)
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
        val serviceIds = intent.getStringArrayListExtra("SERVICE_IDS") ?: arrayListOf()

        // Actualizar UI
        tvCategoryTitle.text = categoryName
        tvCategoryIcon.text = categoryIcon
        tvCategorySubtitle.text = "${serviceIds.size} servicios disponibles"
    }

    private fun setupRecyclerView() {
        // Filtrar servicios según los IDs recibidos
        val serviceIds = intent.getStringArrayListExtra("SERVICE_IDS") ?: arrayListOf()
        val filteredServices = allServices.filter { service ->
            serviceIds.contains(service.id)
        }

        // Configurar RecyclerView
        rvServices.layoutManager = GridLayoutManager(this, 2)
        val spacingPx = (resources.displayMetrics.density * 12).toInt()
        rvServices.addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))

        val adapter = ServiceAdapter(filteredServices) { service ->
            onServiceClick(service)
        }
        rvServices.adapter = adapter
    }

    private fun setupBottomNavbar() {
        // Botón Home
        findViewById<View>(R.id.btn_home).setOnClickListener {
            finish() // Volver al home
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