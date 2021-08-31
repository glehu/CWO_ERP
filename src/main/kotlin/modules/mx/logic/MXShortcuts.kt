package modules.mx.logic

import java.util.*
import kotlin.streams.asSequence

//-------------------------------------v
//---------- USEFUL FUNCTIONS ---------|
//-------------------------------------^

fun String.extractNumbers(): String {
    val numberExtractor = "[0-9]+".toRegex()
    return numberExtractor.find(this)!!.value
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