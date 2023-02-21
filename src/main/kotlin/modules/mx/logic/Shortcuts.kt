package modules.mx.logic

import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
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
  val charArray = text.uppercase(Locale.getDefault()).toCharArray()
  var formatted = ""
  for (i in charArray.indices) {
    //Anything but whitespaces
    val regex = "^\\s$".toRegex()
    if (!regex.matches(charArray[i].toString())) {
      formatted += charArray[i]
    }
  }
  return formatted.take(200)
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
  return Random().ints(size, 0, dictionary.length).asSequence().map(dictionary::get).joinToString("")
}

fun Double.roundTo(numFractionDigits: Int): Double {
  val factor = 10.0.pow(numFractionDigits.toDouble())
  return (this * factor).roundToInt() / factor
}

/**
 * Creates a two-dimensional array with a fixed length.
 * @return a two-dimensional array.
 */
fun d2Array(rows: Int, cols: Int): Array<Array<String>> {
  return Array(rows) { Array(cols) { "" } }
}
