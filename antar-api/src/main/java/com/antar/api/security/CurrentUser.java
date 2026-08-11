package com.antar.api.security;

/**
 * The authenticated caller, as established by Azure App Service Easy Auth.
 *
 * The application performs no token validation of its own. App Service intercepts
 * every request before it reaches this process, completes the OIDC flow, and injects
 * the validated principal as request headers. There is no auth library on the
 * classpath and no client secret in the application.
 *
 * @param id    stable subject identifier - the ownership key, never the display name
 * @param name  display name or email, for the UI only
 * @param idp   which identity provider authenticated them
 */
public record CurrentUser(String id, String name, String idp) {

    /** Used only when platform authentication is not enforced (local development and tests). */
    public static final CurrentUser LOCAL_DEVELOPER =
            new CurrentUser("local-dev-user", "Local developer", "none");

    public CurrentUser {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("a principal must have a stable id");
        }
        if (name == null || name.isBlank()) {
            name = id;
        }
    }
}
