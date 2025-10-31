package com.zeebra.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.zeebra.global.security.jwt.AuthProblemHandler;
import com.zeebra.global.security.jwt.JwtFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("!local")
public class SecurityConfig {

	private static final String[] SWAGGER_WHITELIST = {
		"/swagger-ui.html",
		"/swagger-ui/**",
		"/v3/api-docs",
		"/v3/api-docs/**"
	};

	private final JwtFilter jwtFilter;
	private final AuthProblemHandler authProblemHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
			.headers(headers -> headers
				.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' "))
				.frameOptions(frameOptions -> frameOptions.deny()))
			.cors(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(SWAGGER_WHITELIST).permitAll()
				.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
				.requestMatchers("/api/auth/**").permitAll()
				.anyRequest().authenticated())
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/api/auth/**", "/api/products", "/api/products/**")
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(authProblemHandler)
				.accessDeniedHandler(authProblemHandler))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

}