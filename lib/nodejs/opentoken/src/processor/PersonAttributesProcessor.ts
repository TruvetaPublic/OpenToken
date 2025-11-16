/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { PersonAttributesReader } from '../io/PersonAttributesReader';
import { PersonAttributesWriter } from '../io/PersonAttributesWriter';
import { TokenTransformer } from '../tokentransformer/TokenTransformer';
import { TokenRegistry } from '../tokens/TokenRegistry';
import { AttributeLoader } from '../attributes/AttributeLoader';
import { Metadata } from '../Metadata';

/**
 * Process all person attributes.
 *
 * This class is used to read person attributes from input source,
 * generate tokens for each person record and write the tokens back
 * to the output data source.
 */
export class PersonAttributesProcessor {
  /**
   * Reads person attributes from the input data source, generates tokens,
   * and writes the result back to the output data source.
   */
  static async process(
    reader: PersonAttributesReader,
    writer: PersonAttributesWriter,
    transformers: TokenTransformer[],
    metadata: Metadata
  ): Promise<void> {
    const tokens = TokenRegistry.loadAll();
    const attributes = AttributeLoader.load();
    let rowCounter = 0;

    while (await reader.hasNext()) {
      const row = await reader.next();
      rowCounter++;

      // For each token definition, generate a token
      for (const token of tokens) {
        const tokenId = token.getIdentifier();
        const definition = token.getDefinition();

        // Build token signature from attribute expressions
        const signatureParts: string[] = [];
        for (const expr of definition) {
          // Get attribute class and find corresponding value in row
          // For now, we'll iterate through all row keys to find matches
          let effectiveValue = '';

          for (const attr of Array.from(attributes)) {
            const attrName = attr.getName();
            let value = row.get(attrName);

            // Also try aliases
            if (!value) {
              for (const alias of attr.getAliases()) {
                value = row.get(alias);
                if (value) break;
              }
            }

            if (value && attr.validate(value)) {
              const normalized = attr.normalize(value);
              // Apply expression transformations
              effectiveValue = expr.getEffectiveValue(normalized);
              break;
            }
          }

          signatureParts.push(effectiveValue);
        }

        const signature = signatureParts.join('|');

        // Apply transformers (hash, encrypt)
        let tokenValue = signature;
        for (const transformer of transformers) {
          tokenValue = transformer.transform(tokenValue);
        }

        // Write the token
        const outputRecord = new Map<string, string>();
        outputRecord.set('RecordId', row.get('RecordId') || row.get('record-id') || `row-${rowCounter}`);
        outputRecord.set('RuleId', tokenId);
        outputRecord.set('Token', tokenValue);

        await writer.writeAttributes(outputRecord);
      }

      if (rowCounter % 1000 === 0) {
        console.log(`Processed ${rowCounter} records`);
      }
    }

    metadata.totalRows = rowCounter;
    console.log(`Processed a total of ${rowCounter} records`);
  }
}
