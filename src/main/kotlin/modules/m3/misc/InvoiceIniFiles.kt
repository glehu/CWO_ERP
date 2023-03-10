package modules.m3.misc

import kotlinx.serialization.Serializable

/**
 * The auto storage selection order type.
 *
 * Possible values are:
 *
 * LIFO - Last In First Out
 *
 * FIFO - First In First Out
 *
 * HIFO - Highest In First Out
 *
 * LOFO - Lowest In First Out
 *
 * FEFO - First Expired First Out
 *
 */
enum class AutoStorageSelectionOrderType {
  LIFO, FIFO, HIFO, LOFO, FEFO
}

@Serializable
data class InvoiceIni(
  var statusTexts: MutableMap<Int, String> = mutableMapOf(),
  var todoStatuses: String = "0123",
  var autoCommission: Boolean = true,
  var autoCreateContacts: Boolean = true,
  var autoSendEmailConfirmation: Boolean = false,
  var autoStorageSelection: Boolean = true,
  var autoStorageSelectionOrder: String = "LIFO"
) {
  init {
    val default = mutableMapOf(
            0 to "Draft", 1 to "Commissioned", 2 to "Partially Paid", 3 to "Paid", 8 to "Cancelled", 9 to "Delivery")
    for ((k, v) in default) {
      if (!statusTexts.containsKey(k)) {
        statusTexts[k] = v
      }
    }
  }
}
