package modules.m3

import db.CwODB
import kotlinx.serialization.Serializable
import modules.IEntry
import modules.IInvoice

@Serializable
data class Invoice(override var uID: Int) : IEntry, IInvoice
{
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
    var price: Double = 0.0

    fun initialize()
    {
        if (uID == -1) uID = CwODB().getUniqueID("M3")
    }
}