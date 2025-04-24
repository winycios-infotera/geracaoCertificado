package br.com.infotera.gerarcertificado.controller;


import br.com.infotera.gerarcertificado.config.ModelMapperConfig;
import br.com.infotera.gerarcertificado.model.RequestUser;
import br.com.infotera.gerarcertificado.model.ResponseToken;
import br.com.infotera.gerarcertificado.service.CertificadoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequestMapping("/certificado")
@RestController
@AllArgsConstructor
public class CertificadoController {

    private final CertificadoService certificadoService;

    @PostMapping("/autenticar")
    public ResponseEntity<ResponseToken> autenticar(@RequestBody RequestUser requestUser) throws Exception {

        return ResponseEntity.ok(certificadoService.autenticar(requestUser));
    }
}
