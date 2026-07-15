package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Inyeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InyeccionRepository
        extends JpaRepository<Inyeccion, Long>, JpaSpecificationExecutor<Inyeccion> {

    // KPI: conteo de inyecciones en un rango de fechas (usa el mismo predicado
    // que la especificacion, pero como conteo directo para el KPI de portada)
    long countByFechaHoraInicioBetween(LocalDateTime desde, LocalDateTime hasta);

    @Query("select coalesce(sum(i.volumenTotalMl), 0) from Inyeccion i " +
           "where i.fechaHoraInicio between :desde and :hasta")
    BigDecimal sumarVolumenEntreFechas(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

    // Serie diaria de volumen de contraste para el grafico "Uso de agente de contraste"
    @Query("select function('date', i.fechaHoraInicio) as dia, " +
           "coalesce(sum(i.volumenTotalMl), 0) as volumen, count(i) as total " +
           "from Inyeccion i " +
           "where i.fechaHoraInicio between :desde and :hasta " +
           "group by function('date', i.fechaHoraInicio) " +
           "order by function('date', i.fechaHoraInicio)")
    List<Object[]> volumenDiarioEntreFechas(@Param("desde") LocalDateTime desde,
                                             @Param("hasta") LocalDateTime hasta);

    // "Mantenimiento predictivo": ciclos de uso desde el ultimo mantenimiento
    long countByInyector_IdAndFechaHoraInicioAfter(Long inyectorId, LocalDateTime fecha);

    // "Reportes ejecutivos": comparativa entre sedes
    @Query("select s.id as sedeId, s.nombre as sede, count(i) as totalInyecciones, " +
           "coalesce(sum(i.volumenTotalMl), 0) as volumenTotal, " +
           "sum(case when i.estado = com.contrastiq.backend.model.enums.EstadoInyeccion.ABORTADA " +
           "or i.estado = com.contrastiq.backend.model.enums.EstadoInyeccion.ERROR then 1 else 0 end) as fallidas " +
           "from Inyeccion i join i.inyector inv join inv.sala sa join sa.sede s " +
           "where i.fechaHoraInicio between :desde and :hasta " +
           "group by s.id, s.nombre order by s.nombre")
    List<Object[]> comparativaPorSede(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // Distribucion por identificador anatomico (protocolo) para el donut/barras
    @Query("select ia.nombre as identificador, count(i) as total " +
           "from Inyeccion i join i.identificadorAnatomico ia " +
           "where i.fechaHoraInicio between :desde and :hasta " +
           "group by ia.nombre order by count(i) desc")
    List<Object[]> distribucionPorIdentificadorAnatomico(@Param("desde") LocalDateTime desde,
                                                           @Param("hasta") LocalDateTime hasta);

    // --- Dashboard de paciente ---
    List<Inyeccion> findByPaciente_IdOrderByFechaHoraInicioDesc(Long pacienteId);

    long countByPaciente_Id(Long pacienteId);

    long countByPaciente_IdAndEstadoIn(Long pacienteId, List<com.contrastiq.backend.model.enums.EstadoInyeccion> estados);

    @Query("select coalesce(sum(i.volumenTotalMl), 0) from Inyeccion i where i.paciente.id = :pacienteId")
    BigDecimal sumarVolumenPorPaciente(@Param("pacienteId") Long pacienteId);

    // "Dosis de radiacion combinada": DLP acumulado historico del paciente
    @Query("select coalesce(sum(i.dlpMgyCm), 0) from Inyeccion i where i.paciente.id = :pacienteId")
    BigDecimal sumarDlpPorPaciente(@Param("pacienteId") Long pacienteId);

    @Query("select max(i.fechaHoraInicio) from Inyeccion i where i.paciente.id = :pacienteId")
    LocalDateTime ultimaInyeccionPorPaciente(@Param("pacienteId") Long pacienteId);

    @Query("select count(e) from EventoExtravasacion e where e.inyeccion.paciente.id = :pacienteId " +
           "and e.estadoEda = com.contrastiq.backend.model.enums.EstadoEda.FUERA_DE_RANGO")
    long contarEdaFueraDeRangoPorPaciente(@Param("pacienteId") Long pacienteId);
}
