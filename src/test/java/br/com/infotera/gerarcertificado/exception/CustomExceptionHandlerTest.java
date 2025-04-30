package br.com.infotera.gerarcertificado.exception;

import br.com.infotera.gerarcertificado.exception.model.ModelError;
import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomExceptionHandlerTest {

    private CustomExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CustomExceptionHandler();
    }

    @Test
    @DisplayName("deve tratar MethodArgumentNotValidException")
    void testMethodArgumentNotValidException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/teste");

        FieldError fieldError = new FieldError("objeto", "campo", "mensagem de erro");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        var response = handler.exceptionPersonalized(exception, request);

        assertEquals(401, response.getStatusCode().value());
        ModelError modelError = response.getBody();
        assertNotNull(modelError);
        assertEquals("Erro de validação", modelError.getError());
        assertTrue(modelError.getMessage().contains("campo"));
        assertEquals("/api/teste", modelError.getPath());
    }

    @Test
    @DisplayName("deve tratar varios campos MethodArgumentNotValidException")
    void testMethodArgumentNotValidExceptionWithMultipleFields() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/teste");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(
                new FieldError("objeto", "campo", "mensagem de erro"),
                new FieldError("objeto 2", "campo 2", "mensagem de erro 2")
        ));

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        var response = handler.exceptionPersonalized(exception, request);

        assertEquals(401, response.getStatusCode().value());
        ModelError modelError = response.getBody();
        assertNotNull(modelError);
        assertEquals("Erro de validação", modelError.getError());
        assertTrue(modelError.getMessage().contains("campo"));
        assertTrue(modelError.getMessage().contains("campo 2"));
        assertEquals("/api/teste", modelError.getPath());
    }

    @Test
    @DisplayName("deve tratar ResourceNotFoundException")
    void testResourceNotFoundException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/recurso");

        ResourceException exception = new ResourceException("Recurso não encontrado");

        var response = handler.resourceNotFound(exception, request);

        assertEquals(404, response.getStatusCode().value());
        ModelError modelError = response.getBody();
        assertNotNull(modelError);
        assertEquals("Falha ao processar requisição", modelError.getError());
        assertEquals("Recurso não encontrado", modelError.getMessage());
        assertEquals("/api/recurso", modelError.getPath());
    }
}
