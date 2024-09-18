package by.sirius.task.tracker.api.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomBaseException extends RuntimeException {

    private final HttpStatus status;

    public CustomBaseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
