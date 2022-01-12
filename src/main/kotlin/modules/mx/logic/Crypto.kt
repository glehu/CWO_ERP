package modules.mx.logic

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.tokenGlobal
import org.komputing.khash.keccak.KeccakParameter
import org.komputing.khash.keccak.extensions.digestKeccak
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Used to encrypt an input string with the AES encryption algorithm.
 * If no token is provided, the default one will be used.
 * @return the AES encrypted input string.
 */
fun encryptAES(input: String, token: String = tokenGlobal): String {
  val cipher = Cipher.getInstance("AES")
  val keySpec = SecretKeySpec(token.toByteArray(), "AES")
  cipher.init(Cipher.ENCRYPT_MODE, keySpec)
  val encrypt = cipher.doFinal(input.toByteArray())
  return Base64.getEncoder().encodeToString(encrypt)
}

/**
 * Used to decrypt an AES encrypted input string with the AES decryption algorithm.
 * If no token is provided, the default one will be used.
 * @return the AES decrypted string.
 */
fun decryptAES(input: String, token: String = tokenGlobal): String {
  val cipher = Cipher.getInstance("AES")
  val keySpec = SecretKeySpec(token.toByteArray(), "AES")
  cipher.init(Cipher.DECRYPT_MODE, keySpec)
  val decrypt = cipher.doFinal(Base64.getDecoder().decode(input))
  return String(decrypt)
}

/**
 * Used to encrypt an input string using the Keccak SHA3_256 algorithm.
 * @return the Base64 encoded Keccak SHA3_256 hashed input string.
 */
@InternalAPI
@ExperimentalSerializationApi
fun encryptKeccak(input: String, salt: String = "", pepper: String = ""): String {
  return Base64.getEncoder()
    .encodeToString(
      "$pepper$input$salt"
        .digestKeccak(parameter = KeccakParameter.SHA3_512)
    )
}

/**
 * Validates an input string (e.g. a JSON string) by comparing it
 * to the provided Base64 encoded Keccak SHA3_256 hashed string.
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
