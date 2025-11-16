/**
 * Copyright (c) Truveta. All rights reserved.
 */

import * as fs from 'fs';
import { parse } from 'csv-parse';
import { PersonAttributesReader } from '../PersonAttributesReader';

/**
 * Reads person attributes from a CSV file.
 */
export class PersonAttributesCSVReader implements PersonAttributesReader {
  private parser: any;
  private done: boolean = false;

  constructor(filePath: string) {
    const stream = fs.createReadStream(filePath);
    this.parser = stream.pipe(
      parse({
        columns: true,
        skip_empty_lines: true,
        trim: true,
      })
    );
  }

  async hasNext(): Promise<boolean> {
    if (this.done) {
      return false;
    }

    return new Promise((resolve) => {
      this.parser.once('readable', () => {
        resolve(true);
      });
      this.parser.once('end', () => {
        this.done = true;
        resolve(false);
      });
    });
  }

  async next(): Promise<Map<string, string>> {
    const record = this.parser.read();
    if (!record) {
      throw new Error('No more records');
    }

    const result = new Map<string, string>();
    for (const [key, value] of Object.entries(record)) {
      if (typeof value === 'string') {
        result.set(key, value);
      }
    }

    return result;
  }

  async close(): Promise<void> {
    this.parser.destroy();
  }
}
