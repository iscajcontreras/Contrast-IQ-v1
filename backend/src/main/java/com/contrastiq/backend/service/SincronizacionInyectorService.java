package com.contrastiq.backend.service;

import com.contrastiq.backend.model.*;
import com.contrastiq.backend.model.enums.EstadoInyeccion;
import com.contrastiq.backend.model.enums.EstadoLote;
import com.contrastiq.backend.model.enums.FuenteLote;
import com.contrastiq.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// "Sincronizacion real con el inyector": segun el manual de EmpowerCTA,
// este equipo NO transmite en tiempo real por DICOM -- la integracion
// real es via exportacion periodica desde la IRiS Workstation (un
// archivo, no un stream). Este servicio implementa exactamente eso: lee
// los archivos exportados de una carpeta configurada, los importa, y
// deja registro en lotes_sincronizacion.
//
// Formato esperado del CSV (una fila = una inyeccion; separador coma):
// numero_serie_inyector,mrn_paciente,fecha_hora_iso,protocolo,agente,volumen_ml,presion_max_psi,estado
@Service
@RequiredArgsConstructor
@Slf4j
public class SincronizacionInyectorService {

    @Value("${app.sincronizacion.habilitada:false}")
    private boolean habilitada;

    @Value("${app.sincronizacion.carpeta:./importaciones-iris}")
    private String carpetaImportacion;

    private final InyectorRepository inyectorRepository;
    private final PacienteRepository pacienteRepository;
    private final ProtocoloInyeccionRepository protocoloRepository;
    private final AgenteContrasteRepository agenteRepository;
    private final InyeccionRepository inyeccionRepository;
    private final LoteSincronizacionRepository loteRepository;

    // Cada 15 minutos, solo si esta habilitada explicitamente (por
    // defecto NO, hasta confirmar el formato real de exportacion de la
    // IRiS Workstation del hospital).
    @Scheduled(fixedDelay = 900_000)
    public void sincronizarProgramado() {
        if (habilitada) {
            sincronizarAhora();
        }
    }

    @Transactional
    public LoteSincronizacionDTOResultado sincronizarAhora() {
        Path carpeta = Paths.get(carpetaImportacion);
        Path procesados = carpeta.resolve("procesados");

        try {
            Files.createDirectories(carpeta);
            Files.createDirectories(procesados);
        } catch (IOException e) {
            return registrarLote(FuenteLote.IRIS_WORKSTATION, 0, EstadoLote.FALLIDO,
                    "No se pudo acceder a la carpeta de importacion: " + e.getMessage());
        }

        int importados = 0;
        int fallidos = 0;
        StringBuilder detalle = new StringBuilder();

        try (Stream<Path> archivos = Files.list(carpeta)) {
            List<Path> csv = archivos.filter(p -> p.toString().toLowerCase().endsWith(".csv")).toList();

            for (Path archivo : csv) {
                try {
                    int filas = procesarArchivo(archivo);
                    importados += filas;
                    detalle.append(archivo.getFileName()).append(": ").append(filas).append(" registros. ");
                    Files.move(archivo, procesados.resolve(archivo.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    fallidos++;
                    detalle.append(archivo.getFileName()).append(": ERROR (" + e.getMessage() + "). ");
                    log.warn("Fallo al importar {}", archivo, e);
                }
            }
        } catch (IOException e) {
            return registrarLote(FuenteLote.IRIS_WORKSTATION, importados, EstadoLote.FALLIDO,
                    "Error leyendo la carpeta: " + e.getMessage());
        }

        EstadoLote estado = fallidos == 0
                ? (importados > 0 ? EstadoLote.EXITOSO : EstadoLote.EXITOSO)
                : (importados > 0 ? EstadoLote.PARCIAL : EstadoLote.FALLIDO);

        if (detalle.isEmpty()) {
            detalle.append("No se encontraron archivos nuevos para importar en ").append(carpeta);
        }

        LoteSincronizacion lote = guardarLote(FuenteLote.IRIS_WORKSTATION, importados, estado, detalle.toString());
        return new LoteSincronizacionDTOResultado(lote, fallidos);
    }

    private int procesarArchivo(Path archivo) throws IOException {
        List<String> lineas = Files.readAllLines(archivo);
        int filas = 0;

        for (int i = 1; i < lineas.size(); i++) { // linea 0 = encabezado
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) continue;

            String[] campos = linea.split(",");
            if (campos.length < 8) continue;

            String numeroSerie = campos[0].trim();
            String mrnPaciente = campos[1].trim();
            LocalDateTime fecha = LocalDateTime.parse(campos[2].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String nombreProtocolo = campos[3].trim();
            String nombreAgente = campos[4].trim();
            BigDecimal volumen = new BigDecimal(campos[5].trim());
            BigDecimal presionMax = new BigDecimal(campos[6].trim());
            String estadoTexto = campos[7].trim();

            Inyector inyector = inyectorRepository.findAll().stream()
                    .filter(inv -> inv.getNumeroSerie().equalsIgnoreCase(numeroSerie))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Inyector no encontrado: " + numeroSerie));

            Paciente paciente = pacienteRepository.findByIdentificadorExterno(mrnPaciente)
                    .orElseGet(() -> pacienteRepository.save(
                            Paciente.builder().identificadorExterno(mrnPaciente).build()));

            Optional<ProtocoloInyeccion> protocolo = protocoloRepository.findAll().stream()
                    .filter(p -> p.getNombre().equalsIgnoreCase(nombreProtocolo))
                    .findFirst();
            Optional<AgenteContraste> agente = agenteRepository.findAll().stream()
                    .filter(a -> a.getNombreComercial().equalsIgnoreCase(nombreAgente))
                    .findFirst();

            Inyeccion inyeccion = Inyeccion.builder()
                    .inyector(inyector)
                    .protocolo(protocolo.orElse(null))
                    .paciente(paciente)
                    .fechaHoraInicio(fecha)
                    .volumenTotalMl(volumen)
                    .presionMaximaPsi(presionMax)
                    .estado(EstadoInyeccion.valueOf(estadoTexto))
                    .edaHabilitado(true)
                    .build();

            InyeccionFase fase = InyeccionFase.builder()
                    .inyeccion(inyeccion)
                    .numeroFase((short) 1)
                    .agente(agente.orElse(null))
                    .volumenProgramadoMl(volumen)
                    .volumenRealMl(volumen)
                    .esFaseContraste(true)
                    .build();
            inyeccion.getFases().add(fase);

            inyeccionRepository.save(inyeccion);
            filas++;
        }

        return filas;
    }

    private LoteSincronizacionDTOResultado registrarLote(FuenteLote fuente, int importados, EstadoLote estado, String detalle) {
        return new LoteSincronizacionDTOResultado(guardarLote(fuente, importados, estado, detalle), 0);
    }

    private LoteSincronizacion guardarLote(FuenteLote fuente, int importados, EstadoLote estado, String detalle) {
        LoteSincronizacion lote = LoteSincronizacion.builder()
                .fuente(fuente)
                .fechaHora(LocalDateTime.now())
                .registrosImportados(importados)
                .estado(estado)
                .detalle(detalle)
                .build();
        return loteRepository.save(lote);
    }

    // Envoltura simple para devolver el lote + cuantos archivos fallaron
    public record LoteSincronizacionDTOResultado(LoteSincronizacion lote, int archivosFallidos) {}
}
