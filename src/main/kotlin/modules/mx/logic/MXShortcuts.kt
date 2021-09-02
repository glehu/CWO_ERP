package modules.mx.logic

import java.util.*
import kotlin.streams.asSequence

//-------------------------------------v
//---------- USEFUL FUNCTIONS ---------|
//-------------------------------------^

/**
 * Used to format an input string to be used as an index value. The string gets converted to uppercase letters and
 * whitespaces get filtered out.
 * @return the index formatted string
 */
fun indexFormat(text: String): String {
    val songNameArray = text.uppercase(Locale.getDefault()).toCharArray()
    var formatted = ""
    for (i in songNameArray.indices) {
        //Anything but whitespaces
        val regex = "^\\s$".toRegex()
        if (!regex.matches(songNameArray[i].toString())) {
            formatted += songNameArray[i]
        }
    }
    return formatted
}

/**
 * @return the default date (01.01.1970)
 */
fun getDefaultDate(): String {
    return "01.01.1970"
}

/**
 * @return a random string (letters or numbers) of a certain length.
 */
fun getRandomString(size: Long, numbers: Boolean = false): String {
    val dictionary = if (!numbers) "ABCDEFGHIJKLMNOPQRSTUVWXYZ" else "123456789"
    return Random().ints(size, 0, dictionary.length)
        .asSequence()
        .map(dictionary::get)
        .joinToString("")
}