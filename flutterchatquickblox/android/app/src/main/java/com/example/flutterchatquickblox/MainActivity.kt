package com.example.flutterchatquickblox

import android.os.Bundle
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.ACCOUNT_KEY
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.APP_ID
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.AUTH_KEY
import com.quickblox.auth.session.QBSettings
import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import com.example.flutterchatquickblox.QuickBloxCredentials.Companion.AUTH_SECRET
import com.quickblox.core.QBHttpConnectionConfig
import com.quickblox.core.StoringMechanism
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.model.QBUser
import com.quickblox.core.QBEntityCallback
import com.quickblox.users.QBUsers
import com.quickblox.auth.session.QBSession
import com.quickblox.auth.session.QBSessionParameters
import com.quickblox.auth.session.QBSessionManager


class MainActivity : FlutterActivity() {
    val user = QBUser("testuserlogin11", "testuserpassword")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        init()
        createQuickBloxUser()
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
        QBUsers.signIn(user).performAsync(object : QBEntityCallback<QBUser> {
            override fun onSuccess(user: QBUser, args: Bundle) {
                // success
                AppUtils.logInfo("Success SignIn")
                retreiveUserInfo()
            }

            override fun onError(error: QBResponseException) {
                // error
                AppUtils.logInfo("Fail SignIn")
            }
        })
    }

    private fun retreiveUserInfo() {
        val sessionParameters = QBSessionManager.getInstance().sessionParameters
        sessionParameters.userId //stores user Id if user signed in via email
        sessionParameters.userEmail //stores user's Email if user signed in via email

        sessionParameters.userId //stores access token for social net if user signed in via social provider

        sessionParameters.socialProvider //stores social provider if user signed in via this provider

        var session: QBSession = QBSession()
        AppUtils.logInfo("Token " + session.token)

    }

    private fun signUpQuickBloxUser() {
        QBUsers.signUp(user).performAsync(object : QBEntityCallback<QBUser> {
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
}
