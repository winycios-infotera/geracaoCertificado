package br.com.infotera.gerarcertificado.exception;

import br.com.infotera.gerarcertificado.exception.model.ModelError;
import org.springframework.web.bind.annotation.ControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class CustomExceptionHandler {


    @ExceptionHandler(ResourceException.class)
    public ResponseEntity<ModelError> resourceNotFound(ResourceException e, HttpServletRequest request) {
        String error = "Falha ao processar requisição";
        HttpStatus status = HttpStatus.NOT_FOUND;
        ModelError err = new ModelError(Instant.now(), status.value(), error, e.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }
}