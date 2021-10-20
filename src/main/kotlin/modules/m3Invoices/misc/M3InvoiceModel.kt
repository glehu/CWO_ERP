package modules.m3Invoices.misc

import javafx.beans.property.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3Invoices.M3Invoice
import modules.m3Invoices.M3InvoicePosition
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.observableListOf
import tornadofx.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

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
    val priceProperty = SimpleDoubleProperty()
    var price: Double by priceProperty
    val paidProperty = SimpleDoubleProperty()
    var paid: Double by paidProperty
    val itemsProperty = observableListOf<M3InvoicePosition>()
    val priceCategoryProperty = SimpleIntegerProperty(0)
    var priceCategory: Int by priceCategoryProperty
    val finishedProperty = SimpleBooleanProperty(false)
    var finished: Boolean by finishedProperty
}

@ExperimentalSerializationApi
class InvoiceModel : ItemViewModel<InvoiceProperty>(InvoiceProperty()) {
    val uID = bind(InvoiceProperty::uIDProperty)
    val seller = bind(InvoiceProperty::sellerProperty)
    val sellerUID = bind(InvoiceProperty::sellerUIDProperty)
    val buyer = bind(InvoiceProperty::buyerProperty)
    val buyerUID = bind(InvoiceProperty::buyerUIDProperty)
    var date = bind(InvoiceProperty::dateProperty)
    var text = bind(InvoiceProperty::textProperty)
    var price = bind(InvoiceProperty::priceProperty)
    var paid = bind(InvoiceProperty::paidProperty)
    var items = bind(InvoiceProperty::itemsProperty)
    var priceCategory = bind(InvoiceProperty::priceCategoryProperty)
    var finished = bind(InvoiceProperty::finishedProperty)
}

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
    invoiceProperty.price = invoice.grossPrice
    invoiceProperty.paid = invoice.paid
    for ((_, v) in invoice.items) {
        invoiceProperty.itemsProperty.add(Json.decodeFromString(v))
    }
    invoiceProperty.priceCategory = invoice.priceCategory
    invoiceProperty.finished = invoice.finished
    return invoiceProperty
}

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
    invoice.grossPrice = invoiceProperty.price
    invoice.paid = invoiceProperty.paid
    for (item in invoiceProperty.itemsProperty) {
        invoice.items[invoice.items.size] = Json.encodeToString(item)
    }
    invoice.priceCategory = invoiceProperty.priceCategory
    invoice.finished = invoiceProperty.finished
    return invoice
}
