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
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.OfferEntity
import com.universidad.streamzone.data.model.ServiceEntity
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateEditOfferActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_OFFERS

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

    private var offerId: Long? = null
    private var selectedServiceIds = mutableListOf<Int>()
    private var allServices = listOf<ServiceEntity>()
    private var startDateMillis: Long = System.currentTimeMillis()
    private var endDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 días

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_edit_offer)

        offerId = intent.getLongExtra("OFFER_ID", -1).takeIf { it != -1L }
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
                val db = AppDatabase.getInstance(this@CreateEditOfferActivity)
                val serviceDao = db.serviceDao()

                allServices = serviceDao.getAll()

            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateEditOfferActivity,
                    "Error al cargar servicios: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadOffer(id: Long) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@CreateEditOfferActivity)
                val offerDao = db.offerDao()

                val offer = offerDao.getById(id)
                if (offer == null) {
                    Toast.makeText(
                        this@CreateEditOfferActivity,
                        "Oferta no encontrada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                runOnUiThread {
                    etOfferTitle.setText(offer.title)
                    etOfferDescription.setText(offer.description)
                    etOriginalPrice.setText(offer.originalPrice.toString())
                    etComboPrice.setText(offer.comboPrice.toString())
                    switchIsActive.isChecked = offer.isActive

                    startDateMillis = offer.startDate
                    endDateMillis = offer.endDate
                    updateDateButtons()

                    // Cargar servicios seleccionados
                    selectedServiceIds = offer.serviceIds.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .toMutableList()

                    updateSelectedServicesText()
                    calculateDiscount()
                }

            } catch (e: Exception) {
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
            selectedServiceIds.contains(service.id)
        }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Servicios")
            .setMultiChoiceItems(serviceNames, checkedItems) { _, which, isChecked ->
                val serviceId = allServices[which].id
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
        val serviceIdsString = selectedServiceIds.joinToString(",")

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@CreateEditOfferActivity)
                val offerDao = db.offerDao()

                // Obtener firebaseId si es edición
                var firebaseId: String? = null
                if (offerId != null) {
                    val existingOffer = offerDao.getById(offerId!!)
                    firebaseId = existingOffer?.firebaseId
                }

                val offer = OfferEntity(
                    id = offerId ?: 0,
                    title = title,
                    description = description,
                    serviceIds = serviceIdsString,
                    originalPrice = originalPrice,
                    comboPrice = comboPrice,
                    discountPercent = discount,
                    startDate = startDateMillis,
                    endDate = endDateMillis,
                    isActive = switchIsActive.isChecked,
                    firebaseId = firebaseId
                )

                // Guardar en Room
                val savedId = if (offerId == null) {
                    offerDao.insert(offer)
                } else {
                    offerDao.update(offer)
                    offerId!!
                }

                // Sincronizar con Firebase
                if (isNetworkAvailable()) {
                    val offerToSync = if (offerId == null) {
                        offer.copy(id = savedId)
                    } else {
                        offer
                    }

                    com.universidad.streamzone.data.remote.FirebaseService.sincronizarOferta(
                        offer = offerToSync,
                        onSuccess = { newFirebaseId ->
                            lifecycleScope.launch {
                                // Actualizar firebaseId en Room si es nueva
                                if (firebaseId == null) {
                                    offerDao.update(offerToSync.copy(
                                        firebaseId = newFirebaseId,
                                        sincronizado = true
                                    ))
                                }
                                android.util.Log.d("CreateEditOffer", "✅ Oferta sincronizada con Firebase")
                            }
                        },
                        onFailure = { e ->
                            android.util.Log.e("CreateEditOffer", "❌ Error al sincronizar con Firebase: ${e.message}")
                        }
                    )
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

    /**
     * Verificar conectividad
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false

            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            false
        }
    }
}