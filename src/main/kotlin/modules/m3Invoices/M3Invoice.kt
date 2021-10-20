package modules.m3Invoices

import interfaces.IEntry
import interfaces.IInvoice
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.m3GlobalIndex

@ExperimentalSerializationApi
@Serializable
data class M3Invoice(override var uID: Int) : IEntry, IInvoice {
    //*************************************************
    //********************** User Input Data **********
    //*************************************************

    //----------------------------------v
    //------------- Who? ---------------|
    //----------------------------------^
    var seller: String = "?"
    var sellerUID: Int = -1
    var buyer: String = "?"
    var buyerUID: Int = -1

    //----------------------------------v
    //------------- When? --------------|
    //----------------------------------^
    var date: String = "??.??.????"

    //----------------------------------v
    //------------- What? --------------|
    //----------------------------------^
    var text: String = "?"
    var grossPrice: Double = 0.0
    var netPrice: Double = 0.0
    var paid: Double = 0.0

    /**
     * This map contains the items of the invoice.
     *
     * The key is the position number inside the invoice. The value is the JSON serialized item line.
     */
    var items: MutableMap<Int, String> = mutableMapOf()

    //*************************************************
    //****************** Auto Generated Data **********
    //*************************************************

    var finished: Boolean = false
    var priceCategory: Int = 0
    private var isIncome: Boolean = false

    override fun initialize() {
        if (uID == -1) uID = m3GlobalIndex!!.getUID().toInt()
        if (grossPrice > 0) isIncome = true
    }
}
