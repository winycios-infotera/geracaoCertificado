package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.RequestCreateClient;
import br.com.infotera.gerarcertificado.model.SimpleMultipartFile;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import br.com.infotera.gerarcertificado.util.PfxProcessUtil;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;


@Service
public class CreateCertificateService {

    private final RequestService requestService;
    private final PfxProcessUtil pfxProcessUtil;
    private final Logger logger = Logger.getLogger(CreateCertificateService.class.getName());


    public CreateCertificateService(RequestService requestService, PfxProcessUtil pfxProcessUtil) {
        this.requestService = requestService;
        this.pfxProcessUtil = pfxProcessUtil;
    }

    public String createPixCertificate(RequestCreateClient requestClient) throws Exception {

        requestClient.setNameClient(requestClient.getNameClient().toLowerCase());

        Path pathDiretory = Path.of("", "certificados");

        if (Files.exists(pathDiretory)) {
            logger.info("🧹 Arquivos antigos removidos");
            pfxProcessUtil.deleteDirectoryRecursively(pathDiretory);
        }

        // 1. Criar diretório temporário para os arquivos gerados
        Path tempDir = Files.createDirectory(pathDiretory);
        logger.info("📁 Diretório temporário criado: " + tempDir);

        // 2. Definir caminhos dos arquivos de saída
        String keyPath = tempDir.resolve(requestClient.getNameClient() + ".key").toString();
        String csrPath = tempDir.resolve(requestClient.getNameClient() + ".csr").toString();

        try {
            // 3. Gerar um arquivo csr e key
            pfxProcessUtil.generateKeyAndCsr(requestClient.getNameClient(), requestClient.getClientId(), Path.of(keyPath), Path.of(csrPath));

            // 4. Gerar client_secret e conteudo do arquivo .cer
             ResponseCertificate responseCertificate = requestService.createCertificate(csrPath, requestClient.getToken());

            // 5. Gera um arquivo.pfx
            pfxProcessUtil.gerarPfx(responseCertificate, keyPath, null, requestClient.getNameClient());

            return "pfx/" + requestClient.getNameClient() + ".pfx";
        } catch (Exception e) {
            logger.info("❌ Erro ao processar certificados: " + e.getMessage());
            throw new ResourceException("Erro ao processar certificados: " + e.getMessage());
        } finally {
            logger.info("🎉 Fluxo concluido");
        }
    }
}
