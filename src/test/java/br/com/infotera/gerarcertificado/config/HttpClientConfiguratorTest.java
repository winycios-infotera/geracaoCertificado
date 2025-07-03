package br.com.infotera.gerarcertificado.config;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.util.SSLUtil;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpClientConfiguratorTest {

    @Mock
    private SSLUtil sslUtil;
    private HttpClientConfigurator configurator;
    private RestTemplate restTemplate;
    private HttpComponentsClientHttpRequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        sslUtil = mock(SSLUtil.class);
        configurator = new HttpClientConfigurator(sslUtil);
        requestFactory = new HttpComponentsClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
    }

    @Test
    @DisplayName("deve configurar HttpClient com sucesso")
    void testMustConfigureHttpClient() throws Exception {
        SSLContext mockContext = SSLContexts.createDefault();

        when(sslUtil.createSSLContext("path/to/crt", "path/to/key")).thenReturn(mockContext);

        assertDoesNotThrow(() -> configurator.configure(restTemplate, "path/to/crt", "path/to/key"));
        assertTrue(requestFactory.getHttpClient() instanceof CloseableHttpClient);
        verify(sslUtil, times(1)).createSSLContext("path/to/crt", "path/to/key");
    }

    @Test
    @DisplayName("deve lancar excecao quando SSLContext eh null")
    void testMustThrowWhenSSLContextIsNull() throws Exception {
        when(sslUtil.createSSLContext(anyString(), anyString())).thenReturn(null);

        ResourceException exception = assertThrows(ResourceException.class, () ->
                configurator.configure(restTemplate, "crt", "key"));

        assertEquals("Erro ao configurar o SSL", exception.getMessage());
    }

    @Test
    @DisplayName("deve lancar excecao generica tratada")
    void testMustThrowHandledGenericException() throws Exception {
        when(sslUtil.createSSLContext(anyString(), anyString())).thenThrow(new RuntimeException("Falha qualquer"));

        ResourceException exception = assertThrows(ResourceException.class, () ->
                configurator.configure(restTemplate, "crt", "key"));

        assertEquals("Erro ao configurar o HttpClient", exception.getMessage());
    }
}
