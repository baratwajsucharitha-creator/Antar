package com.antar.api.service;

public class PolicyNotFoundException extends RuntimeException {
    public PolicyNotFoundException(Long id) {
        super("No policy found with id " + id);
    }
}
