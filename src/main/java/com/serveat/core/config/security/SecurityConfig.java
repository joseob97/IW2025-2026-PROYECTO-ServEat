package com.serveat.core.config.security;

import com.serveat.core.security.CustomAuthenticationFailureHandler;
import com.serveat.core.security.CustomAuthenticationSuccessHandler;
import com.serveat.core.security.EmpleadoUserDetailsService;
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

        http
                // Vaadin usa llamadas internas (UIDL/push) que no incluyen CSRF de Spring Security.
                // Se ignoran SOLO esos endpoints para evitar 403 y mantener CSRF en el resto.
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        new RegexRequestMatcher(".*\\?v-r=uidl.*", null),
                        new AntPathRequestMatcher("/VAADIN/push/**")
                ))

                // RUTAS PÚBLICAS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/carta", "/productos/**", "/detalle/**",
                                "/login", "/VAADIN/**", "/css/**", "/js/**" , "/images/**", "/favicon.ico")
                        .permitAll()

                        // PANEL EMPLEADOS POR ROL
                        .requestMatchers("/empleado/camarero/**").hasRole("CAMARERO")
                        .requestMatchers("/empleado/cocinero/**").hasRole("COCINERO")
                        .requestMatchers("/empleado/repartidor/**").hasRole("REPARTIDOR")
                        .requestMatchers("/empleado/admin/**").hasRole("ADMIN")

                        // CLIENTES
                        .requestMatchers("/cliente/**").hasRole("CLIENTE")

                        // CUALQUIER OTRA COSA → necesita login
                        .anyRequest().authenticated()
                )

                // LOGIN PERSONALIZADO
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)     // redirección por rol
                        .failureHandler(failureHandler)     // mensajes personalizados
                        .permitAll()
                )

                // LOGOUT
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
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
