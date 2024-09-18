package by.sirius.task.tracker.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BadRequestException extends CustomBaseException {
    public BadRequestException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
