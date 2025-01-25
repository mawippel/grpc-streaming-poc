package com.mawippel.services;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.io.ByteSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Service
@Slf4j
public class CertificateManagerService {

    private Storage storageTemplate;
    private String bucketName;
    private String fileName;
    private String password;
    private byte[] certificateChainBytes;
    private byte[] privateKeyBytes;
    private byte[] rootCerts;

    public CertificateManagerService(Storage storageTemplate,
                                     @Value("${grpc.server.certificate-authority.bucket-name}") String bucketName,
                                     @Value("${grpc.server.certificate-authority.file-name}") String fileName,
                                     @Value("${grpc.server.certificate-authority.password}") String password,
                                     @Value("${grpc.server.certificate-chain}") byte[] certificateChainBytes,
                                     @Value("${grpc.server.private-key}") byte[] privateKeyBytes,
                                     @Value("${grpc.server.trust-manager}") byte[] rootCerts) {
        this.storageTemplate = storageTemplate;
        this.bucketName = bucketName;
        this.fileName = fileName;
        this.password = password;
        this.certificateChainBytes = certificateChainBytes;
        this.privateKeyBytes = privateKeyBytes;
        this.rootCerts = rootCerts;
    }

    public ByteSource getCertChain() {
        return ByteSource.wrap(certificateChainBytes);
    }

    public ByteSource getPrivateKey() {
        return ByteSource.wrap(privateKeyBytes);
    }

    public ByteSource getRootCerts() {
        return ByteSource.wrap(rootCerts);
    }

    /**
     * This method creates the Certificate Chain file (PKCS12) in a GCS Bucket if it doesn't exist.
     * It creates the file based on the certificate chain received through the first parameter.
     */
    public void createServerCertChainInGCSIfNotExists(InputStream rootCerts) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        // no need to create the file if it already exists
        Blob certChainBlob = storageTemplate.get(BlobId.of(bucketName, fileName));
        if (certChainBlob != null) {
            log.info("{}/{} already exists, skipping creation...", bucketName, fileName);
            return;
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(rootCerts);

        /* load an empty key store for the initial certificate that comes from GSM.
            This is only executed once in a brand-new environment or
            if the p12 file gets deleted (shouldn't happen).
         */
        keyStore.load(null, null);
        keyStore.setCertificateEntry("initial", cert);

        Blob createdBlob = storageTemplate.create(BlobInfo.newBuilder(bucketName, fileName).build());
        WriteChannel writeChannel = createdBlob.writer();
        try (OutputStream os = Channels.newOutputStream(writeChannel)) {
            keyStore.store(os, password.toCharArray());
            log.info("Created initial P12 cert file in GCS");
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an InputStream with the server certificate chain used
     * to verify the client's identity.
     */
    public InputStream getServerCertChain() {
        Blob blob = storageTemplate.get(BlobId.of(bucketName, fileName));
        return new ByteArrayInputStream(blob.getContent());
    }

    /**
     * Returns a X509TrustManager used by the server to verify the client's identity.
     * It initializes the TrustManagerFactory using the PKCS12 file from Google Cloud Storage.
     */
    public TrustManager getTrustManager() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(getServerCertChain(), password.toCharArray());
        trustManagerFactory.init(keyStore);
        TrustManager[] tms = trustManagerFactory.getTrustManagers();
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager trustManager) {
                return trustManager;
            }
        }
        throw new RuntimeException("No X509TrustManager found, unable to start the gRPC server.");
    }
}
