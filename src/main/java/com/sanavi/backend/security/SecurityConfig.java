package com.sanavi.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth

                        // ── 인증 불필요 (공개 엔드포인트) ──────────────────────────────
                        // .requestMatchers(HttpMethod.POST, "/api/members/login").permitAll()
                        // .requestMatchers(HttpMethod.POST, "/api/members/signup").permitAll()
                        // .requestMatchers(HttpMethod.POST, "/api/members/refresh").permitAll()
                        // .requestMatchers(HttpMethod.GET,  "/api/members/check-id").permitAll()
                        // .requestMatchers(HttpMethod.POST, "/api/members/email/**").permitAll()
                        // .requestMatchers(HttpMethod.GET,  "/api/matches").permitAll()
                        // .requestMatchers(HttpMethod.GET,  "/api/matches/{matchId}").permitAll()
                        // .requestMatchers(HttpMethod.GET,  "/api/requestlist/lawyers/**").permitAll()
                        // .requestMatchers("/actuator/**").permitAll()

                        // ── 로그인 사용자만 (role 무관) ────────────────────────────────
                        // .requestMatchers(HttpMethod.POST,   "/api/members/logout").authenticated()
                        // .requestMatchers(HttpMethod.POST,   "/api/analysis").authenticated()
                        // .requestMatchers(HttpMethod.DELETE, "/api/analysis/{taskId}").authenticated()
                        // .requestMatchers(HttpMethod.GET,    "/api/analysis/history").authenticated()
                        // .requestMatchers(HttpMethod.POST,   "/api/matches").authenticated()
                        // .requestMatchers(HttpMethod.DELETE, "/api/matches/{matchId}").authenticated()
                        // .requestMatchers(HttpMethod.PATCH,  "/api/matches/{matchId}/status").authenticated()
                        // .requestMatchers(HttpMethod.GET,    "/api/members/me").authenticated()
                        // .requestMatchers(HttpMethod.PATCH,  "/api/members/me").authenticated()
                        // .requestMatchers(HttpMethod.GET,    "/api/subscribe").authenticated()

                        // ── role_lawyer 전용 ───────────────────────────────────────────
                        // .requestMatchers(HttpMethod.POST,  "/api/matches/{matchId}/bids").hasAuthority("role_lawyer")
                        // .requestMatchers(HttpMethod.GET,   "/api/requestlist/requests/received").hasAuthority("role_lawyer")
                        // .requestMatchers(HttpMethod.PATCH, "/api/requestlist/requests/{matchId}/accept").hasAuthority("role_lawyer")
                        // .requestMatchers(HttpMethod.PATCH, "/api/requestlist/requests/{matchId}/reject").hasAuthority("role_lawyer")

                        // ── role_user 전용 ─────────────────────────────────────────────
                        // .requestMatchers(HttpMethod.POST,  "/api/requestlist/requests").hasAuthority("role_user")
                        // .requestMatchers(HttpMethod.GET,   "/api/requestlist/requests/sent").hasAuthority("role_user")
                        // .requestMatchers(HttpMethod.PATCH, "/api/subscribe/activate").hasAuthority("role_user")

                        // ── role_admin 전용 ────────────────────────────────────────────
                        // .requestMatchers("/api/admin/**").hasAuthority("role_admin")

                        // JWT 필터 검증 완료 후 위 주석 해제하고 아래 제거
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
