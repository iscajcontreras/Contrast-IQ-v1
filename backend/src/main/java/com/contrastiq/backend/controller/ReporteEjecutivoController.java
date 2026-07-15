package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.ComparativaSedeDTO;
import com.contrastiq.backend.service.ReporteEjecutivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteEjecutivoController {

    private final ReporteEjecutivoService service;

    @GetMapping("/comparativa-sedes")
    public List<ComparativaSedeDTO> comparativaSedes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return service.comparativaSedes(desde, hasta);
    }

    @GetMapping("/comparativa-sedes/excel")
    public ResponseEntity<byte[]> comparativaSedesExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        byte[] archivo = service.exportarComparativaSedesExcel(desde, hasta);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("comparativa_sedes.xlsx").build().toString())
                .body(archivo);
    }
}
