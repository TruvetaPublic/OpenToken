/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { BaseAttribute } from './BaseAttribute';
import { Validator } from './validation/Validator';

/**
 * A serializable attribute that can be used for token generation.
 */
export abstract class SerializableAttribute extends BaseAttribute {
  /**
   * Constructs a new SerializableAttribute.
   *
   * @param name - The name of the attribute.
   * @param aliases - The aliases of the attribute.
   * @param validators - The validators for the attribute.
   */
  constructor(name: string, aliases: string[] = [], validators: Validator[] = []) {
    super(name, aliases, validators);
  }
}
