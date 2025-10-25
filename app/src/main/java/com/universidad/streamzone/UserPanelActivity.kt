package com.universidad.streamzone

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UserPanelActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences

    // Declarar las vistas que corresponden a tu layout
    private lateinit var tvUserName: TextView
    private lateinit var tvUserPhone: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvMemberSince: TextView

    // Botones de la barra superior
    private lateinit var btnNotifications: TextView
    private lateinit var btnFavorites: TextView
    private lateinit var btnSearch: TextView
    private lateinit var btnProfile: TextView
    private lateinit var btnHome: TextView
    private lateinit var btnGift: TextView
    private lateinit var btnLock: TextView
    private lateinit var btnSettings: TextView
    private lateinit var btnChat: TextView
    private lateinit var btnInfo: TextView

    // Botones principales
    private lateinit var btnUpdate: MaterialButton
    private lateinit var btnCatalog: MaterialButton
    private lateinit var btnHelp: MaterialButton
    private lateinit var btnTheme: TextView

    // Estado del tema
    private var isDarkTheme = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        // Cargar estado del tema
        isDarkTheme = sharedPrefs.getBoolean("is_dark_theme", true)

        // Aplicar tema antes de setContentView
        applyTheme()

        setContentView(R.layout.activity_panel_usuario)

        initViews()
        loadUserData()
        setupClickListeners()
        updateThemeButton()
    }

    private fun initViews() {
        // Inicializar TextViews de información del usuario
        tvUserName = findViewById(R.id.tv_user_name)
        tvUserPhone = findViewById(R.id.tv_user_phone)
        tvUserEmail = findViewById(R.id.tv_user_email)
        tvMemberSince = findViewById(R.id.tv_member_since)

        // Botones de la barra superior
        btnNotifications = findViewById(R.id.btn_notifications)
        btnFavorites = findViewById(R.id.btn_favorites)
        btnSearch = findViewById(R.id.btn_search)
        btnProfile = findViewById(R.id.btn_profile)
        btnHome = findViewById(R.id.btn_home)
        btnGift = findViewById(R.id.btn_gift)
        btnLock = findViewById(R.id.btn_lock)
        btnSettings = findViewById(R.id.btn_settings)
        btnChat = findViewById(R.id.btn_chat)
        btnInfo = findViewById(R.id.btn_info)

        // Botones principales
        btnUpdate = findViewById(R.id.btn_update)
        btnCatalog = findViewById(R.id.btn_catalog)
        btnHelp = findViewById(R.id.btn_help)
        btnTheme = findViewById(R.id.btn_theme)
    }

    private fun loadUserData() {
        // Obtener datos del usuario desde SharedPreferences
        val userName = sharedPrefs.getString("user_name", "Usuario")
        val userEmail = sharedPrefs.getString("user_email", "email@ejemplo.com")
        val userPhone = sharedPrefs.getString("user_phone", "+593000000000")
        val memberSince = sharedPrefs.getString("member_since", "25/10/2025")

        // Actualizar las vistas con los datos del usuario
        tvUserName.text = userName
        tvUserEmail.text = userEmail
        tvUserPhone.text = userPhone
        tvMemberSince.text = memberSince

        // También actualizar el título de la app si es necesario
        val appName = findViewById<TextView>(R.id.tv_app_name)
        appName.text = "StreamZone - $userName"
    }

    private fun setupClickListeners() {
        // Botón Actualizar
        btnUpdate.setOnClickListener {
            updateUserInfo()
        }

        // Botón Ver Catálogo
        btnCatalog.setOnClickListener {
            openCatalog()
        }

        // Botón Chat de Ayuda
        btnHelp.setOnClickListener {
            openHelpChat()
        }

        // Botón Cambiar Tema
        btnTheme.setOnClickListener {
            toggleTheme()
        }

        // Botones de la barra superior
        setupTopBarListeners()
    }

    private fun setupTopBarListeners() {
        btnNotifications.setOnClickListener {
            showNotifications()
        }

        btnFavorites.setOnClickListener {
            showFavorites()
        }

        btnSearch.setOnClickListener {
            showSearchDialog()
        }

        btnProfile.setOnClickListener {
            // Ya estamos en el perfil
            Toast.makeText(this, "✅ Ya estás en tu perfil", Toast.LENGTH_SHORT).show()
        }

        btnHome.setOnClickListener {
            // Recargar la actividad principal
            Toast.makeText(this, "🔄 Recargando...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, UserPanelActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnGift.setOnClickListener {
            showGiftPromotions()
        }

        btnLock.setOnClickListener {
            showSecurityInfo()
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        btnChat.setOnClickListener {
            openHelpChat()
        }

        btnInfo.setOnClickListener {
            showAppInfo()
        }
    }

    // ========== FUNCIONES IMPLEMENTADAS ==========

    private fun toggleTheme() {
        isDarkTheme = !isDarkTheme

        // Guardar preferencia
        sharedPrefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()

        // Recargar actividad para aplicar el tema
        val intent = Intent(this, UserPanelActivity::class.java)
        startActivity(intent)
        finish()

        // Animación de transición
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun applyTheme() {
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun updateThemeButton() {
        btnTheme.text = if (isDarkTheme) "☀️" else "🌙"
    }

    private fun showNotifications() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🔔 Notificaciones")
            .setMessage("No tienes notificaciones nuevas.\n\nNotificaciones activas:\n• Actualizaciones del sistema\n• Promociones especiales")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Configurar") { _, _ ->
                Toast.makeText(this, "Configuración de notificaciones - Próximamente", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showFavorites() {
        MaterialAlertDialogBuilder(this)
            .setTitle("❤️ Tus Favoritos")
            .setMessage("Tus servicios favoritos aparecerán aquí.\n\nPor el momento no tienes favoritos guardados.")
            .setPositiveButton("Explorar") { _, _ ->
                openCatalog()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showSearchDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🔍 Buscar en StreamZone")
            .setMessage("Función de búsqueda avanzada.\n\nCaracterísticas disponibles:\n• Búsqueda por categoría\n• Filtros por precio\n• Servicios populares")
            .setPositiveButton("Buscar") { _, _ ->
                Toast.makeText(this, "Buscando servicios disponibles...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showGiftPromotions() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🎁 Promociones y Regalos")
            .setMessage("¡Promociones especiales para ti!\n\n• 20% de descuento en tu primera compra\n• Programa de referidos\n• Puntos por compras frecuentes")
            .setPositiveButton("Aplicar Descuento") { _, _ ->
                Toast.makeText(this, "Descuento aplicado a tu próxima compra", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showSecurityInfo() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🔐 Seguridad Garantizada")
            .setMessage("Tu seguridad es nuestra prioridad:\n\n• Encriptación de datos\n• Pagos seguros\n• Protección antifraude\n• Soporte 24/7")
            .setPositiveButton("Ver Certificados") { _, _ ->
                Toast.makeText(this, "Certificados de seguridad verificados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showSettingsDialog() {
        val settingsOptions = arrayOf(
            "🔧 Configuración de cuenta",
            "🔔 Preferencias de notificaciones",
            "🌎 Idioma y región",
            "💳 Métodos de pago",
            "📱 Preferencias de la app"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("⚙️ Configuración")
            .setItems(settingsOptions) { _, which ->
                when (which) {
                    0 -> updateUserInfo()
                    1 -> showNotifications()
                    2 -> showLanguageSettings()
                    3 -> showPaymentMethods()
                    4 -> showAppPreferences()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun updateUserInfo() {
        MaterialAlertDialogBuilder(this)
            .setTitle("👤 Actualizar Información")
            .setMessage("¿Qué información deseas actualizar?\n\nPuedes modificar:\n• Nombre y apellido\n• Número de WhatsApp\n• Correo electrónico\n• Contraseña")
            .setPositiveButton("Editar Perfil") { _, _ ->
                Toast.makeText(this, "Redirigiendo a edición de perfil...", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cambiar Contraseña") { _, _ ->
                showChangePasswordDialog()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openCatalog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("📂 Catálogo de Servicios")
            .setMessage("Explora nuestros servicios:\n\n• Streaming Premium\n• Servicios de IPTV\n• Plataformas deportivas\n• Contenido exclusivo")
            .setPositiveButton("Ver Servicios") { _, _ ->
                Toast.makeText(this, "Cargando catálogo completo...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun openHelpChat() {
        MaterialAlertDialogBuilder(this)
            .setTitle("💬 Soporte en Vivo")
            .setMessage("¿En qué podemos ayudarte?\n\nSoporte disponible:\n• Problemas técnicos\n• Consultas de facturación\n• Asistencia con servicios\n• Reportar problemas")
            .setPositiveButton("Iniciar Chat") { _, _ ->
                simulateChatSupport()
            }
            .setNegativeButton("Más Tarde", null)
            .show()
    }

    private fun showAppInfo() {
        MaterialAlertDialogBuilder(this)
            .setTitle("ℹ️ Información de StreamZone")
            .setMessage("StreamZone v1.0\n\n• Versión: 1.0.0\n• Compilación: 2025.01\n• Desarrollado por: Universidad StreamZone\n• Soporte: soporte@streamzone.com")
            .setPositiveButton("Términos de Uso") { _, _ ->
                showTermsAndConditions()
            }
            .setNeutralButton("Política de Privacidad") { _, _ ->
                showPrivacyPolicy()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // ========== FUNCIONES AUXILIARES ==========

    private fun showLanguageSettings() {
        val languages = arrayOf("Español", "English", "Português", "Français")
        MaterialAlertDialogBuilder(this)
            .setTitle("🌎 Seleccionar Idioma")
            .setItems(languages) { _, which ->
                val selectedLanguage = languages[which]
                Toast.makeText(this, "Idioma cambiado a: $selectedLanguage", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showPaymentMethods() {
        MaterialAlertDialogBuilder(this)
            .setTitle("💳 Métodos de Pago")
            .setMessage("Métodos disponibles:\n\n• Tarjeta de crédito/débito\n• PayPal\n• Transferencia bancaria\n• Cryptomonedas")
            .setPositiveButton("Agregar Método") { _, _ ->
                Toast.makeText(this, "Redirigiendo a gestión de pagos...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showAppPreferences() {
        MaterialAlertDialogBuilder(this)
            .setTitle("📱 Preferencias de la App")
            .setMessage("Configura tu experiencia:\n\n• Modo oscuro/claro\n• Notificaciones push\n• Auto-login\n• Descargas automáticas")
            .setPositiveButton("Configurar") { _, _ ->
                Toast.makeText(this, "Abriendo configuración avanzada...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🔐 Cambiar Contraseña")
            .setMessage("Se enviará un enlace de restablecimiento a tu correo electrónico.")
            .setPositiveButton("Enviar Enlace") { _, _ ->
                Toast.makeText(this, "✅ Enlace enviado a tu correo", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showTermsAndConditions() {
        MaterialAlertDialogBuilder(this)
            .setTitle("📄 Términos de Uso")
            .setMessage("Al usar StreamZone aceptas:\n\n• Uso personal no comercial\n• No redistribución de contenido\n• Respeto a derechos de autor\n• Políticas de privacidad")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🛡️ Política de Privacidad")
            .setMessage("Protegemos tus datos:\n\n• Encriptación de información\n• No compartimos datos con terceros\n• Puedes eliminar tu cuenta cuando quieras\n• Cumplimiento con GDPR")
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun simulateChatSupport() {
        Toast.makeText(this, "🔄 Conectando con soporte...", Toast.LENGTH_SHORT).show()

        // Simular conexión después de 2 segundos
        btnHelp.postDelayed({
            MaterialAlertDialogBuilder(this)
                .setTitle("💬 Chat de Soporte")
                .setMessage("Agente: ¡Hola! ¿En qué puedo ayudarte hoy?\n\nTú: [Escribe tu mensaje aquí]")
                .setPositiveButton("Enviar Mensaje") { _, _ ->
                    Toast.makeText(this, "Mensaje enviado al soporte", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cerrar Chat", null)
                .show()
        }, 2000)
    }

    // ========== MÉTODOS EXISTENTES ==========

    private fun logout() {
        // Limpiar datos de sesión
        sharedPrefs.edit().apply {
            remove("user_name")
            remove("user_email")
            remove("user_phone")
            remove("member_since")
            putInt("login_attempts", 0)
            apply()
        }

        // Regresar al login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        showExitConfirmation()
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }
}