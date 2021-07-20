package modules.mx.misc

import java.util.*

//-------------------------------------v
//---------- USEFUL FUNCTIONS ---------|
//-------------------------------------^

fun String.extractNumbers(): String
{
    val numberExtractor = "[0-9]+".toRegex()
    return numberExtractor.find(this)!!.value
}

fun indexFormat(text: String): String
{
    val songNameArray = text.uppercase(Locale.getDefault()).toCharArray()
    var formatted = ""
    for (i in songNameArray.indices)
    {
        //Only alphanumerical characters (letters and numbers)
        val regex = "^[A-Z]?[0-9]?$".toRegex()
        if (regex.matches(songNameArray[i].toString()))
        {
            formatted += songNameArray[i]
        }
    }
    return formatted
}