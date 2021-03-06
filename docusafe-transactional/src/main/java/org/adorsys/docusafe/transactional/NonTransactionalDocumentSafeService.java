package org.adorsys.docusafe.transactional;

import org.adorsys.docusafe.business.types.MoveType;
import org.adorsys.docusafe.business.types.UserID;
import org.adorsys.docusafe.business.types.complex.BucketContentFQN;
import org.adorsys.docusafe.business.types.complex.BucketContentFQNWithUserMetaData;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.business.types.complex.UserIDAuth;
import org.adorsys.encobject.types.ListRecursiveFlag;
import org.adorsys.encobject.types.OverwriteFlag;
import org.adorsys.encobject.types.PublicKeyJWK;

/**
 * Created by peter on 15.08.18 at 11:55.
 */
public interface NonTransactionalDocumentSafeService {

    // NON-TRANSACTIONAL FOR OWNER
    void createUser(UserIDAuth userIDAuth);
    void destroyUser(UserIDAuth userIDAuth);
    boolean userExists(UserID userID);
    PublicKeyJWK findPublicEncryptionKey(UserID userID);

    // INBOX STUFF
    /**
     * This methods rereads the inbox every time it is called. so even in one tx
     * the context can change.
     *
     * @param userIDAuth user and password
     * @return the recursive list of files found in the inbox
     */
    BucketContentFQNWithUserMetaData nonTxListInbox(UserIDAuth userIDAuth);

}
