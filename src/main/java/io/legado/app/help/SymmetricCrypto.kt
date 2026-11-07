package io.legado.app.help

import io.legado.app.utils.Base64
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SymmetricCrypto(
    val transformation: String,
    key: Any,
    iv: Any? = null
) {
    private val keyBytes: ByteArray = toByteArray(key)
    private val ivBytes: ByteArray? = if (iv == null) null else toByteArray(iv).let { if (it.isEmpty()) null else it }
    private val algorithm: String = transformation.substringBefore('/')

    private fun toByteArray(obj: Any?): ByteArray {
        return when (obj) {
            null -> ByteArray(0)
            is ByteArray -> obj
            is String -> obj.toByteArray(Charsets.UTF_8)
            is Array<*> -> ByteArray(obj.size) { (obj[it] as Number).toByte() }
            is Collection<*> -> ByteArray(obj.size) { (obj.elementAt(it) as Number).toByte() }
            else -> obj.toString().toByteArray(Charsets.UTF_8)
        }
    }

    fun decrypt(data: Any?): ByteArray {
        if (data == null) return ByteArray(0)
        val dataBytes: ByteArray = when (data) {
            is ByteArray -> data
            is Array<*> -> ByteArray(data.size) { (data[it] as Number).toByte() }
            is Collection<*> -> ByteArray(data.size) { (data.elementAt(it) as Number).toByte() }
            is String -> {
                val trimmed = data.trim()
                try {
                    Base64.decode(trimmed, Base64.DEFAULT)
                } catch (e: Exception) {
                    trimmed.toByteArray(Charsets.UTF_8)
                }
            }
            else -> data.toString().toByteArray(Charsets.UTF_8)
        }
        return try {
            symmetric(dataBytes, false) ?: ByteArray(0)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    fun decryptStr(data: Any?): String {
        if (data == null) return ""
        val decrypted = decrypt(data)
        return String(decrypted, Charsets.UTF_8)
    }

    fun encrypt(data: Any?): ByteArray {
        if (data == null) return ByteArray(0)
        val dataBytes: ByteArray = when (data) {
            is ByteArray -> data
            is Array<*> -> ByteArray(data.size) { (data[it] as Number).toByte() }
            is Collection<*> -> ByteArray(data.size) { (data.elementAt(it) as Number).toByte() }
            is String -> data.toByteArray(Charsets.UTF_8)
            else -> data.toString().toByteArray(Charsets.UTF_8)
        }
        return try {
            symmetric(dataBytes, true) ?: ByteArray(0)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    fun encryptBase64(data: Any?): String {
        return Base64.encodeToString(encrypt(data), Base64.NO_WRAP)
    }

    fun encryptHex(data: Any?): String {
        return encrypt(data).joinToString("") { "%02x".format(it) }
    }

    private fun symmetric(data: ByteArray, isEncrypt: Boolean): ByteArray? {
        val keySpec = SecretKeySpec(keyBytes, algorithm)
        val cipher = Cipher.getInstance(transformation)
        val mode = if (isEncrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE
        if (ivBytes == null || ivBytes.isEmpty()) {
            cipher.init(mode, keySpec)
        } else {
            val params: AlgorithmParameterSpec = IvParameterSpec(ivBytes)
            cipher.init(mode, keySpec, params)
        }
        return cipher.doFinal(data)
    }
}
