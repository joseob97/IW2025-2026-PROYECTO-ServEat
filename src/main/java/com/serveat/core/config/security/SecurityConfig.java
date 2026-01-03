package com.serveat.core.config.security;

import com.serveat.core.security.CustomAuthenticationFailureHandler;
import com.serveat.core.security.CustomAuthenticationSuccessHandler;
import com.serveat.core.security.EmpleadoUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
public class SecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityConfig.class);

    private final EmpleadoUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;

    @Autowired
    public SecurityConfig(EmpleadoUserDetailsService userDetailsService,
                          CustomAuthenticationSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Inicializando configuración de seguridad");

        http
                // Vaadin UIDL / Push → sin CSRF
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        new RegexRequestMatcher(".*\\?v-r=uidl.*", null),
                        new AntPathRequestMatcher("/VAADIN/push/**"),
                        new AntPathRequestMatcher("/VAADIN/dynamic/resource/**"),
                        new AntPathRequestMatcher("/VAADIN/dynamic/**")
                ))

                // RUTAS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/carta", "/productos/**", "/detalle/**",
                                "/login", "/VAADIN/**", "/css/**", "/js/**",
                                "/images/**", "/favicon.ico"
                        ).permitAll()

                        .requestMatchers("/empleado/camarero/**").hasRole("CAMARERO")
                        .requestMatchers("/empleado/cocinero/**").hasRole("COCINERO")
                        .requestMatchers("/empleado/repartidor/**").hasRole("REPARTIDOR")
                        .requestMatchers("/empleado/admin/**").hasRole("ADMIN")

                        .requestMatchers("/cliente/**").hasRole("CLIENTE")

                        .anyRequest().authenticated()
                )

                // LOGIN
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )

                // LOGOUT
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessHandler((request, response, authentication) -> {

                            if (authentication != null) {
                                log.info(
                                        "LOGOUT | usuario='{}'",
                                        authentication.getName()
                                );
                            } else {
                                log.info("LOGOUT | usuario anónimo");
                            }

                            response.sendRedirect("/?logout");
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        http.userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return userDetailsService.passwordEncoder();
    }
}
