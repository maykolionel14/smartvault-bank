package co.smartvault.platform;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String,String>> business(BusinessException ex) {
        return ResponseEntity.status(ex.httpStatus()).body(Map.of("detail", ex.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.unprocessableEntity().body(Map.of("detail",ex.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> conflict(IllegalStateException ex) {
        return ResponseEntity.status(409).body(Map.of("detail",ex.getMessage()));
    }
    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String,String>> missing(java.util.NoSuchElementException ex) {
        return ResponseEntity.status(404).body(Map.of("detail",ex.getMessage()));
    }
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String,String>> denied(SecurityException ex) {
        return ResponseEntity.status(401).body(Map.of("detail",ex.getMessage()));
    }
    @ExceptionHandler({
        MethodArgumentNotValidException.class, HttpMessageNotReadableException.class
    }
    )
    public ResponseEntity<Map<String,String>> request(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("detail", "Solicitud inválida"));
    }
}
