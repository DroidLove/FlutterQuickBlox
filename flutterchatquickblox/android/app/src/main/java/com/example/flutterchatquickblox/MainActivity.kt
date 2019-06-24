package com.example.flutterchatquickblox

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.ACCOUNT_KEY
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.APP_ID
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.AUTH_KEY
import com.quickblox.auth.session.QBSettings
import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.AUTH_SECRET
import com.example.flutterchatquickblox.utils.AppUtils
import com.example.flutterchatquickblox.utils.ChatHelper
import com.example.flutterchatquickblox.utils.ChatHelper.getCurrentUser
import com.example.flutterchatquickblox.utils.ChatHelper.isLogged
import com.example.flutterchatquickblox.utils.ChatHelper.loginToChat
import com.example.flutterchatquickblox.utils.SharedPrefsHelper
import com.quickblox.core.QBHttpConnectionConfig
import com.quickblox.core.StoringMechanism
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.model.QBUser
import com.quickblox.core.QBEntityCallback
import com.quickblox.users.QBUsers
import com.quickblox.auth.session.QBSession
import com.quickblox.auth.session.QBSessionParameters
import com.quickblox.auth.session.QBSessionManager
import io.flutter.plugin.common.MethodChannel


class MainActivity : FlutterActivity() {
    val userCurrent = QBUser("testuserlogin11", "testuserpassword")
    private lateinit var currentUser: QBUser
    val TAG = "Checking"

    companion object {
        lateinit var instance: MainActivity
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        init()
        createQuickBloxUser()

        MethodChannel(flutterView, "battery").setMethodCallHandler { call, result ->
            if (call.method == "getBatteryLevel") {
                val batteryLevel = getBatteryLevel()

                if (batteryLevel != -1) {
                    result.success(batteryLevel)
                } else {
                    result.error("Unavailable", "Battery level is not available", null)
                }
            }
        }
    }

    private fun getBatteryLevel() : Int {
        val batteryLevel: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        } else {

            val intent = ContextWrapper(applicationContext).registerReceiver(null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED))
            batteryLevel = intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        }

        return batteryLevel
    }

    private fun init() {
        QBSettings.getInstance().storingMehanism = StoringMechanism.UNSECURED //call before init method for QBSettings
        QBSettings.getInstance().init(applicationContext, APP_ID, AUTH_KEY, AUTH_SECRET)
        QBSettings.getInstance().accountKey = ACCOUNT_KEY

        val timeout = 10000
        QBHttpConnectionConfig.setConnectTimeout(timeout) //timeout value in milliseconds.
        QBHttpConnectionConfig.setReadTimeout(timeout) //timeout value in milliseconds.
    }

    private fun createQuickBloxUser() {
//        signUpQuickBloxUser()
        signInQuickBloxUser()
        trackQuickBloxUser()
    }

    private fun trackQuickBloxUser() {
        QBSessionManager.getInstance().addListener(object : QBSessionManager.QBSessionListener {
            override fun onSessionCreated(session: QBSession) {
                //calls when session was created firstly or after it has been expired
                AppUtils.logInfo("session created " + session.token)
            }

            override fun onSessionUpdated(sessionParameters: QBSessionParameters) {
                //calls when user signed in or signed up
                //QBSessionParameters stores information about signed in user.
                AppUtils.logInfo("session updated " + sessionParameters.accessToken)
            }

            override fun onSessionDeleted() {
                //calls when user signed Out or session was deleted
            }

            override fun onSessionRestored(session: QBSession) {
                //calls when session was restored from local storage
            }

            override fun onSessionExpired() {
                //calls when session is expired
            }

            override fun onProviderSessionExpired(provider: String) {
                //calls when provider's access token is expired or invalid
            }

        })
    }

    private fun signInQuickBloxUser() {
        QBUsers.signIn(userCurrent).performAsync(object : QBEntityCallback<QBUser> {
            override fun onSuccess(user: QBUser, args: Bundle) {
                // success
                AppUtils.logInfo("Success SignIn")
                loginToChat(userCurrent)
            }

            override fun onError(error: QBResponseException) {
                // error
                AppUtils.logInfo("Fail SignIn")
            }
        })
    }

    private fun signUpQuickBloxUser() {
        QBUsers.signUp(userCurrent).performAsync(object : QBEntityCallback<QBUser> {
            override fun onSuccess(user: QBUser, args: Bundle) {
                // success
                AppUtils.logInfo("Success SignUp")
                signInQuickBloxUser()
            }

            override fun onError(error: QBResponseException) {
                // error
                AppUtils.logInfo("failed SignUp")
            }
        })
    }

    private fun checkSignIn(): Boolean {
        return QBSessionManager.getInstance().sessionParameters != null
    }

    private fun restoreChatSession() {
        if (ChatHelper.isLogged()) {
            currentUser = getCurrentUser()
        } else {
            val currentUser: QBUser? = getUserFromSession()
            if (currentUser == null) {
//                Log.e("Current User ", currentUser!!.)
            } else {
                loginToChat(currentUser)
            }
        }
    }

    private fun getUserFromSession(): QBUser? {
        val user = SharedPrefsHelper.getQbUser()
        val qbSessionManager = QBSessionManager.getInstance()
        qbSessionManager.sessionParameters?.let {
            val userId = qbSessionManager.sessionParameters.userId
            user?.id = userId
            return user
        } ?: run {
            ChatHelper.destroy()
            return null
        }
    }

    private fun loginToChat(user: QBUser) {
//        showProgressDialog(R.string.dlg_restoring_chat_session)
        loginToChat(user, object : QBEntityCallback<Void> {
            override fun onSuccess(result: Void?, bundle: Bundle?) {
                Log.v(TAG, "Chat login onSuccess()")
                if (isLogged()) {
                    currentUser = getCurrentUser()
                    AppUtils.logInfo("Checking "+ currentUser.id)
                } else {
                    AppUtils.logInfo("Checking fail")

                }
            }

            override fun onError(e: QBResponseException) {
                Log.w(TAG, "Chat login onError(): $e")
            }
        })
    }
}
