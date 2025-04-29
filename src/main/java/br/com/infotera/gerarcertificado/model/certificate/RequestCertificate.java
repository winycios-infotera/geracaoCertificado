package br.com.infotera.gerarcertificado.model.certificate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RequestCertificate implements Serializable {

    private String bodyCsr;
}
