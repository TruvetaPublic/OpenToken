/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

import java.io.IOException;
import java.util.Map;
public interface MetadataWriter {
    
    void writeMetadata(Map<String, String> metadataMap) throws IOException ;
}
