package com.serveat.service.usuario.impl;

import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.usuario.EmpleadoService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public EmpleadoServiceImpl(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    public List<Empleado> findAll() {
        return empleadoRepository.findAll();
    }

    @Override
    public Optional<Empleado> findById(Long id) {
        return empleadoRepository.findById(id);
    }

    @Override
    public Empleado save(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }

    @Override
    public void delete(Empleado empleado) {
        empleadoRepository.delete(empleado);
    }

    @Override
    public void updatePassword(Empleado empleado, String rawPassword) {
        empleado.setPassword(passwordEncoder.encode(rawPassword));
        empleadoRepository.save(empleado);
    }
}