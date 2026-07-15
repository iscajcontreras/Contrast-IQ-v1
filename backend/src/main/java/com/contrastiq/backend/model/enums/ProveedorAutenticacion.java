package com.contrastiq.backend.model.enums;

// Antes incluia GOOGLE (login federado con Google), removido como
// limpieza de deuda tecnica (Prioridad 4 de PROXIMOS_PASOS.md): ese
// flujo nunca llego a completarse y no tenia ningun codigo activo que
// lo usara. La columna `proveedor` en BD sigue siendo un ENUM('LOCAL',
// 'GOOGLE') por compatibilidad (ver schema_contrast_iq.sql) -- no
// requirio migracion porque Hibernate con ddl-auto=validate no valida
// la lista exacta de valores de un ENUM de MySQL, solo la existencia y
// tipo de la columna.
public enum ProveedorAutenticacion {
    LOCAL
}
