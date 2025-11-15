/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { AttributeExpression } from '../../attributes/AttributeExpression';
import { Token } from '../Token';
/**
 * Represents the token definition for token T5.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * U(last-name)|U(first-name-3)|U(gender)
 */
export declare class T5Token implements Token {
    private static readonly ID;
    private definition;
    constructor();
    getIdentifier(): string;
    getDefinition(): AttributeExpression[];
}
//# sourceMappingURL=T5Token.d.ts.map