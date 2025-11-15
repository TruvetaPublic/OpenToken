"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.StringAttribute = void 0;
const SerializableAttribute_1 = require("../SerializableAttribute");
/**
 * Base class for string-based attributes.
 */
class StringAttribute extends SerializableAttribute_1.SerializableAttribute {
    /**
     * Constructs a new StringAttribute.
     *
     * @param name - The name of the attribute.
     * @param aliases - The aliases of the attribute.
     * @param validators - The validators for the attribute.
     */
    constructor(name, aliases = [], validators = []) {
        super(name, aliases, validators);
    }
}
exports.StringAttribute = StringAttribute;
//# sourceMappingURL=StringAttribute.js.map