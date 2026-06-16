package com.codexen.screenwordalert

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

class WordDetectorService : AccessibilityService() {

    private lateinit var storage: KeywordStorage
    private val CHANNEL_ID = "screenword_channel"
    private val notifiedKeywords = mutableSetOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        storage = KeywordStorage(this)
        createNotificationChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        notifiedKeywords.clear()

        event ?: return

        val targetPkgs = storage.getTargetPackages()
        val eventPkg = event.packageName?.toString() ?: return

        // Ignore our own app
        if (eventPkg == packageName) {
            return
        }

        // Only monitor selected apps
        if (targetPkgs.isNotEmpty()) {

            val matched = targetPkgs.any {
                eventPkg.startsWith(it)
            }

            if (!matched) {
                return
            }
        }

        val keywords = storage.getKeywords()
        if (keywords.isEmpty()) return

        // Check event text first
        val eventText = event.text?.joinToString(" ") ?: ""

        for (keyword in keywords) {
            if (eventText.contains(keyword, ignoreCase = true)) {

                if (!notifiedKeywords.contains(keyword)) {

                    notifiedKeywords.add(keyword)

                    sendAlert(keyword, eventText)
                }
            }
        }

        // Then scan the active window
        val rootNode = rootInActiveWindow

        if (rootNode != null) {
            scanNodesForKeywords(rootNode, keywords)
        }
    }

    private fun scanNodesForKeywords(node: AccessibilityNodeInfo?, keywords: List<String>) {
        node ?: return

        val text = node.text?.toString() ?: ""

        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true)) {
                if (!notifiedKeywords.contains(keyword)) {
                    notifiedKeywords.add(keyword)
                    sendAlert(keyword, text)
                }
            }
        }

        for (i in 0 until node.childCount) {
            scanNodesForKeywords(node.getChild(i), keywords)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ScreenWord Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Keyword match alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendAlert(keyword: String, matchedText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            keyword.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Keyword Detected: $keyword")
            .setContentText("Found in: $matchedText")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(keyword.hashCode(), notification)
    }

    override fun onInterrupt() {}
}