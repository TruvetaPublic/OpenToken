/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { AttributeExpression } from '../../attributes/AttributeExpression';
import { BirthDateAttribute } from '../../attributes/person/BirthDateAttribute';
import { FirstNameAttribute } from '../../attributes/person/FirstNameAttribute';
import { LastNameAttribute } from '../../attributes/person/LastNameAttribute';
import { Token } from '../Token';

// NOTE: PostalCodeAttribute not yet implemented - will need to be added
// For now, we'll create a placeholder
class PostalCodeAttributePlaceholder {}

/**
 * Represents the token definition for token T2.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * U(last-name)|U(first-name)|birth-date|postal-code-3
 */
export class T2Token implements Token {
  private static readonly ID = 'T2';
  private definition: AttributeExpression[];

  constructor() {
    this.definition = [
      new AttributeExpression(LastNameAttribute as any, 'T|U'),
      new AttributeExpression(FirstNameAttribute as any, 'T|U'),
      new AttributeExpression(BirthDateAttribute as any, 'T|D'),
      // TODO: Replace with actual PostalCodeAttribute when implemented
      new AttributeExpression(PostalCodeAttributePlaceholder as any, 'T|S(0,3)|U'),
    ];
  }

  getIdentifier(): string {
    return T2Token.ID;
  }

  getDefinition(): AttributeExpression[] {
    return this.definition;
  }
}
