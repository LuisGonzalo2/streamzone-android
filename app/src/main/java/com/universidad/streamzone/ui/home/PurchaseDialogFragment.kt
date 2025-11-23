package com.universidad.streamzone.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import coil.load
import androidx.fragment.app.DialogFragment
import com.universidad.streamzone.R
import com.universidad.streamzone.ui.reserve.ReserveActivity
import java.util.Locale
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.LinearLayout
import android.text.Spannable
import android.text.SpannableString
import android.graphics.Rect
import java.text.Normalizer

class PurchaseDialogFragment : DialogFragment() {

    // Mapa determinista de serviceId -> logo drawable (prioritario sobre heurística)
    private val LOGO_MAP = mapOf(
        "netflix" to R.drawable.logo_netflix,
        "disney_plus_premium" to R.drawable.logo_disney_plus_premium,
        "disney_plus_standard" to R.drawable.logo_disney_plus_standard,
        "max" to R.drawable.logo_max,
        "prime" to R.drawable.logo_prime,
        "youtube_premium" to R.drawable.logo_youtube_premium,
        "paramount" to R.drawable.logo_paramount,
        "chatgpt" to R.drawable.logo_chatgpt,
        "crunchyroll" to R.drawable.logo_crunchyroll,
        "spotify" to R.drawable.logo_spotify,
        "deezer" to R.drawable.logo_deezer,
        "appletv" to R.drawable.logo_appletv,
        "canva" to R.drawable.logo_canva,
        "office365_year" to R.drawable.logo_office365_year
    )

    companion object {
        private const val ARG_SERVICE_ID = "arg_service_id"
        private const val ARG_SERVICE_TITLE = "arg_service_title"
        private const val ARG_SERVICE_PRICE = "arg_service_price"
        private const val ARG_SERVICE_DESC = "arg_service_desc"
        private const val ARG_USER = "arg_user"
        private const val ARG_LOGO_URL = "arg_logo_url"
        private const val ARG_ICON_RES = "arg_icon_res"

        fun newInstance(
            serviceId: String,
            title: String,
            price: String,
            desc: String,
            user: String,
            logoUrl: String? = null,
            iconRes: Int? = null
        ): PurchaseDialogFragment {
            val f = PurchaseDialogFragment()
            val bundle = Bundle().apply {
                putString(ARG_SERVICE_ID, serviceId)
                putString(ARG_SERVICE_TITLE, title)
                putString(ARG_SERVICE_PRICE, price)
                putString(ARG_SERVICE_DESC, desc)
                putString(ARG_USER, user)
                if (!logoUrl.isNullOrBlank()) putString(ARG_LOGO_URL, logoUrl)
                if (iconRes != null) putInt(ARG_ICON_RES, iconRes)
            }
            f.arguments = bundle
            return f
        }
    }

    private var serviceId: String = ""
    private var serviceTitle: String = ""
    private var servicePrice: String = ""
    private var serviceDesc: String = ""
    private var userFullname: String = ""
    private var logoUrl: String? = null
    private var iconResId: Int = 0

