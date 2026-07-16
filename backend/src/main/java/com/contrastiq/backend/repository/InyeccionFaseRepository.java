package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.InyeccionFase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InyeccionFaseRepository extends JpaRepository<InyeccionFase, Long> {
    // Trazabilidad: todas las fases (y por lo tanto inyecciones/pacientes)
    // que usaron un lote especifico -- la consulta clave ante un recall.
    List<InyeccionFase> findByLoteAgenteIdOrderByInyeccion_FechaHoraInicioDesc(Long loteAgenteId);

    // --- Merma de insumos ---
    // Todas las consultas de merma parten de aqui (volumen_programado_ml -
    // volumen_real_ml por fase) en vez de inyecciones.volumen_residual_ml,
    // para que el KPI agregado, el desglose por sede, el desglose por
    // insumo y la tabla detallada por inyeccion sean consistentes entre
    // si (misma fuente de verdad, distintos agrupamientos).

    // Sumas crudas del periodo completo -- el % y la tendencia se calculan
    // en MermaService. Declarado como List<Object[]> (no Object[] suelto):
    // Spring Data JPA trata un metodo que regresa Object[] como "una fila
    // por elemento del array" (wrapea el List<Object[]> resultante de
    // Hibernate en un Object[] de 1 elemento, que a su vez es el
    // Object[2] real) -- eso causaba un ClassCastException en tiempo de
    // ejecucion (fila[0] resultaba ser el Object[2] completo, no el
    // BigDecimal esperado). Con List<Object[]> se obtiene directamente
    // la fila unica esperada via .get(0) en MermaService.
    @Query("select coalesce(sum(f.volumenProgramadoMl), 0), coalesce(sum(f.volumenRealMl), 0) " +
           "from InyeccionFase f join f.inyeccion i " +
           "where i.fechaHoraInicio between :desde and :hasta")
    List<Object[]> sumasEntreFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select s.id, s.nombre, coalesce(sum(f.volumenProgramadoMl), 0), coalesce(sum(f.volumenRealMl), 0) " +
           "from InyeccionFase f join f.inyeccion i join i.inyector inv join inv.sala sa join sa.sede s " +
           "where i.fechaHoraInicio between :desde and :hasta " +
           "group by s.id, s.nombre order by s.nombre")
    List<Object[]> sumasPorSede(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select ag.id, ag.nombreComercial, ag.tipo, ag.fabricante, " +
           "coalesce(sum(f.volumenProgramadoMl), 0), coalesce(sum(f.volumenRealMl), 0) " +
           "from InyeccionFase f join f.inyeccion i join f.agente ag " +
           "where i.fechaHoraInicio between :desde and :hasta " +
           "group by ag.id, ag.nombreComercial, ag.tipo, ag.fabricante " +
           "order by ag.nombreComercial")
    List<Object[]> sumasPorInsumo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // Detalle por inyeccion individual, paginado y ordenado por merma
    // descendente -- pensado para investigar casos puntuales (ej.
    // procedimientos ABORTADA con merma alta).
    @Query(value = "select i.id, i.fechaHoraInicio, p.nombreCompleto, p.numeroExpediente, s.nombre, sa.nombre, " +
                    "i.estado, i.motivoAborto, " +
                    "coalesce(sum(f.volumenProgramadoMl), 0), coalesce(sum(f.volumenRealMl), 0) " +
                    "from InyeccionFase f join f.inyeccion i join i.inyector inv join inv.sala sa join sa.sede s " +
                    "left join i.paciente p " +
                    "where i.fechaHoraInicio between :desde and :hasta " +
                    "group by i.id, i.fechaHoraInicio, p.nombreCompleto, p.numeroExpediente, s.nombre, sa.nombre, " +
                    "i.estado, i.motivoAborto " +
                    "order by (coalesce(sum(f.volumenProgramadoMl), 0) - coalesce(sum(f.volumenRealMl), 0)) desc",
           countQuery = "select count(distinct i.id) from InyeccionFase f join f.inyeccion i " +
                        "where i.fechaHoraInicio between :desde and :hasta")
    Page<Object[]> detallePorInyeccion(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                        Pageable pageable);
}
