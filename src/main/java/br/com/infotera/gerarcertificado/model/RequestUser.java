package br.com.infotera.gerarcertificado.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The type Request user.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestUser {

    // cliente que está solicitando o certificado
    private String client;

    // clientId do cliente
    private String clientId;

    // clientSecret do cliente
    private String clientSecret;
}
