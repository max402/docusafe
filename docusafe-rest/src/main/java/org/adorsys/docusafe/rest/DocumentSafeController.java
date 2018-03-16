package org.adorsys.docusafe.rest;

import org.adorsys.cryptoutils.exceptions.BaseException;
import org.adorsys.cryptoutils.exceptions.BaseExceptionHandler;
import org.adorsys.cryptoutils.mongodbstoreconnection.MongoDBExtendedStoreConnection;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.business.types.UserID;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DSDocumentStream;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.business.types.complex.UserIDAuth;
import org.adorsys.docusafe.rest.types.CreateLinkTupel;
import org.adorsys.docusafe.rest.types.GrantDocument;
import org.adorsys.encobject.domain.ReadKeyPassword;
import org.adorsys.encobject.filesystem.FileSystemExtendedStorageConnection;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by peter on 22.01.18 at 19:27.
 * UserIDAuth wird natürlich kein expliziter Parameter sein. Aber die JWT Logik kommt
 * erst im zweiten Schritt. Jetzt erst mal loslegen mit explizitem Parameter.
 */
@RestController
public class DocumentSafeController {
    public static STORE_CONNECTION storeConnection = STORE_CONNECTION.FILESYSTEM;
    private final static String APPLICATION_JSON = "application/json";
    private final static String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentSafeController.class);
    private DocumentSafeService service;

    public DocumentSafeController() {
        switch (storeConnection) {
            case MONGO:
                service = new DocumentSafeServiceImpl(new MongoDBExtendedStoreConnection());
                break;
            case FILESYSTEM:
                service = new DocumentSafeServiceImpl(new FileSystemExtendedStorageConnection());
                break;
            default:
                throw new BaseException("missing switch");
        }

    }

    /**
     * USER
     * ===========================================================================================
     */
    @RequestMapping(
            value = "/internal/user",
            method = {RequestMethod.PUT},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public void createUser(@RequestBody UserIDAuth userIDAuth) {
        service.createUser(userIDAuth);
    }

    @RequestMapping(
            value = "/internal/user",
            method = {RequestMethod.DELETE},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public void destroyUser(@RequestBody UserIDAuth userIDAuth) {
        service.destroyUser(userIDAuth);
    }

    @RequestMapping(
            value = "/internal/user/{UserID}",
            method = {RequestMethod.GET},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public
    @ResponseBody
    ResponseEntity<Boolean> userExists(@PathVariable("UserID") String userIDString) {
        UserID userID = new UserID(userIDString);
        LOGGER.info("get user exists: " + userID);
        if (!service.userExists(userID)) {
            LOGGER.debug(userID + " does not exist");
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        LOGGER.debug(userID + " exists");
        return new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
    }

    /**
     * DOCUMENT
     * ===========================================================================================
     */

    /**
     * -- byte orientiert --
     */
    @RequestMapping(
            value = "/document",
            method = {RequestMethod.PUT},
            consumes = {APPLICATION_JSON}
    )
    public void storeDocument(@RequestHeader("userid") String userid,
                              @RequestHeader("password") String password,
                              @RequestBody DSDocument dsDocument) {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        service.storeDocument(userIDAuth, dsDocument);
    }

    @RequestMapping(
            value = "/document/**",
            method = {RequestMethod.GET},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public
    @ResponseBody
    ResponseEntity<DSDocument> readDocument(@RequestHeader("userid") String userid,
                            @RequestHeader("password") String password,
                            HttpServletRequest request
    ) {
        LOGGER.info("get document request arrived");
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        DocumentFQN documentFQN = new DocumentFQN(getFQN(request));
        if (! service.documentExists(userIDAuth, documentFQN)) {
            LOGGER.debug("document " + documentFQN + " does not exist");
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        LOGGER.debug("document " + documentFQN + " exists");
        return new ResponseEntity<>(service.readDocument(userIDAuth, documentFQN), HttpStatus.OK);
    }

    /**
     * -- stream orientiert --
     */
    @RequestMapping(
            value = "/documentstream",
            method = {RequestMethod.PUT},
            consumes = {APPLICATION_OCTET_STREAM}
    )
    public void storeDocumentStream(@RequestHeader("userid") String userid,
                                    @RequestHeader("password") String password,
                                    @RequestHeader("documentFQN") String documentFQNString,
                                    InputStream inputStream) {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        DocumentFQN documentFQN = new DocumentFQN(documentFQNString);
        LOGGER.info("input auf document/stream for " + userIDAuth);
        service.storeDocumentStream(userIDAuth, new DSDocumentStream(documentFQN, inputStream));
    }

    @RequestMapping(
            value = "/documentstream/**",
            method = {RequestMethod.GET},
            produces = {APPLICATION_OCTET_STREAM}

    )
    public void readDocumentStream(@RequestHeader("userid") String userid,
                                   @RequestHeader("password") String password,
                                   HttpServletRequest request,
                                   HttpServletResponse response
    ) {
        try {
            LOGGER.info("get stream request arrived1");
            UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
            DocumentFQN documentFQN = new DocumentFQN(getFQN(request));
            LOGGER.debug("received:" + userIDAuth + " and " + documentFQN);
            DSDocumentStream stream = service.readDocumentStream(userIDAuth, documentFQN);
            InputStream is = stream.getDocumentStream();
            OutputStream os = response.getOutputStream();
            LOGGER.debug("start copy imputstream to outputstream");
            IOUtils.copy(is, os);
            LOGGER.debug("finished copy imputstream to outputstream");
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            LOGGER.debug("return outputstream to sender");
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    /**
     * -- content art unabhängig --
     */
    @RequestMapping(
            value = "/document/**",
            method = {RequestMethod.DELETE},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public void destroyDocument(@RequestHeader("userid") String userid,
                                @RequestHeader("password") String password,
                                HttpServletRequest request
    ) {
        LOGGER.info("destroy document request arrived");
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        DocumentFQN documentFQN = new DocumentFQN(getFQN(request));
        service.deleteDocument(userIDAuth, documentFQN);
        LOGGER.info("destroy document request finished");
    }

    /**
     * GRANT/DOCUMENT
     * ===========================================================================================
     */
    @RequestMapping(
            value = "/grant/document",
            method = {RequestMethod.PUT},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public void grantAccess(@RequestHeader("userid") String userid,
                            @RequestHeader("password") String password,
                            @RequestBody GrantDocument grantDocument) {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        service.grantAccessToUserForFolder(userIDAuth, grantDocument.getReceivingUser(), grantDocument.getDocumentDirectoryFQN(), grantDocument.getAccessType());
    }

    @RequestMapping(
            value = "/granted/document/{ownerUserID}",
            method = {RequestMethod.PUT},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public void storeGrantedDocument(@RequestHeader("userid") String userid,
                                     @RequestHeader("password") String password,
                                     @PathVariable("ownerUserID") String ownerUserIDString,
                                     @RequestBody DSDocument dsDocument) {
        UserID ownerUserID = new UserID(ownerUserIDString);
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        service.storeGrantedDocument(userIDAuth, ownerUserID, dsDocument);
    }

    @RequestMapping(
            value = "/granted/document/{ownerUserID}/**",
            method = {RequestMethod.GET},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public
    @ResponseBody
    DSDocument readGrantedDocument(@RequestHeader("userid") String userid,
                                   @RequestHeader("password") String password,
                                   @PathVariable("ownerUserID") String ownerUserIDString,
                                   HttpServletRequest request
    ) {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        UserID ownerUserID = new UserID(ownerUserIDString);

        final String documentFQNString = getFQN(request);

        DocumentFQN documentFQN = new DocumentFQN(documentFQNString);
        LOGGER.debug("received:" + userIDAuth + " and " + ownerUserID + " and " + documentFQN);
        return service.readGrantedDocument(userIDAuth, ownerUserID, documentFQN);
    }


    /**
     * DOCUMENT/LINK
     * ===========================================================================================
     */
    @RequestMapping(
            value = "/document/link",
            method = {RequestMethod.PUT},
            consumes = {APPLICATION_JSON},
            produces = {APPLICATION_JSON}
    )
    public void createLink(@RequestHeader("userid") String userid,
                           @RequestHeader("password") String password,
                           @RequestBody CreateLinkTupel createLinkTupel) {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID(userid), new ReadKeyPassword(password));
        service.linkDocument(userIDAuth, createLinkTupel.getSource(), createLinkTupel.getDestination());
    }

    private String getFQN(HttpServletRequest request) {
        final String path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        final String documentFQNStringWithQuotes = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
        return documentFQNStringWithQuotes.replaceAll("\"", "");
    }

    public static enum STORE_CONNECTION {
        MONGO,
        FILESYSTEM
    }
}
