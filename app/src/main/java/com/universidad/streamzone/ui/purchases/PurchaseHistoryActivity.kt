package com.universidad.streamzone.ui.purchases

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.repository.PurchaseRepository
import com.universidad.streamzone.ui.components.NavbarManager
import com.universidad.streamzone.ui.profile.adapter.PurchaseCardAdapter
import com.universidad.streamzone.util.toPurchaseEntityList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PurchaseHistoryActivity : AppCompatActivity() {

    private lateinit var navbarManager: NavbarManager
    private lateinit var rvPurchases: RecyclerView
    private lateinit var emptyState: LinearLayout

    // Firebase Repository
    private val purchaseRepository = PurchaseRepository()

    companion object {
        private const val TAG = "PurchaseHistory"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_history)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Aplicar padding superior al contenedor principal
        val mainContainer = findViewById<View>(R.id.purchase_history_main_container)
        mainContainer?.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(
                view.paddingLeft,
                systemBars.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // Configurar navbar
        navbarManager = NavbarManager(this, NavbarManager.Screen.PURCHASES)

        initViews()
        loadPurchases()
    }

    private fun initViews() {
        rvPurchases = findViewById(R.id.rv_all_purchases)
        emptyState = findViewById(R.id.empty_state)
    }

    private fun loadPurchases() {
        val sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) {
            Log.d(TAG, "❌ No hay email de usuario")
            showEmptyState()
            return
        }

        Log.d(TAG, "🔄 Cargando compras para: $userEmail")

        // Escuchar compras del usuario en tiempo real desde Firebase usando Flow
        lifecycleScope.launch {
            try {
                purchaseRepository.getPurchasesByUser(userEmail).collectLatest { firebasePurchases ->
                    Log.d(TAG, "📦 Compras recibidas de Firebase: ${firebasePurchases.size}")

                    firebasePurchases.forEachIndexed { index, purchase ->
                        Log.d(TAG, "  [$index] ${purchase.serviceName} - ${purchase.status} - Credenciales: ${if (purchase.credentials != null) "✅" else "❌"}")
                    }

                    // Convertir a PurchaseEntity para la UI
                    val purchaseEntities = firebasePurchases.toPurchaseEntityList()

                    // Mostrar compras directamente desde Firebase
                    runOnUiThread {
                        if (purchaseEntities.isEmpty()) {
                            Log.d(TAG, "❌ No hay compras para mostrar")
                            showEmptyState()
                        } else {
                            Log.d(TAG, "✅ Mostrando ${purchaseEntities.size} compras")
                            showPurchases(purchaseEntities)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar compras", e)
                runOnUiThread {
                    showEmptyState()
                    Toast.makeText(
                        this@PurchaseHistoryActivity,
                        "Error al cargar compras: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showPurchases(purchases: List<com.universidad.streamzone.data.model.PurchaseEntity>) {
        rvPurchases.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        rvPurchases.layoutManager = LinearLayoutManager(this)
        val adapter = PurchaseCardAdapter(purchases) { purchase ->
            Toast.makeText(this, "Compra: ${purchase.serviceName}", Toast.LENGTH_SHORT).show()
        }
        rvPurchases.adapter = adapter
    }

    private fun showEmptyState() {
        rvPurchases.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}