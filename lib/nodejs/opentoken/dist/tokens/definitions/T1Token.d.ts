/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { AttributeExpression } from '../../attributes/AttributeExpression';
import { Token } from '../Token';
/**
 * Represents the token definition for token T1.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * U(last-name)|U(first-name-1)|U(gender)|birth-date
 */
export declare class T1Token implements Token {
    private static readonly ID;
    private definition;
    constructor();
    getIdentifier(): string;
    getDefinition(): AttributeExpression[];
}
//# sourceMappingURL=T1Token.d.ts.map