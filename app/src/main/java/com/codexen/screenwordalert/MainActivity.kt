package com.codexen.screenwordalert

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var storage: KeywordStorage
    private lateinit var keywordInput: EditText
    private lateinit var addButton: Button
    private lateinit var selectAppButton: Button
    private lateinit var keywordListView: ListView
    private lateinit var targetListView: ListView
    private lateinit var statusText: TextView
    private lateinit var keywordAdapter: ArrayAdapter<String>
    private lateinit var targetAdapter: ArrayAdapter<String>

    private val keywordList = mutableListOf<String>()
    private val targetList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()

        storage = KeywordStorage(this)

        keywordInput = findViewById(R.id.keywordInput)
        addButton = findViewById(R.id.addButton)
        selectAppButton = findViewById(R.id.selectAppButton)
        keywordListView = findViewById(R.id.keywordList)
        targetListView = findViewById(R.id.targetList)
        statusText = findViewById(R.id.statusText)

        // Setup keyword list
        keywordList.addAll(storage.getKeywords())
        keywordAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, keywordList)
        keywordListView.adapter = keywordAdapter

        // Setup target app list
        targetList.addAll(storage.getTargetPackages())
        targetAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, targetList)
        targetListView.adapter = targetAdapter

        // Add keyword
        addButton.setOnClickListener {
            val kw = keywordInput.text.toString().trim()

            if (kw.isEmpty()) {
                Toast.makeText(this, "Enter a keyword", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            storage.saveKeyword(kw)

            keywordList.clear()
            keywordList.addAll(storage.getKeywords())

            keywordAdapter.notifyDataSetChanged()

            keywordInput.setText("")
        }

        // Select app from installed list
        selectAppButton.setOnClickListener {
            showAppPickerDialog()
        }

        // Long press keyword to delete
        keywordListView.setOnItemLongClickListener { _, _, position, _ ->

            val kw = keywordList[position]

            storage.deleteKeyword(kw)

            keywordList.clear()
            keywordList.addAll(storage.getKeywords())

            keywordAdapter.notifyDataSetChanged()

            Toast.makeText(this, "\"$kw\" deleted", Toast.LENGTH_SHORT).show()

            true
        }

        // Long press target app to delete
        targetListView.setOnItemLongClickListener { _, _, position, _ ->

            val pkg = targetList[position]

            storage.deleteTargetPackage(pkg)

            targetList.clear()
            targetList.addAll(storage.getTargetPackages())

            targetAdapter.notifyDataSetChanged()

            Toast.makeText(this, "\"$pkg\" removed", Toast.LENGTH_SHORT).show()

            true
        }

        checkAccessibilityStatus()

        statusText.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun showAppPickerDialog() {

        val pm: PackageManager = packageManager

        // Get all installed apps that can be launched by the user
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                pm.getLaunchIntentForPackage(it.packageName) != null
            }
            .sortedBy {
                pm.getApplicationLabel(it).toString().lowercase()
            }

        val displayNames = apps.map {
            "${pm.getApplicationLabel(it)}\n${it.packageName}"
        }

        val pkgNames = apps.map {
            it.packageName
        }

        val simpleNames = apps.map {
            pm.getApplicationLabel(it).toString()
        }

        val filteredNames = displayNames.toMutableList()
        val filteredPkgs = pkgNames.toMutableList()
        val filteredSimple = simpleNames.toMutableList()

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val searchInput = EditText(this)
        searchInput.hint = "Search e.g. WhatsApp, OneNote..."
        searchInput.setPadding(40, 30, 40, 30)

        val countText = TextView(this)
        countText.text = "Total: ${apps.size} apps"
        countText.setPadding(40, 10, 40, 10)
        countText.setTextColor(0xFF666666.toInt())
        countText.textSize = 12f

        val listView = ListView(this)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            filteredNames
        )

        listView.adapter = adapter

        layout.addView(searchInput)
        layout.addView(countText)
        layout.addView(listView)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Target App")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        searchInput.addTextChangedListener(object : android.text.TextWatcher {

            override fun afterTextChanged(s: android.text.Editable?) {}

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                val query = s.toString().lowercase()

                filteredNames.clear()
                filteredPkgs.clear()
                filteredSimple.clear()

                apps.forEachIndexed { index, _ ->

                    if (
                        simpleNames[index].lowercase().contains(query) ||
                        pkgNames[index].lowercase().contains(query)
                    ) {

                        filteredNames.add(displayNames[index])
                        filteredPkgs.add(pkgNames[index])
                        filteredSimple.add(simpleNames[index])
                    }
                }

                countText.text = "Showing: ${filteredNames.size} apps"

                adapter.notifyDataSetChanged()
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->

            val selectedPkg = filteredPkgs[position]
            val selectedName = filteredSimple[position]

            storage.saveTargetPackage(selectedPkg)

            targetList.clear()
            targetList.addAll(storage.getTargetPackages())

            targetAdapter.notifyDataSetChanged()

            Toast.makeText(
                this,
                "Added: $selectedName",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.80).toInt()
        )
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityStatus()

    }

    private fun checkAccessibilityStatus() {

        val enabled = isAccessibilityEnabled()

        statusText.post {

            if (enabled) {

                statusText.text =
                    "Service is ACTIVE - Monitoring screen"

                statusText.setBackgroundResource(R.color.status_active)

                statusText.setTextColor(
                    resources.getColor(
                        android.R.color.white,
                        theme
                    )
                )

            } else {

                statusText.text =
                    "Service is OFF - Tap here to enable in Accessibility Settings"

                statusText.setBackgroundResource(R.color.status_inactive)

                statusText.setTextColor(
                    resources.getColor(
                        android.R.color.white,
                        theme
                    )
                )
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {

        val expectedService =
            "$packageName/${WordDetectorService::class.java.canonicalName}"

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices
            .split(":")
            .map { it.trim().lowercase() }
            .any {
                it == expectedService.lowercase()
            }
    }


    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}