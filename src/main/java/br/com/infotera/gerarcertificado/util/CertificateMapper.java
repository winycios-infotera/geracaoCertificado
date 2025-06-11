package br.com.infotera.gerarcertificado.util;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class CertificateMapper {


    /**
     * Mapeamento para transformar a resposta bruta em um ResponseCertificate
     *
     * @param rawResponse resposta .csr
     * @return the response certificate
     */
    public static ResponseCertificate certificateMapper(String rawResponse) {
        ResponseCertificate response = new ResponseCertificate();

        String[] lines = rawResponse.split("\\r?\\n");


        StringBuilder certBuilder = new StringBuilder();
        for (int i = 1; i < lines.length; i++) {
            certBuilder.append(lines[i]).append("\n");
        }
        response.setCertificate(certBuilder.toString().trim());

        return response;
    }


    /**
     * Extrai o certificado do arquivo
     *
     * @param filePath caminho do arquivo.cer
     * @return the string response
     */
    public static String extractFromFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            throw new ResourceException("Arquivo de certificado inválido ou não encontrado.");
        }
    }
}
