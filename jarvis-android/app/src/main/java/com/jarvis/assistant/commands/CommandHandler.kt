package com.jarvis.assistant.commands

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings

class CommandHandler(private val context: Context) {

    data class CommandResult(val handled: Boolean, val feedback: String = "")

    fun handle(input: String): CommandResult {
        val q = input.lowercase().trim()
        return when {
            q.contains("open whatsapp")         -> launchApp("com.whatsapp", "Opening WhatsApp now.")
            q.contains("open youtube")          -> launchApp("com.google.android.youtube", "Opening YouTube.")
            q.contains("open maps") ||
            q.contains("open google maps")      -> launchApp("com.google.android.apps.maps", "Opening Maps.")
            q.contains("open spotify")          -> launchApp("com.spotify.music", "Opening Spotify.")
            q.contains("open instagram")        -> launchApp("com.instagram.android", "Opening Instagram.")
            q.contains("open twitter") ||
            q.contains("open x")                -> launchApp("com.twitter.android", "Opening X.")
            q.contains("open settings")         -> openSettings()
            q.contains("open camera")           -> launchApp("com.android.camera2", "Opening the camera.")
            q.contains("set alarm") ||
            q.contains("set an alarm") ||
            q.contains("wake me")               -> setAlarm(q)
            q.contains("search for") ||
            q.startsWith("search ")             -> webSearch(q)
            q.contains("open browser") ||
            q.contains("browse to")             -> openBrowser()
            else                                -> CommandResult(false)
        }
    }

    private fun launchApp(pkg: String, feedback: String): CommandResult {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult(true, feedback)
        } else {
            CommandResult(true, "That app does not seem to be installed.")
        }
    }

    private fun openSettings(): CommandResult {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return CommandResult(true, "Opening device settings.")
    }

    private fun setAlarm(input: String): CommandResult {
        val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val match = timeRegex.find(input)
        return if (match != null) {
            var hour   = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val period = match.groupValues[3].lowercase()

            if (period == "pm" && hour < 12) hour += 12
            if (period == "am" && hour == 12) hour = 0

            context.startActivity(
                Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            CommandResult(true, "Alarm set for $hour:${minute.toString().padStart(2, '0')}.")
        } else {
            context.startActivity(
                Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            CommandResult(true, "Opening the alarm app for you.")
        }
    }

    private fun webSearch(input: String): CommandResult {
        val query = when {
            input.contains("search for ") -> input.substringAfter("search for ").trim()
            input.startsWith("search ")   -> input.substringAfter("search ").trim()
            else -> input
        }
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            CommandResult(true, "Searching for $query.")
        } catch (e: Exception) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            CommandResult(true, "Searching the web for $query.")
        }
    }

    private fun openBrowser(): CommandResult {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return CommandResult(true, "Opening the browser.")
    }
}
