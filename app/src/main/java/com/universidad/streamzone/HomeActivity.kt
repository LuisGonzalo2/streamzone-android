package com.universidad.streamzone

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cargar layout que contiene el WebView
        setContentView(R.layout.activity_home_webview)

        webView = findViewById(R.id.webViewHome)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true

        // Añadir interfaz JS para comunicación web -> Android
        webView.addJavascriptInterface(AndroidBridge(), "Android") //jere: interfaz para recibir eventos desde la web

        // Obtener nombre de usuario enviado por LoginActivity
        val userFullName = intent.getStringExtra("USER_FULLNAME") ?: ""

        // Construir URL local con query param user (encode)
        val encodedUser = try {
            java.net.URLEncoder.encode(userFullName, "UTF-8")
        } catch (_: Exception) {
            ""
        }

        val url = "file:///android_asset/www/index.html" + if (encodedUser.isNotEmpty()) "?user=$encodedUser" else ""

        // Cargar la página local
        webView.loadUrl(url)

        // Manejo de botón atrás con OnBackPressedDispatcher (reemplaza onBackPressed obsoleto)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (this@HomeActivity::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Si no hay historial, dejar que el sistema maneje el back (salir de la activity)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // Clase que expone métodos accesibles desde JavaScript
    inner class AndroidBridge {
        @Suppress("unused") //jere: usado desde WebView via JavascriptInterface
        @JavascriptInterface
        fun onReserve(serviceJson: String) {
            //jere: aquí se recibe la acción de reservar desde la web
            // Puedes implementar la lógica nativa (ej. abrir un Activity de detalles, iniciar compra, etc.)
            Log.d("HomeActivity", "Reserva recibida desde Web: $serviceJson")
            // Ejemplo: abrir un intent para enviar a ListaUsuariosActivity (placeholder)
            // val intent = Intent(this@HomeActivity, CompraActivity::class.java)
            // intent.putExtra("SERVICE_JSON", serviceJson)
            // startActivity(intent)
        }
    }

}