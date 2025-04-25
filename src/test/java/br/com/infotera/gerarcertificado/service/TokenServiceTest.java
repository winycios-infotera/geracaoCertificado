package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.ResponseToken;
import br.com.infotera.gerarcertificado.util.SSLUtil;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    SSLUtil sslUtil;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    TokenService tokenService;

    @BeforeEach
    void setUp() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        lenient().when(restTemplate.getRequestFactory()).thenReturn(new HttpComponentsClientHttpRequestFactory(httpClient));
    }


    @Nested
    @DisplayName("Token Service - Sucesso")
    class TokenServiceSucesso {

        @Test
        @DisplayName("Deve gerar token com sucesso")
        void testGenerateToken() throws Exception {
            String crtPath = "client.crt";
            String keyPath = "client.key";
            String clientId = "cliente";
            String clientSecret = "segredo";

            SSLContext mockSSLContext = SSLContext.getDefault();
            when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

            ResponseToken mockResponseToken = new ResponseToken();
            mockResponseToken.setAccess_token("token123");
            ResponseEntity<ResponseToken> responseEntity = new ResponseEntity<>(mockResponseToken, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(ResponseToken.class)
            )).thenReturn(responseEntity);

            ResponseToken resultado = tokenService.generateToken(crtPath, keyPath, clientId, clientSecret);

            assertNotNull(resultado);
            assertEquals("token123", resultado.getAccess_token());
        }
    }

    @Nested
    @DisplayName("Token Service - falha")
    class TokenServiceFalha {

        @Test
        @DisplayName("deve lançar exceção de SSL ao gerar token")
        void testGenerateTokenSslException() throws Exception {
            String crtPath = "client.crt";
            String keyPath = "client.key";
            String clientId = "cliente";
            String clientSecret = "segredo";

            when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(null);

            Exception exception = Assertions.assertThrows(ResourceException.class, () -> {
                tokenService.generateToken(crtPath, keyPath, clientId, clientSecret);
            });

            Assertions.assertEquals("Erro ao obter token: Erro ao configurar o SSL", exception.getMessage());
        }


        @Test
        @DisplayName("Deve lançar exceção de requisição ao gerar token")
        void testGenerateTokenRequestException() throws Exception {
            String crtPath = "client.crt";
            String keyPath = "client.key";
            String clientId = "cliente";
            String clientSecret = "segredo";

            SSLContext mockSSLContext = SSLContext.getDefault();
            when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

            ResponseToken mockResponseToken = new ResponseToken();
            mockResponseToken.setAccess_token("token123");
            ResponseEntity<ResponseToken> responseEntity = new ResponseEntity<>(mockResponseToken, HttpStatus.UNAUTHORIZED);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(ResponseToken.class)
            )).thenReturn(responseEntity);

            Exception exception = Assertions.assertThrows(ResourceException.class, () -> {
                tokenService.generateToken(crtPath, keyPath, clientId, clientSecret);
            });

            Assertions.assertEquals("Erro ao obter token: Erro na requisição. Código de status: 401 UNAUTHORIZED", exception.getMessage());
        }

    }
}

