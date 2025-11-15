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
exports.HashTokenTransformer = void 0;
const crypto = __importStar(require("crypto"));
/**
 * Transforms the token using a cryptographic hash function with a secret key.
 *
 * Uses HMAC-SHA256 algorithm.
 */
class HashTokenTransformer {
    /**
     * Initializes the transformer with the secret key.
     *
     * @param hashingSecret - The cryptographic secret key.
     */
    constructor(hashingSecret) {
        if (!hashingSecret || hashingSecret.trim().length === 0) {
            throw new Error('Hashing secret cannot be null or empty');
        }
        this.hashingSecret = hashingSecret;
    }
    /**
     * Hash token transformer.
     *
     * The token is transformed using HMAC SHA256 algorithm.
     *
     * @param token - The token to hash.
     * @returns Hashed token in base64 format.
     */
    transform(token) {
        if (!token || token.trim().length === 0) {
            throw new Error('Token cannot be null or empty');
        }
        const hmac = crypto.createHmac('sha256', this.hashingSecret);
        hmac.update(token);
        const hash = hmac.digest();
        return Buffer.from(hash).toString('base64');
    }
}
exports.HashTokenTransformer = HashTokenTransformer;
//# sourceMappingURL=HashTokenTransformer.js.map