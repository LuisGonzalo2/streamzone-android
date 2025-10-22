package com.universidad.streamzone.cloud

import android.util.Log
import com.universidad.streamzone.model.UsuarioEntity
import com.google.firebase.firestore.FirebaseFirestore
import javax.security.auth.callback.Callback

object FirebaseService {
    private val db = FirebaseFirestore.getInstance()

    fun guardarUsuario(usuario: UsuarioEntity){
        val data = hashMapOf(
            "nombre" to usuario.fullname,
            "email" to usuario.email,
            "phone" to usuario.phone,
            "password" to usuario.password,
            "confirm_password" to usuario.confirmPassword


        )
        db.collection("usuarios").add(data)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Usuario guardado correctamente en Firestone")
            }
            .addOnFailureListener {
                Log.e("FirebaseService", "Error al guardar en Firestone")
            }
    }

    fun obtenerUsuarios(callback: (List<UsuarioEntity>) -> Unit) {
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map{doc ->
                    UsuarioEntity(
                        id =0,
                        fullname = doc.getString("nombre")?:"",
                        email = doc.getString("email")?:"",
                        phone = doc.getString("phone")?:"",
                        password = doc.getString("password")?:"",
                        confirmPassword = doc.getString("confirm_password")?:""


                    )
                }
                callback(lista)
            }
            .addOnFailureListener { e->
                Log.e("FirebaseService", "Error al obtener usuarios", e)
                callback(emptyList())
            }
    }
}