package modules.mx.logic

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.komputing.khash.keccak.KeccakParameter
import org.komputing.khash.keccak.extensions.digestKeccak
import java.util.*

/**
 * Used to encrypt an input string using the Keccak SHA3_512 algorithm.
 * @return the Base64 encoded and hashed input string.
 */
@InternalAPI
@ExperimentalSerializationApi
fun encryptKeccak(
  input: String,
  salt: String = "",
  pepper: String = ""
): String {
  return Base64.getEncoder().encodeToString(
          "$pepper$input$salt".digestKeccak(parameter = KeccakParameter.SHA3_512))
}

/**
 * Validates an input string (e.g. a JSON string) by comparing it
 * to the provided Base64 encoded Keccak SHA3_512 hashed string.
 * @return true if the input matched the hash.
 */
@InternalAPI
@ExperimentalSerializationApi
fun validateKeccak(
  input: String,
  base64KeccakString: String,
  salt: String = "",
  pepper: String = ""
): Boolean {
  return (encryptKeccak("$pepper$input$salt") == base64KeccakString)
}
