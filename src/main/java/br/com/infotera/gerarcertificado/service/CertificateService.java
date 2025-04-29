package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.RequestClient;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import br.com.infotera.gerarcertificado.model.token.ResponseToken;
import br.com.infotera.gerarcertificado.util.PfxProcessUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;


/**
 * The type Certificado service.
 */
@Service
public class CertificateService {

    private final RequestService requestService;
    private final PfxProcessUtil pfxProcessUtil;
    private final Logger logger = Logger.getLogger(CertificateService.class.getName());

    public CertificateService(RequestService requestService, PfxProcessUtil pfxProcessUtil) {
        this.requestService = requestService;
        this.pfxProcessUtil = pfxProcessUtil;
    }


    public ResponseCertificate
    renewPixCertificate(RequestClient requestClient, MultipartFile clientPfx) throws Exception {

        if (clientPfx.isEmpty()) {
            throw new ResourceException("Arquivo PFX vazio");
        } else {
            clientPfx.getResource();
        }

        Path pathDiretory = Path.of("", "certificados");

        if (Files.exists(pathDiretory)) {
            logger.info("🧹 Arquivos antigos removidos");
            deleteDirectoryRecursively(pathDiretory);
        }

        // 1. Localizar o arquivo PFX - arquivo vem da requisição

        // 2. Criar diretório temporário para os arquivos gerados
        Path tempDir = Files.createDirectory(pathDiretory);
        logger.info("📁 Diretório temporário criado: " + tempDir);

        // 3. Definir caminhos dos arquivos de saída
        String keyPath = tempDir.resolve(requestClient.getClient() + ".key").toString();
        String crtPath = tempDir.resolve(requestClient.getClient() + ".crt").toString();
        String csrPath = tempDir.resolve(requestClient.getClient() + ".csr").toString();

        try {
            // 4. Processa certificados
            pfxProcessUtil.processPfx(clientPfx, requestClient.getClient(), requestClient.getClientId(), Path.of(keyPath), Path.of(crtPath), Path.of(csrPath));

            // 5. Gerar token
            ResponseToken tokenResponse = requestService.generateToken(crtPath, keyPath, requestClient.getClientId(), requestClient.getClientSecret());

            if (tokenResponse != null) {
                logger.config("✅ Access Token gerado com sucesso:");
                logger.info(tokenResponse.toString());
            }

            // 6. Renovação do certificado (vem como conteudo.csr)
            ResponseCertificate responseCertificate = requestService.renewCertificate(csrPath, crtPath, keyPath, tokenResponse);

            // 7. Gera um arquivo.cer

            return responseCertificate;
        } catch (Exception e) {
            logger.info("❌ Erro ao processar certificados: " + e.getMessage());
            throw new ResourceException("Erro ao processar certificados: " + e.getMessage());
        } finally {
            logger.info("🎉 Fluxo concluido");
        }
    }

    // Limpa o diretório temporário
    private void deleteDirectoryRecursively(Path path) throws IOException {

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