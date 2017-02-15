package cz.koto.misak.keystorecompat

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyNotYetValidException
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.*
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

internal object KeystoreCrypto {

    private val LOG_TAG = javaClass.name
    val ORDER_FOR_ENCRYPTED_DATA = ByteOrder.BIG_ENDIAN

    @TargetApi(Build.VERSION_CODES.M)
    fun encryptAES(secret: ByteArray, secretKeyEntry: KeyStore.SecretKeyEntry, useBase64Encoding: Boolean): String {
        var iv: ByteArray
        var encryptedKeyForRealm: ByteArray
        try {
            val key = secretKeyEntry.secretKey
            val inCipher = Cipher.getInstance(KeystoreCompatImpl.keystoreCompat.getCipherMode())
            inCipher.init(Cipher.ENCRYPT_MODE, key)
            encryptedKeyForRealm = inCipher.doFinal(secret)
            iv = inCipher.iv

        } catch (nve: KeyNotYetValidException) {
            Log.e(LOG_TAG, "encryptAES error: key's validity start date is probably in the future", nve)
            /**
             * TODO solve android.security.keystore.KeyNotYetValidException: Key not yet valid
             * - Indicates that a cryptographic operation failed because the employed key's validity start date is in the future.
             */
            throw nve
        } catch (nae: UserNotAuthenticatedException) {
            Log.e(LOG_TAG, "encryptAES error: user probably exceeded setUserAuthenticationValidityDurationSeconds", nae)
            /**
             *  TODO solve UserNotAuthenticatedException when user want to encrypt data and user exceeded setUserAuthenticationValidityDurationSeconds
             * android.security.keystore.UserNotAuthenticatedException: User not authenticated
             * at android.security.KeyStore.getInvalidKeyException(KeyStore.java:712)
             * at javax.crypto.Cipher.init(Cipher.java:1143)
             */
            throw nae
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Unexpected encryptAES error", e)
            throw e
        }
        val ivAndEncryptedKey = ByteArray(Integer.SIZE + iv.size + encryptedKeyForRealm.size)

        val buffer = ByteBuffer.wrap(ivAndEncryptedKey)
        buffer.order(ORDER_FOR_ENCRYPTED_DATA)
        buffer.putInt(iv.size)
        buffer.put(iv)
        buffer.put(encryptedKeyForRealm)


        if (useBase64Encoding) {
            return Base64.encodeToString(ivAndEncryptedKey, Base64.DEFAULT)
        } else {
            return String(ivAndEncryptedKey, Charsets.UTF_8)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun decryptAES(secretKeyEntry: KeyStore.SecretKeyEntry, encryptedSecret: String, isBase64Encoded: Boolean): ByteArray {

        var ivAndEncryptedKey: ByteArray = if (isBase64Encoded) Base64.decode(encryptedSecret, Base64.DEFAULT) else encryptedSecret.toByteArray(Charsets.UTF_8)


        val buffer = ByteBuffer.wrap(ivAndEncryptedKey)
        buffer.order(ORDER_FOR_ENCRYPTED_DATA)

        val ivLength = buffer.int
        val iv = ByteArray(ivLength)
        val encryptedKey = ByteArray(ivAndEncryptedKey.size - Integer.SIZE - ivLength)

        buffer.get(iv)
        buffer.get(encryptedKey)

        try {
            val cipher = Cipher.getInstance(KeystoreCompatImpl.keystoreCompat.getCipherMode())
            val ivSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKeyEntry.secretKey, ivSpec)

            return cipher.doFinal(encryptedKey)

        } catch (e: Exception) {
            when (e) {
                is InvalidKeyException -> {
                    throw RuntimeException("key is invalid.")
                }
                is UnrecoverableKeyException -> {
                }
                is NoSuchAlgorithmException -> {
                }
                is BadPaddingException -> {
                }
                is KeyStoreException -> {
                }
                is IllegalBlockSizeException -> {
                }
                is InvalidAlgorithmParameterException -> {
                }
            }
            Log.e(LOG_TAG, "decryptAES error", e)
            throw e
        }
    }

    /**
     * Encrypt bytes to Base64 encoded string.
     * For input secret as string use: secret.toByteArray(Charsets.UTF_8)
     */

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun encryptRSA(secret: ByteArray, privateKeyEntry: KeyStore.PrivateKeyEntry, useBase64Encoding: Boolean): String {
        try {
            //When you are using asymmetric encryption algorithms, you need to use the public key to encrypt
            val publicKey = privateKeyEntry.certificate.publicKey as RSAPublicKey

            /**
             * AndroidOpenSSL works on Lollipop.
             * But on marshmallow it throws: java.security.InvalidKeyException: Need RSA private or public key
             *
             * On Android 6.0 you should Not use "AndroidOpenSSL" for cipher creation,
             * it would fail with "Need RSA private or public key" at cipher init for decryption.
             * Simply use Cipher.getInstance("RSA/ECB/PKCS1Padding")
             */
            val inCipher = Cipher.getInstance(KeystoreCompatImpl.keystoreCompat.getCipherMode()/*, "AndroidOpenSSL"*/)
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val outputStream = ByteArrayOutputStream()
            val cipherOutputStream = CipherOutputStream(outputStream, inCipher)
            cipherOutputStream.write(secret)
            cipherOutputStream.close()

            if (useBase64Encoding) {
                return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            } else {
                return String(outputStream.toByteArray(), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "encryptRSA error", e)
            throw e
        }
    }

    /**
     * Decrypt Base64 encoded encrypted byteArray.
     * For output as string user: String(byteArray, 0, byteArray.size, Charsets.UTF_8)
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun decryptRSA(privateKeyEntry: KeyStore.PrivateKeyEntry, encryptedSecret: String, isBase64Encoded: Boolean): ByteArray {
        try {

            var inputByteArray: ByteArray = if (isBase64Encoded) Base64.decode(encryptedSecret, Base64.DEFAULT) else encryptedSecret.toByteArray(Charsets.UTF_8)
            /**
             * AndroidOpenSSL works on Lollipop.
             * But on marshmallow it throws: java.security.InvalidKeyException: Need RSA private or public key
             *
             * On Android 6.0 you should Not use "AndroidOpenSSL" for cipher creation,
             * it would fail with "Need RSA private or public key" at cipher init for decryption.
             * Simply use Cipher.getInstance("RSA/ECB/PKCS1Padding")
             */
            val output = Cipher.getInstance(KeystoreCompatImpl.keystoreCompat.getCipherMode()/*, "AndroidOpenSSL"*/)
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)

            val cipherInputStream = CipherInputStream(ByteArrayInputStream(inputByteArray), output)
            val values = ArrayList <Byte>()
            var nextByte: Int = -1

            while ({ nextByte = cipherInputStream.read(); nextByte }() != -1) {
                values.add(nextByte.toByte())
            }

            val bytes = ByteArray(values.size)
            for (i in bytes.indices) {
                bytes[i] = values[i]
            }

            return bytes

        } catch (e: Exception) {
            Log.e(LOG_TAG, "decryptRSA error", e)
            throw e
        }
    }
}