package com.app.confeitaria.docelivery.exceptions;

import com.app.confeitaria.docelivery.model.entity.Confeiteiro;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
@Getter
@Setter
public class ErrorMessage {

    private LocalDateTime timestamp;
    private String[] message;
    private HttpStatus title;
    private int status;

    public ErrorMessage(LocalDateTime timestamp, String[] message, HttpStatus title) {
        this.timestamp = timestamp;
        this.message = message;
        this.title = title;
        this.status = title.value();
    }
}

