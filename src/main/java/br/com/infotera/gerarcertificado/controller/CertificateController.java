package br.com.infotera.gerarcertificado.controller;


import br.com.infotera.gerarcertificado.model.RequestClient;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import br.com.infotera.gerarcertificado.model.token.ResponseToken;
import br.com.infotera.gerarcertificado.service.CertificateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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
     * @param clientPfx     ultimo arquivo.pfx gerado
     * @return Retorna um objeto ResponseToken
     * @throws Exception the exception
     */
    @PostMapping(value = "/renovar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseCertificate> renewPixCertificate(
            @ModelAttribute @Valid RequestClient requestClient,
            @RequestParam("clientPfx") MultipartFile clientPfx) throws Exception {

        return ResponseEntity.ok(certificateService.renewPixCertificate(requestClient, clientPfx));
    }
}
