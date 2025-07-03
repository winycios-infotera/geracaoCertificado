package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.RequestClient;
import br.com.infotera.gerarcertificado.model.SimpleMultipartFile;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import br.com.infotera.gerarcertificado.model.token.ResponseToken;
import br.com.infotera.gerarcertificado.util.PfxProcessUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;


/**
 * The type Certificado service.
 */
@Service
public class RenewCertificateService {

    private final RequestService requestService;
    private final PfxProcessUtil pfxProcessUtil;
    private final Logger logger = Logger.getLogger(RenewCertificateService.class.getName());
    private final Map<String, String> clientPfxMap;


    public RenewCertificateService(RequestService requestService, PfxProcessUtil pfxProcessUtil, @Qualifier("clientPfxMap") Map<String, String> clientPfxMap) {
        this.requestService = requestService;
        this.pfxProcessUtil = pfxProcessUtil;
        this.clientPfxMap = clientPfxMap;
    }

    public String renewPixCertificate(RequestClient requestClient) throws Exception {

        String filename = clientPfxMap.get(requestClient.getClient().toLowerCase());
        SimpleMultipartFile clientPfx = null;
        if (filename.isBlank()) {
            throw new ResourceException("Cliente informado não encontrado");
        } else {
            clientPfx = pfxProcessUtil.getPfxFileForClient(filename);
            requestClient.setClient(clientPfx.getFilename());
        }

        Path pathDiretory = Path.of("", "certificados");

        if (Files.exists(pathDiretory)) {
            logger.info("🧹 Arquivos antigos removidos");
            pfxProcessUtil.deleteDirectoryRecursively(pathDiretory);
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

            // 7. Gera um arquivo.pfx
            pfxProcessUtil.gerarPfx(responseCertificate, keyPath, clientPfx, requestClient.getClient());

            return "pfx/" + requestClient.getClient() + ".pfx";
        } catch (Exception e) {
            logger.info("❌ Erro ao processar certificados: " + e.getMessage());
            throw new ResourceException("Erro ao processar certificados: " + e.getMessage());
        } finally {
            logger.info("🎉 Fluxo concluido");
        }
    }

}