package com.universidad.streamzone.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.models.PurchaseCredentials
import com.universidad.streamzone.data.firebase.repository.PurchaseRepository
import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.ui.admin.adapter.AdminPurchaseAdapter
import com.universidad.streamzone.util.PermissionManager
import com.universidad.streamzone.util.toPurchaseEntityList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PendingPurchasesActivity : BaseAdminActivity() {

    // Requiere permiso de gestionar compras
    override val requiredPermission: String = PermissionManager.MANAGE_PURCHASES

    // Firebase Repository
    private val purchaseRepository = PurchaseRepository()

    private lateinit var btnBack: ImageButton
    private lateinit var tvPendingCount: TextView
    private lateinit var btnFilterPending: Button
    private lateinit var btnFilterActive: Button
    private lateinit var btnFilterAll: Button
    private lateinit var rvPurchases: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var purchaseAdapter: AdminPurchaseAdapter

    private var currentFilter = "pending"

    companion object {
        private const val TAG = "PendingPurchases"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_purchases)

        // BaseAdminActivity ya valida permisos
        // Solo inicializamos si se llama a onPermissionGranted
    }

    override fun onPermissionGranted() {
        initViews()
        setupRecyclerView()
        setupFilterButtons()
        loadPurchases()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        btnFilterPending = findViewById(R.id.btnFilterPending)
        btnFilterActive = findViewById(R.id.btnFilterActive)
        btnFilterAll = findViewById(R.id.btnFilterAll)
        rvPurchases = findViewById(R.id.rvPurchases)
        llEmptyState = findViewById(R.id.llEmptyState)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        purchaseAdapter = AdminPurchaseAdapter(
            purchases = emptyList(),
            onAssignCredentials = { purchase ->
                showAssignCredentialsDialog(purchase)
            }
        )

        rvPurchases.layoutManager = LinearLayoutManager(this)
        rvPurchases.adapter = purchaseAdapter
    }

    private fun setupFilterButtons() {
        btnFilterPending.setOnClickListener {
            currentFilter = "pending"
            updateFilterButtons()
            loadPurchases()
        }

        btnFilterActive.setOnClickListener {
            currentFilter = "active"
            updateFilterButtons()
            loadPurchases()
        }

        btnFilterAll.setOnClickListener {
            currentFilter = "all"
            updateFilterButtons()
            loadPurchases()
        }
    }

    private fun updateFilterButtons() {
        // Reset todos
        btnFilterPending.setBackgroundColor(getColor(R.color.bg_card))
        btnFilterActive.setBackgroundColor(getColor(R.color.bg_card))
        btnFilterAll.setBackgroundColor(getColor(R.color.bg_card))

        // Activar el seleccionado
        when (currentFilter) {
            "pending" -> btnFilterPending.setBackgroundColor(0xFFF59E0B.toInt())
            "active" -> btnFilterActive.setBackgroundColor(0xFF10B981.toInt())
            "all" -> btnFilterAll.setBackgroundColor(0xFF6366F1.toInt())
        }
    }

    private fun loadPurchases() {
        Log.d(TAG, "🔄 Iniciando listener de compras en tiempo real desde Firebase")

        lifecycleScope.launch {
            try {
                // Observar compras en tiempo real con Flow
                purchaseRepository.getAll().collectLatest { firebasePurchases ->
                    Log.d(TAG, "📦 Compras recibidas de Firebase: ${firebasePurchases.size}")

                    // Convertir a PurchaseEntity para la UI
                    val allPurchases = firebasePurchases.toPurchaseEntityList()

                    // Filtrar las compras según el filtro actual
                    val filteredPurchases = when (currentFilter) {
                        "pending" -> allPurchases.filter { it.status == "pending" }
                        "active" -> allPurchases.filter { it.status == "active" }
                        else -> allPurchases
                    }

                    // Contar pendientes
                    val pendingCount = allPurchases.count { it.status == "pending" }

                    Log.d(TAG, "📊 Total: ${allPurchases.size}, Filtradas: ${filteredPurchases.size}, Pendientes: $pendingCount")

                    runOnUiThread {
                        tvPendingCount.text = "$pendingCount compras pendientes"

                        if (filteredPurchases.isEmpty()) {
                            Log.d(TAG, "❌ No hay compras para mostrar (vacío)")
                            rvPurchases.visibility = View.GONE
                            llEmptyState.visibility = View.VISIBLE
                        } else {
                            Log.d(TAG, "✅ Mostrando ${filteredPurchases.size} compras")
                            rvPurchases.visibility = View.VISIBLE
                            llEmptyState.visibility = View.GONE
                            purchaseAdapter.updatePurchases(filteredPurchases)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar compras", e)
                runOnUiThread {
                    Toast.makeText(
                        this@PendingPurchasesActivity,
                        "Error al cargar compras: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showAssignCredentialsDialog(purchase: PurchaseEntity) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_assign_credentials, null)

        val tvPurchaseInfo = dialogView.findViewById<TextView>(R.id.tvPurchaseInfo)
        val etCredentialEmail = dialogView.findViewById<EditText>(R.id.etCredentialEmail)
        val etCredentialPassword = dialogView.findViewById<EditText>(R.id.etCredentialPassword)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvPurchaseInfo.text = "Compra de ${purchase.serviceName} por ${purchase.userName}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val email = etCredentialEmail.text.toString().trim()
            val password = etCredentialPassword.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa el email de la cuenta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Ingresa la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            assignCredentials(purchase, email, password)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun assignCredentials(purchase: PurchaseEntity, email: String, password: String) {
        lifecycleScope.launch {
            try {
                // Actualizar compra directamente en Firebase
                if (!purchase.firebaseId.isNullOrEmpty()) {
                    purchaseRepository.updateStatus(
                        purchaseId = purchase.firebaseId!!,
                        status = "active",
                        credentials = PurchaseCredentials(email, password)
                    )

                    Log.d(TAG, "✅ Compra actualizada en Firebase con credenciales")

                    runOnUiThread {
                        Toast.makeText(
                            this@PendingPurchasesActivity,
                            "Credenciales asignadas correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        // No es necesario recargar - el Flow detectará automáticamente el cambio
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@PendingPurchasesActivity,
                            "Error: La compra no tiene ID de Firebase",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al asignar credenciales", e)
                runOnUiThread {
                    Toast.makeText(
                        this@PendingPurchasesActivity,
                        "Error al asignar credenciales: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}