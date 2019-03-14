package org.adorsys.docusafe.transactional.exceptions;

import org.adorsys.docusafe.transactional.types.TxID;

public class TxParallelCommittingException extends TxBaseException {
    public TxParallelCommittingException(TxID currentTx, TxID lastTx, String conflictFile) {
        super(String.format("Transaction '%s' cannot be committed. File '%s' conflicted with changes made in parallel transaction '%s'", currentTx.getValue(), conflictFile, lastTx.getValue()));
    }
}
