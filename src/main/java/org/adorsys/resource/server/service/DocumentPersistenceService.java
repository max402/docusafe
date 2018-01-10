package org.adorsys.resource.server.service;

import org.adorsys.encobject.domain.ContentMetaInfo;
import org.adorsys.encobject.domain.ObjectHandle;
import org.adorsys.encobject.params.EncryptionParams;
import org.adorsys.encobject.service.ContainerPersistence;
import org.adorsys.resource.server.exceptions.BaseExceptionHandler;
import org.adorsys.resource.server.persistence.ExtendedObjectPersistence;
import org.adorsys.resource.server.persistence.basetypes.DocumentBucketName;
import org.adorsys.resource.server.persistence.basetypes.DocumentContent;
import org.adorsys.resource.server.persistence.basetypes.DocumentID;
import org.adorsys.resource.server.persistence.basetypes.KeyID;
import org.adorsys.resource.server.persistence.complextypes.DocumentKeyIDWithKey;
import org.adorsys.resource.server.persistence.complextypes.DocumentLocation;
import org.adorsys.resource.server.persistence.complextypes.KeyStoreAccess;
import org.adorsys.resource.server.persistence.keysource.DocumentGuardBasedKeySourceImpl;
import org.adorsys.resource.server.persistence.keysource.DocumentKeyIDWithKeyBasedSourceImpl;
import org.adorsys.resource.server.persistence.keysource.KeySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample use of the encobject api to implement our protocol.
 *
 * @author fpo
 */
public class DocumentPersistenceService {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentPersistenceService.class);

    private ExtendedObjectPersistence objectPersistence;
    private DocumentGuardService documentGuardService;
    private ContainerPersistence containerPersistence;

    public DocumentPersistenceService(ContainerPersistence containerPersistence, ExtendedObjectPersistence objectPersistence, DocumentGuardService documentGuardService) {
        super();
        this.containerPersistence = containerPersistence;
        this.objectPersistence = objectPersistence;
        this.documentGuardService = documentGuardService;
    }

    /**
     * Verschlüsselt den DocumentContent mit dem (symmetrischen) DocumentKey. Erzeugt ein Document,
     * dass den verschlüsselten DocumentContent enthält. Im Header dieses Documents steht die DocumentKeyID.
     * Das Document liegt in einem Bucket mit dem Namen documentBucketName.
     */
    public DocumentLocation persistDocument(
            DocumentKeyIDWithKey documentKeyIDWithKey,
            DocumentBucketName documentBucketName,
            DocumentID documentID,
            DocumentContent documentContent) {

        try {
            LOGGER.info("start persist document with " + documentID);

            // Create object handle
            ObjectHandle location = new ObjectHandle(documentBucketName.getValue(), documentID.getValue());

            // Store object.
            ContentMetaInfo metaInfo = null;
            EncryptionParams encParams = null;

            KeySource keySource = new DocumentKeyIDWithKeyBasedSourceImpl(documentKeyIDWithKey);
            LOGGER.debug("Document wird verschlüsselt mit " + documentKeyIDWithKey);
            // Create container if non existent
            if (!containerPersistence.containerExists(location.getContainer())) {
                containerPersistence.creteContainer(location.getContainer());
            }
            KeyID keyID = new KeyID(documentKeyIDWithKey.getDocumentKeyID().getValue());
            objectPersistence.storeObject(documentContent.getValue(), metaInfo, location, keySource, keyID, encParams);
            DocumentLocation documentLocation = new DocumentLocation(documentID, documentBucketName);
            LOGGER.info("finsihed persist document with " + documentID + " @ " + documentLocation);
            return documentLocation;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    /**
     *
     */
    public DocumentContent loadDocument(
            KeyStoreAccess keyStoreAccess,
            DocumentLocation documentLocation) {

        try {
            LOGGER.info("start load document @ " + documentLocation);
            KeySource keySource = new DocumentGuardBasedKeySourceImpl(documentGuardService, keyStoreAccess);
            DocumentContent documentContent = new DocumentContent(objectPersistence.loadObject(documentLocation.getLocationHandle(), keySource).getData());
            LOGGER.info("start load document @ " + documentLocation);
            return documentContent;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }
}
