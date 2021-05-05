package updated.mysterium.vpn.ui.splash

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import network.mysterium.notification.Notifications
import network.mysterium.vpn.R
import network.mysterium.vpn.databinding.ActivitySplashBinding
import org.koin.android.ext.android.inject
import updated.mysterium.vpn.App
import updated.mysterium.vpn.common.animation.OnAnimationCompletedListener
import updated.mysterium.vpn.common.languages.LanguagesUtil
import updated.mysterium.vpn.ui.balance.BalanceViewModel
import updated.mysterium.vpn.ui.base.BaseActivity
import updated.mysterium.vpn.ui.create.account.CreateAccountActivity
import updated.mysterium.vpn.ui.home.selection.HomeSelectionActivity
import updated.mysterium.vpn.ui.onboarding.OnboardingActivity
import updated.mysterium.vpn.ui.prepare.top.up.PrepareTopUpActivity
import updated.mysterium.vpn.ui.terms.TermsOfUseActivity
import java.util.*

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val balanceViewModel: BalanceViewModel by inject()
    private val viewModel: SplashViewModel by inject()
    private val pushyNotifications = Notifications(this)
    private var isVpnPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ensureVpnServicePermission()
        configure()
        subscribeViewModel()
        setUpPushyNotifications()
    }

    override fun retryLoading() {
        if (isVpnPermissionGranted) {
            startLoading()
        }
    }

    private fun configure() {
        applyDarkMode()
        binding.onceAnimationView.addAnimatorListener(object : OnAnimationCompletedListener() {

            override fun onAnimationEnd(animation: Animator?) {
                viewModel.animationLoaded()
                binding.onceAnimationView.visibility = View.GONE
                binding.onceAnimationView.cancelAnimation()
                binding.loopAnimationView.visibility = View.VISIBLE
                binding.loopAnimationView.playAnimation()
            }
        })
    }

    private fun subscribeViewModel() {
        viewModel.navigateForward.observe(this, {
            navigateForward()
        })
    }

    private fun applyDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        delegate.applyDayNight()
    }

    private fun setUpPushyNotifications() {
        pushyNotifications.register()
        pushyNotifications.listen()
    }

    private fun navigateForward() {
        when {
            !viewModel.isUserAlreadyLogin() -> {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
            !viewModel.isTermsAccepted() -> {
                startActivity(Intent(this, TermsOfUseActivity::class.java))
            }
            viewModel.isTopUpFlowShown() -> {
                startActivity(Intent(this, HomeSelectionActivity::class.java))
            }
            viewModel.isAccountCreated() -> {
                val intent = Intent(this, PrepareTopUpActivity::class.java).apply {
                    putExtra(PrepareTopUpActivity.IS_NEW_USER_KEY, viewModel.isNewUser())
                }
                startActivity(intent)
            }
            viewModel.isTermsAccepted() -> {
                startActivity(Intent(this, CreateAccountActivity::class.java))
            }
        }
        finish()
    }

    private fun ensureVpnServicePermission() {
        val vpnServiceIntent = VpnService.prepare(this)
        if (vpnServiceIntent == null) {
            isVpnPermissionGranted = true
            startLoading()
        } else {
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    isVpnPermissionGranted = true
                    startLoading()
                } else {
                    showPermissionErrorToast()
                    finish()
                }
            }.launch(vpnServiceIntent)
        }
    }

    private fun showPermissionErrorToast() {
        Toast.makeText(
            this,
            getString(R.string.error_vpn_permission),
            Toast.LENGTH_LONG
        ).apply {
            (view.findViewById<View>(android.R.id.message) as TextView).gravity = Gravity.CENTER
        }.show()
    }

    private fun startLoading() {
        if (isInternetAvailable) {
            val deferredMysteriumCoreService = App.getInstance(this).deferredMysteriumCoreService
            balanceViewModel.initDeferredNode(deferredMysteriumCoreService)
            viewModel.startLoading(deferredMysteriumCoreService).observe(this) { result ->
                result.onSuccess {
                    binding.onceAnimationView.playAnimation()
                    viewModel.initRepository()
                }
            }
            checkUserLocale()
        }
    }

    private fun checkUserLocale() {
        val languageForApply = viewModel.initUserLocaleLanguage(
            countryCode = LanguagesUtil.getUserDefaultLanguage()
        )
        Locale.setDefault(Locale(languageForApply))
        resources.configuration.setLocale(Locale(languageForApply))
        resources.updateConfiguration(resources.configuration, resources.displayMetrics)
    }
}
