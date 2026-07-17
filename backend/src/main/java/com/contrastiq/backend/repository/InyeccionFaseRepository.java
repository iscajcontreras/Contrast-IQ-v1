package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.InyeccionFase;
import com.contrastiq.backend.model.enums.EstadoInyeccion;
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

    // Julio 2026: indicador visual en el listado de Lotes ("ya tiene
    // inyecciones realizadas"). Se resuelve en batch por pagina (ver
    // LoteService.buscar) -- un solo IN-query con los ids de la pagina
    // actual, no N+1 por fila.
    @Query("select distinct f.loteAgente.id from InyeccionFase f where f.loteAgente.id in :loteIds")
    List<Long> findLoteIdsConInyecciones(@Param("loteIds") List<Long> loteIds);

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

    // Merma julio 2026: pedido del usuario para la tarjeta nueva del
    // dashboard "Inyecciones de contraste" -- a diferencia de
    // sumasEntreFechas (solo fecha), esta replica TODOS los filtros de la
    // barra del dashboard (InyeccionSpecification.conFiltros), para que
    // la tarjeta muestre la merma de exactamente lo que esta filtrado
    // arriba, no del periodo completo. Cada parametro es opcional (null =
    // "todos"), con el mismo patron "(:param is null or ...)" en vez de
    // Specification/Criteria para mantener una sola query legible.
    //
    // Nota sobre agenteId: a diferencia de InyeccionSpecification (que
    // usa un EXISTS -- "la inyeccion tiene AL MENOS una fase de ese
    // agente" -- y de ser asi suma TODAS sus fases), aqui se filtra
    // directo sobre f.agente.id: solo se suman las fases que usaron ESE
    // agente. Es la semantica correcta para una merma "por insumo" (ver
    // sumasPorInsumo, que agrupa igual) -- sumar fases de otros agentes
    // en una tarjeta filtrada "por este agente" seria enganoso.
    @Query("select coalesce(sum(f.volumenProgramadoMl), 0), coalesce(sum(f.volumenRealMl), 0) " +
           "from InyeccionFase f join f.inyeccion i join i.inyector inv join inv.sala sa join sa.sede se " +
           "where (:desde is null or i.fechaHoraInicio >= :desde) " +
           "and (:hasta is null or i.fechaHoraInicio <= :hasta) " +
           "and (:sedeId is null or se.id = :sedeId) " +
           "and (:salaId is null or sa.id = :salaId) " +
           "and (:agenteId is null or f.agente.id = :agenteId) " +
           "and (:identificadorAnatomicoId is null or i.identificadorAnatomico.id = :identificadorAnatomicoId) " +
           "and (:estado is null or i.estado = :estado) " +
           "and (:soloConAlertaEda = false or exists (" +
           "  select 1 from EventoExtravasacion e where e.inyeccion = i " +
           "  and e.estadoEda = com.contrastiq.backend.model.enums.EstadoEda.FUERA_DE_RANGO))")
    List<Object[]> sumasConFiltros(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta,
                                    @Param("sedeId") Long sedeId,
                                    @Param("salaId") Long salaId,
                                    @Param("agenteId") Long agenteId,
                                    @Param("identificadorAnatomicoId") Long identificadorAnatomicoId,
                                    @Param("estado") EstadoInyeccion estado,
                                    @Param("soloConAlertaEda") Boolean soloConAlertaEda);

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

    // Detalle por inyeccion individual, paginado. Julio 2026: a peticion
    // del usuario el orden cambio de "merma descendente" a "fecha mas
    // reciente primero" (antes era mas util para priorizar casos de
    // merma alta, pero el usuario prefiere ver primero lo mas reciente
    // -- los casos ABORTADA/ERROR se siguen resaltando visualmente en el
    // frontend independientemente del orden).
    @Query(value = "select i.id, i.fechaHoraInicio, p.nombreCompleto, p.numeroExpediente, s.nombre, sa.nombre, " +
                    "i.estado, i.motivoAborto, " +
                    "coalesce(sum(f.volumenProgramadoMl), 0), coalesce(sum(f.volumenRealMl), 0) " +
                    "from InyeccionFase f join f.inyeccion i join i.inyector inv join inv.sala sa join sa.sede s " +
                    "left join i.paciente p " +
                    "where i.fechaHoraInicio between :desde and :hasta " +
                    "group by i.id, i.fechaHoraInicio, p.nombreCompleto, p.numeroExpediente, s.nombre, sa.nombre, " +
                    "i.estado, i.motivoAborto " +
                    "order by i.fechaHoraInicio desc",
           countQuery = "select count(distinct i.id) from InyeccionFase f join f.inyeccion i " +
                        "where i.fechaHoraInicio between :desde and :hasta")
    Page<Object[]> detallePorInyeccion(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                        Pageable pageable);
}
