package com.example.petsocial.ui.Fotos

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import kotlin.math.abs

class GaleriaImagen : Fragment() {

    // ---------- Views ----------
    private lateinit var galleryGrid: GridLayout
    private lateinit var btnAddImages: Button
    private lateinit var tvGalleryCounter: TextView
    private lateinit var scrollView: ScrollView
    private var imageViewerOverlay: FrameLayout? = null

    // ---------- Estado ----------
    private val imageUris = mutableListOf<Uri>()
    private lateinit var sharedPrefs: android.content.SharedPreferences
    private val PREFS_NAME = "GalleryPrefs"
    private val KEY_IMAGE_URIS = "saved_image_uris"

    // Visor de imágenes
    private var currentImageIndex = 0
    private var imageViewInViewer: ImageView? = null
    private var tvImageCounter: TextView? = null
    private var btnPrevImage: Button? = null
    private var btnNextImage: Button? = null

    // Detección de swipe
    private var startX = 0f
    private var startY = 0f
    private var isSwipeDetected = false

    // ---------- Colores UI ----------
    private val TEXT_DARK = Color.parseColor("#222222")
    private val LABEL_COLOR = Color.parseColor("#555555")
    private val HINT_COLOR = Color.parseColor("#9E9E9E")
    private val FIELD_BG = Color.parseColor("#FAFAFA")
    private val BG_WHITE = Color.parseColor("#FFFFFF")
    private val ACCENT_COLOR = Color.parseColor("#2196F3")
    private val CARD_SHADOW = Color.parseColor("#15000000")

