package com.erp.main.config;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FxSecurityErrorHandler securityErrorHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler));
        return http.build();
    }
}
