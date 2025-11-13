package com.serveat.config;

import com.serveat.security.CustomAuthenticationSuccessHandler;
import com.serveat.security.EmpleadoUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final EmpleadoUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(EmpleadoUserDetailsService userDetailsService,
                          CustomAuthenticationSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/carta",
                                "/productos/**",
                                "/producto/**",
                                "/detalle/**",
                                "/images/**",
                                "/VAADIN/**",
                                "/login",
                                "/css/**",
                                "/js/**"
                        ).permitAll()

                        .requestMatchers("/empleado/camarero/**").hasRole("CAMARERO")

                        .requestMatchers("/empleado/cocinero/**").hasRole("COCINERO")

                        .requestMatchers("/empleado/repartidor/**").hasRole("REPARTIDOR")

                        .requestMatchers("/empleado/admin/**").hasRole("ADMIN")

                        .requestMatchers("/cliente/**").hasRole("CLIENTE")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
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
