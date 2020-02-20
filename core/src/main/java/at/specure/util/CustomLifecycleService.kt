package at.specure.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher

/**
 * The class is the copy of androidx.lifecycle.LifecycleService and was extracted to fix the bug RTR-445 related to the issue 'https://issuetracker.google.com/issues/144442247'
 * with possible intent nullability in onStartCommand() and onBind() declared as @NonNull in androidx.lifecycle.LifecycleService. This leads to crash on relaunch app
 * Latest check - not fixed in androidx.lifecycle:lifecycle-service:2.2.0-rc3
 */

open class CustomLifecycleService : Service(), LifecycleOwner {

    private val mDispatcher = ServiceLifecycleDispatcher(this)

    @CallSuper
    override fun onCreate() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    @CallSuper
    override fun onBind(intent: Intent?): IBinder? {
        mDispatcher.onServicePreSuperOnBind()
        return null
    }

    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        mDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    // this method is added only to annotate it with @CallSuper.
// In usual service super.onStartCommand is no-op, but in LifecycleService
// it results in mDispatcher.onServicePreSuperOnStart() call, because
// super.onStartCommand calls onStart().
    // todo: check fix of intent nullability in androidx.lifecycle:lifecycle-service
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    override fun getLifecycle(): Lifecycle {
        return mDispatcher.lifecycle
    }
}