package network.mysterium.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import me.pushy.sdk.Pushy
import network.mysterium.vpn.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import updated.mysterium.vpn.network.provider.usecase.UseCaseProvider
import updated.mysterium.vpn.ui.create.account.CreateAccountActivity
import updated.mysterium.vpn.ui.home.selection.HomeSelectionActivity
import updated.mysterium.vpn.ui.onboarding.OnboardingActivity
import updated.mysterium.vpn.ui.prepare.top.up.PrepareTopUpActivity
import updated.mysterium.vpn.ui.terms.TermsOfUseActivity
import updated.mysterium.vpn.ui.wallet.WalletActivity

class PushReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val PUSHY_BALANCE_ACTION = "android.intent.action.BALANCE_RUNNING_OUT"
        const val PUSHY_CONNECTION_ACTION = "android.intent.action.PUSHY_CONNECTION_ACTION"
        const val NOTIFICATION_TITLE = "title"
        const val NOTIFICATION_MESSAGE = "message"
        private const val PUSHY_NOTIFICATION_ID = 1
        private const val BALANCE_NOTIFICATION_ID = 2
        private const val CONNECTION_NOTIFICATION_ID = 3
    }

    private val useCaseProvider: UseCaseProvider by inject()
    private val loginUseCase = useCaseProvider.login()
    private val termsUseCase = useCaseProvider.terms()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PUSHY_BALANCE_ACTION -> {
                showBalancePush(context, intent)
            }
            PUSHY_CONNECTION_ACTION -> {
                showConnectionPush(context, intent)
            }
            else -> {
                showMarketingPush(context, intent)
            }
        }
    }

    private fun showBalancePush(context: Context, intent: Intent) {
        val builder = createNotification(intent, context)
        val resultIntent = Intent(context, WalletActivity::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, FLAG_UPDATE_CURRENT)
        }
        builder.setContentIntent(resultPendingIntent)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(BALANCE_NOTIFICATION_ID, builder.build())
    }

    private fun showConnectionPush(context: Context, intent: Intent) {
        val builder = createNotification(intent, context)
        val resultIntent = Intent(context, HomeSelectionActivity::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, FLAG_UPDATE_CURRENT)
        }
        builder.setContentIntent(resultPendingIntent)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(CONNECTION_NOTIFICATION_ID, builder.build())
    }

    private fun showMarketingPush(context: Context, intent: Intent) {
        val builder = createNotification(intent, context)
        val resultIntent = getMarketingPushResultIntent(context)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, FLAG_UPDATE_CURRENT)
        }
        builder.setContentIntent(resultPendingIntent)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(PUSHY_NOTIFICATION_ID, builder.build())
    }

    private fun createNotification(intent: Intent, context: Context): NotificationCompat.Builder {
        val title = intent.getStringExtra(NOTIFICATION_TITLE) ?: "MysteriumVPN"
        val message = intent.getStringExtra(NOTIFICATION_MESSAGE) ?: ""
        val builder = NotificationCompat.Builder(context, "")
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.notification_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setVibrate(longArrayOf(0, 400, 250, 400))
            .setSound(RingtoneManager.getDefaultUri(TYPE_NOTIFICATION))

        Pushy.setNotificationChannel(builder, context)

        return builder
    }

    private fun getMarketingPushResultIntent(context: Context) = when {
        !loginUseCase.isAlreadyLogin() -> {
            Intent(context, OnboardingActivity::class.java)
        }
        loginUseCase.isTopFlowShown() -> {
            Intent(context, HomeSelectionActivity::class.java)
        }
        loginUseCase.isAccountCreated() -> {
            Intent(context, PrepareTopUpActivity::class.java).apply {
                putExtra(PrepareTopUpActivity.IS_NEW_USER_KEY, loginUseCase.isNewUser())
            }
        }
        termsUseCase.isTermsAccepted() -> {
            Intent(context, CreateAccountActivity::class.java)
        }
        else -> {
            Intent(context, TermsOfUseActivity::class.java)
        }
    }
}
