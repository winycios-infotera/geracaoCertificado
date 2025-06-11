package br.com.infotera.gerarcertificado.util;

import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class CertificateMapperTest {

    @Nested
    @DisplayName("Mapeamento de certificado")
    class CertificateMapping {
        @Test
        @DisplayName("Deve mapear corretamente a resposta bruta para ResponseCertificate")
        void shouldMapValidRawResponseToResponseCertificate() {
            String rawResponse = """
                    Secret: 3f6c75bf-8a54-4643-ac75-786916da47b5
                    -----BEGIN CERTIFICATE-----
                    MIIDizCCAnOgAwIBAgITLgAAACb+81zoTyaebQAAAAAAJjANBgkqhkiG9w0BAQsF
                    -----END CERTIFICATE-----""";

            ResponseCertificate response = CertificateMapper.certificateMapper(rawResponse);

            assertAll(
                    () -> assertNotNull(response, "ResponseCertificate não deveria ser nulo"),
                    () -> assertEquals("3f6c75bf-8a54-4643-ac75-786916da47b5", response.getSecret(), "Secret mapeado incorretamente"),
                    () -> assertEquals(
                            """
                                    -----BEGIN CERTIFICATE-----
                                    MIIDizCCAnOgAwIBAgITLgAAACb+81zoTyaebQAAAAAAJjANBgkqhkiG9w0BAQsF
                                    -----END CERTIFICATE-----""",
                            response.getCertificate(),
                            "Certificado mapeado corretamente"
                    )
            );
        }

        @Test
        @DisplayName("Deve lançar exceção ao receber resposta vazia")
        void shouldThrowExceptionForEmptyInput() {
            String rawResponse = "";

            ResourceException exception = assertThrows(ResourceException.class, () ->
                    CertificateMapper.certificateMapper(rawResponse)
            );

            assertAll(
                    () -> assertEquals("Resposta de certificado inválida.", exception.getMessage(), exception.getMessage()),
                    () -> assertNull(exception.getCause(), "A causa da exceção deveria ser nula")
            );
        }

        @Test
        @DisplayName("Deve lançar exceção ao receber resposta sem Secret")
        void shouldThrowExceptionWhenSecretIsMissing() {
            String rawResponse =
                    """
                            -----BEGIN CERTIFICATE-----
                            SomeCertDataHere
                            -----END CERTIFICATE-----""";

            ResourceException exception = assertThrows(ResourceException.class, () ->
                    CertificateMapper.certificateMapper(rawResponse)
            );

            assertAll(
                    () -> assertEquals("Resposta de certificado inválida.", exception.getMessage(), exception.getMessage()),
                    () -> assertNull(exception.getCause(), "A causa da exceção deveria ser nula")
            );
        }
    }

    @Nested
    @Disabled("Desabilitado, é necessário fornecer um arquivo .csr")
    @DisplayName("Extracao de certificado .csr")
    class CertificateExtraction {

        @Test
        @DisplayName("Deve extrair o corpo do certificado .csr corretamente")
        void shouldExtractCertificateBody() {
            String csrPath = "certificados/brasileirosporai.csr";

            String response = CertificateMapper.extractFromFile(csrPath);

            Assertions.assertEquals("""
                    -----BEGIN CERTIFICATE REQUEST-----
                    MIICyzCCAbMCAQAwgYUxCzAJBgNVBAYTAkJSMQswCQYDVQQIDAJTUDESMBAGA1UE
                    BwwJU2FvIFBhdWxvMRkwFwYDVQQKDBBCUkFTSUxFSVJPU1BPUkFJMQswCQYDVQQL
                    DAJJVDEtMCsGA1UEAwwkMDRhMGRmMmUtZDllMC00YzMyLWI5YWMtZGUzMTlkZTJi
                    NmE2MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1RFFBJIcVXn2hGCe
                    U9+vaUPLCIWawEnaH67VfWj8He9yKdeovyABnbiI35Q9djaTe2wHD3N651MNQOjz
                    1Ozopg8SKRGby0Ao3/zk6FTV+UuW2So84rKISvBgSlXimuAx8NfXJASTuohX8wsU
                    RawBa1M0Dr7UX/Ezz7TgPd8j01Q1byfO4T1BFjRAeM97PiF6hMOh3HM+PsMr6jkD
                    0qYHym42MC40z472SIgjJZI9zT4GOn6sVrplc8GypoeiHq+NumsbtEKFgOIiH5+c
                    4mnhehkL/HTyPwKwUp4kREOHZ+Au7kR/o3hWXvvwJfH9dFVpKs39JnmHnenWpGvu
                    8/6AmwIDAQABoAAwDQYJKoZIhvcNAQELBQADggEBABuJEs1V3B4xTmJcaa4DiFxl
                    yXaVFpTRsO7k9xGq7qzfRLTvHxGowEvCR1JUSF+5HSXuQiCBm6vyiAtwwYnL+s3h
                    XE5B+VAkaeJ9+nb5GSpvWCCTRRgT9UcKi2/XTtBxUsz/po1AwNjMOptnTTB2rOCl
                    JRHHbqpL53OBTuiUNOmyPS8lFw+Vpmb77SZgEKBIlTdtSB3hu+3kW14gctoyFgKv
                    qzZzh3V/DAI7kH1r4WT2+wlmXDdBsk9VtZ8o6N+kKLC4Hay0OuszLwTb9XJHcJmZ
                    nB3/90nBPjRsB8jhZQ1alie2MIvvxoxjRSsXwUHlfcWCGxR72cINKEXs8Oldn/Q=
                    -----END CERTIFICATE REQUEST-----
                    """, response);
            assertNotNull(response);
        }

        @Test
        @DisplayName("Deve lançar exceção quando não encontrar o arquivo .csr")
        void shouldThrowExceptionForEmptyInput() {
            String rawResponse = "";

            ResourceException exception = assertThrows(ResourceException.class, () ->
                    CertificateMapper.extractFromFile(rawResponse)
            );

            assertAll(
                    () -> assertEquals("Arquivo de certificado inválido ou não encontrado.", exception.getMessage()),
                    () -> assertNull(exception.getCause(), "A causa da exceção deveria ser nula")
            );
        }
    }
}
