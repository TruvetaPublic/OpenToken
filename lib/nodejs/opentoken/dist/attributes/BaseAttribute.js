"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.BaseAttribute = void 0;
/**
 * Base class for all attributes.
 */
class BaseAttribute {
    /**
     * Constructs a new BaseAttribute.
     *
     * @param name - The name of the attribute.
     * @param aliases - The aliases of the attribute.
     * @param validators - The validators for the attribute.
     */
    constructor(name, aliases = [], validators = []) {
        this.name = name;
        this.aliases = aliases;
        this.validators = validators;
    }
    getName() {
        return this.name;
    }
    getAliases() {
        return this.aliases;
    }
    /**
     * Normalizes the attribute value.
     * Default implementation returns the value as-is.
     * Subclasses should override this method to provide custom normalization.
     *
     * @param value - The attribute value.
     * @returns The normalized attribute value.
     */
    normalize(value) {
        return value;
    }
    /**
     * Validates the attribute value using all configured validators.
     *
     * @param value - The attribute value.
     * @returns true if the value passes all validators; false otherwise.
     */
    validate(value) {
        if (!value || value.trim().length === 0) {
            return false;
        }
        // Run all validators
        for (const validator of this.validators) {
            if (!validator.validate(value)) {
                return false;
            }
        }
        return true;
    }
}
exports.BaseAttribute = BaseAttribute;
//# sourceMappingURL=BaseAttribute.js.map