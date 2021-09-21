package modules.mx.logic

import modules.mx.tokenGlobal
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

fun encryptAES(input: String, token: String = tokenGlobal): String {
    val cipher = Cipher.getInstance("AES")
    val keySpec = SecretKeySpec(token.toByteArray(), "AES")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec)
    val encrypt = cipher.doFinal(input.toByteArray())
    return Base64.getEncoder().encodeToString(encrypt)
}

fun decryptAES(input: String, token: String = tokenGlobal): String {
    val cipher = Cipher.getInstance("AES")
    val keySpec = SecretKeySpec(token.toByteArray(), "AES")
    cipher.init(Cipher.DECRYPT_MODE, keySpec)
    val decrypt = cipher.doFinal(Base64.getDecoder().decode(input))
    return String(decrypt)
}