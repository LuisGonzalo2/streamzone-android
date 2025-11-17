package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.ui.admin.adapter.OfferAdapter
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

class OffersManagerActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_OFFERS

    private lateinit var btnBack: ImageButton
    private lateinit var btnNewOffer: Button
    private lateinit var rvOffers: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var offerAdapter: OfferAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offers_manager)

        initViews()
        setupRecyclerView()
        loadOffers()
    }

    override fun onResume() {
        super.onResume()
        // Recargar ofertas cuando volvemos de crear/editar
        loadOffers()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnNewOffer = findViewById(R.id.btnNewOffer)
        rvOffers = findViewById(R.id.rvOffers)
        llEmptyState = findViewById(R.id.llEmptyState)

        btnBack.setOnClickListener {
            finish()
        }

        btnNewOffer.setOnClickListener {
            val intent = Intent(this, CreateEditOfferActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        offerAdapter = OfferAdapter(
            offers = emptyList(),
            onEditClick = { offer ->
                val intent = Intent(this, CreateEditOfferActivity::class.java)
                intent.putExtra("OFFER_ID", offer.id)
                startActivity(intent)
            },
            onDeleteClick = { offer ->
                showDeleteConfirmation(offer.id, offer.title)
            }
        )

        rvOffers.layoutManager = LinearLayoutManager(this)
        rvOffers.adapter = offerAdapter
    }

    private fun loadOffers() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@OffersManagerActivity)
                val offerDao = db.offerDao()

                val offers = offerDao.getAll()

                runOnUiThread {
                    if (offers.isEmpty()) {
                        rvOffers.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                    } else {
                        rvOffers.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        offerAdapter.updateOffers(offers)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@OffersManagerActivity,
                        "Error al cargar ofertas: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation(offerId: Long, offerTitle: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Oferta")
            .setMessage("¿Estás seguro de que deseas eliminar \"$offerTitle\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteOffer(offerId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteOffer(offerId: Long) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@OffersManagerActivity)
                val offerDao = db.offerDao()

                offerDao.deleteById(offerId)

                runOnUiThread {
                    Toast.makeText(
                        this@OffersManagerActivity,
                        "Oferta eliminada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadOffers()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@OffersManagerActivity,
                        "Error al eliminar oferta: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}