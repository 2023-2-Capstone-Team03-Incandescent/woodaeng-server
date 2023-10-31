package com.incandescent.woodaengserver.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CustomException extends Exception {
    private ResponseStatus status;
}