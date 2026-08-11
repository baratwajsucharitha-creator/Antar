package com.antar.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Lets any controller method declare a CurrentUser parameter and receive the
 * authenticated caller, without touching HttpServletRequest itself.
 *
 * Easy Auth injects these headers after it has validated the token:
 *   X-MS-CLIENT-PRINCIPAL-ID    stable subject id
 *   X-MS-CLIENT-PRINCIPAL-NAME  display name or email
 *   X-MS-CLIENT-PRINCIPAL-IDP   which provider authenticated
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    static final String HEADER_ID = "X-MS-CLIENT-PRINCIPAL-ID";
    static final String HEADER_NAME = "X-MS-CLIENT-PRINCIPAL-NAME";
    static final String HEADER_IDP = "X-MS-CLIENT-PRINCIPAL-IDP";

    /**
     * True in the azure profile. When true, a request without a platform principal
     * is rejected rather than silently falling back to a shared local identity -
     * a fallback that reached production would hand every visitor the same account.
     */
    private final boolean requirePlatformPrincipal;

    public CurrentUserArgumentResolver(
            @Value("${antar.auth.require-platform-principal:false}") boolean requirePlatformPrincipal) {
        this.requirePlatformPrincipal = requirePlatformPrincipal;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String id = request == null ? null : request.getHeader(HEADER_ID);

        if (id == null || id.isBlank()) {
            if (requirePlatformPrincipal) {
                throw new UnauthenticatedException();
            }
            return CurrentUser.LOCAL_DEVELOPER;
        }

        return new CurrentUser(
                id,
                request.getHeader(HEADER_NAME),
                request.getHeader(HEADER_IDP));
    }
}
