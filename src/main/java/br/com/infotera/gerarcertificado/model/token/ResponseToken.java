package br.com.infotera.gerarcertificado.model.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseToken implements Serializable {

    @SerializedName("access_token")
    @JsonProperty("access_token")
    private String accessToken;

    @SerializedName("token_type")
    @JsonProperty("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    @JsonProperty("expires_in")
    private Integer expiresIn;

    @SerializedName("refresh_token")
    @JsonProperty("refresh_token")
    private String refreshToken;
    private String scope;
    private Boolean active;
}
