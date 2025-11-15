"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.T3Token = void 0;
const AttributeExpression_1 = require("../../attributes/AttributeExpression");
const BirthDateAttribute_1 = require("../../attributes/person/BirthDateAttribute");
const FirstNameAttribute_1 = require("../../attributes/person/FirstNameAttribute");
const LastNameAttribute_1 = require("../../attributes/person/LastNameAttribute");
const SexAttribute_1 = require("../../attributes/person/SexAttribute");
/**
 * Represents the token definition for token T3.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * U(last-name)|U(first-name)|U(gender)|birth-date
 */
class T3Token {
    constructor() {
        this.definition = [
            new AttributeExpression_1.AttributeExpression(LastNameAttribute_1.LastNameAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(FirstNameAttribute_1.FirstNameAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(SexAttribute_1.SexAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(BirthDateAttribute_1.BirthDateAttribute, 'T|D'),
        ];
    }
    getIdentifier() {
        return T3Token.ID;
    }
    getDefinition() {
        return this.definition;
    }
}
exports.T3Token = T3Token;
T3Token.ID = 'T3';
//# sourceMappingURL=T3Token.js.map