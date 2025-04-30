package br.com.infotera.gerarcertificado.config;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.util.SSLUtil;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

/**
 * Configura a requisição do HttpClient + SSL + certificado digital.
 */
@Configuration
public class HttpClientConfigurator {

    private final SSLUtil sslUtil;

    public HttpClientConfigurator(SSLUtil sslUtil) {
        this.sslUtil = sslUtil;
    }

    /**
     * Configuração
     *
     * @param restTemplate the rest template
     * @param crtPath      the crt path
     * @param keyPath      the key path
     */
    public void configure(RestTemplate restTemplate, String crtPath, String keyPath) {
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

        } catch (Exception e) {
            if (!e.getMessage().contains("SSL")) {
                throw new ResourceException("Erro ao configurar o HttpClient");
            }
            throw new ResourceException(e.getMessage());
        }
    }
}
