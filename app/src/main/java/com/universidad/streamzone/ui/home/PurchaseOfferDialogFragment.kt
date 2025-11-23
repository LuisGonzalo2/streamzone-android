package com.universidad.streamzone.ui.home

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.models.Purchase
import com.universidad.streamzone.data.firebase.repository.OfferRepository
import com.universidad.streamzone.data.firebase.repository.PurchaseRepository
import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.util.toUIServiceList
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PurchaseOfferDialogFragment : DialogFragment() {

    // Firebase Repositories
    private val offerRepository = OfferRepository()
    private val purchaseRepository = PurchaseRepository()

    companion object {
        private const val ARG_OFFER_ID = "arg_offer_id"
        private const val ARG_USER_EMAIL = "arg_user_email"
        private const val ARG_USER_NAME = "arg_user_name"

        fun newInstance(
            offerId: String,
            userEmail: String,
            userName: String
        ): PurchaseOfferDialogFragment {
            val f = PurchaseOfferDialogFragment()
            val bundle = Bundle().apply {
                putString(ARG_OFFER_ID, offerId)
                putString(ARG_USER_EMAIL, userEmail)
                putString(ARG_USER_NAME, userName)
            }
            f.arguments = bundle
            return f
        }
    }

    // UI data class for offer with services
    private data class OfferData(
        val id: String,
        val title: String,
        val description: String,
        val originalPrice: Double,
        val comboPrice: Double,
        val discountPercent: Int,
        val endDate: Date,
        val services: List<Service>
    )

    private var offerId: String = ""
    private var userEmail: String = ""
    private var userName: String = ""
    private var offerData: OfferData? = null
    private var selectedDuration: Int = 1

    private lateinit var tvDialogTitle: TextView
    private lateinit var tvOfferTitle: TextView
    private lateinit var tvOfferDescription: TextView
    private lateinit var tvOriginalPrice: TextView
    private lateinit var tvComboPrice: TextView
    private lateinit var tvSavings: TextView
    private lateinit var tvOfferExpiry: TextView
    private lateinit var llServices: LinearLayout
    private lateinit var btnDuration1: Button
    private lateinit var btnDuration2: Button
    private lateinit var btnDuration3: Button
    private lateinit var btnDuration6: Button
    private lateinit var btnPurchaseOffer: Button
    private lateinit var btnClose: ImageButton

    private lateinit var tvTotalPrice: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)

        arguments?.let {
            offerId = it.getString(ARG_OFFER_ID, "")
            userEmail = it.getString(ARG_USER_EMAIL, "")
            userName = it.getString(ARG_USER_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_offer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadOffer()
        setupListeners()
    }

    private fun initViews(view: View) {
        tvDialogTitle = view.findViewById(R.id.tvDialogTitle)
        tvOfferTitle = view.findViewById(R.id.tvOfferTitle)
        tvOfferDescription = view.findViewById(R.id.tvOfferDescription)
        tvOriginalPrice = view.findViewById(R.id.tvOriginalPrice)
        tvComboPrice = view.findViewById(R.id.tvComboPrice)
        tvSavings = view.findViewById(R.id.tvSavings)
        tvOfferExpiry = view.findViewById(R.id.tvOfferExpiry)
        llServices = view.findViewById(R.id.llServices)
        btnDuration1 = view.findViewById(R.id.btnDuration1)
        btnDuration2 = view.findViewById(R.id.btnDuration2)
        btnDuration3 = view.findViewById(R.id.btnDuration3)
        btnDuration6 = view.findViewById(R.id.btnDuration6)
        btnPurchaseOffer = view.findViewById(R.id.btnPurchaseOffer)
        btnClose = view.findViewById(R.id.btnClose)
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice)
    }

    private fun loadOffer() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("OfferDialog", "🔍 Buscando oferta con ID: $offerId")

                // Obtener oferta con servicios desde Firebase
                val firebaseOfferData = offerRepository.getOfferWithServices(offerId)

                if (firebaseOfferData == null) {
                    android.util.Log.e("OfferDialog", "❌ Oferta no encontrada con ID: $offerId")
                    Toast.makeText(requireContext(), "Oferta no encontrada", Toast.LENGTH_SHORT).show()
                    dismiss()
                    return@launch
                }

                android.util.Log.d("OfferDialog", "✅ Oferta encontrada: ${firebaseOfferData.offer.title}")
                android.util.Log.d("OfferDialog", "   Total servicios: ${firebaseOfferData.services.size}")

                // Convertir a modelo UI
                offerData = OfferData(
                    id = firebaseOfferData.offer.id,
                    title = firebaseOfferData.offer.title,
                    description = firebaseOfferData.offer.description,
                    originalPrice = firebaseOfferData.offer.originalPrice,
                    comboPrice = firebaseOfferData.offer.comboPrice,
                    discountPercent = firebaseOfferData.offer.discountPercent,
                    endDate = firebaseOfferData.offer.endDate.toDate(),
                    services = firebaseOfferData.services.toUIServiceList()
                )

                displayOffer()

            } catch (e: Exception) {
                android.util.Log.e("OfferDialog", "❌ Error completo: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cargar oferta: ${e.message}", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun displayOffer() {
        val data = offerData ?: return

        // Título y descripción
        tvOfferTitle.text = data.title
        tvOfferDescription.text = data.description

        // Precios
        tvOriginalPrice.text = "US$ %.2f".format(data.originalPrice)
        tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        tvComboPrice.text = "US$ %.2f".format(data.comboPrice)

        val savings = data.originalPrice - data.comboPrice
        tvSavings.text = "Ahorras ${data.discountPercent}% (US$ ${"%.2f".format(savings)}/mes)"

        // Vigencia
        val dateFormat = SimpleDateFormat("dd 'de' MMMM", Locale("es", "ES"))
        tvOfferExpiry.text = "⏰ Oferta válida hasta el ${dateFormat.format(data.endDate)}"

        // Servicios incluidos
        llServices.removeAllViews()
        data.services.forEach { service ->
            addServiceItem(service)
        }
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val data = offerData ?: return
        val totalPrice = data.comboPrice * selectedDuration
        tvTotalPrice.text = "US$ %.2f".format(totalPrice)
    }

    private fun addServiceItem(service: Service) {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_service_combo, llServices, false)

        val tvServiceName = itemView.findViewById<TextView>(R.id.tvServiceName)
        val tvServicePrice = itemView.findViewById<TextView>(R.id.tvServicePrice)

        tvServiceName.text = "✓ ${service.title}"
        tvServicePrice.text = service.price

        llServices.addView(itemView)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }


        // Selección de duración
        btnDuration1.setOnClickListener { selectDuration(1, btnDuration1) }
        btnDuration2.setOnClickListener { selectDuration(2, btnDuration2) }
        btnDuration3.setOnClickListener { selectDuration(3, btnDuration3) }
        btnDuration6.setOnClickListener { selectDuration(6, btnDuration6) }

        // Seleccionar 1 mes por defecto
        selectDuration(1, btnDuration1)

        // Comprar combo
        btnPurchaseOffer.setOnClickListener {
            purchaseCombo()
        }
    }

    private fun selectDuration(months: Int, button: Button) {
        selectedDuration = months

        // Reset todos los botones
        listOf(btnDuration1, btnDuration2, btnDuration3, btnDuration6).forEach {
            it.setBackgroundColor(resources.getColor(R.color.bg_card, null))
        }

        // Activar el seleccionado
        button.setBackgroundColor(0xFF6366F1.toInt())
        // Actualizar precio total
        updateTotalPrice()
    }

    private fun purchaseCombo() {
        val data = offerData ?: return
        android.util.Log.d("OfferDialog", "userEmail: '$userEmail'")
        android.util.Log.d("OfferDialog", "userName: '$userName'")

        if (userEmail.isEmpty() || userName.isEmpty()) {
            Toast.makeText(requireContext(), "Error: usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Calcular fecha de expiración
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, selectedDuration)
                val expirationDate = Timestamp(calendar.time)

                // Crear UNA sola compra para el combo completo
                val purchase = Purchase(
                    userId = "",
                    userEmail = userEmail,
                    userName = userName,
                    serviceId = "combo_${data.id}",  // ID único del combo
                    serviceName = data.title,  // "Combo: Netflix + Spotify"
                    servicePrice = "US$ %.2f".format(data.comboPrice * selectedDuration),
                    serviceDuration = "$selectedDuration ${if (selectedDuration == 1) "mes" else "meses"}",
                    credentials = null,
                    purchaseDate = Timestamp.now(),
                    expirationDate = expirationDate,
                    status = "pending",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                // Guardar en Firebase
                purchaseRepository.insert(purchase)
                android.util.Log.d("OfferDialog", "✅ Compra guardada en Firebase")

                // Mostrar diálogo de éxito
                dismiss()
                showSuccessDialog(data.services.size, selectedDuration, data.comboPrice)

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al procesar compra: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showSuccessDialog(serviceCount: Int, months: Int, totalPrice: Double) {
        val successDialog = PurchaseCompleteDialogFragment.newInstance(
            serviceId = "combo_$offerId",
            serviceName = "Combo de $serviceCount servicios",
            servicePrice = "US$ %.2f".format(totalPrice * months),
            duration = "$months ${if (months == 1) "mes" else "meses"}",
            userName = userName,
            userEmail = userEmail
        )
        successDialog.show(parentFragmentManager, "purchaseComplete")
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}