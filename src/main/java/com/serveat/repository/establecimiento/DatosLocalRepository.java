package com.serveat.repository.establecimiento;

import com.serveat.domain.establecimiento.DatosLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatosLocalRepository extends JpaRepository<DatosLocal, Long> {
}
