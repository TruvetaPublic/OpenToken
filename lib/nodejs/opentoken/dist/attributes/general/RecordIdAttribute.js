"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.RecordIdAttribute = void 0;
const StringAttribute_1 = require("./StringAttribute");
/**
 * Represents an attribute for handling record identifiers.
 *
 * This class extends StringAttribute and provides functionality for working with
 * identifier fields. It recognizes "RecordId" and "Id" as valid aliases for
 * this attribute type.
 *
 * The attribute normalizes values by trimming leading and trailing whitespace.
 */
class RecordIdAttribute extends StringAttribute_1.StringAttribute {
    constructor() {
        super(RecordIdAttribute.NAME, RecordIdAttribute.ALIASES);
    }
    normalize(value) {
        return value ? value.trim() : value;
    }
}
exports.RecordIdAttribute = RecordIdAttribute;
RecordIdAttribute.NAME = 'RecordId';
RecordIdAttribute.ALIASES = ['RecordId', 'Id'];
//# sourceMappingURL=RecordIdAttribute.js.map