    // Selector múltiple de imágenes
    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
            }
            addMultipleImagesToGallery(uris)
        } else {
            Toast.makeText(requireContext(), "No se seleccionaron imágenes", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- UI ----------
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        loadSavedImages()

        scrollView = ScrollView(requireContext()).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
            setBackgroundColor(BG_WHITE)
        }
        scrollView.addView(content)

        // Título principal
        content.addView(TextView(requireContext()).apply {
            text = "Galería de Fotos"
            textSize = 22f
            setTextColor(TEXT_DARK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })

        content.addView(TextView(requireContext()).apply {
            text = "Galeria de imágenes PetSocial"
            textSize = 15f
            setTextColor(HINT_COLOR)
            setPadding(0, 0, 0, dp(20))
        })

        // Header de galería con contador
        val galleryHeader = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(16))
        }
        content.addView(galleryHeader)

        galleryHeader.addView(TextView(requireContext()).apply {
            text = "Mis Fotos"
            textSize = 18f
            setTextColor(LABEL_COLOR)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        tvGalleryCounter = TextView(requireContext()).apply {
            text = "📸 0"
            textSize = 15f
            setTextColor(ACCENT_COLOR)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(Color.parseColor("#E3F2FD"))
            gravity = Gravity.CENTER
        }
        galleryHeader.addView(tvGalleryCounter)

        // Grid de imágenes mejorado
        galleryGrid = GridLayout(requireContext()).apply {
            columnCount = 3
            rowCount = GridLayout.UNDEFINED
            setPadding(dp(8))
            setBackgroundColor(FIELD_BG)
        }
        content.addView(galleryGrid, matchWrap())

        // Botón para agregar imágenes
        btnAddImages = Button(requireContext()).apply {
            text = "➕ Agregar Imágenes"
            setBackgroundColor(ACCENT_COLOR)
            setTextColor(Color.WHITE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                pickImages.launch(arrayOf("image/*"))
            }
        }
        content.addView(btnAddImages, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(16)
            bottomMargin = dp(20)
        })

        refreshGallery()

        return scrollView
    }

    // ---------- Gallery Functions ----------
    private fun loadSavedImages() {
        val savedUrisString = sharedPrefs.getString(KEY_IMAGE_URIS, "") ?: ""
        if (savedUrisString.isNotEmpty()) {
            val uriStrings = savedUrisString.split("|")
            imageUris.clear()
            uriStrings.forEach { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    imageUris.add(uri)
                } catch (e: Exception) { }
            }
        }
    }

    private fun saveImages() {
        val urisString = imageUris.joinToString("|") { it.toString() }
        sharedPrefs.edit().putString(KEY_IMAGE_URIS, urisString).apply()
    }

    private fun addMultipleImagesToGallery(uris: List<Uri>) {
        if (uris.isEmpty()) {
            Toast.makeText(requireContext(), "Error: No hay imágenes para cargar", Toast.LENGTH_SHORT).show()
            return
        }

        imageUris.addAll(uris)
        saveImages()
        refreshGallery()

        val message = if (uris.size == 1) {
            "✓ 1 imagen agregada"
        } else {
            "✓ ${uris.size} imágenes agregadas"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        updateGalleryCounter()

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun updateGalleryCounter() {
        tvGalleryCounter.text = "📸 ${imageUris.size}"
    }

    private fun refreshGallery() {
        galleryGrid.removeAllViews()

        if (imageUris.isEmpty()) {
            showEmptyState()
            return
        }

        imageUris.forEachIndexed { index, uri ->
            val container = FrameLayout(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(140)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                setBackgroundColor(Color.WHITE)
                elevation = dp(4).toFloat()

                // Efecto visual mejorado al presionar
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.alpha = 0.85f
                            v.scaleX = 0.96f
                            v.scaleY = 0.96f
                            v.elevation = dp(8).toFloat()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.alpha = 1f
                            v.scaleX = 1f
                            v.scaleY = 1f
                            v.elevation = dp(4).toFloat()
                        }
                    }
                    false
                }
            }

            val imageView = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
                clipToOutline = true
                setOnClickListener {
                    showImageViewer(index)
                }
            }
            container.addView(imageView)

            // Overlay gradient sutil
            val overlay = View(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    dp(60)
                ).apply {
                    gravity = Gravity.TOP
                }
                background = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.parseColor("#AA000000"), Color.TRANSPARENT)
                )
            }
            container.addView(overlay)

            // Botón de eliminar sin fondo - solo X
            val btnDelete = TextView(requireContext()).apply {
                text = "✕"
                textSize = 20f
                layoutParams = FrameLayout.LayoutParams(
                    dp(36),
                    dp(36)
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                setTextColor(Color.WHITE)
                setPadding(0)
                gravity = Gravity.CENTER
                elevation = dp(3).toFloat()
                setTypeface(null, android.graphics.Typeface.BOLD)

                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.alpha = 0.6f
                            v.scaleX = 0.9f
                            v.scaleY = 0.9f
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.alpha = 1f
                            v.scaleX = 1f
                            v.scaleY = 1f
                            if (event.action == MotionEvent.ACTION_UP) {
                                removeImage(index)
                            }
                        }
                    }
                    true
                }
            }
            container.addView(btnDelete)

            // Badge de número mejorado
            val tvNumber = TextView(requireContext()).apply {
                text = "${index + 1}"
                textSize = 11f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setBackgroundColor(ACCENT_COLOR)
                gravity = Gravity.CENTER
                elevation = dp(2).toFloat()
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
            }
            container.addView(tvNumber)

            galleryGrid.addView(container)
        }
    }

    private fun showEmptyState() {
        val emptyContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(40))
            layoutParams = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.MATCH_PARENT
                columnSpec = GridLayout.spec(0, 3)
            }
        }

        emptyContainer.addView(TextView(requireContext()).apply {
            text = "🖼️"
            textSize = 64f
            gravity = Gravity.CENTER
        })

        emptyContainer.addView(TextView(requireContext()).apply {
            text = "No hay imágenes"
            textSize = 18f
            setTextColor(LABEL_COLOR)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        })

        emptyContainer.addView(TextView(requireContext()).apply {
            text = "Toca el botón de abajo para agregar\nmúltiples imágenes a la vez"
            textSize = 14f
            setTextColor(HINT_COLOR)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        galleryGrid.addView(emptyContainer)
    }

    private fun removeImage(index: Int) {
        if (index in imageUris.indices) {
            imageUris.removeAt(index)
            saveImages()
            refreshGallery()
            updateGalleryCounter()
            Toast.makeText(requireContext(), "✓ Imagen eliminada", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Visor de Imágenes con Navegación ----------
    private fun showImageViewer(startIndex: Int) {
        if (imageUris.isEmpty()) return

        currentImageIndex = startIndex

        imageViewerOverlay = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F8000000"))
            elevation = 100f
            isClickable = true
            isFocusable = true
        }

        val contentContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
        }

        // Barra superior
        val topBar = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(Color.parseColor("#E5000000"))
            elevation = dp(4).toFloat()
            isClickable = true
        }

        tvImageCounter = TextView(requireContext()).apply {
            text = "Imagen ${currentImageIndex + 1} de ${imageUris.size}"
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(tvImageCounter)

        // Botón cerrar limpio - solo X sin fondo
        topBar.addView(TextView(requireContext()).apply {
            text = "✕"
            textSize = 28f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                setMargins(dp(12), 0, 0, 0)
            }
            setPadding(0)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.alpha = 0.6f
                        v.scaleX = 0.9f
                        v.scaleY = 0.9f
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.alpha = 1f
                        v.scaleX = 1f
                        v.scaleY = 1f
                        if (event.action == MotionEvent.ACTION_UP) {
                            closeImageViewer()
                        }
                    }
                }
                true
            }
        })

        contentContainer.addView(topBar)

        // Espacio superior
        contentContainer.addView(Space(requireContext()), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.5f
        ))

        // Container de la imagen con detección de swipe
        val imageContainer = FrameLayout(requireContext()).apply {
            val screenHeight = resources.displayMetrics.heightPixels
            val maxImageHeight = (screenHeight * 0.65).toInt()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxImageHeight
            )
            setPadding(dp(20), 0, dp(20), 0)
        }

        imageViewInViewer = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            elevation = dp(4).toFloat()

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        isSwipeDetected = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.x - startX
                        val deltaY = event.y - startY

                        if (!isSwipeDetected && abs(deltaX) > abs(deltaY) && abs(deltaX) > dp(50)) {
                            isSwipeDetected = true
                            if (deltaX > 0) {
                                navigateToPreviousImage()
                            } else {
                                navigateToNextImage()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isSwipeDetected = false
                        true
                    }
                    else -> false
                }
            }
        }
        imageContainer.addView(imageViewInViewer)

        // Botones de navegación overlay
        if (imageUris.size > 1) {
            btnPrevImage = Button(requireContext()).apply {
                text = "◀"
                textSize = 24f
                layoutParams = FrameLayout.LayoutParams(dp(56), dp(56)).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setMargins(dp(8), 0, 0, 0)
                }
                setBackgroundColor(Color.parseColor("#AA000000"))
                setTextColor(Color.WHITE)
                elevation = dp(6).toFloat()
                setOnClickListener { navigateToPreviousImage() }
            }
            imageContainer.addView(btnPrevImage)

            btnNextImage = Button(requireContext()).apply {
                text = "▶"
                textSize = 24f
                layoutParams = FrameLayout.LayoutParams(dp(56), dp(56)).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    setMargins(0, 0, dp(8), 0)
                }
                setBackgroundColor(Color.parseColor("#AA000000"))
                setTextColor(Color.WHITE)
                elevation = dp(6).toFloat()
                setOnClickListener { navigateToNextImage() }
            }
            imageContainer.addView(btnNextImage)
        }

        contentContainer.addView(imageContainer)

        // Espacio inferior
        contentContainer.addView(Space(requireContext()), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.5f
        ))

        // Barra inferior con opciones
        val bottomBar = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#E5000000"))
            elevation = dp(4).toFloat()
            isClickable = true
        }

       contentContainer.addView(bottomBar)

        imageViewerOverlay?.addView(contentContainer)
        (requireActivity().window.decorView as ViewGroup).addView(imageViewerOverlay)

        updateImageInViewer()
        updateNavigationButtons()
    }

    private fun navigateToPreviousImage() {
        if (currentImageIndex > 0) {
            currentImageIndex--
            updateImageInViewer()
            updateNavigationButtons()
        }
    }

    private fun navigateToNextImage() {
        if (currentImageIndex < imageUris.size - 1) {
            currentImageIndex++
            updateImageInViewer()
            updateNavigationButtons()
        }
    }

    private fun updateImageInViewer() {
        if (currentImageIndex in imageUris.indices) {
            imageViewInViewer?.setImageURI(imageUris[currentImageIndex])
            tvImageCounter?.text = "Imagen ${currentImageIndex + 1} de ${imageUris.size}"
        }
    }

    private fun updateNavigationButtons() {
        btnPrevImage?.isEnabled = currentImageIndex > 0
        btnPrevImage?.alpha = if (currentImageIndex > 0) 1f else 0.3f

        btnNextImage?.isEnabled = currentImageIndex < imageUris.size - 1
        btnNextImage?.alpha = if (currentImageIndex < imageUris.size - 1) 1f else 0.3f
    }

    private fun closeImageViewer() {
        imageViewerOverlay?.let { overlay ->
            (requireActivity().window.decorView as ViewGroup).removeView(overlay)
        }
        imageViewerOverlay = null
        imageViewInViewer = null
        tvImageCounter = null
        btnPrevImage = null
        btnNextImage = null
    }

    // ---------- Helpers UI ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(4) }

    // ---------- Lifecycle ----------
    override fun onDestroyView() {
        super.onDestroyView()
        closeImageViewer()
    }
}