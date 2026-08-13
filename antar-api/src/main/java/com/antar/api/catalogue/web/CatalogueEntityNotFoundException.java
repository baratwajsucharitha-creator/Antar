package com.antar.api.catalogue.web;

/** A catalogue lookup (insurer or product) by id found nothing. Reference data has no owner to check. */
public class CatalogueEntityNotFoundException extends RuntimeException {

    public CatalogueEntityNotFoundException(String entityName, Long id) {
        super("No " + entityName + " found with id " + id);
    }
}
