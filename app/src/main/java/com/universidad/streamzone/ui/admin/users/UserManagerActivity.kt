package com.universidad.streamzone.ui.admin.users

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.UsuarioEntity
import android.text.TextWatcher
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.universidad.streamzone.data.local.database.AppDatabase
import kotlinx.coroutines.launch


class UserManagerActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var searchInput: EditText

    private lateinit var btnBack: MaterialButton
    private var allUsersList: List<UsuarioEntity> = listOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_manager)

        rvUsers = findViewById(R.id.rvUsers)
        searchInput = findViewById(R.id.searchInput)
        btnBack = findViewById(R.id.btn_back)




        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        window.setDecorFitsSystemWindows(false)

        // 2. Encontramos el contenedor principal
        val mainContainer: ConstraintLayout = findViewById(R.id.main_container)

        // 3. Le aplicamos el listener
        mainContainer.setOnApplyWindowInsetsListener { view, insets ->
            // Obtiene la altura de la barra de estado (donde está el notch)
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Para Android 11 y superior, usamos el método moderno.
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                // Para versiones anteriores, usamos los métodos deprecados.
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }

            view.setPadding(
                view.paddingLeft,
                systemBars.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        setupRecyclerView()
        setupSearchListener()
        loadUsersFromLocalDB()
        setupBackButton()


    }


    private fun setupRecyclerView() {

        userAdapter = UserAdapter()

        rvUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagerActivity)
            adapter = userAdapter
        }
    }

    private fun setupBackButton() {
        // 3. Asigna la acción de clic
        btnBack.setOnClickListener {
            finish() // Cierra la actividad actual y regresa a la anterior.
        }
    }


    private fun setupSearchListener() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cada vez que el texto cambia, llamamos a la función de filtrado
                filterUserList(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }



    private fun filterUserList(query: String) {
        // Filtra la lista completa de usuarios
        val filteredList = if (query.isBlank()) {
            // Si la búsqueda está vacía, muestra todos los usuarios
            allUsersList
        } else {
            // Si hay texto, filtra la lista
            allUsersList.filter { user ->
                // Comprueba si el nombre completo del usuario contiene el texto de búsqueda (ignorando mayúsculas/minúsculas)
                user.fullname.contains(query, ignoreCase = true)
            }
        }
        // Actualiza el adaptador con la lista filtrada
        userAdapter.submitList(filteredList)
    }

    private fun loadUsersFromLocalDB() {
        // Usamos una corrutina para acceder a la base de datos de forma segura.
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@UserManagerActivity).usuarioDao()

            dao.obtenerTodos().collect { userListFromRoom ->

                if (userListFromRoom.isNotEmpty()) {
                    // 3. Guardamos la lista completa obtenida de Room.
                    allUsersList = userListFromRoom
                    // 4. Actualizamos el adaptador con esta lista.
                    userAdapter.submitList(allUsersList)
                } else {
                    Log.d(
                        "UserManagerActivity",
                        "No se encontraron usuarios en la base de datos local."
                    )
                    Toast.makeText(
                        this@UserManagerActivity,
                        "No hay usuarios registrados localmente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }




    }
}
