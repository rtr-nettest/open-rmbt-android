package at.rtr.rmbt.android.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Tracks which of the app's own activities is currently in picture-in-picture mode.
 * There is no reliable public API to expand someone else's pinned task
 * (ActivityManager.moveTaskToFront on a pinned task is a no-op on some OEMs, and the
 * pinned task created by reparenting is invisible to ActivityManager.appTasks), so
 * navigation needs in-process knowledge of the pinned instance to handle it.
 */
object PipRegistry {

    private val pinned = mutableMapOf<Class<*>, WeakReference<Activity>>()

    fun onPictureInPictureModeChanged(activity: Activity, isInPictureInPictureMode: Boolean) {
        if (isInPictureInPictureMode) {
            pinned[activity.javaClass] = WeakReference(activity)
        } else {
            removeIfSame(activity)
        }
    }

    fun onDestroyed(activity: Activity) {
        removeIfSame(activity)
    }

    fun getPinned(activityClass: Class<out Activity>): Activity? = pinned[activityClass]?.get()

    private fun removeIfSame(activity: Activity) {
        val current = pinned[activity.javaClass]?.get()
        if (current === activity || current == null) {
            pinned.remove(activity.javaClass)
        }
    }
}

/**
 * Brings an already running instance of [activityClass] back to the foreground instead of
 * launching a second instance with startActivity.
 *
 * A pinned (picture-in-picture) instance cannot be expanded reliably from the outside, so
 * it is finished (window disappears) and the caller is told to start the screen fresh in
 * its own task - the measurement state lives in services/singletons and survives this.
 *
 * @return true when an existing task was moved to the front, false when the caller should
 * start the activity normally
 */
fun Context.bringActivityTaskToFront(activityClass: Class<out Activity>): Boolean {
    PipRegistry.getPinned(activityClass)?.let { pinnedActivity ->
        Timber.d("Finishing pinned ${activityClass.name} instance, caller will relaunch fullscreen")
        try {
            pinnedActivity.finishAndRemoveTask()
        } catch (e: Exception) {
            Timber.e("Unable to finish pinned ${activityClass.name}: ${e.message}")
        }
        return false
    }

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
    }
    if (existingTask != null) {
        return try {
            Timber.d("Bringing existing $className task to front")
            existingTask.moveToFront()
            true
        } catch (e: Exception) {
            Timber.e("Unable to move $className task to front: ${e.message}")
            false
        }
    }
    return bringRunningTaskToFront(activityManager, className)
}

/**
 * appTasks is backed by the recents list and misses tasks the system creates by
 * reparenting. getRunningTasks still reports the app's own tasks; moving one to the front
 * requires the normal-level REORDER_TASKS permission. Pinned tasks are handled before this
 * via [PipRegistry] (moveTaskToFront does not expand them on all devices).
 */
private fun bringRunningTaskToFront(activityManager: ActivityManager, className: String): Boolean {
    return try {
        @Suppress("DEPRECATION")
        val runningTask = activityManager.getRunningTasks(50).firstOrNull { task ->
            task.topActivity?.className == className || task.baseActivity?.className == className
        } ?: return false
        @Suppress("DEPRECATION")
        val taskId = runningTask.id
        Timber.d("Bringing running $className task $taskId to front")
        activityManager.moveTaskToFront(taskId, 0)
        true
    } catch (e: Exception) {
        Timber.e("Unable to move running $className task to front: ${e.message}")
        false
    }
}
