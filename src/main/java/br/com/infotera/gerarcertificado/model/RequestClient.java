package br.com.infotera.gerarcertificado.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * The type Request user.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestClient implements Serializable {

    // cliente que está solicitando o certificado
    @NotBlank
    @Size(min = 2, max = 50)
    private String client;

    // clientId do cliente
    @NotBlank
    private String clientId;

    // clientSecret do cliente
    @NotBlank
    private String clientSecret;
}
