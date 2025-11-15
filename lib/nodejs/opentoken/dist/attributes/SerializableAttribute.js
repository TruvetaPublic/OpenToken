"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.SerializableAttribute = void 0;
const BaseAttribute_1 = require("./BaseAttribute");
/**
 * A serializable attribute that can be used for token generation.
 */
class SerializableAttribute extends BaseAttribute_1.BaseAttribute {
    /**
     * Constructs a new SerializableAttribute.
     *
     * @param name - The name of the attribute.
     * @param aliases - The aliases of the attribute.
     * @param validators - The validators for the attribute.
     */
    constructor(name, aliases = [], validators = []) {
        super(name, aliases, validators);
    }
}
exports.SerializableAttribute = SerializableAttribute;
//# sourceMappingURL=SerializableAttribute.js.map