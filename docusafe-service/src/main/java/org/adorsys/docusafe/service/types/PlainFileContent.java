package org.adorsys.docusafe.service.types;

import org.adorsys.cryptoutils.basetypes.BaseTypeByteArray;

/**
 * Created by peter on 19.01.18 at 16:47.
 */
public class PlainFileContent extends BaseTypeByteArray {
    public PlainFileContent(byte[] content) {
        super(content);
    }
}
