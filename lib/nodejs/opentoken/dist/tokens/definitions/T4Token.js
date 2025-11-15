"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.T4Token = void 0;
const AttributeExpression_1 = require("../../attributes/AttributeExpression");
const BirthDateAttribute_1 = require("../../attributes/person/BirthDateAttribute");
const SexAttribute_1 = require("../../attributes/person/SexAttribute");
const SocialSecurityNumberAttribute_1 = require("../../attributes/person/SocialSecurityNumberAttribute");
/**
 * Represents the token definition for token T4.
 *
 * It is a collection of attribute expressions that are concatenated together
 * to get the token signature. The token signature is as follows:
 * social-security-number|U(gender)|birth-date
 */
class T4Token {
    constructor() {
        this.definition = [
            new AttributeExpression_1.AttributeExpression(SocialSecurityNumberAttribute_1.SocialSecurityNumberAttribute, 'T|M(\\d+)'),
            new AttributeExpression_1.AttributeExpression(SexAttribute_1.SexAttribute, 'T|U'),
            new AttributeExpression_1.AttributeExpression(BirthDateAttribute_1.BirthDateAttribute, 'T|D'),
        ];
    }
    getIdentifier() {
        return T4Token.ID;
    }
    getDefinition() {
        return this.definition;
    }
}
exports.T4Token = T4Token;
T4Token.ID = 'T4';
//# sourceMappingURL=T4Token.js.map