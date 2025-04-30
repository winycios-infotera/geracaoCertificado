package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import br.com.infotera.gerarcertificado.model.token.ResponseToken;
import br.com.infotera.gerarcertificado.util.CertificateMapper;
import br.com.infotera.gerarcertificado.util.SSLUtil;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    SSLUtil sslUtil;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    RequestService requestService;

    @BeforeEach
    void setUp() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        lenient().when(restTemplate.getRequestFactory()).thenReturn(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Nested
    @DisplayName("Token Service")
    class TokenServiceTest {
        @Nested
        @DisplayName("Token Service - Sucesso")
        class RequestServiceSucesso {

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
                mockResponseToken.setAccessToken("token123");
                ResponseEntity<ResponseToken> responseEntity = new ResponseEntity<>(mockResponseToken, HttpStatus.OK);

                when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(ResponseToken.class)
                )).thenReturn(responseEntity);

                ResponseToken resultado = requestService.generateToken(crtPath, keyPath, clientId, clientSecret);

                assertNotNull(resultado);
                assertEquals("token123", resultado.getAccessToken());
            }
        }

        @Nested
        @DisplayName("Token Service - falha")
        class RequestServiceFalha {

            @Test
            @DisplayName("deve lançar exceção de SSL ao gerar token")
            void testGenerateTokenSslException() throws Exception {
                String crtPath = "client.crt";
                String keyPath = "client.key";
                String clientId = "cliente";
                String clientSecret = "segredo";

                when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(null);

                Exception exception = Assertions.assertThrows(ResourceException.class, () -> {
                    requestService.generateToken(crtPath, keyPath, clientId, clientSecret);
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
                mockResponseToken.setAccessToken("token123");
                ResponseEntity<ResponseToken> responseEntity = new ResponseEntity<>(mockResponseToken, HttpStatus.UNAUTHORIZED);

                when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(ResponseToken.class)
                )).thenReturn(responseEntity);

                Exception exception = Assertions.assertThrows(ResourceException.class, () -> {
                    requestService.generateToken(crtPath, keyPath, clientId, clientSecret);
                });

                Assertions.assertEquals("Erro ao obter token: Erro na requisição. Código de status: 401 UNAUTHORIZED", exception.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Renew Certificate Service")
    class RenewCertificateTest {

        @Nested
        @DisplayName("Renew Certificate - Sucesso")
        class RequestServiceSucesso {

            @Test
            @DisplayName("Deve renovar certificado .csr com sucesso")
            void testRenewCertificate() throws Exception {
                String csrPath = "client.csr";
                String crtPath = "client.crt";
                String keyPath = "client.key";
                ResponseToken mockResponseToken = new ResponseToken();
                mockResponseToken.setAccessToken("token123");

                SSLContext mockSSLContext = SSLContext.getDefault();
                when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

                // ‘String’ da resposta simulada
                String certificadoString = "certificado";

                ResponseCertificate mockResponseCertificate = new ResponseCertificate();
                mockResponseCertificate.setCertificate(certificadoString);

                try (MockedStatic<CertificateMapper> mocked = mockStatic(CertificateMapper.class)) {
                    mocked.when(() -> CertificateMapper.extractFromFile(csrPath)).thenReturn("conteudo do csr");
                    mocked.when(() -> CertificateMapper.certificateMapper(certificadoString)).thenReturn(mockResponseCertificate);

                    ResponseEntity<String> responseEntity = new ResponseEntity<>(certificadoString, HttpStatus.OK);

                    when(restTemplate.exchange(
                            anyString(),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(String.class)
                    )).thenReturn(responseEntity);

                    ResponseCertificate resultado = requestService.renewCertificate(csrPath, crtPath, keyPath, mockResponseToken);

                    assertNotNull(resultado);
                    assertEquals("certificado", resultado.getCertificate());
                }
            }
        }

        @Nested
        @DisplayName("Renew Certificate - Falha")
        class RequestServiceFalha {

            @Test
            @DisplayName("deve lançar exceção ao tentar extrair o certificado null")
            void testExceptionCertificateMapperExtractFromNull() throws Exception {
                String csrPath = "client.csr";
                String crtPath = "client.crt";
                String keyPath = "client.key";
                ResponseToken mockResponseToken = new ResponseToken();
                mockResponseToken.setAccessToken("token123");

                SSLContext mockSSLContext = SSLContext.getDefault();
                when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

                // ‘String’ da resposta simulada
                String certificadoString = "certificado";

                ResponseCertificate mockResponseCertificate = new ResponseCertificate();
                mockResponseCertificate.setCertificate(certificadoString);

                try (MockedStatic<CertificateMapper> mocked = mockStatic(CertificateMapper.class)) {
                    mocked.when(() -> CertificateMapper.extractFromFile(csrPath)).thenReturn(null);

                    Exception exception = assertThrows(ResourceException.class, () -> {
                        requestService.renewCertificate(csrPath, crtPath, keyPath, mockResponseToken);
                    });

                    Assertions.assertEquals("Erro ao renovar certificado: Erro ao extrair arquivo.csr", exception.getMessage());
                }
            }

            @Test
            @DisplayName("deve lançar exceção ao tentar extrair o certificado vazio")
            void testExceptionCertificateMapperExtractFromEmpty() throws Exception {
                String csrPath = "client.csr";
                String crtPath = "client.crt";
                String keyPath = "client.key";
                ResponseToken mockResponseToken = new ResponseToken();
                mockResponseToken.setAccessToken("token123");

                SSLContext mockSSLContext = SSLContext.getDefault();
                when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

                // ‘String’ da resposta simulada
                String certificadoString = "certificado";

                ResponseCertificate mockResponseCertificate = new ResponseCertificate();
                mockResponseCertificate.setCertificate(certificadoString);

                try (MockedStatic<CertificateMapper> mocked = mockStatic(CertificateMapper.class)) {
                    mocked.when(() -> CertificateMapper.extractFromFile(csrPath)).thenReturn("");

                    Exception exception = assertThrows(ResourceException.class, () -> {
                        requestService.renewCertificate(csrPath, crtPath, keyPath, mockResponseToken);
                    });

                    Assertions.assertEquals("Erro ao renovar certificado: Erro ao extrair arquivo.csr", exception.getMessage());
                }
            }


            @Test
            @DisplayName("deve lançar exceção ao tentar mapear o resultado null")
            void testExceptionCertificateMapperMappingNull() throws Exception {
                String csrPath = "client.csr";
                String crtPath = "client.crt";
                String keyPath = "client.key";
                ResponseToken mockResponseToken = new ResponseToken();
                mockResponseToken.setAccessToken("token123");

                SSLContext mockSSLContext = SSLContext.getDefault();
                when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

                String certificadoString = "certificado";

                ResponseCertificate mockResponseCertificate = new ResponseCertificate();
                mockResponseCertificate.setCertificate(certificadoString);

                try (MockedStatic<CertificateMapper> mocked = mockStatic(CertificateMapper.class)) {
                    mocked.when(() -> CertificateMapper.extractFromFile(csrPath)).thenReturn(certificadoString);
                    mocked.when(() -> CertificateMapper.certificateMapper(certificadoString)).thenReturn(null);

                    Exception exception = assertThrows(ResourceException.class, () -> {
                        requestService.renewCertificate(csrPath, crtPath, keyPath, mockResponseToken);
                    });

                    assertTrue(exception.getMessage().contains("Erro na requisição. Problema interno do servidor"));
                    assertEquals("Erro ao renovar certificado: Erro na requisição. Problema interno do servidor", exception.getMessage());
                }
            }

            @Test
            @DisplayName("deve lançar exceção ao tentar enviar o token null")
            void testExceptionSendTokenNull() throws Exception {
                String csrPath = "client.csr";
                String crtPath = "client.crt";
                String keyPath = "client.key";

                SSLContext mockSSLContext = SSLContext.getDefault();
                lenient().when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

                String certificadoString = "certificado";

                ResponseCertificate mockResponseCertificate = new ResponseCertificate();
                mockResponseCertificate.setCertificate(certificadoString);

                try (MockedStatic<CertificateMapper> mocked = mockStatic(CertificateMapper.class)) {
                    mocked.when(() -> CertificateMapper.extractFromFile(csrPath)).thenReturn("conteudo do csr");
                    mocked.when(() -> CertificateMapper.certificateMapper(certificadoString)).thenReturn(mockResponseCertificate);

                    Exception exception = assertThrows(ResourceException.class, () -> {
                        requestService.renewCertificate(csrPath, crtPath, keyPath, new ResponseToken());
                    });

                    assertTrue(exception.getMessage().contains("Token inválido"));
                    assertEquals("Erro ao renovar certificado: Token inválido", exception.getMessage());
                }
            }


            @Test
            @DisplayName("deve lançar exceção ao realizar requisição e retornar conflict")
            void testExceptionSendConflict() throws Exception {
                String csrPath = "client.csr";
                String crtPath = "client.crt";
                String keyPath = "client.key";
                ResponseToken mockResponseToken = new ResponseToken();
                mockResponseToken.setAccessToken("token123");

                SSLContext mockSSLContext = SSLContext.getDefault();
                when(sslUtil.createSSLContext("client.crt", "client.key")).thenReturn(mockSSLContext);

                String certificadoString = "certificado";

                String exceptionMessage = "{<EOL><EOL>?\"mensagem\": \"O certificado ainda está valido. Renovação não permitida fora do prazo de 60 dias antes do vencimento.\",<EOL><EOL>?\"acao\": \"Utilize o certificado vigente.\",<EOL><EOL>?\"codigo_erro\": \"C700\"<EOL><EOL>}";

                ResponseCertificate mockResponseCertificate = new ResponseCertificate();
                mockResponseCertificate.setCertificate(certificadoString);

                try (MockedStatic<CertificateMapper> mocked = mockStatic(CertificateMapper.class)) {
                    mocked.when(() -> CertificateMapper.extractFromFile(csrPath)).thenReturn(certificadoString);
                    mocked.when(() -> CertificateMapper.certificateMapper(certificadoString)).thenReturn(mockResponseCertificate);

                    HttpClientErrorException exception409 = new HttpClientErrorException(HttpStatus.CONFLICT, "Erro ao renovar certificado: Certificado já renovado", exceptionMessage.getBytes(), StandardCharsets.UTF_8);

                    when(restTemplate.exchange(
                            anyString(),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(String.class)))
                            .thenThrow(exception409);

                    ResourceException thrown = assertThrows(ResourceException.class, () -> {
                        requestService.renewCertificate(csrPath, crtPath, keyPath, mockResponseToken);
                    });

                    assertTrue(thrown.getMessage().contains("Erro ao renovar certificado: Certificado já renovado"));
                }
            }
        }
    }
}

