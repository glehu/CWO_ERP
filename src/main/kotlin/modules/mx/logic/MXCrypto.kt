package modules.mx.logic

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

enum class OutputFormat {
    String, ByteArray
}

/**
 * Used to encrypt an input string using the Keccak SHA3_256 algorithm.
 * An output format has to be specified, resulting in the encrypted input string being returned either as
 * a String or a ByteArray.
 * @return the Keccak SHA3_256 encrypted/hashed input string.
 */
fun encryptKeccak(input: String, outputFormat: OutputFormat): Any {
    return when (outputFormat) {
        OutputFormat.ByteArray -> input.digestKeccak(KeccakParameter.SHA3_256)
        OutputFormat.String -> input.digestKeccak(KeccakParameter.SHA3_256).toString()
    }
}