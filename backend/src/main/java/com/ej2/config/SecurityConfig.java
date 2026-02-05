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
                .antMatchers(OPTIONS, "/**").permitAll() // cors用 option 許可
                .antMatchers("/", "/resources/**", "/api/auth/**").permitAll()  // 基本経路

                // 公開API (認証不要)
                .antMatchers(GET, "/api/posts/**").permitAll()  // 投稿閲覧
                .antMatchers(GET, "/api/comments/**").permitAll()  // コメント閲覧
                .antMatchers(GET, "/api/boards/**").permitAll()  // 掲示板一覧
                .antMatchers(GET, "/api/users").permitAll()  // ユーザー一覧
                .antMatchers(GET, "/api/chat/**").permitAll()  // チャット閲覧
                .antMatchers("/api/chat/rooms/*/nickname").permitAll()  // チャットニックネーム取得
                .antMatchers("/ws/**").permitAll()  // WebSocket

                // 認証必要API - 投稿関連
                .antMatchers(POST, "/api/posts").authenticated()  // 投稿作成
                .antMatchers(POST, "/api/posts/*/view").permitAll()  // 閲覧数（認証不要）
                .antMatchers(POST, "/api/posts/*/like").authenticated()  // いいね
                .antMatchers(POST, "/api/posts/*/dislike").authenticated()  // 嫌い
                .antMatchers(PUT, "/api/posts/**").authenticated()  // 投稿修正
                .antMatchers(DELETE, "/api/posts/**").authenticated()  // 投稿削除

                // 認証必要API - コメント関連
                .antMatchers(POST, "/api/comments").authenticated()  // コメント作成
                .antMatchers(POST, "/api/comments/*/like").authenticated()  // コメントいいね
                .antMatchers(PUT, "/api/comments/**").authenticated()  // コメント修正
                .antMatchers(DELETE, "/api/comments/**").authenticated()  // コメント削除

                // 認証必要API - 通報
                .antMatchers(POST, "/api/reports").authenticated()  // 通報作成

                // 管理者専用API
                .antMatchers("/api/admin/**").authenticated()  // 管理者機能（追加でADMINロール検証が必要）

                .anyRequest().permitAll()  // 残りはフリー（開発用）
            )
            .csrf(csrf -> csrf.disable());  // 開発時のみ

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
