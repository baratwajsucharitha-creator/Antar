package com.antar.api.security;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("No authenticated principal on this request");
    }
}
