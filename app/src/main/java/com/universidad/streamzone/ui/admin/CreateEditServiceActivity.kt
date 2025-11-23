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
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.models.Service
import com.universidad.streamzone.data.firebase.repository.CategoryRepository
import com.universidad.streamzone.data.firebase.repository.ServiceRepository
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.util.PermissionManager
import com.universidad.streamzone.util.toCategoryEntityList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class CreateEditServiceActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_SERVICES

    // Firebase Repositories
    private val serviceRepository = ServiceRepository()
    private val categoryRepository = CategoryRepository()

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
    private var selectedCategoryId: String = ""
    private var serviceId: String? = null
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
        val serviceIdFromIntent = intent.getStringExtra("SERVICE_ID")
        if (serviceIdFromIntent != null && serviceIdFromIntent.isNotEmpty()) {
            serviceId = serviceIdFromIntent
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
                // Obtener categorías activas desde Firebase
                val firebaseCategories = categoryRepository.getActiveCategories()
                categories = firebaseCategories.toCategoryEntityList()

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
                            selectedCategoryId = categories[position].firebaseId ?: ""
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // No hacer nada
                        }
                    }

                    // Seleccionar primera categoría por defecto
                    if (categories.isNotEmpty()) {
                        selectedCategoryId = categories[0].firebaseId ?: ""
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("CreateService", "❌ Error al cargar categorías", e)
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

    private fun loadServiceData(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtener servicio desde Firebase
                val service = serviceRepository.findById(id)

                withContext(Dispatchers.Main) {
                    if (service != null) {
                        etServiceName.setText(service.name)
                        etServicePrice.setText(service.price)
                        etServiceDescription.setText(service.description)
                        switchIsPopular.isChecked = service.isPopular
                        switchIsActive.isChecked = service.isActive

                        // TODO: Cargar imagen desde Firebase Storage URL si existe
                        // (Por ahora no cargamos imagen en edición)

                        // Seleccionar categoría correspondiente
                        val categoryIndex = categories.indexOfFirst { it.firebaseId == service.categoryId }
                        if (categoryIndex >= 0) {
                            spinnerCategory.setSelection(categoryIndex)
                        }
                    } else {
                        Toast.makeText(
                            this@CreateEditServiceActivity,
                            "Servicio no encontrado",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("CreateService", "❌ Error al cargar servicio", e)
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
                // Crear objeto Service de Firebase
                val service = Service(
                    id = serviceId ?: "", // Si es edición, usar serviceId; si es nuevo, Firebase generará el ID
                    serviceId = name.lowercase().replace(" ", "_"),
                    name = name,
                    price = price,
                    description = description,
                    iconUrl = null, // TODO: Implementar subida a Firebase Storage
                    categoryId = selectedCategoryId,
                    isActive = switchIsActive.isChecked,
                    isPopular = switchIsPopular.isChecked,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                // Guardar o actualizar en Firebase
                if (serviceId == null) {
                    // TODO: Si hay imageBase64, primero subir a Firebase Storage y obtener URL
                    serviceRepository.insert(service)
                    android.util.Log.d("CreateService", "✅ Nuevo servicio creado en Firebase")
                } else {
                    serviceRepository.update(service)
                    android.util.Log.d("CreateService", "✅ Servicio actualizado en Firebase")
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditServiceActivity,
                        if (serviceId != null) "Servicio actualizado" else "Servicio creado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                android.util.Log.e("CreateService", "❌ Error al guardar servicio", e)
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