    // UI state
    private var selectedDurationMonths = 1
    private var deviceCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            serviceId = it.getString(ARG_SERVICE_ID, "")
            serviceTitle = it.getString(ARG_SERVICE_TITLE, "")
            servicePrice = it.getString(ARG_SERVICE_PRICE, "")
            serviceDesc = it.getString(ARG_SERVICE_DESC, "")
            userFullname = it.getString(ARG_USER, "")
            logoUrl = it.getString(ARG_LOGO_URL)
            iconResId = it.getInt(ARG_ICON_RES, 0)
        }
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog_Alert)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.dialog_purchase, container, false)

        val ivServiceLogo = root.findViewById<ImageView>(R.id.ivServiceLogo)
        val tvServiceName = root.findViewById<TextView>(R.id.tvServiceName)
        val tvServicePrice = root.findViewById<TextView>(R.id.tvServicePrice)
        val btnClose = root.findViewById<ImageButton>(R.id.btnClose)

        val btnDuration1 = root.findViewById<Button>(R.id.btnDuration1)
        val btnDuration2 = root.findViewById<Button>(R.id.btnDuration2)
        val btnDuration3 = root.findViewById<Button>(R.id.btnDuration3)
        val btnDuration4 = root.findViewById<Button>(R.id.btnDuration4)

        val btnDeviceMinus = root.findViewById<Button>(R.id.btnDeviceMinus)
        val btnDevicePlus = root.findViewById<Button>(R.id.btnDevicePlus)
        val tvDeviceCount = root.findViewById<TextView>(R.id.tvDeviceCount)

        val ivPayment1 = root.findViewById<ImageView>(R.id.ivPayment1)
        val ivPayment2 = root.findViewById<ImageView>(R.id.ivPayment2)
        val ivPayment3 = root.findViewById<ImageView>(R.id.ivPayment3)
        val ivPayment4 = root.findViewById<ImageView>(R.id.ivPayment4)
        val paymentCard1 = root.findViewById<LinearLayout>(R.id.paymentCard1)
        val paymentCard2 = root.findViewById<LinearLayout>(R.id.paymentCard2)
        val paymentCard3 = root.findViewById<LinearLayout>(R.id.paymentCard3)
        val paymentCard4 = root.findViewById<LinearLayout>(R.id.paymentCard4)

        val tvTotalAmount = root.findViewById<TextView>(R.id.tvTotalAmount)
        val btnCancel = root.findViewById<Button>(R.id.btnCancel)
        val btnComplete = root.findViewById<Button>(R.id.btnComplete)
        val tvImportantText = root.findViewById<TextView>(R.id.tvImportantText)

        // Set initial texts
        tvServiceName.text = serviceTitle
        tvServicePrice.text = servicePrice

        // Cargar logo si se suministró una URL; usar drawable por defecto si no
        fun setServiceLogoFallback(iv: ImageView, title: String): Boolean {
            // Primero, intentar usar el mapa determinista si existe entrada para el serviceId
            val mapped = LOGO_MAP[serviceId.lowercase(Locale.ROOT)]
            if (mapped != null) {
                try {
                    // background: preferir iconResId si está, sino buscar rounded_square_{serviceId}, sino fallback
                    if (iconResId != 0) {
                        iv.setBackgroundResource(iconResId)
                    } else {
                        val bgName = "rounded_square_${serviceId.lowercase(Locale.ROOT).replace("\\s+".toRegex(), "_")}"
                        val bgId = resources.getIdentifier(bgName, "drawable", requireContext().packageName)
                        if (bgId != 0) iv.setBackgroundResource(bgId) else iv.setBackgroundResource(R.drawable.rounded_square)
                    }
                    iv.setImageResource(mapped)
                    iv.visibility = View.VISIBLE
                    return true
                } catch (ex: Exception) {
                    // si falla, continuar con la heurística
                }
            }

            // Preferir el icono explícito pasado en el bundle (más robusto)
            if (iconResId != 0) {
                iv.setBackgroundResource(iconResId)
            } else {
                val key = title.lowercase(Locale.ROOT).replace("\\s+".toRegex(), "_")
                val bgResName = "rounded_square_$key"
                val bgResId = resources.getIdentifier(bgResName, "drawable", requireContext().packageName)
                if (bgResId != 0) iv.setBackgroundResource(bgResId) else iv.setBackgroundResource(R.drawable.rounded_square)
            }

            // Normalizar una cadena para buscar recursos: quitar acentos, reemplazar símbolos comunes
            fun normalizeName(input: String): String {
                var s = Normalizer.normalize(input, Normalizer.Form.NFD)
                s = s.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                s = s.lowercase(Locale.ROOT)
                // Reemplazar '+' por 'plus', '&' por 'and' y cualquier caracter no alfanumérico por '_'
                s = s.replace("+", "plus")
                s = s.replace("&", "and")
                s = s.replace("[^a-z0-9]+".toRegex(), "_")
                // limpiar guiones bajos iniciales/finales y duplicados
                s = s.replace("__+".toRegex(), "_")
                s = s.trim('_')
                return s
            }

            val candidates = mutableListOf<String>()
            // Añadir serviceId si existe (ya debería venir sin espacios)
            if (serviceId.isNotBlank()) {
                candidates.add(serviceId)
                // también añadir normalized serviceId
                candidates.add(normalizeName(serviceId))
                // añadir variante con prefijo logo_
                candidates.add("logo_$serviceId")
                // variante normalizada con logo_
                candidates.add("logo_${normalizeName(serviceId)}")
            }

            // versión simple del título (solo lowercase y espacios->underscore) para compatibilidad con nombres existentes
            val simpleTitleKey = title.lowercase(Locale.ROOT).replace("\\s+".toRegex(), "_")

            val titleKey = normalizeName(title)
            // variantes útiles: base, with logo_ prefix, with _standard/_premium/_hd suffixes
            val suffixes = listOf("", "_standard", "_premium", "_hd", "_plus")

            // PRE-COMPUTE: variantes sin _plus para usar cuando el drawable está nombrado sin la palabra 'plus'
            val simpleTitleKeyNoPlus = simpleTitleKey.replace("_plus", "").replace("__+".toRegex(), "_").trim('_')
            val titleKeyNoPlus = titleKey.replace("_plus", "").replace("__+".toRegex(), "_").trim('_')

            for (suf in suffixes) {
                // usar interpolación Kotlin correcta
                candidates.add("logo_${titleKey}${suf}")
                candidates.add("${titleKey}${suf}")
                candidates.add("logo_${simpleTitleKey}${suf}")
                candidates.add("${simpleTitleKey}${suf}")
            }

            // Intentar además variaciones donde 'plus' se mantenga como palabra separada
            if (titleKey.contains("plus")) {
                val alt = titleKey.replace("plus", "_plus").replace("__+".toRegex(), "_")
                candidates.add("logo_${alt}")
                candidates.add(alt)
                // también intentar sin la palabra plus (ej. 'paramount' para 'Paramount+')
                val withoutPlus = titleKeyNoPlus
                if (withoutPlus.isNotBlank()) {
                    candidates.add("logo_${withoutPlus}")
                    candidates.add(withoutPlus)
                    if (simpleTitleKeyNoPlus.isNotBlank()) {
                        candidates.add("logo_${simpleTitleKeyNoPlus}")
                        candidates.add(simpleTitleKeyNoPlus)
                    }
                }
            }

            var logoSet = false
            val tried = mutableSetOf<String>()
            for (name in candidates) {
                if (name.isBlank()) continue
                if (tried.contains(name)) continue
                tried.add(name)
                val logoId = resources.getIdentifier(name, "drawable", requireContext().packageName)
                if (logoId != 0) {
                    try {
                        iv.setImageResource(logoId)
                        logoSet = true
                        break
                    } catch (ex: Exception) {
                        iv.setImageDrawable(null)
                    }
                }
            }

            // Mantener parche para Netflix por compatibilidad si aún no se encontró
            if (!logoSet && serviceId.lowercase(Locale.ROOT) == "netflix") {
                try {
                    iv.setBackgroundResource(R.drawable.rounded_square_netflix)
                    iv.setImageResource(R.drawable.logo_netflix)
                    logoSet = true
                } catch (ex: Exception) {
                    // ignore
                }
            }

            if (!logoSet) iv.setImageDrawable(null)
            Log.d("PurchaseDialogFragment", "setServiceLogoFallback: bgId=${iconResId}, serviceId=${serviceId}, titleKey=${titleKey}, logoFound=${logoSet}")
            iv.visibility = View.VISIBLE
            return logoSet
        }

        if (!logoUrl.isNullOrBlank()) {
            try {
                ivServiceLogo.load(logoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.rounded_square)
                    error(R.drawable.rounded_square)
                }
            } catch (ex: Exception) {
                Log.w("PurchaseDialogFragment", "No se pudo cargar logo: ${ex.message}")
                setServiceLogoFallback(ivServiceLogo, serviceTitle)
            }
        } else {
            setServiceLogoFallback(ivServiceLogo, serviceTitle)
        }

        // Copy-to-clipboard helper
        fun copyToClipboard(label: String, value: String) {
            try {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, value)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "$label copiado", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Log.e("PurchaseDialogFragment", "Error copiando al portapapeles", ex)
                Toast.makeText(requireContext(), "No se pudo copiar", Toast.LENGTH_SHORT).show()
            }
        }

        // Assign click listeners on payment cards to copy account info
        paymentCard1.setOnClickListener {
            copyToClipboard("Cuenta Banco Pichincha", "2209034638")
        }
        paymentCard2.setOnClickListener {
            copyToClipboard("Cuenta Banco Guayaquil", "0122407273")
        }
        paymentCard3.setOnClickListener {
            copyToClipboard("Cuenta Banco Pacífico", "1061220256")
        }
        paymentCard4.setOnClickListener {
            copyToClipboard("PayPal Email", "guale2023@outlook.com")
        }

        // Duration buttons behavior
        val durationButtons = mapOf(
            1 to btnDuration1,
            2 to btnDuration2,
            3 to btnDuration3,
            6 to btnDuration4
        )

        fun updateDurationSelection() {
            durationButtons.forEach { (months, button) ->
                if (months == selectedDurationMonths) {
                    button.setBackgroundResource(R.drawable.btn_gradient)
                } else {
                    button.setBackgroundResource(R.drawable.btn_outline)
                }
            }
            // update total
            tvTotalAmount.text = formatTotal(calculateTotal())
        }

        btnDuration1.setOnClickListener { selectedDurationMonths = 1; updateDurationSelection() }
        btnDuration2.setOnClickListener { selectedDurationMonths = 2; updateDurationSelection() }
        btnDuration3.setOnClickListener { selectedDurationMonths = 3; updateDurationSelection() }
        btnDuration4.setOnClickListener { selectedDurationMonths = 6; updateDurationSelection() }

        // Device count behavior
        fun updateDeviceCountText() {
            // Mostrar número grande y etiqueta debajo (usar plurals)
            val label = resources.getQuantityString(R.plurals.device_count, deviceCount, deviceCount)
            val sb = SpannableStringBuilder()
            val numberStr = deviceCount.toString()
            sb.append(numberStr)
            val numberEnd = sb.length
            sb.append("\n")
            val labelStart = sb.length
            sb.append(label)
            val labelEnd = sb.length
            // agrandar el número y reducir tamaño de la etiqueta
            // Ajuste: aumentar contraste — número más grande y etiqueta ligeramente más pequeña
            sb.setSpan(RelativeSizeSpan(1.8f), 0, numberEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(0.9f), labelStart, labelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvDeviceCount.text = sb
            tvTotalAmount.text = formatTotal(calculateTotal())
        }

        btnDeviceMinus.setOnClickListener {
            if (deviceCount > 1) deviceCount--
            updateDeviceCountText()
        }
        btnDevicePlus.setOnClickListener {
            if (deviceCount < 5) deviceCount++
            updateDeviceCountText()
        }

        // Cancel / Close
        btnClose.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }

        // Complete purchase -> ACTUALIZADO: guardar compra real
        btnComplete.setOnClickListener {
            try {
                // Obtener email del usuario desde SharedPreferences
                val sharedPrefs = requireContext().getSharedPreferences("StreamZoneData", Context.MODE_PRIVATE)
                val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

                // Calcular duración en texto legible
                val selectedDuration = "$selectedDurationMonths mes${if (selectedDurationMonths > 1) "es" else ""}"

                // Mostrar diálogo de confirmación con datos reales
                val completeDlg = PurchaseCompleteDialogFragment.newInstance(
                    serviceId = serviceId,
                    serviceName = serviceTitle,
                    servicePrice = tvTotalAmount.text.toString(),
                    duration = selectedDuration,
                    userName = userFullname,
                    userEmail = userEmail
                )
                completeDlg.show(parentFragmentManager, "purchaseCompleteDialog")
            } catch (ex: Exception) {
                Log.e("PurchaseDialogFragment", "Error al mostrar diálogo de compra", ex)
                Toast.makeText(requireContext(), "Error al procesar compra", Toast.LENGTH_SHORT).show()
            }
            // Cerrar el diálogo de compra actual
            dismiss()
        }

        // helper: crear un bitmap con un emoji (fallback si no hay drawable)
        fun createEmojiBitmap(emoji: String, sizePx: Int = 64): Bitmap {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textSize = sizePx * 0.6f
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val bounds = Rect()
            paint.getTextBounds(emoji, 0, emoji.length, bounds)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.TRANSPARENT)
            val x = (sizePx - bounds.width()) / 2f - bounds.left
            val y = (sizePx + bounds.height()) / 2f - bounds.bottom
            canvas.drawText(emoji, x, y, paint)
            return bmp
        }

        // Cargar logos pequeños en tarjetas de pago si existen en res/drawable; si no, usar emoji fallback
        fun tryLoadPaymentLogo(iv: ImageView, emojiFallback: String, vararg candidates: String) {
            var loaded = false
            // padding en px (8dp)
            val pad = (8 * resources.displayMetrics.density).toInt()
            for (name in candidates) {
                if (name.isBlank()) continue
                val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
                if (resId != 0) {
                    try {
                        // Si el recurso encontrado es del tipo "logo_*", usarlo como background para conservar su gradiente/forma
                        if (name.startsWith("logo_")) {
                            iv.setBackgroundResource(resId)
                            iv.setPadding(pad, pad, pad, pad)
                            val defaultIcon = when (iv.id) {
                                R.id.ivPayment4 -> R.drawable.ic_bill
                                else -> R.drawable.ic_bank
                            }
                            iv.setImageResource(defaultIcon)
                        } else {
                            // recurso normal: poner como src sobre fondo redondeado
                            val bgRes = when (iv.id) {
                                R.id.ivPayment1, R.id.ivPayment2, R.id.ivPayment3, R.id.ivPayment4 -> R.drawable.rounded_circle
                                else -> R.drawable.rounded_square
                            }
                            iv.setBackgroundResource(bgRes)
                            iv.setPadding(pad, pad, pad, pad)
                            iv.setImageResource(resId)
                        }
                    } catch (ex: Exception) {
                        // fallback a Coil si setImageResource falla
                        try {
                            val bgRes = when (iv.id) {
                                R.id.ivPayment1, R.id.ivPayment2, R.id.ivPayment3, R.id.ivPayment4 -> R.drawable.rounded_circle
                                else -> R.drawable.rounded_square
                            }
                            iv.setBackgroundResource(bgRes)
                            iv.setPadding(pad, pad, pad, pad)
                            iv.load(resId) {
                                placeholder(bgRes)
                                error(bgRes)
                            }
                        } catch (e: Exception) {
                            iv.setImageResource(R.drawable.rounded_square)
                        }
                    }
                    loaded = true
                    break
                }
            }
            if (!loaded) {
                // fallback: renderizar emoji dentro de un bitmap y mostrar
                try {
                    val bmp = createEmojiBitmap(emojiFallback, 56)
                    iv.setImageBitmap(bmp)
                    val bgRes = when (iv.id) {
                        R.id.ivPayment1, R.id.ivPayment2, R.id.ivPayment3, R.id.ivPayment4 -> R.drawable.rounded_circle
                        else -> R.drawable.rounded_square
                    }
                    iv.setBackgroundResource(bgRes)
                    iv.setPadding(pad, pad, pad, pad)
                    loaded = true
                } catch (ex: Exception) {
                    // nada
                }
            }
            iv.visibility = if (loaded) View.VISIBLE else View.GONE
        }

        // nombres candidatos (ajusta si tus assets tienen otros nombres); si no hay drawable se mostrará emoji
        tryLoadPaymentLogo(ivPayment1, "🏦", "logo_pichincha", "pichincha", "bank_pichincha")
        tryLoadPaymentLogo(ivPayment2, "🏦", "logo_guayaquil", "guayaquil")
        tryLoadPaymentLogo(ivPayment3, "🏦", "logo_pacifico", "pacifico")
        tryLoadPaymentLogo(ivPayment4, "💸", "logo_paypal", "paypal")

        // Formatear el texto de instrucciones importantes: poner en negrita "debes confirmar tu compra"
        val importantFull = getString(R.string.texto_instrucciones)
        val span = SpannableString(importantFull)
        val boldPhrase = "debes confirmar tu compra"
        val start = importantFull.indexOf(boldPhrase)
        if (start >= 0) {
            span.setSpan(StyleSpan(Typeface.BOLD), start, start + boldPhrase.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        tvImportantText.text = span

        // initialize UI
        updateDurationSelection()
        updateDeviceCountText()

        return root
    }

    private fun calculateTotal(): Double {
        val unit = parsePriceToDouble(servicePrice)
        // total = unit * months * devices (assuming price is per device/month)
        return unit * selectedDurationMonths * deviceCount
    }

    private fun parsePriceToDouble(priceStr: String): Double {
        if (priceStr.isBlank()) return 0.0
        // eliminar símbolos y textos como "US$" y "/mes" y dejar números y separadores
        var s = priceStr.replace("US$", "", ignoreCase = true)
        s = s.replace("/mes", "", ignoreCase = true)
        s = s.replace("/año", "", ignoreCase = true)
        s = s.replace("/a\u0000o", "", ignoreCase = true)
        s = s.replace("USD", "", ignoreCase = true)
        s = s.replace(" ", "")
        // permitir coma decimal
        s = s.replace("[^0-9,.]".toRegex(), "")
        // si hay coma y no punto, reemplazar coma por punto
        s = if (s.contains(",") && !s.contains(".")) s.replace(",", ".") else s.replace(",", "")
        return try {
            s.toDouble()
        } catch (ex: Exception) {
            Log.e("PurchaseDialogFragment", "Error parseando precio: $priceStr", ex)
            0.0
        }
    }

    private fun formatTotal(value: Double): String {
        val formatted = String.format(Locale.US, "%.2f", value).replace('.', ',')
        return "US$ $formatted"
    }
}