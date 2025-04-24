package br.com.infotera.gerarcertificado.util;


import br.com.infotera.gerarcertificado.service.CertificadoService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

@Component
public class PfxProcessorUtil {

    private final Logger logger = Logger.getLogger(PfxProcessorUtil.class.getName());

    @Value("${app.certificates.pfxPassword}")
    private String pfxPassword;

    public void processPfx(Path pfxPath, String client, String clientId, Path keyPath, Path crtPath, Path csrPath) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // Carregar PFX
        KeyStore keystore = KeyStore.getInstance("PKCS12", "BC");
        try (var fis = Files.newInputStream(pfxPath)) {
            keystore.load(fis, pfxPassword.toCharArray());
        }

        String alias = keystore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, pfxPassword.toCharArray());
        X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);

        // Salvar KEY (formato PEM)
        savePrivateKey(privateKey, keyPath);

        // Salvar CRT (formato PEM)
        saveCertificate(cert, crtPath);

        // Gerar CSR
        generateCSR(privateKey, cert, client, clientId, csrPath);

        // ‘Log’ das operações
        logger.info("[1/3] ✅ KEY gerado: " + keyPath);
        logger.info("[2/3] ✅ CRT gerado: " + crtPath);
        logger.info("[3/3] ✅ CSR gerado: " + csrPath);
    }

    private void savePrivateKey(PrivateKey privateKey, Path keyPath) throws IOException {
        try (var writer = new JcaPEMWriter(new FileWriter(keyPath.toFile()))) {
            writer.writeObject(privateKey);
        }
    }

    private void saveCertificate(X509Certificate cert, Path crtPath) throws IOException {
        try (var writer = new JcaPEMWriter(new FileWriter(crtPath.toFile()))) {
            writer.writeObject(cert);
        }
    }

    private void generateCSR(PrivateKey privateKey, X509Certificate cert, String client, String clientId, Path csrPath) throws Exception {
        X500Name subject = new X500Name(String.format("C=BR, ST=SP, L=Sao Paulo, O=%s, OU=IT, CN=%s", client.toUpperCase(), clientId));

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(cert.getPublicKey().getEncoded());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
        PKCS10CertificationRequestBuilder csrBuilder = new PKCS10CertificationRequestBuilder(subject, publicKeyInfo);
        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        try (var writer = new JcaPEMWriter(new FileWriter(csrPath.toFile()))) {
            writer.writeObject(csr);
        }
    }

}
