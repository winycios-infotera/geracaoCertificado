package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.ResponseToken;
import br.com.infotera.gerarcertificado.util.SSLUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.modelmapper.ModelMapper;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.util.logging.Logger;

@Service
public class TokenService {
    private final Logger logger = Logger.getLogger(TokenService.class.getName());

    private final ModelMapper modelMapper;
    private final SSLUtil sslUtil;

    public TokenService(ModelMapper modelMapper, SSLUtil sslUtil) {
        this.modelMapper = modelMapper;
        this.sslUtil = sslUtil;
    }

    public ResponseToken gerarToken(String crtPath, String keyPath,
                                    String clientId, String clientSecret) {
        try {
            // 1. Configuração SSL
            SSLContext sslContext = sslUtil.createSSLContext(crtPath, keyPath);

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{"TLSv1.2", "TLSv1.3"},
                    null,
                    new DefaultHostnameVerifier()
            );

            // 3. Configurar PoolingHttpClientConnectionManager
            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            // 4. Construir o HttpClient
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .evictExpiredConnections()
                    .build();

            // 5. Configurar RestTemplate
            HttpComponentsClientHttpRequestFactory factory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);
            RestTemplate restTemplate = new RestTemplate(factory);

            // 6. Preparar requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s",
                    clientId, clientSecret);
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // 7. Fazer chamada
            String url = "https://sts.itau.com.br/api/oauth/token";
            ResponseEntity<ResponseToken> response = restTemplate.exchange(url, HttpMethod.POST, request, ResponseToken.class);


            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("✅ Token gerado com sucesso");
                return response.getBody();

            } else {
                logger.severe("Erro na requisição: " + response.getBody());
                throw new ResourceException("Erro na requisição. Código de status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.severe("Erro ao obter token: " + e.getMessage());
            throw new ResourceException("Erro ao obter token: " + e.getMessage());
        }
    }
}