package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.DatosPacienteHisDTO;

// "Integracion clinica: traer datos del paciente desde el HIS".
//
// Esta interfaz es el contrato real que usaria el resto de la
// aplicacion. Mientras no se confirme con el hospital cual es el HIS
// especifico y su forma de integracion (HL7 v2, FHIR, base de datos
// compartida, etc.), se usa HisIntegracionServiceSimulado como
// implementacion por defecto -- una vez confirmado el protocolo real,
// basta con escribir una nueva clase que implemente esta interfaz
// (por ejemplo HisIntegracionServiceFhir) y activarla con
// app.his.habilitado=true en application.properties, sin tocar el
// resto del sistema (controllers, frontend, etc. no cambian).
public interface HisIntegracionService {

    // Busca un paciente en el HIS por su identificador externo (MRN).
    // Devuelve null si no se encuentra.
    DatosPacienteHisDTO buscarPaciente(String identificadorExterno);
}
