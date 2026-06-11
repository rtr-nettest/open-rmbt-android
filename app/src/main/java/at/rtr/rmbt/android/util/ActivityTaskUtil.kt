package at.rtr.rmbt.android.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * Brings an already running instance of [activityClass] back to the foreground - expanding
 * it when it sits in a pinned picture-in-picture task. Launching such an activity again
 * with startActivity would either create a second instance in the caller's task or
 * relaunch it inside the pinned task without leaving picture-in-picture mode.
 *
 * @return true when an existing task was moved to the front, false when none was found
 * (the caller should start the activity normally in that case)
 */
fun Context.bringActivityTaskToFront(activityClass: Class<out Activity>): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val className = activityClass.name
    val existingTask = activityManager.appTasks.firstOrNull { task ->
        val info = try {
            task.taskInfo
        } catch (e: Exception) {
            null
        }
        val matchesTopActivity = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                info?.topActivity?.className == className
        val matchesBaseIntent = info?.baseIntent?.component?.className == className
        matchesTopActivity || matchesBaseIntent
    } ?: return false
    return try {
        Timber.d("Bringing existing $className task to front")
        existingTask.moveToFront()
        true
    } catch (e: Exception) {
        Timber.e("Unable to move $className task to front: ${e.message}")
        false
    }
}
