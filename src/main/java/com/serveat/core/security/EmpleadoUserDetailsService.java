package com.serveat.core.security;

import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;

@Service
public class EmpleadoUserDetailsService implements UserDetailsService {

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public EmpleadoUserDetailsService(EmpleadoRepository empleadoRepository,
                                      ClienteRepository clienteRepository) {
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
    }

    public PasswordEncoder passwordEncoder() {
        return encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        //Buscar primero EMPLEADO
        Empleado empleado = empleadoRepository.findByUsername(username).orElse(null);
        if (empleado != null) {
            if (!empleado.isEnabled()) {
                // 👇 Esta excepción es la que ahora detecta el FailureHandler
                throw new DisabledException("El usuario está desactivado");
            }

            return User.withUsername(empleado.getUsername())
                    .password(empleado.getPassword())
                    .roles(empleado.getRol().toUpperCase())
                    .build();
        }

        //Buscar CLIENTE si no era empleado
        Cliente cliente = clienteRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.withUsername(cliente.getUsername())
                .password(cliente.getPassword())
                .roles("CLIENTE")
                .build();
    }
}
