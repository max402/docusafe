package org.adorsys.docusafe.transactional.impl.helper;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.transactional.exceptions.TxParallelCommittingException;
import org.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import org.adorsys.docusafe.transactional.impl.TxIDHashMap;
import org.adorsys.docusafe.transactional.impl.TxIDHashMapWrapper;
import org.adorsys.docusafe.transactional.types.TxID;

import java.util.*;
import java.util.stream.Collectors;

public class ParallelTransactionLogic {

    public static TxIDHashMapWrapper join(TxIDHashMapWrapper stateLastCommittedTx, TxIDHashMapWrapper stateAtBeginOfCurrentTx, TxIDHashMapWrapper stateAtEndOfCurrentTx, TxIDHashMap documentsReadInTx) {

        // if no parallel commits
        LastCommitedTxID lastCommitedTxID = stateAtEndOfCurrentTx.getLastCommitedTxID();
        if (lastCommitedTxID != null && lastCommitedTxID.equals(stateAtBeginOfCurrentTx.getLastCommitedTxID())) {
            return stateAtEndOfCurrentTx;
        }

        // changed files have same TxID as currentTxID
        Set<DocumentFQN> docsTouched = stateAtEndOfCurrentTx.getMap().entrySet().stream()
                .filter(e -> e.getValue().equals(stateAtEndOfCurrentTx.getCurrentTxID()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        // add read
        docsTouched.addAll(documentsReadInTx.keySet());
        // add deleted
        MapDifference<DocumentFQN, TxID> currentTxDiff = Maps.difference(stateAtBeginOfCurrentTx.getMap(), stateAtEndOfCurrentTx.getMap());
        docsTouched.addAll(currentTxDiff.entriesOnlyOnLeft().keySet());

        MapDifference<DocumentFQN, TxID> parallelTxDiff = Maps.difference(stateLastCommittedTx.getMap(), stateAtBeginOfCurrentTx.getMap());
        List<DocumentFQN> docsTouchedInParallel = new ArrayList<>(parallelTxDiff.entriesDiffering().keySet());
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnLeft().keySet());
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnRight().keySet());

        for(DocumentFQN d : docsTouched) {
            if(docsTouchedInParallel.contains(d)) {
                throw new TxParallelCommittingException(stateAtBeginOfCurrentTx.getCurrentTxID(), stateLastCommittedTx.getCurrentTxID(), d.getValue());
            }
        }

        return TxIDHashMapWrapper.builder()
                .lastCommitedTxID(new LastCommitedTxID(stateLastCommittedTx.getCurrentTxID().getValue()))
                .currentTxID(new TxID())
                .beginTx(new Date())
                .endTx(new Date())
                .map(stateAtEndOfCurrentTx.getMap())
                .mergedTxID(stateAtEndOfCurrentTx.getCurrentTxID())
                .build();

    }
}
