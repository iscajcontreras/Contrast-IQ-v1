package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.DatosPacienteHisDTO;
import com.contrastiq.backend.dto.LoteSincronizacionDTO;
import com.contrastiq.backend.service.HisIntegracionService;
import com.contrastiq.backend.service.IntegracionClinicaService;
import com.contrastiq.backend.service.SincronizacionInyectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracion-clinica")
@RequiredArgsConstructor
public class IntegracionClinicaController {

    private final HisIntegracionService hisIntegracionService;
    private final SincronizacionInyectorService sincronizacionInyectorService;
    private final IntegracionClinicaService integracionClinicaService;

    // "Traer datos del paciente desde el HIS" -- vista previa, sin guardar
    @GetMapping("/his/{identificadorExterno}")
    public ResponseEntity<DatosPacienteHisDTO> buscarEnHis(@PathVariable String identificadorExterno) {
        DatosPacienteHisDTO datos = hisIntegracionService.buscarPaciente(identificadorExterno);
        return datos != null ? ResponseEntity.ok(datos) : ResponseEntity.notFound().build();
    }

    // Trae del HIS y actualiza (o crea) el paciente local con esos datos
    @PostMapping("/his/{identificadorExterno}/sincronizar")
    public ResponseEntity<DatosPacienteHisDTO> sincronizarDesdeHis(@PathVariable String identificadorExterno) {
        DatosPacienteHisDTO datos = integracionClinicaService.sincronizarPacienteDesdeHis(identificadorExterno);
        return datos != null ? ResponseEntity.ok(datos) : ResponseEntity.notFound().build();
    }

    // "Sincronizacion real con el inyector" -- disparar manualmente
    // (ademas del job programado cada 15 min si esta habilitada)
    @PostMapping("/sincronizar-inyector")
    public LoteSincronizacionDTO sincronizarInyector() {
        var resultado = sincronizacionInyectorService.sincronizarAhora();
        return integracionClinicaService.aDto(resultado.lote());
    }

    @GetMapping("/historial-sincronizacion")
    public Page<LoteSincronizacionDTO> historialSincronizacion(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return integracionClinicaService.historialSincronizacion(pageable);
    }
}
