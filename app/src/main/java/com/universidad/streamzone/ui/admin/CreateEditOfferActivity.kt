package com.universidad.streamzone.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.models.Offer
import com.universidad.streamzone.data.firebase.models.Service
import com.universidad.streamzone.data.firebase.repository.OfferRepository
import com.universidad.streamzone.data.firebase.repository.ServiceRepository
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateEditOfferActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_OFFERS

    // Firebase Repositories
    private val offerRepository = OfferRepository()
    private val serviceRepository = ServiceRepository()

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etOfferTitle: EditText
    private lateinit var etOfferDescription: EditText
    private lateinit var tvSelectedServices: TextView
    private lateinit var btnSelectServices: Button
    private lateinit var etOriginalPrice: EditText
    private lateinit var etComboPrice: EditText
    private lateinit var tvCalculatedDiscount: TextView
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var switchIsActive: SwitchCompat
    private lateinit var btnSaveOffer: Button

    private var offerId: String? = null
    private var selectedServiceIds = mutableListOf<String>()
    private var allServices = listOf<Service>()
    private var startDateMillis: Long = System.currentTimeMillis()
    private var endDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 días

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_edit_offer)

        offerId = intent.getStringExtra("OFFER_ID")?.takeIf { it.isNotEmpty() }
    }

    override fun onPermissionGranted() {
        initViews()
        setupListeners()
        loadServices()

        if (offerId != null) {
            tvTitle.text = "Editar Oferta"
            loadOffer(offerId!!)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        etOfferTitle = findViewById(R.id.etOfferTitle)
        etOfferDescription = findViewById(R.id.etOfferDescription)
        tvSelectedServices = findViewById(R.id.tvSelectedServices)
        btnSelectServices = findViewById(R.id.btnSelectServices)
        etOriginalPrice = findViewById(R.id.etOriginalPrice)
        etComboPrice = findViewById(R.id.etComboPrice)
        tvCalculatedDiscount = findViewById(R.id.tvCalculatedDiscount)
        btnStartDate = findViewById(R.id.btnStartDate)
        btnEndDate = findViewById(R.id.btnEndDate)
        switchIsActive = findViewById(R.id.switchIsActive)
        btnSaveOffer = findViewById(R.id.btnSaveOffer)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSelectServices.setOnClickListener {
            showServiceSelectionDialog()
        }

        // Calcular descuento automáticamente
        val priceWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateDiscount()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etOriginalPrice.addTextChangedListener(priceWatcher)
        etComboPrice.addTextChangedListener(priceWatcher)

        btnStartDate.setOnClickListener {
            showDatePicker(true)
        }

        btnEndDate.setOnClickListener {
            showDatePicker(false)
        }

        btnSaveOffer.setOnClickListener {
            saveOffer()
        }

        // Actualizar texto de botones con fechas por defecto
        updateDateButtons()
    }

    private fun loadServices() {
        lifecycleScope.launch {
            try {
                // Obtener servicios activos desde Firebase
                allServices = serviceRepository.getActiveServices()

            } catch (e: Exception) {
                android.util.Log.e("CreateEditOffer", "❌ Error al cargar servicios", e)
            }
        }
    }

    private fun loadOffer(id: String) {
        lifecycleScope.launch {
            try {
                // Obtener oferta desde Firebase
                val offer = offerRepository.findById(id)
                if (offer == null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@CreateEditOfferActivity,
                            "Oferta no encontrada",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    return@launch
                }

                runOnUiThread {
                    etOfferTitle.setText(offer.title)
                    etOfferDescription.setText(offer.description)
                    etOriginalPrice.setText(offer.originalPrice.toString())
                    etComboPrice.setText(offer.comboPrice.toString())
                    switchIsActive.isChecked = offer.isActive

                    startDateMillis = offer.startDate.seconds * 1000
                    endDateMillis = offer.endDate.seconds * 1000
                    updateDateButtons()

                    // Cargar servicios seleccionados (ahora son String IDs de Firebase)
                    selectedServiceIds = offer.serviceIds.toMutableList()

                    updateSelectedServicesText()
                    calculateDiscount()
                }

            } catch (e: Exception) {
                android.util.Log.e("CreateEditOffer", "❌ Error al cargar oferta", e)
                runOnUiThread {
                    Toast.makeText(
                        this@CreateEditOfferActivity,
                        "Error al cargar oferta: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showServiceSelectionDialog() {
        if (allServices.isEmpty()) {
            Toast.makeText(this, "No hay servicios disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceNames = allServices.map { it.name }.toTypedArray()
        val checkedItems = allServices.map { service ->
            selectedServiceIds.contains(service.id) // service.id es String ahora
        }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Servicios")
            .setMultiChoiceItems(serviceNames, checkedItems) { _, which, isChecked ->
                val serviceId = allServices[which].id // String ID de Firebase
                if (isChecked) {
                    if (!selectedServiceIds.contains(serviceId)) {
                        selectedServiceIds.add(serviceId)
                    }
                } else {
                    selectedServiceIds.remove(serviceId)
                }
            }
            .setPositiveButton("Aceptar") { _, _ ->
                updateSelectedServicesText()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateSelectedServicesText() {
        if (selectedServiceIds.isEmpty()) {
            tvSelectedServices.text = "Ningún servicio seleccionado"
            tvSelectedServices.setTextColor(getColor(android.R.color.holo_red_light))
        } else {
            val selectedNames = allServices
                .filter { selectedServiceIds.contains(it.id) }
                .joinToString(", ") { it.name }

            tvSelectedServices.text = "${selectedServiceIds.size} servicios: $selectedNames"
            tvSelectedServices.setTextColor(getColor(R.color.white))
        }
    }

    private fun calculateDiscount() {
        val originalPrice = etOriginalPrice.text.toString().toDoubleOrNull() ?: 0.0
        val comboPrice = etComboPrice.text.toString().toDoubleOrNull() ?: 0.0

        if (originalPrice > 0 && comboPrice > 0 && comboPrice < originalPrice) {
            val discount = ((originalPrice - comboPrice) / originalPrice * 100).toInt()
            tvCalculatedDiscount.text = "Descuento: $discount% (Ahorras US$ %.2f)".format(originalPrice - comboPrice)
            tvCalculatedDiscount.visibility = View.VISIBLE
        } else {
            tvCalculatedDiscount.visibility = View.GONE
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = if (isStartDate) startDateMillis else endDateMillis

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                if (isStartDate) {
                    startDateMillis = calendar.timeInMillis
                } else {
                    endDateMillis = calendar.timeInMillis
                }
                updateDateButtons()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtons() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        btnStartDate.text = dateFormat.format(Date(startDateMillis))
        btnEndDate.text = dateFormat.format(Date(endDateMillis))
    }

    private fun saveOffer() {
        // Validaciones
        val title = etOfferTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val description = etOfferDescription.text.toString().trim()
        if (description.isEmpty()) {
            Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedServiceIds.size < 2) {
            Toast.makeText(this, "Selecciona al menos 2 servicios", Toast.LENGTH_SHORT).show()
            return
        }

        val originalPrice = etOriginalPrice.text.toString().toDoubleOrNull()
        if (originalPrice == null || originalPrice <= 0) {
            Toast.makeText(this, "Ingresa un precio original válido", Toast.LENGTH_SHORT).show()
            return
        }

        val comboPrice = etComboPrice.text.toString().toDoubleOrNull()
        if (comboPrice == null || comboPrice <= 0) {
            Toast.makeText(this, "Ingresa un precio combo válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (comboPrice >= originalPrice) {
            Toast.makeText(this, "El precio combo debe ser menor al original", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDateMillis <= startDateMillis) {
            Toast.makeText(this, "La fecha de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
            return
        }

        val discount = ((originalPrice - comboPrice) / originalPrice * 100).toInt()

        lifecycleScope.launch {
            try {
                // Crear objeto Offer de Firebase
                val offer = Offer(
                    id = offerId ?: "", // Si es edición, usar offerId; si es nuevo, Firebase generará el ID
                    title = title,
                    description = description,
                    serviceIds = selectedServiceIds, // Lista de String IDs
                    originalPrice = originalPrice,
                    comboPrice = comboPrice,
                    discountPercent = discount,
                    startDate = Timestamp(Date(startDateMillis)),
                    endDate = Timestamp(Date(endDateMillis)),
                    isActive = switchIsActive.isChecked,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                // Guardar o actualizar en Firebase
                if (offerId == null) {
                    offerRepository.insert(offer)
                    android.util.Log.d("CreateEditOffer", "✅ Nueva oferta creada en Firebase")
                } else {
                    offerRepository.update(offer)
                    android.util.Log.d("CreateEditOffer", "✅ Oferta actualizada en Firebase")
                }

                runOnUiThread {
                    Toast.makeText(
                        this@CreateEditOfferActivity,
                        "Oferta guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                android.util.Log.e("CreateEditOffer", "❌ Error al guardar oferta", e)
                runOnUiThread {
                    Toast.makeText(
                        this@CreateEditOfferActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}