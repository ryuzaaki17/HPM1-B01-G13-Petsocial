package com.example.petsocial.ui.main


import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.doOnLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.petsocial.ui.Fotos.GaleriaImagen
import com.example.petsocial.ui.profile.ProfileFragment
import com.google.android.material.appbar.MaterialToolbar
import java.io.InputStream
import com.example.petsocial.ui.video.WebVideo
import com.example.petsocial.ui.post.PostFragment


class MainActivity : AppCompatActivity() {

    // -------- Views
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var contentFrame: FrameLayout
    private lateinit var list: ListView
    private lateinit var sideContainer: FrameLayout
    private val contentId = View.generateViewId()

    // -------- Colores
    private val ORANGE = Color.parseColor("#FFA45B")
    private val WHITE = Color.WHITE

    // -------- Desplazamiento del contenido (se calcula en tiempo de ejecución)
    private var contentShiftPx = 0

    // -------- Modelo de menú
    data class MenuItemM(
        val title: String,
        val assetIcon: String? = null,
        val emoji: String = "🐾",
        val screen: () -> Fragment
    )

    private val items = listOf(
        MenuItemM("Perfil", "icons/perfil.png", "👤") { ProfileFragment()  },
        MenuItemM("Fotos",  "icons/fotos.png",  "🖼️") { GaleriaImagen() },
        MenuItemM("Videos", "icons/videos.png", "▶️") { WebVideo() },
        MenuItemM("Web",    "icons/web.png",    "🌐") { Placeholder("Web (placeholder)") },
        MenuItemM("Muro",  "icons/subir.png",  "⬆️") { PostFragment() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== Drawer raíz
        drawer = DrawerLayout(this)

        // ===== Toolbar: título centrado, naranja y en negrita
        val toolbar = MaterialToolbar(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTitleTextColor(ORANGE)
            setTitleCentered(true)
        }
        val boldTitle = android.text.SpannableString("Petsocial").apply {
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        toolbar.title = boldTitle
        setSupportActionBar(toolbar)

        // Botón para fijar / desfijar el menú (siempre visible)
        val pinItem = toolbar.menu.add("📌").apply {
            // muéstralo SIEMPRE como acción (no en el menú de 3 puntos)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            // opcional: usar un icono (por si en algún dispositivo no muestra texto)
            icon = textAsBitmapDrawable("📌", 18f, ORANGE)
        }

        toolbar.setOnMenuItemClickListener { item ->
            if (item == pinItem) {
                isPinned = !isPinned
                if (isPinned) {
                    drawer.openDrawer(GravityCompat.START)
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.START)
                    (contentFrame.layoutParams as LinearLayout.LayoutParams).apply {
                        marginStart = contentShiftPx
                        contentFrame.layoutParams = this
                    }
                    // opcional: cambia icono cuando está fijado
                    pinItem.icon = textAsBitmapDrawable("📍", 18f, ORANGE)
                } else {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
                    drawer.closeDrawer(GravityCompat.START)
                    (contentFrame.layoutParams as LinearLayout.LayoutParams).apply {
                        marginStart = 0
                        contentFrame.layoutParams = this
                    }
                    pinItem.icon = textAsBitmapDrawable("📌", 18f, ORANGE)
                }
                true
            } else false
        }



        // ===== Columna: toolbar + contenedor de contenido
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contentFrame = FrameLayout(this).apply { id = contentId }
        column.addView(toolbar, LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        column.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        // ===== Lateral (solo esquinas derechas redondeadas)
        sideContainer = FrameLayout(this).apply {
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.WRAP_CONTENT, // ancho se define luego
                DrawerLayout.LayoutParams.MATCH_PARENT,
                GravityCompat.START
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(ORANGE)
                cornerRadii = floatArrayOf(
                    0f, 0f,               // top-left plano
                    rdp(24f), rdp(24f),   // top-right redondo
                    rdp(24f), rdp(24f),   // bottom-right redondo
                    0f, 0f                // bottom-left plano
                )
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        // ===== ListView con adaptador (icono + texto + resaltado)
        list = ListView(this).apply {
            divider = null
            setBackgroundColor(Color.TRANSPARENT)
            adapter = MenuAdapter(items)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        sideContainer.addView(
            list,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // ===== Montaje
        drawer.addView(
            column,
            DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT,
                DrawerLayout.LayoutParams.MATCH_PARENT
            )
        )
        drawer.addView(sideContainer)
        setContentView(drawer)

        // ===== Calcular ancho MÍNIMO del menú para NO partir los títulos
        fun recalcDrawerSizesAuto() {
            val iconPx       = dp(20)      // igual que en el adapter
            val gapPx        = dp(12)      // espacio icono-texto
            val rowPadH      = dp(10) * 2  // padding horizontal del row
            val containerPad = dp(8)  * 2  // padding del contenedor naranja

            var maxRowWidth = 0
            for (it in items) {
                val tw = textWidthPx(it.title, 16f)        // ancho del texto a 16sp
                val total = containerPad + rowPadH + iconPx + gapPx + tw
                if (total > maxRowWidth) maxRowWidth = total
            }

            // holgura para la "burbuja" blanca y radios
            maxRowWidth += dp(12)

            // tope para no invadir demasiado (45% del ancho de pantalla)
            val maxCap = (resources.displayMetrics.widthPixels * 0.45f).toInt()
            val menuWidth = maxRowWidth.coerceAtMost(maxCap)

            // fija el ancho del panel
            (sideContainer.layoutParams as DrawerLayout.LayoutParams).apply {
                width = menuWidth
                sideContainer.layoutParams = this
            }

            // cuánto corre el contenido al abrir (80% del ancho del menú)
            contentShiftPx = (menuWidth * 0.80f).toInt()
        }
        recalcDrawerSizesAuto()
        drawer.doOnLayout { recalcDrawerSizesAuto() }

        // ===== Empujar SOLO el contenido al abrir el menú (título queda fijo)
        drawer.setScrimColor(Color.TRANSPARENT)
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val lp = contentFrame.layoutParams as LinearLayout.LayoutParams
                val shift = (contentShiftPx * slideOffset).toInt()
                if (lp.marginStart != shift) {
                    lp.marginStart = shift
                    contentFrame.layoutParams = lp
                }
            }
            override fun onDrawerClosed(drawerView: View) {
                val lp = contentFrame.layoutParams as LinearLayout.LayoutParams
                if (lp.marginStart != 0) {
                    lp.marginStart = 0
                    contentFrame.layoutParams = lp
                }
            }
        })



        // ===== Toggle hamburguesa
        toggle = ActionBarDrawerToggle(this, drawer, toolbar, 0, 0)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        // ===== Navegación
        list.setOnItemClickListener { _, _, pos, _ ->
            (list.adapter as MenuAdapter).selected = pos
            (list.adapter as MenuAdapter).notifyDataSetChanged()
            open(items[pos].screen())

            // Cierra solo si NO está fijado
            if (!isPinned) {
                drawer.closeDrawer(GravityCompat.START)
            }
        }


        // ===== Pantalla inicial
        if (savedInstanceState == null) {
            open(items.first().screen())
            (list.adapter as MenuAdapter).selected = 0
            (list.adapter as MenuAdapter).notifyDataSetChanged()
        }

        // (Opcional) Arrancar fijado:
        /*drawer.openDrawer(GravityCompat.START)
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.START)
        (contentFrame.layoutParams as LinearLayout.LayoutParams).apply {
        marginStart = contentShiftPx
        contentFrame.layoutParams = this
         }*/
    }


    private var isPinned = false


    private fun open(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(contentId, fragment).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)

    // ---------- Adaptador personalizado ----------
    inner class MenuAdapter(private val data: List<MenuItemM>) : BaseAdapter() {
        var selected = 0
        override fun getCount() = data.size
        override fun getItem(p: Int) = data[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(p: Int, convertView: View?, parent: ViewGroup?): View {
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT,
                    AbsListView.LayoutParams.WRAP_CONTENT
                )
                setPadding(dp(10), dp(12), dp(10), dp(12))
                minimumHeight = dp(44)

                val icon = ImageView(context).apply {
                    id = View.generateViewId()
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
                }
                val label = TextView(context).apply {
                    id = View.generateViewId()
                    textSize = 16f
                    setPadding(dp(12), 0, 0, 0)
                    maxLines = 1         // 👈 no partir líneas
                    isSingleLine = true
                    ellipsize = null
                }
                addView(icon); addView(label)
                tag = Holder(icon, label)
            }

            val h = row.tag as Holder
            val item = data[p]

            // Icono desde assets o emoji
            if (item.assetIcon != null) {
                try {
                    val isx: InputStream = assets.open(item.assetIcon)
                    h.icon.setImageBitmap(BitmapFactory.decodeStream(isx))
                } catch (_: Exception) {
                    h.icon.setImageDrawable(textAsBitmapDrawable(item.emoji, 18f, WHITE))
                }
            } else {
                h.icon.setImageDrawable(textAsBitmapDrawable(item.emoji, 18f, WHITE))
            }

            // Estilos por selección
            if (p == selected) {
                row.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(WHITE)
                    cornerRadii = floatArrayOf(
                        rdp(12f), rdp(12f), rdp(12f), rdp(12f),
                        rdp(12f), rdp(12f), rdp(12f), rdp(12f)
                    )
                }
                h.label.setTextColor(ORANGE)
                tint(h.icon, ORANGE)
            } else {
                row.background = null
                h.label.setTextColor(WHITE)
                tint(h.icon, WHITE)
            }

            h.label.text = item.title
            return row
        }
    }

    // ---------- Helpers ----------
    private data class Holder(val icon: ImageView, val label: TextView)
    private fun tint(iv: ImageView, color: Int) = iv.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rdp(v: Float) = v * resources.displayMetrics.density

    private fun textWidthPx(text: String, sp: Float): Int {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp * resources.displayMetrics.scaledDensity
        }
        return p.measureText(text).toInt()
    }

    private fun textAsBitmapDrawable(text: String, sp: Float, color: Int): android.graphics.drawable.Drawable {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sp * resources.displayMetrics.scaledDensity
        }
        val r = Rect()
        paint.getTextBounds(text, 0, text.length, r)
        val bmp = Bitmap.createBitmap(r.width() + dp(6), r.height() + dp(6), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawText(text, dp(3).toFloat(), r.height().toFloat() + dp(2), paint)
        return android.graphics.drawable.BitmapDrawable(resources, bmp)
    }

    // Placeholder
    class Placeholder(private val textValue: String) : Fragment() {
        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
            TextView(requireContext()).apply { text = textValue; textSize = 20f; setPadding(24, 24, 24, 24) }
    }
}
