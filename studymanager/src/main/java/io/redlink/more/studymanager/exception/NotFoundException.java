package io.redlink.more.studymanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String type, Object id) {
        super(String.format("%s with id %s cannot be found", type, id.toString()));
    }
    public static NotFoundException Study(long id) {
        return new NotFoundException("Study", id);
    }
}
