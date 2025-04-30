package br.com.infotera.gerarcertificado.util;


import br.com.infotera.gerarcertificado.model.RequestClient;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;
import java.util.logging.Logger;

@Component
public class PfxProcessUtil {

    private final Logger logger = Logger.getLogger(PfxProcessUtil.class.getName());

    @Value("${app.certificates.pfxPassword}")
    private String pfxPassword;

    //    Processa arquivo PFX
    public void processPfx(MultipartFile clientPfx, String client, String clientId, Path keyPath, Path crtPath, Path csrPath) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyStore keystore = KeyStore.getInstance("PKCS12", "BC");

        try (InputStream fis = clientPfx.getInputStream()) {
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

        logger.info("[1/3] ✅ KEY gerado: " + keyPath);
        logger.info("[2/3] ✅ CRT gerado: " + crtPath);
        logger.info("[3/3] ✅ CSR gerado: " + csrPath);
    }

    //Gera arquivo .key
    void savePrivateKey(PrivateKey privateKey, Path keyPath) throws IOException {
        try (var writer = new JcaPEMWriter(new FileWriter(keyPath.toFile()))) {
            writer.writeObject(privateKey);
        }
    }

    //Gera arquivo.crt
    void saveCertificate(X509Certificate cert, Path crtPath) throws IOException {
        try (var writer = new JcaPEMWriter(new FileWriter(crtPath.toFile()))) {
            writer.writeObject(cert);
        }
    }

    //Gera arquivo .csr
    void generateCSR(PrivateKey privateKey, X509Certificate cert, String client, String clientId, Path csrPath) throws Exception {
        X500Name subject = new X500Name(String.format("C=BR, ST=SP, L=Sao Paulo, O=%s, OU=IT, CN=%s", client.toUpperCase(), clientId));

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(cert.getPublicKey().getEncoded());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
        PKCS10CertificationRequestBuilder csrBuilder = new PKCS10CertificationRequestBuilder(subject, publicKeyInfo);
        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        try (var writer = new JcaPEMWriter(new FileWriter(csrPath.toFile()))) {
            writer.writeObject(csr);
        }
    }


    public void gerarPfx(ResponseCertificate responseCertificate, String pathKey, MultipartFile pathPfx, RequestClient requestClient) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // 1. Criar pasta "pfx" se não existir
        Path pastaPfx = Paths.get("pfx");
        if (!Files.exists(pastaPfx)) {
            Files.createDirectories(pastaPfx);
        }

        // 2. Carregar certificado da string
        String certPem = responseCertificate.getCertificate()
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] decodedCert = Base64.getDecoder().decode(certPem);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate cert;
        try (ByteArrayInputStream certStream = new ByteArrayInputStream(decodedCert)) {
            cert = certFactory.generateCertificate(certStream);
        }

        // 3. Carregar chave privada (.key) podendo ser PKCS#1 ou PKCS#8
        String keyContent = new String(Files.readAllBytes(Paths.get(pathKey)));
        PrivateKey privateKey;

        if (keyContent.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            // PKCS#1
            String privateKeyPEM = keyContent
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
            ASN1Sequence primitive = (ASN1Sequence) ASN1Sequence.fromByteArray(keyBytes);
            RSAPrivateKey rsa = RSAPrivateKey.getInstance(primitive);
            RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(
                    rsa.getModulus(),
                    rsa.getPublicExponent(),
                    rsa.getPrivateExponent(),
                    rsa.getPrime1(),
                    rsa.getPrime2(),
                    rsa.getExponent1(),
                    rsa.getExponent2(),
                    rsa.getCoefficient()
            );
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(spec);

        } else if (keyContent.contains("-----BEGIN PRIVATE KEY-----")) {
            // PKCS#8
            String privateKeyPEM = keyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(spec);
        } else {
            throw new IllegalArgumentException("Formato de chave privada não suportado.");
        }

        // 4. Criar KeyStore PKCS12
        KeyStore pkcs12 = KeyStore.getInstance("PKCS12", "BC");


        try (InputStream fis = pathPfx.getInputStream()) {
            pkcs12.load(fis, pfxPassword.toCharArray());
        }

        String alias = pkcs12.aliases().nextElement();
        pkcs12.setKeyEntry(alias, privateKey, pfxPassword.toCharArray(), new Certificate[]{cert});

        // 5. Salvar PFX
        String nomeArquivo = requestClient.getClient() + ".pfx";
        Path caminhoSaidaPfx = pastaPfx.resolve(nomeArquivo);
        try (FileOutputStream out = new FileOutputStream(caminhoSaidaPfx.toFile())) {
            pkcs12.store(out, pfxPassword.toCharArray());
        }

        System.out.println("Arquivo PFX gerado com sucesso em: " + caminhoSaidaPfx.toAbsolutePath());
    }
}
