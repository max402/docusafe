package org.adorsys.documentsafe.service;

import org.adorsys.documentsafe.layer00common.exceptions.BaseExceptionHandler;
import org.adorsys.documentsafe.layer01persistence.ExtendedBlobStoreConnection;
import org.adorsys.documentsafe.layer01persistence.ExtendedKeystorePersistence;
import org.adorsys.documentsafe.layer01persistence.types.KeyStoreID;
import org.adorsys.documentsafe.layer01persistence.types.complextypes.BucketPath;
import org.adorsys.documentsafe.layer01persistence.types.complextypes.KeyStoreBucketPath;
import org.adorsys.documentsafe.layer01persistence.types.complextypes.KeyStoreLocation;
import org.adorsys.documentsafe.layer02service.KeyStoreService;
import org.adorsys.documentsafe.layer02service.generators.KeyStoreCreationConfig;
import org.adorsys.documentsafe.layer02service.impl.KeyStoreServiceImpl;
import org.adorsys.documentsafe.layer02service.types.ReadKeyPassword;
import org.adorsys.documentsafe.layer02service.types.ReadStorePassword;
import org.adorsys.documentsafe.layer02service.types.complextypes.KeyStoreAccess;
import org.adorsys.documentsafe.layer02service.types.complextypes.KeyStoreAuth;
import org.adorsys.encobject.service.BlobStoreContextFactory;
import org.adorsys.encobject.service.ContainerPersistence;
import org.adorsys.encobject.utils.TestFsBlobStoreFactory;

import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by peter on 02.01.18.
 */
public class KeyStoreServiceTest {

    private static String keystoreContainer = "keystore-container-" + KeyStoreServiceTest.class.getSimpleName();
    private BlobStoreContextFactory factory;


    public KeyStoreServiceTest(BlobStoreContextFactory factory) {
        this.factory = factory;
    }

    public KeyStoreStuff createKeyStore() {
        return createKeyStore(keystoreContainer, new ReadStorePassword("storePassword"), new ReadKeyPassword("keypassword"), new KeyStoreID("key-store-id-123"), null);
    }

    public KeyStoreStuff createKeyStore(String keystoreContainer,
                                        ReadStorePassword readStorePassword,
                                        ReadKeyPassword readKeyPassword,
                                        KeyStoreID keyStoreID,
                                        KeyStoreCreationConfig config) {
        try {
            KeyStoreBucketPath keyStoreBucketPath = new KeyStoreBucketPath(keystoreContainer);

            ContainerPersistence containerPersistence = new ContainerPersistence(new ExtendedBlobStoreConnection(factory));
            containerPersistence.creteContainer(keyStoreBucketPath.getObjectHandlePath());
            AllServiceTest.buckets.add(keyStoreBucketPath);

            KeyStoreService keyStoreService = new KeyStoreServiceImpl(factory);
            KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);
            KeyStoreLocation keyStoreLocation = keyStoreService.createKeyStore(keyStoreID, keyStoreAuth, keyStoreBucketPath, config);
            KeyStore keyStore = keyStoreService.loadKeystore(keyStoreLocation, keyStoreAuth.getReadStoreHandler());
            return new KeyStoreStuff(keyStore, factory, keyStoreID, new KeyStoreAccess(keyStoreLocation, keyStoreAuth));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }


    public static class KeyStoreStuff {
        public final KeyStore keyStore;
        public final BlobStoreContextFactory factory;
        public final KeyStoreAccess keyStoreAccess;


        public KeyStoreStuff(KeyStore keyStore, BlobStoreContextFactory factory, KeyStoreID keyStoreID, KeyStoreAccess keyStoreAccess) {
            this.keyStore = keyStore;
            this.factory = factory;
            this.keyStoreAccess = keyStoreAccess;
        }
    }
}
