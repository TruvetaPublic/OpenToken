"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.T5Token = void 0;
const AttributeExpression_1 = require("../../attributes/AttributeExpression");
const FirstNameAttribute_1 = require("../../attributes/person/FirstNameAttribute");
const LastNameAttribute_1 = require("../../attributes/person/LastNameAttribute");
const SexAttribute_1 = require("../../attributes/person/SexAttribute");
/**
 * Represents the token definition for token T5.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * U(last-name)|U(first-name-3)|U(gender)
 */
class T5Token {
    constructor() {
        this.definition = [
            new AttributeExpression_1.AttributeExpression(LastNameAttribute_1.LastNameAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(FirstNameAttribute_1.FirstNameAttribute, 'T|S(0,3)|U'),
            new AttributeExpression_1.AttributeExpression(SexAttribute_1.SexAttribute, 'T|U'),
        ];
    }
    getIdentifier() {
        return T5Token.ID;
    }
    getDefinition() {
        return this.definition;
    }
}
exports.T5Token = T5Token;
T5Token.ID = 'T5';
//# sourceMappingURL=T5Token.js.map