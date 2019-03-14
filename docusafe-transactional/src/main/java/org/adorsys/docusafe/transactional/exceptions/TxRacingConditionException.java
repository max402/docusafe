package org.adorsys.docusafe.transactional.exceptions;

import org.adorsys.docusafe.transactional.types.TxID;

/**
 * Created by peter on 03.12.18 14:06.
 */
public class TxRacingConditionException extends TxBaseException {
    public TxRacingConditionException(TxID currentTx, TxID lastTx, TxID previous) {
        super(currentTx  + " can not be committed, because the last committed tx is now " + lastTx + " but expected was " + previous );
    }
    public TxRacingConditionException(TxID currentTx, TxID lastTx, String conflictFile) {
        super(String.format("Transaction '%s' cannot be committed. File '%s' conflicted with changes made in parallel transaction '%s'", currentTx.getValue(), conflictFile, lastTx.getValue()));
    }
}
