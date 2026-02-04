package com.ej2.config;

import static org.springframework.http.HttpMethod.*;

import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("시큐리티 설정 로드됨!");

        http
            .cors() // cors 허용...
            .and()
            .authorizeHttpRequests(authz -> authz
                .antMatchers(OPTIONS, "/**").permitAll() // cors용 option 허용
                .antMatchers("/", "/resources/**", "/api/auth/**").permitAll()  // 기본경로
                //.antMatchers(GET, "/api/posts/**").permitAll()  // 로긴필요없는 api (get)
                .antMatchers(POST, "/api/posts/**").authenticated()  // 로긴필요한 api (post)
                //.antMatchers(PUT, "").authenticated() // 로긴필요 api (put)
                //.antMatchers(DELETE, "").authenticated() // 로긴필요 api (delete)
                // 로긴필요한 get방식 api가 있나? 잇다면 추가...
                .anyRequest().permitAll()  // 나머진 프리
            )
            .csrf(csrf -> csrf.disable());  // 개발할때만?

        http.exceptionHandling(handler -> handler
            .authenticationEntryPoint((request, response, authException) -> {
                response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            })
        );
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
