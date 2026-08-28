package com.skuri.skuri_backend.infra.auth.firebase;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.storage.MediaStorageProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ASYNC_AUTHENTICATION_ATTRIBUTE =
            FirebaseAuthenticationFilter.class.getName() + ".ASYNC_AUTHENTICATION";

    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final ObjectProvider<MemberRepository> memberRepositoryProvider;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final MediaStorageProperties mediaStorageProperties;

    @Value("${security.allowed-email-domain:sungkyul.ac.kr}")
    private String allowedEmailDomain;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.equals("/v1/app-notices") || uri.startsWith("/v1/app-notices/")) {
            return isPublicAppNoticeReadRoute(uri)
                    && !StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION));
        }
        String mediaUrlPrefix = mediaStorageProperties.normalizedUrlPrefix();
        boolean localMediaPublicRoute = mediaStorageProperties.getProvider() == com.skuri.skuri_backend.infra.storage.StorageProviderType.LOCAL
                && (uri.equals(mediaUrlPrefix) || uri.startsWith(mediaUrlPrefix + "/"));
        return uri.startsWith("/v1/legal-documents/")
                || "/v1/legal-documents".equals(uri)
                || uri.startsWith("/v1/campus-banners/")
                || "/v1/campus-banners".equals(uri)
                || uri.startsWith("/v1/app-versions/")
                || isPublicSharePreviewRoute(uri)
                || localMediaPublicRoute;
    }

    private boolean isPublicAppNoticeReadRoute(String uri) {
        return "/v1/app-notices".equals(uri)
                || uri.matches("^/v1/app-notices/[^/]+$");
    }

    private boolean isPublicSharePreviewRoute(String uri) {
        return "/v1/share-links/cafeteria/preview".equals(uri)
                || uri.matches("^/v1/share-links/(notice|board)/[^/]+/preview$");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SSE re-dispatches pass through Spring Security again after the stream starts.
        // Re-authenticate on ASYNC dispatches so AuthorizationFilter sees the same principal.
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication asyncAuthentication = resolveAsyncAuthentication(request);
        if (asyncAuthentication != null) {
            log.debug("Firebase auth cache hit: dispatcherType={}, uri={}", request.getDispatcherType(), request.getRequestURI());
            setAuthentication(asyncAuthentication);
            filterChain.doFilter(request, response);
            return;
        }

        if (isAsyncDispatch(request)) {
            log.debug("Firebase auth cache miss on async dispatch: uri={}", request.getRequestURI());
        }

        String idToken = resolveIdToken(request);
        if (!StringUtils.hasText(idToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FirebaseTokenClaims claims = firebaseTokenVerifier.verify(idToken);
            validateEmailDomain(claims.email());

            AuthenticatedMember principal = AuthenticatedMember.from(claims);
            Member persistedMember = resolvePersistedMember(principal.uid());
            log.debug(
                    "Firebase auth resolved persisted member: uri={}, uid={}, dispatcherType={}, memberPresent={}",
                    request.getRequestURI(),
                    principal.uid(),
                    request.getDispatcherType(),
                    persistedMember != null
            );
            ensureMemberAccessAllowed(request, persistedMember);
            Collection<? extends GrantedAuthority> authorities = resolveAuthorities(persistedMember);
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    null,
                    authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            cacheAsyncAuthentication(request, authentication);
            setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (EmailDomainRestrictedException e) {
            SecurityContextHolder.clearContext();
            accessDeniedHandler.handle(request, response, e);
        } catch (WithdrawnMemberAccessDeniedException e) {
            SecurityContextHolder.clearContext();
            accessDeniedHandler.handle(request, response, e);
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new BadCredentialsException(e.getMessage(), e));
        }
    }

    private String resolveIdToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private Authentication resolveAsyncAuthentication(HttpServletRequest request) {
        if (!isAsyncDispatch(request)) {
            return null;
        }

        Object authentication = request.getAttribute(ASYNC_AUTHENTICATION_ATTRIBUTE);
        if (authentication instanceof Authentication asyncAuthentication) {
            return asyncAuthentication;
        }
        return null;
    }

    private void cacheAsyncAuthentication(HttpServletRequest request, Authentication authentication) {
        request.setAttribute(ASYNC_AUTHENTICATION_ATTRIBUTE, authentication);
    }

    private void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void validateEmailDomain(String email) {
        if (!StringUtils.hasText(email)) {
            throw new EmailDomainRestrictedException();
        }
        String normalizedAllowedDomain = "@" + allowedEmailDomain.toLowerCase(Locale.ROOT);
        if (!email.toLowerCase(Locale.ROOT).endsWith(normalizedAllowedDomain)) {
            throw new EmailDomainRestrictedException();
        }
    }

    private void ensureMemberAccessAllowed(HttpServletRequest request, Member persistedMember) {
        if (isMemberBootstrapRequest(request)) {
            return;
        }

        if (persistedMember != null && persistedMember.isWithdrawn()) {
            throw new WithdrawnMemberAccessDeniedException();
        }
    }

    private boolean isMemberBootstrapRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && "/v1/members".equals(request.getRequestURI());
    }

    private Collection<? extends GrantedAuthority> resolveAuthorities(Member persistedMember) {
        return java.util.Optional.ofNullable(persistedMember)
                .filter(Member::isAdmin)
                .map(value -> List.of(new SimpleGrantedAuthority(ROLE_ADMIN)))
                .orElse(Collections.emptyList());
    }

    private Member resolvePersistedMember(String uid) {
        MemberRepository memberRepository = memberRepositoryProvider.getIfAvailable();
        if (memberRepository == null) {
            return null;
        }
        return memberRepository.findById(uid).orElse(null);
    }
}
