/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { Attribute } from '../attributes/Attribute';

export interface PersonAttributesReader {
  hasNext(): Promise<boolean>;
  next(): Promise<Map<typeof Attribute, string>>;
  close(): Promise<void>;
}
