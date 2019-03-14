package org.adorsys.docusafe.transactional.impl.helper;

import com.googlecode.catchexception.CatchException;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import org.adorsys.docusafe.transactional.impl.TxIDHashMap;
import org.adorsys.docusafe.transactional.impl.TxIDHashMapWrapper;
import org.adorsys.docusafe.transactional.types.TxID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class ParallelTransactionLogicTest {

    private static final TxID OLD_TX_ID = new TxID("ooo");
    private static final TxID CURRENT_TX_ID = new TxID("ccc");
    private static final TxID PARALLEL_TX_ID = new TxID("ppp1");
    private static final TxID PARALLEL_TX_ID_2 = new TxID("ppp2");

    @Test
    public void parallelCreationSameFile() {
        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(CURRENT_TX_ID)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), PARALLEL_TX_ID);
        lastCommitMap.put(new DocumentFQN("B"), PARALLEL_TX_ID);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(PARALLEL_TX_ID)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(new LastCommitedTxID(PARALLEL_TX_ID.getValue()))
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();
        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx));
        Assert.assertNotNull(CatchException.caughtException());
    }

    @Test
    public void parallelCreationDifferentFiles() {
        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(CURRENT_TX_ID)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), PARALLEL_TX_ID);
        lastCommitMap.put(new DocumentFQN("B"), PARALLEL_TX_ID);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(PARALLEL_TX_ID)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("C"), CURRENT_TX_ID);
        endStateMap.put(new DocumentFQN("D"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(new LastCommitedTxID(PARALLEL_TX_ID.getValue()))
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();

        TxIDHashMapWrapper result = ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx,
                stateAtEndOfCurrentTx, documentsReadInTx);

        log.info(result.toString());
    }

    @Test
    public void readFileUpdatedInParallelTx() {
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(CURRENT_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), PARALLEL_TX_ID);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(PARALLEL_TX_ID)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("C"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(new LastCommitedTxID(PARALLEL_TX_ID.getValue()))
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();
        documentsReadInTx.put(new DocumentFQN("A"), CURRENT_TX_ID);

        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx));
        Assert.assertNotNull(CatchException.caughtException());
    }

    @Test
    public void readFileDeletedInParallelTx() {
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(CURRENT_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), PARALLEL_TX_ID);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(PARALLEL_TX_ID)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("C"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(new LastCommitedTxID(PARALLEL_TX_ID.getValue()))
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();
        documentsReadInTx.put(new DocumentFQN("B"), CURRENT_TX_ID);

        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx));
        Assert.assertNotNull(CatchException.caughtException());
    }

    @Test
    public void createFileCreatedAndDeletedInParallelTx() {
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("C"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(CURRENT_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("B"), PARALLEL_TX_ID);
        lastCommitMap.put(new DocumentFQN("C"), PARALLEL_TX_ID);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(PARALLEL_TX_ID)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), CURRENT_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("C"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(new LastCommitedTxID(PARALLEL_TX_ID.getValue()))
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();

        TxIDHashMapWrapper result = ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx,
                stateAtEndOfCurrentTx, documentsReadInTx);

        log.info(result.toString());
    }

    @Test
    public void createFileCreatedInParallelTx() {
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(CURRENT_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), OLD_TX_ID);
        lastCommitMap.put(new DocumentFQN("B"), OLD_TX_ID);
        lastCommitMap.put(new DocumentFQN("C"), PARALLEL_TX_ID);
        lastCommitMap.put(new DocumentFQN("D"), PARALLEL_TX_ID_2);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(PARALLEL_TX_ID)
                .map(lastCommitMap)
                .build();

        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("C"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(new LastCommitedTxID(PARALLEL_TX_ID.getValue()))
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();

        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx));
        Assert.assertNotNull(CatchException.caughtException());
    }
}