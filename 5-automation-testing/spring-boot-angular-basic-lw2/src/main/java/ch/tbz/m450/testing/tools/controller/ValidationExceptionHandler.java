package ch.tbz.m450.testing.tools.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uebersetzt fehlgeschlagene Bean-Validation in eine maschinenlesbare Antwort.
 *
 * Ohne diesen Handler antwortet Spring zwar auch mit 400, aber mit einem
 * generischen Body - das Frontend koennte die Meldung dann nicht dem richtigen
 * Eingabefeld zuordnen.
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            // Bei mehreren Regeln pro Feld gewinnt die erste - eine Meldung
            // pro Feld reicht dem Formular.
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validierungsfehler");
        body.put("fields", fields);

        return ResponseEntity.badRequest().body(body);
    }
}
