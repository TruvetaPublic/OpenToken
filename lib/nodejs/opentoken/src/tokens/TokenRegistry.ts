/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { Token } from './Token';
import { T1Token } from './definitions/T1Token';
import { T2Token } from './definitions/T2Token';
import { T3Token } from './definitions/T3Token';
import { T4Token } from './definitions/T4Token';
import { T5Token } from './definitions/T5Token';

/**
 * Registry for all available tokens in the system.
 */
export class TokenRegistry {
  private constructor() {
    // Private constructor to prevent instantiation
  }

  /**
   * Load all available token definitions.
   */
  static loadAll(): Token[] {
    return [new T1Token(), new T2Token(), new T3Token(), new T4Token(), new T5Token()];
  }

  /**
   * Find a token by its ID.
   */
  static findById(id: string): Token | undefined {
    const tokens = this.loadAll();
    return tokens.find((token) => token.getIdentifier() === id);
  }
}
