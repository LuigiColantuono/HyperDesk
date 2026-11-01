package com.ukm.app.iconscustomizer

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.LruCache
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ukm.app.iconscustomizer.ui.IconPacks
import com.ukm.app.iconscustomizer.ui.PrefStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IconPickerActivity : AppCompatActivity() {

    /** The main pack, which per-app choices are stored under. */
    private lateinit var iconPackPackage: String
    /** The pack whose icons the grid is showing; mixing means it can differ from the main one. */
    private lateinit var browsedPack: String
    private lateinit var appName: String
    private lateinit var currentIconImage: ImageView
    private lateinit var componentString: String
    private lateinit var adapter: IconGridAdapter
    private lateinit var spinner: ProgressBar
    private lateinit var searchBox: com.google.android.material.textfield.TextInputEditText
    private var loadJob: Job? = null
    private val allAvailableIcons = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_icon_picker)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val materialToolbar = findViewById<MaterialToolbar>(R.id.materialToolbar)
        setSupportActionBar(materialToolbar)
        iconPackPackage = intent.getStringExtra("EXTRA_ICON_PACK") ?: return finish()
        browsedPack = iconPackPackage
        appName = intent.getStringExtra("EXTRA_APP_NAME") ?: "App"
        componentString = intent.getStringExtra("EXTRA_COMPONENT_STRING") ?: ""

        currentIconImage = findViewById(R.id.currentIconImage)
        val appNameTextView = findViewById<TextView>(R.id.themingAppName)
        val recyclerView = findViewById<RecyclerView>(R.id.iconRecyclerView)
        spinner = findViewById(R.id.loadingSpinner)
        searchBox = findViewById(R.id.searchIconEditText)
        appNameTextView.text = customLabel() ?: appName

        lifecycleScope.launch {
            loadCurrentIcon(currentIconImage)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 4)
        adapter = IconGridAdapter(emptyList())
        recyclerView.adapter = adapter

        loadBrowsedPack()

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
            }
        })
    }

    private fun loadBrowsedPack() {
        supportActionBar?.subtitle = IconPacks.label(this, browsedPack)
        spinner.visibility = View.VISIBLE
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val pack = browsedPack
            val icons = withContext(Dispatchers.IO) {
                IconPackHelper.getAllIconsFromPack(this@IconPickerActivity, pack)
            }
            allAvailableIcons.clear()
            allAvailableIcons.addAll(icons)
            spinner.visibility = View.GONE
            adapter.filter(searchBox.text?.toString().orEmpty())
        }
    }

    private fun showPackSwitcher() {
        val packs = IconPacks.installed(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_switch_pack)
            .setSingleChoiceItems(
                packs.map { it.name }.toTypedArray(),
                packs.indexOfFirst { it.packageName == browsedPack }
            ) { dialog, which ->
                browsedPack = packs[which].packageName
                dialog.dismiss()
                loadBrowsedPack()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun customLabel() = PrefStore.get(Prefs.labelKey(componentString)) as? String

    private fun saveLabel(label: String?) {
        PrefStore.put(Prefs.labelKey(componentString), label)
        findViewById<TextView>(R.id.themingAppName).text = label ?: appName
        UIHelpers.restartLauncher(this)
    }

    private fun showRenameDialog() {
        val input = EditText(this).apply {
            setSingleLine()
            hint = appName
            setText(customLabel())
            setSelection(text.length)
        }
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(padding, padding / 3, padding, 0)
            addView(input)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_rename_app)
            .setView(container)
            .setPositiveButton(R.string.action_save) { _, _ ->
                saveLabel(input.text.toString().trim().ifEmpty { null })
            }
            .setNegativeButton(R.string.action_cancel, null)
        if (customLabel() != null) {
            dialog.setNeutralButton(R.string.action_reset_short) { _, _ -> saveLabel(null) }
        }
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.icon_picker_menu, menu)
        return true
    }

    private suspend fun loadCurrentIcon(imageView: ImageView) {
        val prefs = App.mService?.getRemotePreferences(MainActivity.PREF_NAME)
            ?: getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE)
        val manualIcon = prefs.getString("custom_icon_${iconPackPackage}_$componentString", null)
        val chain = IconPackHelper.packChain(
            iconPackPackage,
            Prefs.string(prefs, Prefs.ICON_PACK_FALLBACKS)
        )
        val pkgName = componentString.substringAfter("{").substringBefore("/")
        val className = componentString.substringAfter("/").substringBefore("}")
        val loadedDrawable = withContext(Dispatchers.IO) {
            val refs = IconPackHelper.candidates(
                this@IconPickerActivity,
                chain,
                pkgName,
                className,
                manualIcon,
                overrideOnly = Prefs.isShortcutKey(componentString)
            )
            val icon = IconPackHelper.loadFirst(this@IconPickerActivity, refs)
            if (icon != null && Prefs.bool(prefs, Prefs.ICON_NORMALIZE)) {
                IconNormalizer.normalize(this@IconPickerActivity, icon, Prefs.int(prefs, Prefs.ICON_NORMALIZE_SIZE))
            } else {
                icon
            }
        }
        if (loadedDrawable != null) {
            imageView.setImageDrawable(loadedDrawable)
        } else {
            loadStockAppIcon(imageView)
        }
    }

    private fun loadStockAppIcon(imageView: ImageView) {
        try {
            val pkgName = componentString.substringAfter("{").substringBefore("/")
            val icon = packageManager.getApplicationIcon(pkgName)
            imageView.setImageDrawable(icon)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuRenameApp -> {
                showRenameDialog()
                true
            }
            R.id.menuSwitchPack -> {
                showPackSwitcher()
                true
            }
            R.id.menuResetDefault -> {
                resetIconToDefault()
                UIHelpers.restartLauncher(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun resetIconToDefault() {
        val remotePrefs = App.mService?.getRemotePreferences(MainActivity.PREF_NAME)
            ?: getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE)
        val prefKey = "custom_icon_${iconPackPackage}_$componentString"
        remotePrefs.edit {
            remove(prefKey)
        }
        val localPrefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE)
        localPrefs.edit(commit = true) {
            remove(prefKey)
        }
        Toast.makeText(this, "Icon reset to default!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::adapter.isInitialized) {
            adapter.cleanUp()
        }
    }

    private fun saveIconChoice(choice: IconPackHelper.IconRef) {
        val manualOverrideKey = "custom_icon_${iconPackPackage}_$componentString"
        val value = IconPackHelper.encodeOverride(iconPackPackage, choice)
        UIHelpers.pushLocalPref(this, manualOverrideKey, value)
        UIHelpers.pushRemotePref(manualOverrideKey, value)
        finish()
    }

    inner class IconGridAdapter(private var displayedIcons: List<String>) :
        RecyclerView.Adapter<IconGridAdapter.IconViewHolder>() {

        private val adapterScope = CoroutineScope(Dispatchers.Main + Job())
        private val memoryCache = LruCache<String, Drawable>(200)

        inner class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconImage: ImageView = view.findViewById(R.id.singleIconImage)
            var currentJob: Job? = null // Track the specific job for this cell!
        }

        override fun onViewRecycled(holder: IconViewHolder) {
            super.onViewRecycled(holder)
            holder.currentJob?.cancel()
            holder.iconImage.setImageDrawable(null)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_icon, parent, false)
            return IconViewHolder(view)
        }

        override fun getItemCount(): Int = displayedIcons.size

        override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
            val drawableName = displayedIcons[position]
            val pack = browsedPack
            val iconKey = "$pack|$drawableName"

            holder.currentJob?.cancel()

            holder.iconImage.tag = iconKey
            holder.iconImage.setImageDrawable(null)

            val cachedIcon = memoryCache.get(iconKey)
            if (cachedIcon != null) {
                holder.iconImage.setImageDrawable(cachedIcon)
            } else {
                holder.currentJob = adapterScope.launch {
                    val loadedDrawable = withContext(Dispatchers.IO) {
                        IconPackHelper.loadIcon(applicationContext, pack, drawableName)
                    }
                    if (loadedDrawable != null) {
                        memoryCache.put(iconKey, loadedDrawable)
                        if (holder.iconImage.tag == iconKey) {
                            holder.iconImage.setImageDrawable(loadedDrawable)
                        }
                    }
                }
            }
            holder.itemView.setOnClickListener {
                saveIconChoice(IconPackHelper.IconRef(pack, drawableName))
                UIHelpers.restartLauncher(this@IconPickerActivity)
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun updateData(newIcons: List<String>) {
            displayedIcons = newIcons
            notifyDataSetChanged()
        }

        @SuppressLint("NotifyDataSetChanged")
        fun filter(query: String) {
            displayedIcons = if (query.isEmpty()) {
                allAvailableIcons
            } else {
                allAvailableIcons.filter { it.contains(query, ignoreCase = true) }
            }
            notifyDataSetChanged()
        }

        fun cleanUp() {
            adapterScope.cancel()
            memoryCache.evictAll()
        }
    }
}