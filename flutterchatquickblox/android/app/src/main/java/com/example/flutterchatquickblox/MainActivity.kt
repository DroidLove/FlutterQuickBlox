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
import com.example.flutterchatquickblox.utils.CHAT_HISTORY_ITEMS_PER_PAGE
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
import com.quickblox.chat.QBChatService
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.chat.model.QBDialogType
import com.quickblox.core.request.QBRequestGetBuilder
import com.quickblox.sample.chat.kotlin.utils.qb.QbChatDialogMessageListenerImpl
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.chat.ChatMessageListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainActivity : FlutterActivity() {
    val TAG = "Checking"
//    testuserpassword
    val userCurrent = QBUser("testuserlogin10", "testuserpassword")
    private lateinit var currentUser: QBUser
    private lateinit var qbChatDialog: QBChatDialog
    private lateinit var requestBuilder: QBRequestGetBuilder
    private var skipPagination = 0
    lateinit var channel: MethodChannel
    private var chatMessageListener: ChatMessageListener = ChatMessageListener()

    companion object {
        lateinit var instance: MainActivity
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        init()
        createQuickBloxUser()

        channel = MethodChannel(flutterView, "quickbloxbridge")

        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "getChatMessageEntered" -> {
                    val message = call.argument<String>("message")
                    AppUtils.logInfo("Entered response $message")
                    sendChatMessage(message)
                }
            }
        }
    }

    private fun initChatDialog() {
        try {
            qbChatDialog.initForChat(QBChatService.getInstance())
        } catch (e: IllegalStateException) {
            Log.v(TAG, "The error registerCallback fro chat. Error message is : " + e.message)
            finish()
        }
        qbChatDialog.addMessageListener(chatMessageListener)
    }

    private fun sendChatMessage(message: String?) {
        if (isLogged()) {
            val chatMessage = QBChatMessage()
            chatMessage.body = message

            chatMessage.setSaveToHistory(true)
            chatMessage.dateSent = System.currentTimeMillis() / 1000
            chatMessage.isMarkable = true

            try {
                Log.d(TAG, "Sending Message with ID: " + chatMessage.id)
                qbChatDialog.sendMessage(chatMessage)

                if (qbChatDialog.type == QBDialogType.PRIVATE) {
//                    showMessage(chatMessage)
                    // Manual update listing here
                }

            } catch (e: SmackException.NotConnectedException) {
                Log.w(TAG, e)
            }

        } else {
            // Show login error or retry login
        }
    }

    private fun loadChatHistory() {
        ChatHelper.loadChatHistory(qbChatDialog, skipPagination, object : QBEntityCallback<ArrayList<QBChatMessage>> {
            override fun onSuccess(messages: ArrayList<QBChatMessage>, args: Bundle?) {
                // The newest messages should be in the end of list,
                // so we need to reverse list to show messages in the right order
                messages.reverse()

                var messageArray = ArrayList<String>()
                var isMessageCurrentUserArray = ArrayList<Boolean>()
                for ((index, value) in messages.withIndex()) {
                    println("the element at $index is $value")

                    var isMessageOfCurrentUser = false
                    if (value.senderId == currentUser.id) {
                        isMessageOfCurrentUser = true
                    }
//                    val chatListingModel = ChatListingModel(value.body, isMessageOfCurrentUser)

                    messageArray.add(value.body)
                    isMessageCurrentUserArray.add(isMessageOfCurrentUser)
                    AppUtils.logInfo("Messages " + (messageArray?.get(index) ?: ""))
                }
                channel.invokeMethod("getChatHistory", hashMapOf( "messageList" to messageArray,
                        "isCurrentUserMessage" to isMessageCurrentUserArray))
            }

            override fun onError(e: QBResponseException) {
//                progressBar.visibility = View.GONE
//                skipPagination -= CHAT_HISTORY_ITEMS_PER_PAGE
//                showErrorSnackbar(R.string.connection_error, e, null)v
                AppUtils.logInfo("" + e.message)
            }
        })
        skipPagination += CHAT_HISTORY_ITEMS_PER_PAGE
    }

    private fun loadDialogsFromQb() {
        ChatHelper.getDialogs(requestBuilder, object : QBEntityCallback<ArrayList<QBChatDialog>> {
            override fun onSuccess(dialogs: ArrayList<QBChatDialog>, bundle: Bundle?) {
//                DialogJoinerAsyncTask(this@DialogsActivity, dialogs, clearDialogHolder).execute()
                AppUtils.logInfo("Dialog info " + dialogs[0].dialogId)
                qbChatDialog = dialogs[0]
                //Todo: Get count from database instead
                initChatDialog()

                loadChatHistory()
            }

            override fun onError(e: QBResponseException) {
                AppUtils.logInfo("QBResponseException ${e.message}")
//                disableProgress()
//                shortToast(e.message)
            }
        })
    }

    private fun getBatteryLevel(): Int {
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

        val timeout = 90000
        QBHttpConnectionConfig.setConnectTimeout(timeout) //timeout value in milliseconds.
        QBHttpConnectionConfig.setReadTimeout(timeout) //timeout value in milliseconds.

        requestBuilder = QBRequestGetBuilder()
    }

    private fun createQuickBloxUser() {
//      signUpQuickBloxUser()
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
                    AppUtils.logInfo("Checking " + currentUser.id)
                    loadDialogsFromQb()
                } else {
                    AppUtils.logInfo("Checking fail")

                }
            }

            override fun onError(e: QBResponseException) {
                Log.w(TAG, "Chat login onError(): $e")
            }
        })
    }

    private inner class ChatMessageListener : QbChatDialogMessageListenerImpl() {
        override fun processMessage(s: String, qbChatMessage: QBChatMessage, integer: Int?) {
            Log.d(TAG, "Processing Received Message: " + qbChatMessage.body)
//            showMessage(qbChatMessage)
            // Update the dart listing in the ui
            var receivedMessage = qbChatMessage.body


            channel.invokeMethod("updateChatListing", hashMapOf( "messageReceived" to receivedMessage,
                    "isCurrentUserMessage" to false))
        }
    }

//    MethodChannel(flutterView, "quickbloxbridge").setMethodCallHandler { call, result ->
//        if (call.method == "getChatMessageEntered") {
//            val batteryLevel = getBatteryLevel()
//
//            val text = call.argument<String>("message")
//            AppUtils.logInfo("Entered response $text")
//
//            if (batteryLevel != -1) {
//                result.success(batteryLevel)
//            } else {
//                result.error("Unavailable", "Battery level is not available", null)
//            }
//        }
//    }
}
