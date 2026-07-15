package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.DatosPacienteHisDTO;
import com.contrastiq.backend.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementacion SIMULADA de la integracion con el HIS (ver la
// interfaz para el porque). Devuelve los datos que YA existen en
// nuestra base para ese paciente -- no inventa informacion clinica --
// pero los marca explicitamente como "simulado" para que nadie los
// confunda con una integracion real ya conectada.
//
// Se activa por defecto (app.his.habilitado=false). Si en el futuro se
// implementa HisIntegracionServiceFhir (u otra), esta clase deja de
// usarse simplemente cambiando esa propiedad y el @Primary/condicional
// correspondiente.
@Service
@RequiredArgsConstructor
public class HisIntegracionServiceSimulado implements HisIntegracionService {

    private final PacienteRepository pacienteRepository;

    @Value("${app.his.habilitado:false}")
    private boolean habilitado;

    @Override
    @Transactional(readOnly = true)
    public DatosPacienteHisDTO buscarPaciente(String identificadorExterno) {
        return pacienteRepository.findByIdentificadorExterno(identificadorExterno)
                .map(p -> DatosPacienteHisDTO.builder()
                        .identificadorExterno(p.getIdentificadorExterno())
                        .nombreCompleto(p.getNombreCompleto())
                        .sexo(p.getSexo() != null ? p.getSexo().name() : null)
                        .pesoKg(p.getPesoKg())
                        .alergias(p.getAlergias())
                        .simulado(!habilitado)
                        .fuente(habilitado ? "HIS" : "SIMULADO (paciente ya existente en el sistema local)")
                        .build())
                .orElse(null);
    }
}
