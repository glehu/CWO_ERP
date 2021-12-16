package modules.m3.misc

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceIni(
    var statusTexts: MutableMap<Int, String> = mutableMapOf(),
    var todoStatuses: String = "0",
    var autoCommission: Boolean = true,
    var autoCreateContacts: Boolean = true,
    var autoSendEMailConfirmation: Boolean = true
) {
    init {
        val default = mutableMapOf(
            0 to "Draft",
            1 to "Commissioned",
            2 to "Partially Paid",
            3 to "Paid",
            8 to "Cancelled",
            9 to "Finished"
        )
        for ((k, v) in default) {
            if (!statusTexts.containsKey(k)) {
                statusTexts[k] = v
            }
        }
    }
}
