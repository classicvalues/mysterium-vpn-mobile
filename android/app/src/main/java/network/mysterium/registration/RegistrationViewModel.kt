package network.mysterium.registration

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mysterium.RegisterIdentityRequest
import network.mysterium.db.AppDatabase
import network.mysterium.registration.RegistrationProgress.*
import network.mysterium.service.core.NodeRepository
import network.mysterium.ui.IdentityModel
import network.mysterium.ui.IdentityRegistrationStatus
import java.math.BigDecimal

class RegistrationViewModel(private val nodeRepository: NodeRepository, private val db: AppDatabase) : ViewModel() {

    val topupAmount = MutableLiveData<BigDecimal>()
    val registrationFee = MutableLiveData<BigDecimal>(BigDecimal(0))
    val totalAmount = MediatorLiveData<BigDecimal>()
    val identity = MutableLiveData<IdentityModel>()
    val registrationDone = MutableLiveData<Boolean>(false)
    val balance = MutableLiveData<BigDecimal>(BigDecimal(0))
    val progress = MutableLiveData<RegistrationProgress>(NOT_STARTED)

    init {
        val sumTotal = fun(): BigDecimal {
            val t = topupAmount.value ?: BigDecimal(0)
            val r = registrationFee.value ?: BigDecimal(0)
            return t.plus(r)
        }
        totalAmount.addSource(topupAmount) {
            totalAmount.value = sumTotal()
        }
    }

    suspend fun load() {
        loadIdentity()
        loadRegistrationFees()
        initListeners()
    }

    private suspend fun loadIdentity() {
        try {
            // Load node identity and its registration status.
            val nodeIdentity = nodeRepository.getIdentity()
            val identity = IdentityModel(
                    address = nodeIdentity.address,
                    channelAddress = nodeIdentity.channelAddress,
                    status = IdentityRegistrationStatus.parse(nodeIdentity.registrationStatus)
            )
            this.identity.postValue(identity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load account identity", e)
        }
    }

    private suspend fun loadRegistrationFees() {
        val fees = nodeRepository.identityRegistrationFees()
        registrationFee.value = BigDecimal.valueOf(fees.fee).setScale(0)
    }

    private suspend fun initListeners() {
        nodeRepository.registerIdentityRegistrationChangeCallback { statusString ->
            handleRegistrationStatusChange(IdentityRegistrationStatus.parse(statusString))
        }
        nodeRepository.registerBalanceChangeCallback { balance ->
            CoroutineScope(Dispatchers.Main).launch {
                handleBalanceChange(balance)
            }
        }
    }

    private fun handleRegistrationStatusChange(status: IdentityRegistrationStatus) {
        Log.i(TAG, "Identity registration status changed: ${this.identity.value} → $status")
        val identity = this.identity.value?.apply {
            this.status = status
        } ?: return
        this.identity.postValue(identity)
        if (identity.registered) {
            progress.postValue(DONE)
            CoroutineScope(Dispatchers.Main).launch {
                db.identityDao().delete()
                db.identityDao().insert(identity)
            }
        }
    }

    private fun handleBalanceChange(balance: Double) {
        if (this.balance.value?.compareTo(BigDecimal(balance)) == 0) {
            return
        }
        Log.i(TAG, "Balance changed: ${this.balance.value} → $balance")
        this.balance.postValue(BigDecimal.valueOf(balance))
    }

    fun balanceSufficientForRegistration(): Boolean {
        val balance = this.balance.value ?: return false
        val total = this.totalAmount.value ?: return false
        return balance >= total
    }

    suspend fun registered(): Boolean {
        val id = db.identityDao().get()
        return id?.registered ?: false
    }

    suspend fun register() {
        val identity = this.identity.value ?: return
        Log.i(TAG, "Registering identity ${identity.address}")
        try {
            val req = RegisterIdentityRequest().apply {
                identityAddress = identity.address
            }
            nodeRepository.registerIdentity(req)
            progress.postValue(IN_PROGRESS)
        } catch (e: Exception) {
            Log.i(TAG, "Failed to register identity ${identity.address}", e)
            progress.postValue(NOT_STARTED)
        }
    }

    companion object {
        private const val TAG = "RegistrationViewModel"
    }

}
