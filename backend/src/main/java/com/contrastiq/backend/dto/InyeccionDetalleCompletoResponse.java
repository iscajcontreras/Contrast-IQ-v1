package com.contrastiq.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Respuesta de GET /api/inyecciones/{id}/detalle-completo: todo lo
// necesario para la pantalla de detalle de una inyeccion -- demograficos
// del paciente, metadatos clinicos/operativos, agente/lote, las dos
// graficas (presion y flujo) y el comparativo de fases Planeado /
// Programado / Real. Datos ya aplanados, igual que InyeccionResumenDTO.
@Getter
@Builder
public class InyeccionDetalleCompletoResponse {

    private Long inyeccionId;

    private PacienteInfoDTO paciente;
    private ContrasteInfoDTO contraste;
    private MetadatosInyeccionDTO metadatos;
    private List<PuntoPresionDTO> seriePresion;
    private List<PuntoFlujoDTO> serieFlujo;
    private List<ComparativoFaseDTO> comparativoFases;

    @Getter
    @Builder
    public static class PacienteInfoDTO {
        private Long id;
        private String identificadorExterno;
        private String nombreCompleto;
        private String numeroExpediente;
        private String sexo;
        private LocalDate fechaNacimiento;
        private BigDecimal pesoKg;
        private BigDecimal tallaM;
        private String grupoEtnico;
        private BigDecimal gfrMlMin;
        private BigDecimal creatininaMgDl;
        private String alergias;
    }

    @Getter
    @Builder
    public static class ContrasteInfoDTO {
        private String agentePrincipal;
        private String concentracion;
        private String fabricante;
        private String numeroLote;
        private LocalDate loteFechaCaducidad;
        private BigDecimal dosisContrasteGl;
        private BigDecimal volumenTotalMl;
    }

    @Getter
    @Builder
    public static class MetadatosInyeccionDTO {
        private LocalDateTime fechaHoraInicio;
        private LocalDateTime fechaHoraFin;
        private Integer duracionSeg;
        private String sede;
        private String sala;
        private String inyector;
        private String operador;
        private String protocolo;
        private String identificadorAnatomico;
        private String estado;
        private String numeroAccesion;
        private String procedimientoProgramado;
        private String calibreAguja;
        private String accesoAguja;
        private BigDecimal avanceSalinaMl;
        private Boolean salinaJumpUsado;
        private String scanner;
        private String notas;
        private Integer retrasoEscaneoSeg;
        private BigDecimal presionMaximaPsi;
        private BigDecimal presionPromedioPsi;
        private BigDecimal presionLimitePsi;
        private Boolean edaHabilitado;
        private BigDecimal ctdiVolMgy;
        private BigDecimal dlpMgyCm;
    }

    // Un punto de la grafica de flujo (contraste + salina) vs. tiempo.
    @Getter
    @Builder
    public static class PuntoFlujoDTO {
        private BigDecimal tiempoSeg;
        private BigDecimal flujoContrasteMlS;
        private BigDecimal flujoSalinaMlS;
    }

    // Comparativo de una fase, unido por numero/orden de fase: lo
    // planeado en el protocolo, lo programado en el inyector y lo
    // realmente ejecutado. Cualquiera de los tres puede venir nulo si
    // esa fase no tiene registro en la tabla correspondiente.
    @Getter
    @Builder
    public static class ComparativoFaseDTO {
        private Integer numeroFase;

        private String planeadoAgente;
        private BigDecimal planeadoVolumenMl;
        private BigDecimal planeadoVelocidadFlujoMlS;

        private String programadoAgente;
        private BigDecimal programadoVolumenMl;
        private BigDecimal programadoVelocidadFlujoMlS;

        private String realAgente;
        private BigDecimal realVolumenProgramadoMl;
        private BigDecimal realVolumenRealMl;
        private BigDecimal realVelocidadFlujoMlS;
    }
}
