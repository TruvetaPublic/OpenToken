"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.T1Token = void 0;
const AttributeExpression_1 = require("../../attributes/AttributeExpression");
const BirthDateAttribute_1 = require("../../attributes/person/BirthDateAttribute");
const FirstNameAttribute_1 = require("../../attributes/person/FirstNameAttribute");
const LastNameAttribute_1 = require("../../attributes/person/LastNameAttribute");
const SexAttribute_1 = require("../../attributes/person/SexAttribute");
/**
 * Represents the token definition for token T1.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * U(last-name)|U(first-name-1)|U(gender)|birth-date
 */
class T1Token {
    constructor() {
        this.definition = [
            new AttributeExpression_1.AttributeExpression(LastNameAttribute_1.LastNameAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(FirstNameAttribute_1.FirstNameAttribute, 'T|S(0,1)|U'),
            new AttributeExpression_1.AttributeExpression(SexAttribute_1.SexAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(BirthDateAttribute_1.BirthDateAttribute, 'T|D'),
        ];
    }
    getIdentifier() {
        return T1Token.ID;
    }
    getDefinition() {
        return this.definition;
    }
}
exports.T1Token = T1Token;
T1Token.ID = 'T1';
//# sourceMappingURL=T1Token.js.map