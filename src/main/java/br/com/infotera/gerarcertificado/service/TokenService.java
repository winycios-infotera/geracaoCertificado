package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.ResponseToken;
import br.com.infotera.gerarcertificado.util.SSLUtil;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.util.logging.Logger;

@Service
public class TokenService {
    private final Logger logger = Logger.getLogger(TokenService.class.getName());

    private final SSLUtil sslUtil;
    private final RestTemplate restTemplate;


    public TokenService(SSLUtil sslUtil, RestTemplate restTemplate) {
        this.sslUtil = sslUtil;
        this.restTemplate = restTemplate;
    }

    public ResponseToken generateToken(String crtPath, String keyPath,
                                    String clientId, String clientSecret) {
        try {
            // 1. Configuração SSL
            SSLContext sslContext = sslUtil.createSSLContext(crtPath, keyPath);

            if (sslContext == null) {
                throw new ResourceException("Erro ao configurar o SSL");
            }
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{"TLSv1.2", "TLSv1.3"},
                    null,
                    new DefaultHostnameVerifier()
            );

            // 2. Configurar PoolingHttpClientConnectionManager
            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .evictExpiredConnections()
                    .build();

            // 3. Construir o HttpClient
            ((HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory()).setHttpClient(httpClient);

            // 4. Preparar requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s",
                    clientId, clientSecret);
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // 5. Fazer chamada
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
