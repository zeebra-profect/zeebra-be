package com.zeebra.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.zeebra.global.security.jwt.AuthProblemHandler;
import com.zeebra.global.security.jwt.JwtFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("local")
public class SecurityConfigLocal {

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

		// 임시로 모든 api 열어둠
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(SWAGGER_WHITELIST).permitAll()
				// .requestMatchers("/api/auth/**", "/api/products", "/api/products/**").permitAll()
				// 임시로 모든 api 허용
				.requestMatchers("/api/**").permitAll()
				.anyRequest().authenticated()
			)
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(authProblemHandler)
				.accessDeniedHandler(authProblemHandler))
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

}