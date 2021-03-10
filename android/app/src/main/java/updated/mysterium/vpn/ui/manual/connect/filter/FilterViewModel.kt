package updated.mysterium.vpn.ui.manual.connect.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import updated.mysterium.vpn.model.filter.NodeFilter
import updated.mysterium.vpn.model.filter.NodePrice
import updated.mysterium.vpn.model.filter.NodeQuality
import updated.mysterium.vpn.model.filter.NodeType
import updated.mysterium.vpn.model.manual.connect.CountryNodesModel
import updated.mysterium.vpn.model.manual.connect.PriceLevel
import updated.mysterium.vpn.model.manual.connect.ProposalModel

class FilterViewModel : ViewModel() {

    val proposalsList: LiveData<List<ProposalModel>>
        get() = _proposalsList

    lateinit var nodesModel: CountryNodesModel
    private val _proposalsList = MutableLiveData<List<ProposalModel>>()

    fun applyInitialFilter(countryNodesModel: CountryNodesModel, nodeFilter: NodeFilter) {
        nodesModel = countryNodesModel
        nodesModel.proposalList.sortedBy {
            it.payment.rate.perBytes
        }.forEachIndexed { index, proposalModel ->
            when {
                proposalModel.payment.rate.perBytes == 0.0 -> {
                    proposalModel.priceLevel = PriceLevel.FREE
                }
                index <= nodesModel.proposalList.size * 0.3 -> {
                    proposalModel.priceLevel = PriceLevel.LOW
                }
                index >= nodesModel.proposalList.size * 0.7 -> {
                    proposalModel.priceLevel = PriceLevel.HIGH
                }
                else -> {
                    proposalModel.priceLevel = PriceLevel.MEDIUM
                }
            }
        }
        _proposalsList.value = getFilteredProposalList(nodeFilter)
    }

    fun filterList(nodeFilter: NodeFilter) {
        _proposalsList.value = getFilteredProposalList(nodeFilter)
    }

    private fun getFilteredProposalList(nodeFilter: NodeFilter) = nodesModel.proposalList
        .filter {
            filterByType(it, nodeFilter.typeFilter)
        }.filter {
            filterByPrice(it, nodeFilter.priceFilter)
        }.filter {
            filterByQuality(it, nodeFilter.qualityFilter)
        }

    private fun filterByType(proposalModel: ProposalModel, nodeType: NodeType): Boolean {
        return if (nodeType == NodeType.ALL) {
            true
        } else {
            NodeType.from(proposalModel.nodeType) == nodeType
        }
    }

    private fun filterByPrice(proposalModel: ProposalModel, nodePrice: NodePrice): Boolean {
        val currentNodePrice = proposalModel.priceLevel
        return if (nodePrice == NodePrice.HIGH || currentNodePrice == PriceLevel.FREE) {
            true
        } else {
            NodePrice.from(currentNodePrice) == nodePrice
        }
    }

    private fun filterByQuality(proposalModel: ProposalModel, nodeQuality: NodeQuality): Boolean {
        val currentNodeQuality = NodeQuality.from(proposalModel.qualityLevel)
        return when (nodeQuality) {
            NodeQuality.LOW -> true
            NodeQuality.MEDIUM -> {
                currentNodeQuality == NodeQuality.MEDIUM || currentNodeQuality == NodeQuality.HIGH
            }
            NodeQuality.HIGH -> {
                currentNodeQuality == NodeQuality.HIGH
            }
        }
    }
}
