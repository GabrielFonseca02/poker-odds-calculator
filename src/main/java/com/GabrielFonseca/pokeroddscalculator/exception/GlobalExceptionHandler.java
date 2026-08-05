package com.GabrielFonseca.pokeroddscalculator.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.GabrielFonseca.pokeroddscalculator.dto.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {

        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ApiError error = new ApiError(400, "Dados inválidos na requisição", details, LocalDateTime.now());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleDomainErrors(RuntimeException ex) {

        ApiError error = new ApiError(400, ex.getMessage(), List.of(), LocalDateTime.now());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleResourceNotFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {

        logger.error("Erro inesperado", ex);

        ApiError error = new ApiError(500, "Erro interno inesperado", List.of(), LocalDateTime.now());

        return ResponseEntity.internalServerError().body(error);
    }
}