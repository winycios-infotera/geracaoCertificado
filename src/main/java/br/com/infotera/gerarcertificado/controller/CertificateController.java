package br.com.infotera.gerarcertificado.controller;


import br.com.infotera.gerarcertificado.model.RequestClient;
import br.com.infotera.gerarcertificado.service.CertificateService;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    private final CertificateService certificateService;

    /**
     * Renova o certificado pix.
     *
     * @param requestClient dados do cliente
     * @return Retorna o arquivo.pfx renovado em bytes para ‘download’
     * @throws Exception the exception
     */
    @PostMapping(value = "/renovar/byte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> renewPixCertificate(
            @ModelAttribute @Valid RequestClient requestClient) throws Exception {

        Path path = Paths.get(certificateService.renewPixCertificate(requestClient));
        Resource resource = new FileSystemResource(path);

        String fileName = path.getFileName().toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/x-pkcs12")
                .contentLength(Files.size(path))
                .body(resource);
    }
}
