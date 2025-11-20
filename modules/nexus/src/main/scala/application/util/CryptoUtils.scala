package app.mosia.nexus
package application.util

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey, Signature}
import java.util.Base64

object CryptoUtils:
  def loadECPublicKeyFromDer(derBytes: Array[Byte]): PublicKey =
    val kf   = KeyFactory.getInstance("EC")
    val spec = X509EncodedKeySpec(derBytes)
    kf.generatePublic(spec)

  /** signatureBase64 is either DER-encoded ECDSA signature or raw. We expect DER here. */
  def verifyEcdsaSha256(publicKeyDer: Array[Byte], data: Array[Byte], signatureBase64: String): Boolean =
    try
      val pub = loadECPublicKeyFromDer(publicKeyDer)
      val sig = Signature.getInstance("SHA256withECDSA")
      sig.initVerify(pub)
      sig.update(data)
      val sigBytes = Base64.getDecoder.decode(signatureBase64)
      sig.verify(sigBytes)
    catch case _: Throwable => false
