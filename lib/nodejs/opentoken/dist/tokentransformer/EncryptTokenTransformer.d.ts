/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { TokenTransformer } from './TokenTransformer';
/**
 * Transforms the token using AES-256 symmetric encryption.
 *
 * Uses AES-256-GCM encryption algorithm.
 */
export declare class EncryptTokenTransformer implements TokenTransformer {
    private static readonly ENCRYPTION_ALGORITHM;
    private static readonly KEY_BYTE_LENGTH;
    private static readonly IV_SIZE;
    private secretKey;
    /**
     * Initializes the underlying cipher (AES) with the encryption secret.
     *
     * @param encryptionKey - The encryption key. The key must be 32 characters long.
     */
    constructor(encryptionKey: string);
    /**
     * Encryption token transformer.
     *
     * Encrypts the token using AES-256-GCM symmetric encryption algorithm.
     *
     * @param token - The token to encrypt.
     * @returns Encrypted token in base64 format.
     */
    transform(token: string): string;
}
//# sourceMappingURL=EncryptTokenTransformer.d.ts.map