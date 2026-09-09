package org.fossify.messages.helpers

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle

/**
 * Stops kids from hiding SMS/MMS (threads, messages, recycle bin, notification Delete).
 *
 * [DISABLE_DELETE_KEY]: only an explicit `false` allows delete.
 * Missing key or `true` keeps delete off.
 *
 * Does not cover archive, copy, forward, share, drafts, or scheduled-send cleanup.
 */
object SmsDeleteGuard {
    const val DISABLE_DELETE_KEY = "disable_delete"

    fun isDeleteDisabled(context: Context): Boolean {
        return isDeleteDisabled(applicationRestrictions(context))
    }

    /**
     * Only an explicit `false` (boolean or the strings `false` / `0`) allows delete.
     * Missing key, `true`, or anything else keeps delete off.
     */
    internal fun isDeleteDisabled(restrictions: Bundle?): Boolean {
        if (restrictions == null || !restrictions.containsKey(DISABLE_DELETE_KEY)) {
            return true
        }
        return when (val value = restrictions.get(DISABLE_DELETE_KEY)) {
            is Boolean -> value
            is String -> {
                val normalized = value.trim().lowercase()
                normalized != "false" && normalized != "0"
            }
            else -> true
        }
    }

    private fun applicationRestrictions(context: Context): Bundle? {
        return context.getSystemService(RestrictionsManager::class.java)?.applicationRestrictions
    }
}
