package updated.mysterium.vpn.common

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import updated.mysterium.vpn.common.extensions.TAG
import updated.mysterium.vpn.common.playstore.PlayStoreHelper

class PlayStoreHelperImpl : PlayStoreHelper {

    override fun showReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            } else {
                Log.e(TAG, task.exception?.localizedMessage ?: task.exception.toString())
            }
        }
    }
}
