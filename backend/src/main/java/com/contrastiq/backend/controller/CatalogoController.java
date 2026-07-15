package com.contrastiq.backend.controller;

import com.contrastiq.backend.dto.OpcionFiltroDTO;
import com.contrastiq.backend.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Un endpoint por cada <select> de la barra de filtros del dashboard.
// Angular llama estos al montar el componente para poblar los combos.
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/roles")
    public List<OpcionFiltroDTO> roles() {
        return catalogoService.roles();
    }
    @GetMapping("/sedes")
    public List<OpcionFiltroDTO> sedes() {
        return catalogoService.sedes();
    }

    @GetMapping("/salas")
    public List<OpcionFiltroDTO> salas(@RequestParam(required = false) Long sedeId) {
        return catalogoService.salasPorSede(sedeId);
    }

    @GetMapping("/inyectores")
    public List<OpcionFiltroDTO> inyectores(@RequestParam(required = false) Long salaId) {
        return catalogoService.inyectoresPorSala(salaId);
    }

    @GetMapping("/protocolos")
    public List<OpcionFiltroDTO> protocolos(
            @RequestParam(required = false) Long identificadorAnatomicoId) {
        return catalogoService.protocolos(identificadorAnatomicoId);
    }

    @GetMapping("/agentes-contraste")
    public List<OpcionFiltroDTO> agentesContraste() {
        return catalogoService.agentesContraste();
    }

    @GetMapping("/identificadores-anatomicos")
    public List<OpcionFiltroDTO> identificadoresAnatomicos() {
        return catalogoService.identificadoresAnatomicos();
    }
}
