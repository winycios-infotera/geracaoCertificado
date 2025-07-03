package br.com.infotera.gerarcertificado.controller;


import br.com.infotera.gerarcertificado.model.RequestClient;
import br.com.infotera.gerarcertificado.model.RequestCreateClient;
import br.com.infotera.gerarcertificado.service.CreateCertificateService;
import br.com.infotera.gerarcertificado.service.RenewCertificateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * The type Certificado controller.
 */
@RequestMapping("/certificado")
@RestController
@AllArgsConstructor
public class CertificateController {

    private final RenewCertificateService renewCertificateService;

    private final CreateCertificateService createCertificateService;


    /**
     * Renova o certificado pix.
     *
     * @param requestClient dados do cliente
     * @return Retorna o arquivo.pfx renovado em bytes para ‘download’
     * @throws Exception the exception
     */
    @PostMapping(value = "/renovar/byte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> renewPixCertificate(@ModelAttribute @Valid RequestClient requestClient) throws Exception {
        Path path = Paths.get(renewCertificateService.renewPixCertificate(requestClient));
        String fileName = path.getFileName().toString();

        URI downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/certificado/download/")
                .path(fileName)
                .build()
                .toUri();

        // Redireciona o navegador para o download
        return ResponseEntity.status(HttpStatus.SEE_OTHER) // ou FOUND (302)
                .location(downloadUrl)
                .build();
    }

    /**
     * Cria o certificado pix.
     *
     * @param requestClient dados do cliente
     * @return Retorna o novo arquivo,pfx
     * @throws Exception the exception
     */
    @PostMapping(value = "/create/byte")
    public ResponseEntity<String> createPixCertificate(@ModelAttribute @Valid RequestCreateClient requestClient) throws Exception {
        Path path = Paths.get((createCertificateService.createPixCertificate(requestClient)));
        String fileName = path.getFileName().toString();

        URI uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/certificado/download/")
                .path(fileName)
                .build()
                .toUri();

        return ResponseEntity.status(200).body("Para baixar o certificado, acesse em seu navegador: " + uri);
    }


    /**
     * Download certificado.
     *
     * @param fileName nome do arquivo
     * @return Retorna uma hiperligação para ‘download’ do certificado
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadCertificado(@PathVariable String fileName) throws IOException {
        Path filePath = Paths.get("pfx/").resolve(fileName).normalize();

        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(Files.size(filePath))
                .body(resource);
    }
}
