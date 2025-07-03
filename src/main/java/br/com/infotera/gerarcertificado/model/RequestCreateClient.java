package br.com.infotera.gerarcertificado.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RequestCreateClient implements Serializable {

    // Nome da empresa
    @NotBlank
    private String nameClient;

    // Client Id enviado pelo itau
    @NotBlank
    private String clientId;

    // Token enviado pelo itau
    @NotBlank
    private String token;
}
