package by.sirius.task.tracker.api.exceptions;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@Log4j2
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomBaseException.class)
    public ResponseEntity<ErrorDto> handleCustomBaseException(CustomBaseException ex) {

        log.error("Application Exception: ", ex);

        ErrorDto errorDto = ErrorDto.builder()
                .error(ex.getStatus().getReasonPhrase())
                .errorDescription(ex.getMessage())
                .build();

        return ResponseEntity
                .status(ex.getStatus())
                .body(errorDto);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorDto errorDto = ErrorDto.builder()
                .error("Validation Error")
                .errorDescription(errors)
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }
}
