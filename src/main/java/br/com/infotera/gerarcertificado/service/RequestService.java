package br.com.infotera.gerarcertificado.service;

import br.com.infotera.gerarcertificado.config.HttpClientConfigurator;
import br.com.infotera.gerarcertificado.exception.ResourceException;
import br.com.infotera.gerarcertificado.model.certificate.ErrorCertificate;
import br.com.infotera.gerarcertificado.model.certificate.ResponseCertificate;
import br.com.infotera.gerarcertificado.model.token.ResponseToken;
import br.com.infotera.gerarcertificado.util.CertificateMapper;
import br.com.infotera.gerarcertificado.util.SSLUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.modelmapper.ModelMapper;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * The type Token service.
 */
@Service
public class RequestService {

    private final Logger logger = Logger.getLogger(RequestService.class.getName());
    private final String url = "https://sts.itau.com.br/";

    private final SSLUtil sslUtil;
    private final RestTemplate restTemplate;

    private final ModelMapper modelMapper = new ModelMapper();
    private final Gson gson = new Gson();


    public RequestService(SSLUtil sslUtil, RestTemplate restTemplate) {
        this.sslUtil = sslUtil;
        this.restTemplate = restTemplate;
    }


    /**
     * Gera um novo ‘token’ para poder fazer a renovação do certificado.
     *
     * @param crtPath      the crt path
     * @param keyPath      the key path
     * @param clientId     the client id
     * @param clientSecret the client secret
     * @return Retorna um objeto ResponseToken
     */
    public ResponseToken generateToken(String crtPath, String keyPath,
                                       String clientId, String clientSecret) {
        try {
            // 1. Configurar HttpClient com SSL + certificado digital
            HttpClientConfigurator httpClientConfigurator = new HttpClientConfigurator(sslUtil);
            httpClientConfigurator.configure(restTemplate, crtPath, keyPath);

            // 2. Preparar requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s",
                    clientId, clientSecret);
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // 3. Fazer chamada
            String requestUrl = url + "api/oauth/token";
            ResponseEntity<ResponseToken> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, ResponseToken.class);

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


    /**
     * Renova o certificado.
     *
     * @param crtPath the crt path
     * @param keyPath the key path
     * @return the response token
     */
    public ResponseCertificate renewCertificate(String csrPath, String crtPath, String keyPath, ResponseToken responseToken) {
        try {
            if (responseToken == null || responseToken.getAccessToken() == null || responseToken.getAccessToken().isEmpty()) {
                throw new ResourceException("Token inválido");
            }

            // 1. Configurar HttpClient
            HttpClientConfigurator httpClientConfigurator = new HttpClientConfigurator(sslUtil);
            httpClientConfigurator.configure(restTemplate, crtPath, keyPath);

            // 2. Preparar requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setBearerAuth(responseToken.getAccessToken());

            // 3. Processa arquivo.csr
            String body = CertificateMapper.extractFromFile(csrPath);
            if (body == null || body.isEmpty()) {
                throw new ResourceException("Erro ao extrair arquivo.csr");
            }
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // 4. Fazer chamada
            String urlRequest = url + "seguranca/v1/certificado/renovacao";
            ResponseEntity<String> response = restTemplate.exchange(urlRequest, HttpMethod.POST, request, String.class);

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                logger.info("✅ Certificado renovado com sucesso");
                return CertificateMapper.certificateMapper(response.getBody() != null ? response.getBody() : "");
            } else if (response == null || response.getBody() == null) {
                throw new ResourceException("Erro na requisição. Problema interno do servidor");
            } else {
                throw new ResourceException("Erro na requisição. Código de status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            // Tratamento da mensagem de erro
            String msg = e.getMessage().replaceAll("<EOL>|\\?", "");

            int jsonStart = msg.indexOf("{");
            int jsonEnd = msg.lastIndexOf("}");

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                JsonElement json = JsonParser.parseString(msg.substring(jsonStart, jsonEnd + 1));

                ErrorCertificate errorCertificate = gson.fromJson(json, ErrorCertificate.class);
                throw new ResourceException("Erro ao renovar certificado: " + errorCertificate.getMensagem());
            } else {
                throw new ResourceException("Erro ao renovar certificado: " + e.getMessage());
            }
        }
    }

    /**
     * Gera o primeiro certificado.
     *
     * @param csrPath the csr path
     * @param responseToken token passado pelo itau
     * @return the response token
     */
    public ResponseCertificate createCertificate(String csrPath, String responseToken) {
        try {

            // 1. Configurar HttpClient
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .evictExpiredConnections()
                    .build();

            factory.setHttpClient(httpClient);
            restTemplate.setRequestFactory(factory);

            // 2. Preparar requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setBearerAuth(responseToken);

            // 3. Processa arquivo.csr
            String body = CertificateMapper.extractFromFile(csrPath);
            if (body == null || body.isEmpty()) {
                throw new ResourceException("Erro ao extrair arquivo.csr");
            }
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // 4. Fazer chamada
            String urlRequest = url + "seguranca/v1/certificado/solicitacao";
            ResponseEntity<String> response = restTemplate.exchange(urlRequest, HttpMethod.POST, request, String.class);

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                logger.info("✅ Certificado gerado com sucesso");
                ResponseCertificate responseCertificate =  CertificateMapper.certificateMapper(response.getBody() != null ? response.getBody() : "");

                // Gravar certificado em um arquivo
                if (responseCertificate.getCertificate() != null) {
                    Path outputPath = Paths.get("certificados/certificado_cer.txt");
                    Files.writeString(outputPath, response.getBody(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    logger.info("📁 Certificado gravado em: " + outputPath.toAbsolutePath());
                }
                return responseCertificate;
            } else if (response == null || response.getBody() == null) {
                throw new ResourceException("Erro na requisição. Problema interno do servidor");
            } else {
                throw new ResourceException("Erro na requisição. Código de status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            // Tratamento da mensagem de erro
            String msg = e.getMessage().replaceAll("<EOL>|\\?", "");

            int jsonStart = msg.indexOf("{");
            int jsonEnd = msg.lastIndexOf("}");

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                JsonElement json = JsonParser.parseString(msg.substring(jsonStart, jsonEnd + 1));

                ErrorCertificate errorCertificate = gson.fromJson(json, ErrorCertificate.class);
                throw new ResourceException("Erro ao renovar certificado: " + errorCertificate.getMensagem());
            } else {
                throw new ResourceException("Erro ao renovar certificado: " + e.getMessage());
            }
        }
    }
}
