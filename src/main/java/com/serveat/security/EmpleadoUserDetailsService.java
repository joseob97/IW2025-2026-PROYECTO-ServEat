package com.serveat.security;

import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.ClienteRepository;
import com.serveat.repository.EmpleadoRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmpleadoUserDetailsService implements UserDetailsService {

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public EmpleadoUserDetailsService(EmpleadoRepository empleadoRepository,
                                      ClienteRepository clienteRepository) {
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Empleado empleado = empleadoRepository.findByUsername(username).orElse(null);
        if (empleado != null) {
            String rol = empleado.getRol().toUpperCase();

            return User.withUsername(empleado.getUsername())
                    .password(empleado.getPassword())
                    .roles(rol)
                    .build();
        }

        Cliente cliente = clienteRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String rolCliente = (cliente.getRol() == null ? "CLIENTE" : cliente.getRol().toUpperCase());

        return User.withUsername(cliente.getUsername())
                .password(cliente.getPassword())
                .roles(rolCliente)
                .build();
    }
}
