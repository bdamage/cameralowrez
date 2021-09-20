package net.znordic.cameralowrez

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log


class RemoteIntentConfig(activity: Activity) : BroadcastReceiver() {
    var APP_PACKAGE_NAME = "net.znordic.cameralowrez"
    val configRemote = ".REMOTE_CONFIG"
    
    var mContext: Context
    var mRemoteConfigEvent: RemoteConfigListener? = null
    fun unregisterReceiver() {
        mContext.unregisterReceiver(this)
    }

    fun registerReceiver() {
        val filter = IntentFilter()
        //com.zebra.webkiosk.REMOTE_CONFIG
        //com.zebra.webkiosk.REMOTE_CONFIG
        filter.addAction(APP_PACKAGE_NAME + configRemote)
        Log.d(TAG, "Register intent filter: " + APP_PACKAGE_NAME + configRemote)
        //  filter.addCategory("android.intent.category.DEFAULT");
        mContext.registerReceiver(this, filter)
    }

    // Container Activity must implement this interface
    interface RemoteConfigListener {
        fun onRemoteConfigEvent(intent: Intent?)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Action: $action")
        if (action == APP_PACKAGE_NAME + configRemote)
            mRemoteConfigEvent!!.onRemoteConfigEvent(intent)
    }

    companion object {
        const val TAG = "RemoteIntentConfig"

    }

    init {
        mContext = activity.applicationContext
        APP_PACKAGE_NAME = activity.application.packageName
        mRemoteConfigEvent = try {
            activity as RemoteConfigListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                this.toString()
                        + " must implement RemoteConfigListener"
            )
        }
        registerReceiver()
    }
}