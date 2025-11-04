package com.universidad.streamzone.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
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
        setContentView(R.layout.activity_home_native)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        rvServices = findViewById(R.id.rvServices)
        tvGreeting = findViewById(R.id.tvGreeting)

        currentUser = intent.getStringExtra("USER_FULLNAME") ?: ""
        tvGreeting.text = if (currentUser.isNotEmpty()) "Bienvenido, $currentUser" else "Bienvenido"

        // Configurar botón de cerrar sesión
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
        }

        val tvTitle: TextView? = findViewById(R.id.tvAppTitle)
        tvTitle?.post {
            val width = tvTitle.paint.measureText(tvTitle.text.toString())
            val colors = intArrayOf(
                getColorByName("brand_blue"),
                getColorByName("brand_purple"),
                getColorByName("brand_pink")
            )
            val textShader: Shader = LinearGradient(0f, 0f, width, tvTitle.textSize, colors, null, Shader.TileMode.CLAMP)
            tvTitle.paint.shader = textShader
            tvTitle.invalidate()
        }

        rvServices.layoutManager = GridLayoutManager(this, 2)
        val spacingPx = (resources.displayMetrics.density * 12).toInt()
        rvServices.addItemDecoration(GridSpacingItemDecoration(2, spacingPx, true))

        val adapter = ServiceAdapter(services) { service -> onReserve(service) }
        rvServices.adapter = adapter
    }

    private fun getColorByName(name: String): Int {
        val id = resources.getIdentifier(name, "color", packageName)
        return if (id != 0) ContextCompat.getColor(this, id) else Color.parseColor("#FFFFFF")
    }

    private fun cerrarSesion() {
        sharedPrefs.edit().apply {
            remove("logged_in_user_email")
            remove("logged_in_user_name")
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
        // En lugar de abrir directamente ReserveActivity, mostrar el diálogo de compra
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
}