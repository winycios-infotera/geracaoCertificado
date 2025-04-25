package br.com.infotera.gerarcertificado.controller;


import br.com.infotera.gerarcertificado.model.RequestClient;
import br.com.infotera.gerarcertificado.model.ResponseToken;
import br.com.infotera.gerarcertificado.service.CertificadoService;
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
public class CertificadoController {

    private final CertificadoService certificadoService;

    /**
     * Renova o certificado pix.
     *
     * @param requestClient dados do cliente
     * @param clientPfx     ultimo arquivo.pfx gerado
     * @return Retorna um objeto ResponseToken
     * @throws Exception the exception
     */
    @PostMapping(value = "/renovar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseToken> renewPixCertificate(
            @ModelAttribute @Valid RequestClient requestClient,
            @RequestParam("clientPfx") MultipartFile clientPfx) throws Exception {

        return ResponseEntity.ok(certificadoService.renewPixCertificate(requestClient, clientPfx));
    }
}
