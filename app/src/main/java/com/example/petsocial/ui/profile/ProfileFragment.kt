package com.example.petsocial.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.petsocial.data.AppDatabase
import com.example.petsocial.data.PetProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    // ---------- Views (vínculo de variables) ----------
    private lateinit var photo: ImageView
    private lateinit var btnPick: Button
    private lateinit var btnCancelPhoto: Button
    private lateinit var nameEt: EditText
    private lateinit var breedEt: EditText
    private lateinit var ageEt: EditText
    private lateinit var interestsEt: EditText
    private lateinit var btnSave: Button

    // ---------- Estado de foto ----------
    private var savedPhotoUri: String? = null     // lo que está en BD
    private var pendingPhotoUri: String? = null   // selección temporal hasta "Guardar"

    // ---------- DAO ----------
    private val dao by lazy { AppDatabase.get(requireContext()).petProfileDao() }

    // ---------- Colores UI ----------
    private val TEXT_DARK   = Color.parseColor("#222222")
    private val LABEL_COLOR = Color.parseColor("#555555")
    private val HINT_COLOR  = Color.parseColor("#9E9E9E")
    private val FIELD_BG    = Color.parseColor("#F2F2F6")

    // ---------- Placeholder en assets (silueta "huella") ----------
    private val PET_PLACEHOLDER = "icons/huella.png"

    // Selector de imagen con permiso persistente
    private val pickImage = registerForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* ya otorgado o no requerido */ }

            pendingPhotoUri = uri.toString()        // aún no se guarda en BD
            setPhotoFromUriString(pendingPhotoUri!!) // previsualiza como "foto real"
            btnCancelPhoto.visibility = View.VISIBLE
        }
    }

    // ---------- UI ----------
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Contenedor con scroll
        val scroll = ScrollView(requireContext()).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
        }
        scroll.addView(content)

        // Título del fragmento (opcional)
        content.addView(TextView(requireContext()).apply {
            text = "Perfil"
            textSize = 22f
            setTextColor(LABEL_COLOR)
            setPadding(0, 0, 0, dp(8))
        })

        // Avatar + botones
        photo = ImageView(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96))
        }
        btnPick = Button(requireContext()).apply {
            text = "Cambiar foto"
            setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        }
        btnCancelPhoto = Button(requireContext()).apply {
            text = "Cancelar"
            visibility = View.GONE
            setOnClickListener { cancelPhotoChange() }
        }
        content.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(photo)
            addView(Space(requireContext()), LinearLayout.LayoutParams(dp(12), dp(12)))
            addView(btnPick)
            addView(Space(requireContext()), LinearLayout.LayoutParams(dp(8), dp(8)))
            addView(btnCancelPhoto)
        })

        // Labels + campos
        content.addView(makeLabel("Nombre"))
        nameEt = makeField("Ingresa el nombre", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        content.addView(nameEt, matchWrap())

        content.addView(makeLabel("Raza"))
        breedEt = makeField("Ej: Labrador", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        content.addView(breedEt, matchWrap())

        content.addView(makeLabel("Edad"))
        ageEt = makeField("Ej: 3", InputType.TYPE_CLASS_NUMBER)
        content.addView(ageEt, matchWrap())

        content.addView(makeLabel("Intereses"))
        interestsEt = makeField(
            hint = "Jugar, paseos, otros…",
            inputType = InputType.TYPE_CLASS_TEXT,
            multi = true,
            minLines = 4,
            maxLines = 8
        )
        content.addView(interestsEt, matchWrap())

        // Botón Guardar
        btnSave = Button(requireContext()).apply {
            text = "Guardar"
            setOnClickListener { onSave() }
        }
        content.addView(btnSave, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        // Placeholder por defecto hasta que lleguen datos
        showPlaceholder()

        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargar datos actuales y poblar UI
        viewLifecycleOwner.lifecycleScope.launch {
            dao.watch().collectLatest { p ->
                if (p == null) return@collectLatest

                savedPhotoUri = p.photoUri
                pendingPhotoUri = null

                nameEt.setText(p.name)
                breedEt.setText(p.breed ?: "")
                ageEt.setText(p.age?.toString() ?: "")
                interestsEt.setText(p.interests ?: "")

                if (savedPhotoUri != null) {
                    val u = Uri.parse(savedPhotoUri)
                    try {
                        requireContext().contentResolver.takePersistableUriPermission(
                            u, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) { /* ok */ }
                    setPhotoFromUriString(savedPhotoUri!!)
                } else {
                    showPlaceholder()
                }
                btnCancelPhoto.visibility = View.GONE
            }
        }

        // Si no hay registro aún, crea uno vacío para editar
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (dao.getOnce() == null) {
                dao.upsert(
                    PetProfileEntity(
                        id = 1, photoUri = null, name = "", breed = null, age = null, interests = null
                    )
                )
            }
        }
    }

    // ---------- Guardar ----------
    private fun onSave() {
        val name = nameEt.text.toString().trim()
        val breed = breedEt.text.toString().trim().ifEmpty { null }
        val age = ageEt.text.toString().trim().toIntOrNull()
        val interests = interestsEt.text.toString().trim().ifEmpty { null }

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val finalPhoto = pendingPhotoUri ?: savedPhotoUri
        val entity = PetProfileEntity(
            id = 1,
            photoUri = finalPhoto,
            name = name,
            breed = breed,
            age = age,
            interests = interests
        )

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            dao.upsert(entity)
            withContext(Dispatchers.Main) {
                savedPhotoUri = finalPhoto
                pendingPhotoUri = null
                btnCancelPhoto.visibility = View.GONE
                Toast.makeText(requireContext(), "Perfil guardado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Acciones auxiliares ----------
    private fun cancelPhotoChange() {
        pendingPhotoUri = null
        if (savedPhotoUri != null) setPhotoFromUriString(savedPhotoUri!!)
        else showPlaceholder()
        btnCancelPhoto.visibility = View.GONE
    }

    // ---------- Helpers UI ----------
    private fun makeLabel(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 14f
        setTextColor(LABEL_COLOR)
        setPadding(0, dp(12), 0, dp(6))
    }

    private fun makeField(
        hint: String,
        inputType: Int,
        multi: Boolean = false,
        minLines: Int = 1,
        maxLines: Int = 1
    ) = EditText(requireContext()).apply {
        id = View.generateViewId()
        this.hint = hint
        this.inputType = inputType
        setTextColor(TEXT_DARK)
        setHintTextColor(HINT_COLOR)
        textSize = 16f
        setPadding(dp(12))
        background = null
        setBackgroundColor(FIELD_BG)
        if (multi) {
            isSingleLine = false
            setMinLines(minLines)
            setMaxLines(maxLines)
            gravity = Gravity.TOP
        } else {
            isSingleLine = true
            setMaxLines(1)
        }
        layoutParams = matchWrap()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(4) }

    // ---------- Helpers avatar/placeholder ----------
    private fun loadAssetBitmap(path: String): Bitmap? =
        runCatching { requireContext().assets.open(path).use(BitmapFactory::decodeStream) }.getOrNull()

    private fun applyPhotoStyle(isPlaceholder: Boolean) {
        photo.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#EEEEEE"))
        }
        photo.clipToOutline = true
        if (isPlaceholder) {
            photo.scaleType = ImageView.ScaleType.CENTER_INSIDE
            photo.setPadding(dp(12))
        } else {
            photo.scaleType = ImageView.ScaleType.CENTER_CROP
            photo.setPadding(0)
        }
    }

    private fun showPlaceholder() {
        val bmp = loadAssetBitmap(PET_PLACEHOLDER)
        if (bmp != null) photo.setImageBitmap(bmp)
        else photo.setImageResource(android.R.drawable.sym_def_app_icon)
        applyPhotoStyle(true)
    }

    private fun setPhotoFromUriString(uriStr: String) {
        photo.setImageURI(Uri.parse(uriStr))
        applyPhotoStyle(false)
    }

    // ---------- Utils ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
