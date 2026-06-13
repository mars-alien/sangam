package com.gatherup.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, String resourceType, UUID resourceId) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, UUID resourceId) {
        super(resourceType + " not found with id: " + resourceId);
    }
}
