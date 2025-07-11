package br.com.infotera.gerarcertificado.util;


import br.com.infotera.gerarcertificado.model.SimpleMultipartFile;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.logging.Logger;

@Component
public class PfxProcessUtil {

    private final Logger logger = Logger.getLogger(PfxProcessUtil.class.getName());

    @Value("${app.certificates.pfxPassword}")
    private String pfxPassword;

    // mapeia o arquivo pfx
    public SimpleMultipartFile getPfxFileForClient(String filename) throws IOException {
        Resource resource = new ClassPathResource("pfx/" + filename);

        if (!resource.exists()) {
            throw new IllegalStateException("Arquivo PFX não encontrado: " + filename);
        }

        byte[] content;
        try (InputStream is = resource.getInputStream()) {
            content = is.readAllBytes();
        }

        return new SimpleMultipartFile(filename.substring(0, filename.lastIndexOf('.')), filename, "application/x-pkcs12", content);
    }


    //    Processa arquivo PFX
    public void processPfx(SimpleMultipartFile clientPfx, String client, String clientId, Path keyPath, Path crtPath, Path csrPath) throws Exception {
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
    public void savePrivateKey(PrivateKey privateKey, Path keyPath) throws IOException {
        try (var writer = new JcaPEMWriter(new FileWriter(keyPath.toFile()))) {
            writer.writeObject(privateKey);
        }
    }

    //Gera arquivo.crt
    public void saveCertificate(X509Certificate cert, Path crtPath) throws IOException {
        try (var writer = new JcaPEMWriter(new FileWriter(crtPath.toFile()))) {
            writer.writeObject(cert);
        }
    }

    //Gera arquivo .csr
    public void generateCSR(PrivateKey privateKey, X509Certificate cert, String client, String clientId, Path csrPath) throws Exception {
        X500Name subject = new X500Name(String.format("C=BR, ST=SP, L=Sao Paulo, O=%s, OU=IT, CN=%s", client.toUpperCase(), clientId));

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(cert.getPublicKey().getEncoded());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
        PKCS10CertificationRequestBuilder csrBuilder = new PKCS10CertificationRequestBuilder(subject, publicKeyInfo);
        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        try (var writer = new JcaPEMWriter(new FileWriter(csrPath.toFile()))) {
            writer.writeObject(csr);
        }
    }

    public void generateKeyAndCsr(String client, String clientId, Path keyPath, Path csrPath) throws Exception {
        // Adiciona o provider BouncyCastle
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // 1. Gerar par de chaves
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name(String.format("C=BR, ST=SP, L=Sao Paulo, O=%s, OU=IT, CN=%s", client.toUpperCase(), clientId));

        // 3. Gerar CSR
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        ContentSigner signer = new JcaContentSignerBuilder("SHA512withRSA").build(keyPair.getPrivate());
        PKCS10CertificationRequestBuilder csrBuilder = new PKCS10CertificationRequestBuilder(subject, publicKeyInfo);
        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        // 4. Salvar chave privada (.key)
        try (var writer = new JcaPEMWriter(new FileWriter(keyPath.toFile()))) {
            writer.writeObject(keyPair.getPrivate());
        }

        // 5. Salvar CSR (.csr)
        try (var writer = new JcaPEMWriter(new FileWriter(csrPath.toFile()))) {
            writer.writeObject(csr);
        }

        logger.info("✅ KEY e CSR gerados com sucesso.");
    }

    public void gerarPfx(ResponseCertificate responseCertificate, String pathKey, SimpleMultipartFile pathPfx, String clientName) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // 1. Criar pasta "pfx" se não existir
        Path pastaPfx = Paths.get("pfx");
        if (!Files.exists(pastaPfx)) {
            Files.createDirectories(pastaPfx);
        }

        // 2. Processar certificado
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

        if (pathPfx != null) {
            try (InputStream fis = pathPfx.getInputStream()) {
                pkcs12.load(fis, pfxPassword.toCharArray());
            }
        } else {
            pkcs12.load(null, pfxPassword.toCharArray());
        }

        // Remover todas as entradas antigas
        Enumeration<String> aliases = pkcs12.aliases();
        while (aliases.hasMoreElements()) {
            String existingAlias = aliases.nextElement();
            pkcs12.deleteEntry(existingAlias);
        }

        // 4. Adicionar nova chave e certificado com aliás = clientName
        pkcs12.setKeyEntry(clientName, privateKey, pfxPassword.toCharArray(), new Certificate[]{cert});


        Path caminhoSaidaPfx = pastaPfx.resolve( clientName + ".pfx");
        try (FileOutputStream out = new FileOutputStream(caminhoSaidaPfx.toFile())) {
            pkcs12.store(out, pfxPassword.toCharArray());
        }

        logger.info("Arquivo PFX gerado com sucesso em: " + caminhoSaidaPfx.toAbsolutePath());
    }

    // Limpa o diretório temporário
    public void deleteDirectoryRecursively(Path path) throws IOException {

        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException e) {
                                    throw new RuntimeException("Erro ao deletar: " + p, e);
                                }
                            }
                    );
        }
    }
}
