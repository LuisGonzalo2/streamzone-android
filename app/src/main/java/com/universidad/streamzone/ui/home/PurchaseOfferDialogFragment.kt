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
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.OfferWithServices
import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.data.model.Service
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PurchaseOfferDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_OFFER_ID = "arg_offer_id"
        private const val ARG_USER_EMAIL = "arg_user_email"
        private const val ARG_USER_NAME = "arg_user_name"

        fun newInstance(
            offerId: Long,
            userEmail: String,
            userName: String
        ): PurchaseOfferDialogFragment {
            val f = PurchaseOfferDialogFragment()
            val bundle = Bundle().apply {
                putLong(ARG_OFFER_ID, offerId)
                putString(ARG_USER_EMAIL, userEmail)
                putString(ARG_USER_NAME, userName)
            }
            f.arguments = bundle
            return f
        }
    }

    private var offerId: Long = 0L
    private var userEmail: String = ""
    private var userName: String = ""
    private var offerWithServices: OfferWithServices? = null
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
            offerId = it.getLong(ARG_OFFER_ID, 0L)
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
                val db = AppDatabase.getInstance(requireContext())
                val offerDao = db.offerDao()
                val serviceDao = db.serviceDao()

                // ✅ LOG 1: Ver qué oferta se está cargando
                android.util.Log.d("OfferDialog", "🔍 Buscando oferta con ID: $offerId")

                val offer = offerDao.getById(offerId)
                if (offer == null) {
                    // ✅ LOG 2: Si no encuentra la oferta
                    android.util.Log.e("OfferDialog", "❌ Oferta no encontrada con ID: $offerId")
                    Toast.makeText(requireContext(), "Oferta no encontrada", Toast.LENGTH_SHORT).show()
                    dismiss()
                    return@launch
                }

                // ✅ LOG 3: Ver datos de la oferta
                android.util.Log.d("OfferDialog", "✅ Oferta encontrada: ${offer.title}")
                android.util.Log.d("OfferDialog", "   serviceIds: ${offer.serviceIds}")
                android.util.Log.d("OfferDialog", "   precio: ${offer.comboPrice}")

                // Obtener servicios del combo
                val serviceIds = offer.serviceIds.split(",")
                    .mapNotNull { it.trim().toLongOrNull() }

                // ✅ LOG 4: Ver IDs parseados
                android.util.Log.d("OfferDialog", "   IDs parseados: $serviceIds")

                val services = serviceIds.mapNotNull { id ->
                    serviceDao.getById(id)?.let { entity ->
                        android.util.Log.d("OfferDialog", "   ✅ Servicio cargado: ${entity.name}")
                        Service(
                            id = entity.serviceId,
                            title = entity.name,
                            price = entity.price,
                            desc = entity.description,
                            iconRes = entity.iconDrawable ?: 0
                        )
                    }
                }

                // ✅ LOG 5: Ver cuántos servicios se cargaron
                android.util.Log.d("OfferDialog", "   Total servicios: ${services.size}")

                offerWithServices = OfferWithServices(offer, services)
                displayOffer()

            } catch (e: Exception) {
                // ✅ LOG 6: Ver el error exacto
                android.util.Log.e("OfferDialog", "❌ Error completo: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cargar oferta: ${e.message}", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun displayOffer() {
        val offerData = offerWithServices ?: return

        // Título y descripción
        tvOfferTitle.text = offerData.offer.title
        tvOfferDescription.text = offerData.offer.description

        // Precios
        tvOriginalPrice.text = "US$ %.2f".format(offerData.offer.originalPrice)
        tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        tvComboPrice.text = "US$ %.2f".format(offerData.offer.comboPrice)

        val savings = offerData.offer.originalPrice - offerData.offer.comboPrice
        tvSavings.text = "Ahorras ${offerData.offer.discountPercent}% (US$ ${"%.2f".format(savings)}/mes)"

        // Vigencia
        val dateFormat = SimpleDateFormat("dd 'de' MMMM", Locale("es", "ES"))
        val endDate = Date(offerData.offer.endDate)
        tvOfferExpiry.text = "⏰ Oferta válida hasta el ${dateFormat.format(endDate)}"

        // Servicios incluidos
        llServices.removeAllViews()
        offerData.services.forEach { service ->
            addServiceItem(service)
        }
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val offer = offerWithServices ?: return
        val totalPrice = offer.offer.comboPrice * selectedDuration
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
        val offer = offerWithServices ?: return
        android.util.Log.d("OfferDialog", "userEmail: '$userEmail'")
        android.util.Log.d("OfferDialog", "userName: '$userName'")

        if (userEmail.isEmpty() || userName.isEmpty()) {
            Toast.makeText(requireContext(), "Error: usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                val purchaseDao = db.purchaseDao()

                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = now
                calendar.add(Calendar.MONTH, selectedDuration)
                val expirationDate = calendar.timeInMillis

                // Crear UNA sola compra para el combo completo
                val purchase = PurchaseEntity(
                    userEmail = userEmail,
                    userName = userName,
                    serviceId = "combo_${offer.offer.id}",  // ID único del combo
                    serviceName = offer.offer.title,  // "Combo: Netflix + Spotify"
                    servicePrice = "US$ %.2f".format(offer.offer.comboPrice * selectedDuration),
                    serviceDuration = "$selectedDuration ${if (selectedDuration == 1) "mes" else "meses"}",
                    email = null,
                    password = null,
                    purchaseDate = now,
                    expirationDate = expirationDate,
                    status = "pending"
                )

                purchaseDao.insertar(purchase)

                // Mostrar diálogo de éxito
                dismiss()
                showSuccessDialog(offer.services.size, selectedDuration, offer.offer.comboPrice)

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