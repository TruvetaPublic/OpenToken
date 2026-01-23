/**
 * Key exchange infrastructure for OpenToken using Elliptic Curve Diffie-Hellman (ECDH).
 * <p>
 * This package provides classes for managing ECDH key pairs, performing key exchange,
 * and deriving cryptographic keys for token hashing and encryption. The implementation
 * uses the P-384 (secp384r1) curve for broad compatibility and security.
 * 
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link com.truveta.opentoken.keyexchange.KeyPairManager} - Manages key pair lifecycle</li>
 *   <li>{@link com.truveta.opentoken.keyexchange.KeyExchange} - Performs ECDH and key derivation</li>
 *   <li>{@link com.truveta.opentoken.keyexchange.PublicKeyLoader} - Loads and validates public keys</li>
 *   <li>{@link com.truveta.opentoken.keyexchange.PemUtils} - PEM encoding/decoding utilities</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Sender side
 * KeyPairManager keyPairManager = new KeyPairManager();
 * KeyPair senderKeyPair = keyPairManager.getOrCreateKeyPair();
 * 
 * PublicKeyLoader loader = new PublicKeyLoader();
 * PublicKey receiverPublicKey = loader.loadPublicKey("receiver_public_key.pem");
 * 
 * KeyExchange keyExchange = new KeyExchange();
 * DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
 *     senderKeyPair.getPrivate(),
 *     receiverPublicKey
 * );
 * 
 * // Use keys.getHashingKeyAsString() and keys.getEncryptionKeyAsString()
 * // for token transformers
 * }</pre>
 * 
 * @see <a href="https://tools.ietf.org/html/rfc5869">RFC 5869: HKDF</a>
 * @see <a href="https://csrc.nist.gov/publications/detail/fips/186/4/final">FIPS 186-4: EC DSA</a>
 */
package com.truveta.opentoken.keyexchange;
