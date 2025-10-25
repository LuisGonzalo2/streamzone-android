package com.universidad.streamzone

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.adapter.ServiceAdapter
import com.universidad.streamzone.data.ServiceData
import com.universidad.streamzone.model.ServiceItem
import com.universidad.streamzone.sync.SyncService

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences

    // Views Hero
    private lateinit var txtHeroTitle: TextView
    private lateinit var txtHeroGreeting: TextView
    private lateinit var btnViewCatalog: Button
    private lateinit var btnLoginHero: Button
    private lateinit var btnViewCombos: Button

    // RecyclerView
    private lateinit var rvServices: RecyclerView
    private lateinit var serviceAdapter: ServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        initViews()
        setupRecyclerView()
        setupButtons()
        setupHeroSection()
        loadServices()

        // Sincronizar datos pendientes si hay internet
        if (isNetworkAvailable()) {
            SyncService.sincronizarUsuariosPendientes(this) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "✅ Datos sincronizados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun initViews() {
        // Hero
        txtHeroTitle = findViewById(R.id.txtHeroTitle)
        txtHeroGreeting = findViewById(R.id.txtHeroGreeting)
        btnViewCatalog = findViewById(R.id.btnViewCatalog)
        btnLoginHero = findViewById(R.id.btnLoginHero)
        btnViewCombos = findViewById(R.id.btnViewCombos)

        // RecyclerView
        rvServices = findViewById(R.id.rvServices)
    }

    private fun setupRecyclerView() {
        // GridLayoutManager: 2 columnas en portrait, 4 en landscape
        val spanCount = if (resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE) 4 else 2

        val gridLayoutManager = GridLayoutManager(this, spanCount)
        rvServices.layoutManager = gridLayoutManager

        // Adapter
        serviceAdapter = ServiceAdapter { service ->
            onServiceClicked(service)
        }

        rvServices.adapter = serviceAdapter
    }

    private fun setupButtons() {
        btnViewCatalog.setOnClickListener {
            // Scroll suave al catálogo
            findViewById<RecyclerView>(R.id.rvServices).smoothScrollToPosition(0)
        }

        btnLoginHero.setOnClickListener {
            // Navegar al login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnViewCombos.setOnClickListener {
            Toast.makeText(this, "📦 Ver Combos - Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHeroSection() {
        // Aplicar gradiente al título "StreamZone"
        applyGradientToTitle()

        // Verificar si el usuario está logueado
        val userName = sharedPrefs.getString("logged_in_user_name", null)
        val isLoggedIn = !userName.isNullOrEmpty()

        if (isLoggedIn) {
            // Usuario logueado: mostrar saludo y botón "Ver Catálogo"
            txtHeroGreeting.visibility = TextView.VISIBLE
            txtHeroGreeting.text = getString(R.string.hero_greeting, userName)

            btnViewCatalog.visibility = Button.VISIBLE
            btnLoginHero.visibility = Button.GONE
        } else {
            // Usuario NO logueado: ocultar saludo y mostrar botón "Iniciar Sesión"
            txtHeroGreeting.visibility = TextView.GONE

            btnViewCatalog.visibility = Button.GONE
            btnLoginHero.visibility = Button.VISIBLE
        }
    }

    private fun applyGradientToTitle() {
        // Crear gradiente para el título "StreamZone"
        // Gradiente: blue-600 → purple-600 → pink-600
        val title = txtHeroTitle.text.toString()
        val spannable = SpannableString(title)

        // Colores del gradiente
        val colors = intArrayOf(
            Color.parseColor("#2563EB"), // blue-600
            Color.parseColor("#9333EA"), // purple-600
            Color.parseColor("#DB2777")  // pink-600
        )

        // Aplicar gradiente
        try {
            // Para API 33 (Tiramisu) y superior
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val colors = intArrayOf(
                    Color.parseColor("#A855F7"), // inicio del gradiente (púrpura)
                    Color.parseColor("#9333EA")  // final del gradiente (violeta más oscuro)
                )

                // Crea el texto con formato Spannable (si aún no lo tienes)
                val spannable = SpannableString(title)

                // Crea el gradiente lineal basado en el ancho del texto
                val shader = LinearGradient(
                    0f, 0f,
                    txtHeroTitle.paint.measureText(title), 0f, // longitud horizontal
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )

                // Aplica el gradiente al TextView
                txtHeroTitle.paint.shader = shader
                txtHeroTitle.text = spannable
            } else {
                // Fallback: usar color sólido
                txtHeroTitle.setTextColor(Color.parseColor("#A855F7"))
            }
        } catch (e: Exception) {
            // Fallback en caso de error
            txtHeroTitle.setTextColor(Color.parseColor("#A855F7"))
        }
    }

    private fun loadServices() {
        // Cargar servicios desde ServiceData
        val services = ServiceData.getServices()
        serviceAdapter.submitList(services)
    }

    private fun onServiceClicked(service: ServiceItem) {
        // Mostrar toast con el servicio seleccionado
        val message = getString(R.string.toast_service_selected, service.title)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // TODO: Aquí irías a una pantalla de detalles o compra
        // val intent = Intent(this, ServiceDetailActivity::class.java)
        // intent.putExtra("SERVICE_ID", service.id)
        // startActivity(intent)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    override fun onResume() {
        super.onResume()
        // Recargar el hero por si el usuario se logueó/deslogueó
        setupHeroSection()
    }
}