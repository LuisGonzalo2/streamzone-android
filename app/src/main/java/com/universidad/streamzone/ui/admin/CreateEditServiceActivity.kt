package com.universidad.streamzone.ui.admin

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.data.model.ServiceEntity
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class CreateEditServiceActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_SERVICES

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etServiceName: TextInputEditText
    private lateinit var etServicePrice: TextInputEditText
    private lateinit var etServiceDescription: TextInputEditText
    private lateinit var ivServicePreview: ImageView
    private lateinit var llImagePlaceholder: LinearLayout
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var btnRemoveImage: MaterialButton
    private lateinit var spinnerCategory: Spinner
    private lateinit var switchIsPopular: SwitchCompat
    private lateinit var switchIsActive: SwitchCompat
    private lateinit var btnSave: MaterialButton

    private var categories: List<CategoryEntity> = emptyList()
    private var selectedCategoryId: Int = 0
    private var serviceId: Int? = null
    private var imageBase64: String? = null

    // Launcher para seleccionar imagen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadImageFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_edit_service)
    }

    override fun onPermissionGranted() {
        initViews()
        loadCategories()

        // Verificar si es edición
        val serviceIdLong = intent.getLongExtra("SERVICE_ID", -1L)
        if (serviceIdLong != -1L) {
            serviceId = serviceIdLong.toInt()
            tvTitle.text = "Editar Servicio"
            loadServiceData(serviceId!!)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        etServiceName = findViewById(R.id.etServiceName)
        etServicePrice = findViewById(R.id.etServicePrice)
        etServiceDescription = findViewById(R.id.etServiceDescription)
        ivServicePreview = findViewById(R.id.ivServicePreview)
        llImagePlaceholder = findViewById(R.id.llImagePlaceholder)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        switchIsPopular = findViewById(R.id.switchIsPopular)
        switchIsActive = findViewById(R.id.switchIsActive)
        btnSave = findViewById(R.id.btnSave)

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveService() }
        btnSelectImage.setOnClickListener { selectImage() }
        btnRemoveImage.setOnClickListener { removeImage() }
    }

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CreateEditServiceActivity)
                val categoryDao = db.categoryDao()
                categories = categoryDao.obtenerCategoriasActivasSync()

                withContext(Dispatchers.Main) {
                    if (categories.isEmpty()) {
                        Toast.makeText(
                            this@CreateEditServiceActivity,
                            "No hay categorías disponibles. Crea una categoría primero.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return@withContext
                    }

                    // Configurar spinner
                    val categoryNames = categories.map { "${it.icon} ${it.name}" }
                    val adapter = ArrayAdapter(
                        this@CreateEditServiceActivity,
                        android.R.layout.simple_spinner_item,
                        categoryNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerCategory.adapter = adapter

                    spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedCategoryId = categories[position].id
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // No hacer nada
                        }
                    }

                    // Seleccionar primera categoría por defecto
                    if (categories.isNotEmpty()) {
                        selectedCategoryId = categories[0].id
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditServiceActivity,
                        "Error al cargar categorías: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadServiceData(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CreateEditServiceActivity)
                val serviceDao = db.serviceDao()
                val service = serviceDao.obtenerPorId(id)

                withContext(Dispatchers.Main) {
                    if (service != null) {
                        etServiceName.setText(service.name)
                        etServicePrice.setText(service.price)
                        etServiceDescription.setText(service.description)
                        switchIsPopular.isChecked = service.isPopular
                        switchIsActive.isChecked = service.isActive

                        // Cargar imagen si existe
                        if (!service.iconBase64.isNullOrEmpty()) {
                            imageBase64 = service.iconBase64
                            try {
                                val decodedBytes = Base64.decode(service.iconBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                ivServicePreview.setImageBitmap(bitmap)
                                ivServicePreview.visibility = View.VISIBLE
                                llImagePlaceholder.visibility = View.GONE
                                btnRemoveImage.visibility = View.VISIBLE
                            } catch (e: Exception) {
                                // Si hay error, simplemente no mostramos la imagen
                            }
                        }

                        // Seleccionar categoría correspondiente
                        val categoryIndex = categories.indexOfFirst { it.id == service.categoryId }
                        if (categoryIndex >= 0) {
                            spinnerCategory.setSelection(categoryIndex)
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditServiceActivity,
                        "Error al cargar servicio: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveService() {
        val name = etServiceName.text?.toString()?.trim()
        val price = etServicePrice.text?.toString()?.trim()
        val description = etServiceDescription.text?.toString()?.trim()

        // Validaciones
        if (name.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del servicio", Toast.LENGTH_SHORT).show()
            return
        }

        if (price.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el precio del servicio", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa la descripción del servicio", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CreateEditServiceActivity)
                val serviceDao = db.serviceDao()

                val service = ServiceEntity(
                    id = serviceId ?: 0,
                    serviceId = name.lowercase().replace(" ", "_"),
                    name = name,
                    price = price,
                    description = description,
                    iconDrawable = null,
                    iconBase64 = imageBase64,
                    iconUrl = null,
                    categoryId = selectedCategoryId,
                    isActive = switchIsActive.isChecked,
                    isPopular = switchIsPopular.isChecked
                )

                if (serviceId != null) {
                    // Actualizar servicio existente
                    serviceDao.actualizar(service)
                } else {
                    // Insertar nuevo servicio
                    serviceDao.insertar(service)
                }

                // Sincronizar con Firebase
                com.universidad.streamzone.data.remote.FirebaseService.sincronizarServicio(
                    serviceId = service.serviceId,
                    name = service.name,
                    description = service.description,
                    categoryId = service.categoryId,
                    price = service.price,
                    duration = service.duration,
                    imageUrl = service.imageUrl,
                    isActive = service.isActive,
                    isPopular = service.isPopular,
                    onSuccess = {
                        android.util.Log.d("CreateService", "✅ Servicio sincronizado con Firebase")
                    },
                    onFailure = { e ->
                        android.util.Log.e("CreateService", "❌ Error al sincronizar: ${e.message}")
                    }
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditServiceActivity,
                        if (serviceId != null) "Servicio actualizado" else "Servicio creado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditServiceActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Redimensionar imagen si es muy grande
            val resizedBitmap = resizeBitmap(bitmap, 800, 800)

            // Convertir a Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // Mostrar preview
            ivServicePreview.setImageBitmap(resizedBitmap)
            ivServicePreview.visibility = View.VISIBLE
            llImagePlaceholder.visibility = View.GONE
            btnRemoveImage.visibility = View.VISIBLE

            Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun removeImage() {
        imageBase64 = null
        ivServicePreview.setImageDrawable(null)
        ivServicePreview.visibility = View.GONE
        llImagePlaceholder.visibility = View.VISIBLE
        btnRemoveImage.visibility = View.GONE
        Toast.makeText(this, "Imagen removida", Toast.LENGTH_SHORT).show()
    }
}