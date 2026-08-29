package com.skuri.skuri_backend.infra.auth.config;

import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.storage.MediaStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({MediaStorageProperties.class, MinecraftBridgeProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    private final MinecraftInternalSecretFilter minecraftInternalSecretFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final MediaStorageProperties mediaStorageProperties;

    @Value("${app.openapi.enabled:true}")
    private boolean openApiEnabled;

    @Value("${app.api.allowed-origin-patterns:}")
    private String[] apiAllowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    String mediaUrlPrefix = mediaStorageProperties.normalizedUrlPrefix();
                    authorize.requestMatchers(SseDisconnectRequestSupport::isDisconnectedSseErrorDispatch).permitAll();
                    authorize.requestMatchers(
                            HttpMethod.GET,
                            "/v1/app-versions/**",
                            "/v1/app-notices",
                            "/v1/app-notices/*",
                            "/v1/legal-documents/**",
                            "/v1/campus-banners/**",
                            "/v1/share-links/notice/*/preview",
                            "/v1/share-links/board/*/preview",
                            "/v1/share-links/cafeteria/preview"
                    ).permitAll();
                    authorize.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
                    if (mediaStorageProperties.getProvider() == com.skuri.skuri_backend.infra.storage.StorageProviderType.LOCAL) {
                        authorize.requestMatchers(HttpMethod.GET, mediaUrlPrefix, mediaUrlPrefix + "/**").permitAll();
                    }
                    authorize.requestMatchers("/ws/**", "/ws-native/**").permitAll();
                    if (openApiEnabled) {
                        authorize.requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/scalar/**"
                        ).permitAll();
                    }
                    authorize.requestMatchers("/internal/minecraft/**").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler)
                )
                .addFilterBefore(minecraftInternalSecretFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        LinkedHashSet<String> allowedOriginPatterns = new LinkedHashSet<>();
        Arrays.stream(apiAllowedOriginPatterns)
                .filter(value -> value != null && !value.isBlank())
                .forEach(allowedOriginPatterns::add);
        allowedOriginPatterns.add("https://link.skuri.kr");
        allowedOriginPatterns.add("https://open.skuri.kr");
        configuration.setAllowedOriginPatterns(List.copyOf(allowedOriginPatterns));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public FilterRegistrationBean<FirebaseAuthenticationFilter> firebaseAuthenticationFilterRegistration(
            FirebaseAuthenticationFilter firebaseAuthenticationFilter
    ) {
        FilterRegistrationBean<FirebaseAuthenticationFilter> registration = new FilterRegistrationBean<>(firebaseAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
