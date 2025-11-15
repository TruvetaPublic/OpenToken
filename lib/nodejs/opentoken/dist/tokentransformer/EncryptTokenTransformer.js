"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.EncryptTokenTransformer = void 0;
const crypto = __importStar(require("crypto"));
/**
 * Transforms the token using AES-256 symmetric encryption.
 *
 * Uses AES-256-GCM encryption algorithm.
 */
class EncryptTokenTransformer {
    /**
     * Initializes the underlying cipher (AES) with the encryption secret.
     *
     * @param encryptionKey - The encryption key. The key must be 32 characters long.
     */
    constructor(encryptionKey) {
        if (encryptionKey.length !== EncryptTokenTransformer.KEY_BYTE_LENGTH) {
            throw new Error(`Key must be ${EncryptTokenTransformer.KEY_BYTE_LENGTH} characters long`);
        }
        this.secretKey = Buffer.from(encryptionKey, 'utf8');
    }
    /**
     * Encryption token transformer.
     *
     * Encrypts the token using AES-256-GCM symmetric encryption algorithm.
     *
     * @param token - The token to encrypt.
     * @returns Encrypted token in base64 format.
     */
    transform(token) {
        if (!token || token.trim().length === 0) {
            throw new Error('Token cannot be null or empty');
        }
        // Generate random IV (Initialization Vector)
        const iv = crypto.randomBytes(EncryptTokenTransformer.IV_SIZE);
        // Create cipher
        const cipher = crypto.createCipheriv(EncryptTokenTransformer.ENCRYPTION_ALGORITHM, this.secretKey, iv);
        // Encrypt the token
        let encrypted = cipher.update(token, 'utf8');
        encrypted = Buffer.concat([encrypted, cipher.final()]);
        // Get the authentication tag
        const authTag = cipher.getAuthTag();
        // Combine IV + encrypted data + auth tag
        const result = Buffer.concat([iv, encrypted, authTag]);
        // Return as base64
        return result.toString('base64');
    }
}
exports.EncryptTokenTransformer = EncryptTokenTransformer;
EncryptTokenTransformer.ENCRYPTION_ALGORITHM = 'aes-256-gcm';
EncryptTokenTransformer.KEY_BYTE_LENGTH = 32;
EncryptTokenTransformer.IV_SIZE = 12;
//# sourceMappingURL=EncryptTokenTransformer.js.map