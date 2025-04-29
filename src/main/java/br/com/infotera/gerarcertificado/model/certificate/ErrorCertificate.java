package br.com.infotera.gerarcertificado.model.certificate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorCertificate implements Serializable {

    private String mensagem;
    private String acao;

    @JsonProperty("codigo_erro")
    @SerializedName("codigo_erro")
    private String codigoErro;
}
