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
import com.google.firebase.firestore.ListenerRegistration
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.remote.FirebaseService
import com.universidad.streamzone.ui.components.NavbarManager
import com.universidad.streamzone.ui.profile.adapter.PurchaseCardAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PurchaseHistoryActivity : AppCompatActivity() {

    private lateinit var navbarManager: NavbarManager
    private lateinit var rvPurchases: RecyclerView
    private lateinit var emptyState: LinearLayout
    private var purchasesListener: ListenerRegistration? = null

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

        Log.d(TAG, "🔄 Iniciando listener de compras para: $userEmail")

        // Limpiar listener anterior si existe
        purchasesListener?.remove()

        // Escuchar compras del usuario en tiempo real desde Firebase
        purchasesListener = FirebaseService.escucharComprasPorUsuario(userEmail) { purchasesFromFirebase ->
            Log.d(TAG, "📦 Compras recibidas de Firebase: ${purchasesFromFirebase.size}")

            purchasesFromFirebase.forEachIndexed { index, purchase ->
                Log.d(TAG, "  [$index] ${purchase.serviceName} - ${purchase.status} - Credenciales: ${if (purchase.email != null) "✅" else "❌"}")
            }

            lifecycleScope.launch {
                try {
                    // Sincronizar a Room en segundo plano
                    val dao = AppDatabase.getInstance(this@PurchaseHistoryActivity).purchaseDao()

                    purchasesFromFirebase.forEach { firebasePurchase ->
                        try {
                            val existingPurchase = dao.getAll()
                                .find { it.firebaseId == firebasePurchase.firebaseId }

                            if (existingPurchase != null) {
                                val updated = existingPurchase.copy(
                                    email = firebasePurchase.email,
                                    password = firebasePurchase.password,
                                    status = firebasePurchase.status,
                                    sincronizado = true
                                )
                                dao.update(updated)
                            } else {
                                dao.insertar(firebasePurchase)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al sincronizar compra: ${firebasePurchase.serviceName}", e)
                        }
                    }

                    // Mostrar compras directamente desde Firebase
                    runOnUiThread {
                        if (purchasesFromFirebase.isEmpty()) {
                            Log.d(TAG, "❌ No hay compras para mostrar")
                            showEmptyState()
                        } else {
                            Log.d(TAG, "✅ Mostrando ${purchasesFromFirebase.size} compras")
                            showPurchases(purchasesFromFirebase)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al procesar compras", e)
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

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar listener de Firebase
        purchasesListener?.remove()
        Log.d(TAG, "Listener de compras removido")
    }
}