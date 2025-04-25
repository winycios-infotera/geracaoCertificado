package br.com.infotera.gerarcertificado.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;

@Component
public class SSLUtil {


    /**
     * Cria um certificado digital SSL.
     *
     * @param certPath caminho do certificado
     * @param keyPath  caminho da chave
     * @return retorna o contexto SSL
     * @throws Exception the exception
     */
    public SSLContext createSSLContext(String certPath, String keyPath) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        X509Certificate certificate = readCertificate(certPath);
        PrivateKey privateKey = readPrivateKey(keyPath);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("alias", privateKey, null, new java.security.cert.Certificate[]{certificate});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, null);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), createTrustAllManager(), new java.security.SecureRandom());

        return sslContext;
    }

    private X509Certificate readCertificate(String path) throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(path))) {
            X509CertificateHolder holder = (X509CertificateHolder) parser.readObject();
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        }
    }

    private PrivateKey readPrivateKey(String path) throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(path))) {
            Object keyObject = parser.readObject();
            PrivateKeyInfo keyInfo = (keyObject instanceof PEMKeyPair) ?
                    ((PEMKeyPair) keyObject).getPrivateKeyInfo() : (PrivateKeyInfo) keyObject;

            return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(keyInfo);
        }
    }

    // TODO: encontrar uma forma melhor de declarar o TrustManager
    private TrustManager[] createTrustAllManager() {
        return new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };
    }
}
