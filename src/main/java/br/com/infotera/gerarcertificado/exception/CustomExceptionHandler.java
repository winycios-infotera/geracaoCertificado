package br.com.infotera.gerarcertificado.exception;

import br.com.infotera.gerarcertificado.exception.model.ModelError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class CustomExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ModelError> exceptionPersonalized(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);

        });
        ModelError err = new ModelError(Instant.now(), 401, "Erro de validação", errors.toString(),
                request.getRequestURI());
        return ResponseEntity.status(401).body(err);
    }

    @ExceptionHandler(ResourceException.class)
    public ResponseEntity<ModelError> resourceNotFound(ResourceException e, HttpServletRequest request) {
        String error = "Falha ao processar requisição";
        HttpStatus status = HttpStatus.NOT_FOUND;
        ModelError err = new ModelError(Instant.now(), status.value(), error, e.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }
}