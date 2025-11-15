"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.T2Token = void 0;
const AttributeExpression_1 = require("../../attributes/AttributeExpression");
const BirthDateAttribute_1 = require("../../attributes/person/BirthDateAttribute");
const FirstNameAttribute_1 = require("../../attributes/person/FirstNameAttribute");
const LastNameAttribute_1 = require("../../attributes/person/LastNameAttribute");
// NOTE: PostalCodeAttribute not yet implemented - will need to be added
// For now, we'll create a placeholder
class PostalCodeAttributePlaceholder {
}
/**
 * Represents the token definition for token T2.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * U(last-name)|U(first-name)|birth-date|postal-code-3
 */
class T2Token {
    constructor() {
        this.definition = [
            new AttributeExpression_1.AttributeExpression(LastNameAttribute_1.LastNameAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(FirstNameAttribute_1.FirstNameAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(BirthDateAttribute_1.BirthDateAttribute, 'T|D'),
            // TODO: Replace with actual PostalCodeAttribute when implemented
            new AttributeExpression_1.AttributeExpression(PostalCodeAttributePlaceholder, 'T|S(0,3)|U'),
        ];
    }
    getIdentifier() {
        return T2Token.ID;
    }
    getDefinition() {
        return this.definition;
    }
}
exports.T2Token = T2Token;
T2Token.ID = 'T2';
//# sourceMappingURL=T2Token.js.map