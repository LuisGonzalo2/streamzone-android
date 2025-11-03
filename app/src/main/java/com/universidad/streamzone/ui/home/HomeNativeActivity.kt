package com.universidad.streamzone.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.ui.home.adapter.GridSpacingItemDecoration
import com.universidad.streamzone.ui.home.adapter.ServiceAdapter
import com.universidad.streamzone.ui.reserve.ReserveActivity

class HomeNativeActivity : AppCompatActivity() {

    private lateinit var rvServices: RecyclerView
    private lateinit var tvGreeting: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private var currentUser: String = ""

    private val services = listOf(
        // Mensuales
        Service("netflix", "Netflix", "US$ 4,00 /mes", "Acceso inmediato", R.drawable.rounded_square_netflix),
        Service("disney_plus_premium", "Disney+ Premium", "US$ 3,75 /mes", "Acceso inmediato", R.drawable.rounded_square_disney_premium),
        Service("disney_plus_standard", "Disney+ Standard", "US$ 3,25 /mes", "Acceso inmediato", R.drawable.rounded_square_disney_standard),
        Service("max", "Max", "US$ 3,00 /mes", "Acceso inmediato", R.drawable.rounded_square_max),
        Service("vix", "ViX", "US$ 2,50 /mes", "Acceso inmediato", R.drawable.rounded_square_vix),
        Service("prime", "Prime Video", "US$ 3,00 /mes", "Acceso inmediato", R.drawable.rounded_square_prime),
        Service("youtube_premium", "YouTube Premium", "US$ 3,35 /mes", "Acceso inmediato", R.drawable.rounded_square_yt),
        Service("paramount", "Paramount+", "US$ 2,75 /mes", "Acceso inmediato", R.drawable.rounded_square_paramount),
        Service("chatgpt", "ChatGPT", "US$ 4,00 /mes", "Acceso inmediato", R.drawable.rounded_square_chatgpt),
        Service("crunchyroll", "Crunchyroll", "US$ 2,50 /mes", "Acceso inmediato", R.drawable.rounded_square_crunchyroll),
        Service("spotify", "Spotify", "US$ 3,50 /mes", "Acceso inmediato", R.drawable.rounded_square_spotify),
        Service("deezer", "Deezer", "US$ 3,00 /mes", "Acceso inmediato", R.drawable.rounded_square_deezer),
        Service("appletv", "Apple TV+", "US$ 3,50 /mes", "Acceso inmediato", R.drawable.rounded_square_appletv),
        Service("canva", "Canva Pro", "US$ 2,00 /mes", "Acceso inmediato", R.drawable.rounded_square_canva),

        // Licencias anuales
        Service("canva_year", "Canva Pro (1 año)", "US$ 17,50 /año", "Licencia anual", R.drawable.rounded_square_canva),
        Service("m365_year", "Microsoft 365 (M365)", "US$ 15,00 /año", "Licencia anual", R.drawable.rounded_square_m365),
        Service("autodesk_year", "Autodesk (AD)", "US$ 12,50 /año", "Licencia anual", R.drawable.rounded_square_autodesk),
        Service("office365_year", "Office 365 (O365)", "US$ 15,00 /año", "Licencia anual", R.drawable.rounded_square_office365)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usar el layout base con navbar inferior
        setContentView(R.layout.activity_base)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        // Inflar el contenido específico del home
        val contentContainer = findViewById<FrameLayout>(R.id.content_container)
        LayoutInflater.from(this).inflate(R.layout.activity_home_native, contentContainer, true)

        setupViews()
        setupRecyclerView()
        setupBottomNavbar() // Configurar navbar inferior
    }

    private fun setupViews() {
        rvServices = findViewById(R.id.rvServices)
        tvGreeting = findViewById(R.id.tvGreeting)

        currentUser = intent.getStringExtra("USER_FULLNAME") ?: ""
        tvGreeting.text = if (currentUser.isNotEmpty()) "Bienvenido, $currentUser" else "Bienvenido"

        // Ocultar el botón de cerrar sesión antiguo
        findViewById<Button>(R.id.btnCerrarSesion).visibility = View.GONE

        val tvTitle: TextView? = findViewById(R.id.tvAppTitle)
        tvTitle?.post {
            val width = tvTitle.paint.measureText(tvTitle.text.toString())
            val colors = intArrayOf(
                ContextCompat.getColor(this, R.color.brand_blue),
                ContextCompat.getColor(this, R.color.brand_purple),
                ContextCompat.getColor(this, R.color.brand_pink)
            )
            val textShader: Shader = LinearGradient(0f, 0f, width, tvTitle.textSize, colors, null, Shader.TileMode.CLAMP)
            tvTitle.paint.shader = textShader
            tvTitle.invalidate()
        }
    }

    private fun setupRecyclerView() {
        rvServices.layoutManager = GridLayoutManager(this, 2)
        val spacingPx = (resources.displayMetrics.density * 12).toInt()
        rvServices.addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))

        val adapter = ServiceAdapter(services) { service -> onReserve(service) }
        rvServices.adapter = adapter
    }

    // Configurar navbar inferior
    private fun setupBottomNavbar() {
        // Botón Home
        findViewById<View>(R.id.btn_home).setOnClickListener {
            // Ya estamos en home
            showToast("Estás en el inicio")
        }

        // Botón Regalos
        findViewById<View>(R.id.btn_gift).setOnClickListener {
            showToast("Próximamente: Sección de Regalos")
        }

        // Botón Perfil - ABRE LA NUEVA ACTIVIDAD
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        // Botón Cerrar Sesión
        findViewById<View>(R.id.btn_logout_nav).setOnClickListener {
            cerrarSesion()
        }
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

    private fun onReserve(service: Service) {
        Log.d("HomeNativeActivity", "Reserva iniciada: ${service.id} - ${service.title}")
        val intent = Intent(this, ReserveActivity::class.java)
        intent.putExtra("SERVICE_ID", service.id)
        intent.putExtra("SERVICE_TITLE", service.title)
        intent.putExtra("SERVICE_PRICE", service.price)
        intent.putExtra("SERVICE_DESC", service.desc)
        intent.putExtra("USER_FULLNAME", currentUser)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}