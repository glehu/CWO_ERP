package modules.m3.misc

import io.ktor.util.*
import javafx.beans.property.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.M3Invoice
import modules.m3.M3InvoicePosition
import modules.m3.logic.M3CLIController
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.observableListOf
import tornadofx.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class InvoiceProperty {
    val uIDProperty = SimpleIntegerProperty(-1)
    var uID: Int by uIDProperty
    val sellerProperty = SimpleStringProperty()
    var seller: String by sellerProperty
    val sellerUIDProperty = SimpleIntegerProperty(-1)
    var sellerUID: Int by sellerUIDProperty
    val buyerProperty = SimpleStringProperty()
    var buyer: String by buyerProperty
    val buyerUIDProperty = SimpleIntegerProperty(-1)
    var buyerUID: Int by buyerUIDProperty
    val dateProperty = SimpleObjectProperty(LocalDate.now())
    var date: LocalDate by dateProperty
    val textProperty = SimpleStringProperty()
    var text: String by textProperty
    val grossTotalProperty = SimpleDoubleProperty()
    var grossTotal: Double by grossTotalProperty
    val netTotalProperty = SimpleDoubleProperty()
    var netTotal: Double by netTotalProperty
    val paidGrossProperty = SimpleDoubleProperty()
    var paidGross: Double by paidGrossProperty
    val paidNetProperty = SimpleDoubleProperty()
    var paidNet: Double by paidNetProperty
    val itemsProperty = observableListOf<M3InvoicePosition>()
    val priceCategoryProperty = SimpleIntegerProperty(0)
    var priceCategory: Int by priceCategoryProperty
    val statusProperty = SimpleIntegerProperty(0)
    var status: Int by statusProperty
    val statusTextProperty = SimpleStringProperty(M3CLIController().getStatusText(0))
    var statusText: String by statusTextProperty
    val finishedProperty = SimpleBooleanProperty(false)
    var finished: Boolean by finishedProperty
    val customerNoteProperty = SimpleStringProperty("?")
    var customerNote: String by customerNoteProperty
    val internalNoteProperty = SimpleStringProperty("?")
    var internalNote: String by internalNoteProperty
    val emailConfirmationSentProperty = SimpleBooleanProperty(false)
    var emailConfirmationSent: Boolean by emailConfirmationSentProperty
}

@InternalAPI
@ExperimentalSerializationApi
class InvoiceModel : ItemViewModel<InvoiceProperty>(InvoiceProperty()) {
    val uID = bind(InvoiceProperty::uIDProperty)
    val seller = bind(InvoiceProperty::sellerProperty)
    val sellerUID = bind(InvoiceProperty::sellerUIDProperty)
    val buyer = bind(InvoiceProperty::buyerProperty)
    val buyerUID = bind(InvoiceProperty::buyerUIDProperty)
    var date = bind(InvoiceProperty::dateProperty)
    var text = bind(InvoiceProperty::textProperty)
    var grossTotal = bind(InvoiceProperty::grossTotalProperty)
    var netTotal = bind(InvoiceProperty::netTotalProperty)
    var paidGross = bind(InvoiceProperty::paidGrossProperty)
    var paidNet = bind(InvoiceProperty::paidNetProperty)
    var items = bind(InvoiceProperty::itemsProperty)
    var priceCategory = bind(InvoiceProperty::priceCategoryProperty)
    var status = bind(InvoiceProperty::statusProperty)
    var statusText = bind(InvoiceProperty::statusTextProperty)
    var finished = bind(InvoiceProperty::finishedProperty)
    var customerNote = bind(InvoiceProperty::customerNoteProperty)
    var internalNote = bind(InvoiceProperty::internalNoteProperty)
    var emailConfirmationSent = bind(InvoiceProperty::emailConfirmationSentProperty)
}

@InternalAPI
@ExperimentalSerializationApi
fun getInvoicePropertyFromInvoice(invoice: M3Invoice): InvoiceProperty {
    val invoiceProperty = InvoiceProperty()
    invoiceProperty.uID = invoice.uID
    invoiceProperty.seller = invoice.seller
    invoiceProperty.sellerUID = invoice.sellerUID
    invoiceProperty.buyer = invoice.buyer
    invoiceProperty.buyerUID = invoice.buyerUID
    invoiceProperty.date = LocalDate.parse(invoice.date, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    invoiceProperty.text = invoice.text
    invoiceProperty.grossTotal = invoice.grossTotal
    invoiceProperty.netTotal = invoice.netTotal
    invoiceProperty.paidGross = invoice.grossPaid
    invoiceProperty.paidNet = invoice.netPaid
    for ((_, v) in invoice.items) {
        invoiceProperty.itemsProperty.add(Json.decodeFromString(v))
    }
    invoiceProperty.priceCategory = invoice.priceCategory
    invoiceProperty.status = invoice.status
    invoiceProperty.statusText = M3CLIController().getStatusText(invoice.status)
    invoiceProperty.finished = invoice.finished
    invoiceProperty.customerNote = invoice.customerNote
    invoiceProperty.internalNote = invoice.internalNote
    invoiceProperty.emailConfirmationSent = invoice.emailConfirmationSent
    return invoiceProperty
}

@InternalAPI
@ExperimentalSerializationApi
fun getInvoiceFromInvoiceProperty(invoiceProperty: InvoiceProperty): M3Invoice {
    val invoice = M3Invoice(-1)
    invoice.uID = invoiceProperty.uID
    invoice.seller = invoiceProperty.seller
    invoice.sellerUID = invoiceProperty.sellerUID
    invoice.buyer = invoiceProperty.buyer
    invoice.buyerUID = invoiceProperty.buyerUID
    invoice.date = invoiceProperty.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    invoice.text = invoiceProperty.text
    invoice.grossTotal = invoiceProperty.grossTotal
    invoice.netTotal = invoiceProperty.netTotal
    invoice.grossPaid = invoiceProperty.paidGross
    invoice.netPaid = invoiceProperty.paidNet
    for (item in invoiceProperty.itemsProperty) {
        invoice.items[invoice.items.size] = Json.encodeToString(item)
    }
    invoice.priceCategory = invoiceProperty.priceCategory
    invoice.status = invoiceProperty.status
    invoice.statusText = M3CLIController().getStatusText(invoiceProperty.status)
    invoice.finished = invoiceProperty.finished
    invoice.customerNote = invoiceProperty.customerNote
    invoice.internalNote = invoiceProperty.internalNote
    invoice.emailConfirmationSent = invoiceProperty.emailConfirmationSent
    return invoice
}
