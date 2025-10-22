package com.universidad.streamzone

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Dao
import com.universidad.streamzone.cloud.FirebaseService
import com.universidad.streamzone.database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListaUsuariosActivity : AppCompatActivity() {
    private lateinit var rv: RecyclerView
    private lateinit
    var adapter: UsuarioAdapter
    private lateinit var btnFirebase: Button
    private lateinit var btnRoom: Button
    override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView (R.layout.activity_lista_usuarios)


        rv = findViewById (R.id.rvUsuarios)
        adapter = UsuarioAdapter ()
        rv . layoutManager = LinearLayoutManager (this)
        rv . adapter = adapter

        btnFirebase = findViewById(R.id.btnFirebase)
        btnRoom = findViewById(R.id.btnRoom)
        val dao = AppDatabase.getInstance(this).usuarioDao()

        btnRoom.setOnClickListener {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED){
                    dao.obtenerTodos().collectLatest {lista ->
                        adapter.submitList((lista))
                    }
                }
            }
        }

        btnFirebase.setOnClickListener {
            FirebaseService.obtenerUsuarios { listaFirestore ->
                adapter.submitList(listaFirestore)
            }
        }


        //  lifecycleScope . launch {
        //             dao.obtenerTodos().collectLatest { lista ->
        //             adapter.submitList(lista)
        //             }
        //  }

    }
}