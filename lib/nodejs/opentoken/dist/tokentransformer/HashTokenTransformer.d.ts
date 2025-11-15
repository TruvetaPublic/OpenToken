/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { TokenTransformer } from './TokenTransformer';
/**
 * Transforms the token using a cryptographic hash function with a secret key.
 *
 * Uses HMAC-SHA256 algorithm.
 */
export declare class HashTokenTransformer implements TokenTransformer {
    private hashingSecret;
    /**
     * Initializes the transformer with the secret key.
     *
     * @param hashingSecret - The cryptographic secret key.
     */
    constructor(hashingSecret: string);
    /**
     * Hash token transformer.
     *
     * The token is transformed using HMAC SHA256 algorithm.
     *
     * @param token - The token to hash.
     * @returns Hashed token in base64 format.
     */
    transform(token: string): string;
}
//# sourceMappingURL=HashTokenTransformer.d.ts.map