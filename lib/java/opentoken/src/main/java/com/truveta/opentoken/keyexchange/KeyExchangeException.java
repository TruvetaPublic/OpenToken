/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

/**
 * Exception thrown when key exchange operations fail.
 * <p>
 * This exception is used to wrap various cryptographic and I/O exceptions
 * that may occur during ECDH key exchange, key derivation, or key management operations.
 */
public class KeyExchangeException extends Exception {
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new KeyExchangeException with the specified detail message.
     *
     * @param message the detail message
     */
    public KeyExchangeException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new KeyExchangeException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public KeyExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
