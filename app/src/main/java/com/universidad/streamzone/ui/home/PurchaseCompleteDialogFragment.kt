package com.universidad.streamzone.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.data.remote.FirebaseService
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.appcompat.widget.AppCompatImageButton

class PurchaseCompleteDialogFragment : DialogFragment() {

    private lateinit var tvMessage: TextView
    private lateinit var btnWhatsApp: Button
    private lateinit var btnClose: AppCompatImageButton

    private var serviceId: String = ""
    private var serviceName: String = ""
    private var servicePrice: String = ""
    private var duration: String = "1 mes"
    private var userName: String = ""
    private var userEmail: String = ""

    companion object {
        private const val ARG_SERVICE_ID = "service_id"
        private const val ARG_SERVICE_NAME = "service_name"
        private const val ARG_SERVICE_PRICE = "service_price"
        private const val ARG_DURATION = "duration"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_USER_EMAIL = "user_email"

        fun newInstance(
            serviceId: String,
            serviceName: String,
            servicePrice: String,
            duration: String,
            userName: String,
            userEmail: String
        ): PurchaseCompleteDialogFragment {
            val fragment = PurchaseCompleteDialogFragment()
            val args = Bundle()
            args.putString(ARG_SERVICE_ID, serviceId)
            args.putString(ARG_SERVICE_NAME, serviceName)
            args.putString(ARG_SERVICE_PRICE, servicePrice)
            args.putString(ARG_DURATION, duration)
            args.putString(ARG_USER_NAME, userName)
            args.putString(ARG_USER_EMAIL, userEmail)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog_Alert)

        arguments?.let {
            serviceId = it.getString(ARG_SERVICE_ID, "")
            serviceName = it.getString(ARG_SERVICE_NAME, "")
            servicePrice = it.getString(ARG_SERVICE_PRICE, "")
            duration = it.getString(ARG_DURATION, "1 mes")
            userName = it.getString(ARG_USER_NAME, "")
            userEmail = it.getString(ARG_USER_EMAIL, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_purchase_complete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvMessage = view.findViewById(R.id.tv_message)
        btnWhatsApp = view.findViewById(R.id.btn_whatsapp)
        btnClose = view.findViewById(R.id.btn_close)

        tvMessage.text = "¡Compra de $serviceName registrada! 🎉\n\nAhora contacta a nuestro agente para recibir tus credenciales."

        btnWhatsApp.setOnClickListener {
            // Guardar la compra ANTES de abrir WhatsApp
            guardarCompra()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun guardarCompra() {
        Log.d("PurchaseComplete", "Guardando compra: $serviceName para $userEmail")

        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(requireContext()).purchaseDao()

                // Calcular fechas
                val purchaseDate = System.currentTimeMillis()
                val expirationDate = calcularFechaExpiracion(purchaseDate, duration)

                // Crear la compra
                val purchase = PurchaseEntity(
                    userEmail = userEmail,
                    userName = userName,
                    serviceId = serviceId,
                    serviceName = serviceName,
                    servicePrice = servicePrice,
                    serviceDuration = duration,
                    email = null, // Se llenará después cuando el admin entregue credenciales
                    password = null,
                    purchaseDate = purchaseDate,
                    expirationDate = expirationDate,
                    status = "pending", // pending hasta que se entreguen credenciales
                    sincronizado = false
                )

                // Guardar en Room
                val purchaseId = dao.insertar(purchase)
                Log.d("PurchaseComplete", "Compra guardada en Room con ID: $purchaseId")

                // Intentar sincronizar con Firebase
                FirebaseService.guardarCompra(
                    purchase = purchase,
                    onSuccess = { firebaseId ->
                        lifecycleScope.launch {
                            dao.marcarComoSincronizada(purchaseId.toInt(), firebaseId)
                            Log.d("PurchaseComplete", "Compra sincronizada con Firebase")
                        }
                    },
                    onFailure = { e ->
                        Log.e("PurchaseComplete", "Error al sincronizar con Firebase", e)
                        // No pasa nada, se sincronizará después
                    }
                )

                // Abrir WhatsApp
                abrirWhatsApp()

            } catch (e: Exception) {
                Log.e("PurchaseComplete", "Error al guardar compra", e)
                Toast.makeText(
                    requireContext(),
                    "Error al registrar compra. Intenta de nuevo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun calcularFechaExpiracion(fechaInicio: Long, duracion: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = fechaInicio

        when {
            duracion.contains("mes", ignoreCase = true) -> {
                val meses = duracion.filter { it.isDigit() }.toIntOrNull() ?: 1
                calendar.add(Calendar.MONTH, meses)
            }
            duracion.contains("año", ignoreCase = true) -> {
                val anios = duracion.filter { it.isDigit() }.toIntOrNull() ?: 1
                calendar.add(Calendar.YEAR, anios)
            }
            else -> {
                calendar.add(Calendar.MONTH, 1) // Por defecto 1 mes
            }
        }

        return calendar.timeInMillis
    }

    private fun abrirWhatsApp() {
        val phoneNumber = "+593984280334"
        val message = """
            Hola! 👋
            
            Acabo de completar mi compra:
            📦 Servicio: $serviceName
            💰 Precio: $servicePrice
            ⏱️ Duración: $duration
            👤 Usuario: $userName
            📧 Email: $userEmail
            
            ¿Podrían enviarme las credenciales de acceso?
            
            ¡Gracias! 😊
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(
                "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
            )
            startActivity(intent)
            dismiss()
        } catch (e: Exception) {
            Log.e("PurchaseComplete", "Error al abrir WhatsApp", e)
            Toast.makeText(
                requireContext(),
                "No se pudo abrir WhatsApp. Instala WhatsApp e intenta de nuevo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}