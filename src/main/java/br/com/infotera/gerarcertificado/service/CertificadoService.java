package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.RequestUser;
import br.com.infotera.gerarcertificado.model.ResponseToken;

import br.com.infotera.gerarcertificado.util.PfxProcessorUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.logging.Logger;

@Service
public class CertificadoService {

    private final TokenService tokenService;
    private final PfxProcessorUtil pfxProcessorUtil;
    private final Logger logger = Logger.getLogger(CertificadoService.class.getName());

    public CertificadoService(TokenService tokenService, PfxProcessorUtil pfxProcessorUtil) {
        this.tokenService = tokenService;
        this.pfxProcessorUtil = pfxProcessorUtil;
    }

    public ResponseToken autenticar(RequestUser requestUser) throws Exception {
        if (requestUser.getClient().isEmpty() || requestUser.getClientId().isEmpty() || requestUser.getClientSecret().isEmpty()) {
            throw new ResourceException("Uso: java GerarCertificadoEToken <client> <clientId> <clientSecret>");
        }

        Path pathDiretory = Path.of("", "certificados");

        if (Files.exists(pathDiretory)) {
            logger.info("🧹 Arquivos antigos removidos");
            deletarDiretorioRecursivamente(pathDiretory);
        }

        // 1. Localizar o arquivo PFX no resources/documentos
        String pfxFile = requestUser.getClient() + ".pfx";
        Path tempPfxPath = getPfxFromResources(pfxFile);

        // 2. Criar diretório temporário para os arquivos gerados
        Path tempDir = Files.createDirectory(pathDiretory);
        logger.info("📁 Diretório temporário criado: " + tempDir);

        // 3. Definir caminhos dos arquivos de saída
        String keyPath = tempDir.resolve(requestUser.getClient() + ".key").toString();
        String crtPath = tempDir.resolve(requestUser.getClient() + ".crt").toString();
        String csrPath = tempDir.resolve(requestUser.getClient() + ".csr").toString();

        try {
            // 4. Processa certificados
            pfxProcessorUtil.processPfx(tempPfxPath, requestUser.getClient(), requestUser.getClientId(), Path.of(keyPath), Path.of(crtPath), Path.of(csrPath));

            // Ajustar permissões
            runCommand("chmod", "644", crtPath);
            runCommand("chmod", "644", keyPath);

            // 5. Gerar token
            ResponseToken tokenResponse = tokenService.gerarToken(crtPath, keyPath, requestUser.getClientId(), requestUser.getClientSecret());

            if (tokenResponse != null) {
                logger.config("✅ Access Token gerado com sucesso:");
                logger.info(tokenResponse.toString());
            }
            return tokenResponse;
        } catch (Exception e) {
            logger.info("❌ Erro ao processar certificados: " + e.getMessage());
            throw new ResourceException("❌ Erro ao processar certificados: " + e.getMessage());
        } finally {
            logger.info("🎉 Fluxo concluido");
        }
    }

    // Localizar o arquivo PFX no resources
    private Path getPfxFromResources(String pfxFile) throws IOException {
        String resourcePath = "documentos/" + pfxFile;
        InputStream inputStream = new ClassPathResource(resourcePath).getInputStream();

        if (inputStream == null) {
            throw new ResourceException("❌ Arquivo \"" + pfxFile + "\" não encontrado em resources/documentos.");
        }

        // Cria um arquivo temporário para o PFX
        Path tempPfxPath = Files.createTempFile("temp_", ".pfx");
        Files.copy(inputStream, tempPfxPath, StandardCopyOption.REPLACE_EXISTING);
        inputStream.close();

        return tempPfxPath;
    }

    private static void runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            throw new ResourceException("Erro ao executar comando: " + String.join(" ", command));
        }
    }

    // Limpa o diretório temporário
    private void deletarDiretorioRecursivamente(Path path) throws IOException {

        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException("Erro ao deletar: " + p, e);
                        }
                    });
        }
    }

}