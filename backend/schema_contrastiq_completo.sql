-- =====================================================================
-- schema_contrastiq_completo.sql
-- Schema completo de ContrastIQ (43 tablas), consolidado a partir del
-- backup real que el usuario exporto de su MySQL en vivo
-- (BackupFullContrastIQ_Schema.sql, mysqldump --no-data, 2026-07-16).
-- Ya incluye TODAS las migraciones aplicadas hasta ahora, en este orden:
--   1. schema_contrast_iq.sql (base original, 27 tablas)
--   2. migration_dashboard_paciente_v2.sql (columnas nuevas en
--      pacientes/inyecciones + inyeccion_fases_programadas +
--      inyeccion_serie_flujo)
--   3. migration_roles_permisos.sql (modulos, permisos, rol_modulo_permiso)
--   4. migration_modulo_mermas.sql (modulo INSUMOS_MERMAS, el 9o)
--   5. migration_fix_qa_hallazgos.sql (usuarios.bloqueado_hasta, fix DEF-01)
--   6. fix_trazabilidad_lotes.sql (UPDATE, no cambia estructura -- no aplica aqui)
--
-- Confirmado en este archivo: usuarios.bloqueado_hasta YA esta presente,
-- o sea que migration_fix_qa_hallazgos.sql ya se corrio contra la BD real.
--
-- ADVERTENCIA (contradiccion detectada, no resuelta silenciosamente):
-- este backup incluye 6 tablas -- tenants, tenant_planes,
-- tenant_dominios, tenant_branding, tenant_branding_historial y
-- tenant_modulo_habilitado -- que NO tienen ninguna entidad JPA ni
-- repositorio/servicio/controller correspondiente en el codigo backend
-- actual (com.contrastiq.backend.model). Existen en la base de datos
-- real pero el backend de ContrastIQ no las usa todavia. Si son de un
-- modulo multi-tenant/branding en desarrollo aparte, o si son residuo
-- de otro proyecto de referencia (ej. CEROGAS GPS) y no deberian estar
-- en esta base, es una decision que solo el usuario puede confirmar --
-- no se tocaron ni se eliminaron de este script.
--
-- Para partir de cero: correr este archivo primero (crea la BD y las
-- 43 tablas vacias), y despues, si se quiere, datos_dummy_contrastiq.sql
-- (dataset de prueba de 2 anios).
-- =====================================================================

CREATE DATABASE  IF NOT EXISTS `contrast_iq` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `contrast_iq`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: contrast_iq
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `agentes_contraste`
--

DROP TABLE IF EXISTS `agentes_contraste`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agentes_contraste` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nombre_comercial` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `concentracion` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fabricante` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo` enum('CONTRASTE','SOLUCION_SALINA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CONTRASTE',
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `alertas_sistema`
--

DROP TABLE IF EXISTS `alertas_sistema`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alertas_sistema` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tipo` enum('EQUIPO_MANTENIMIENTO','STOCK_BAJO','FALLA_COMUNICACION','EDA_FUERA_DE_RANGO','OTRO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `severidad` enum('INFO','ADVERTENCIA','CRITICA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INFO',
  `inyector_id` bigint unsigned DEFAULT NULL,
  `mensaje` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resuelta` tinyint(1) NOT NULL DEFAULT '0',
  `resuelta_por` bigint unsigned DEFAULT NULL,
  `fecha_resolucion` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_alerta_inyector` (`inyector_id`),
  KEY `fk_alerta_resolutor` (`resuelta_por`),
  KEY `idx_alerta_resuelta` (`resuelta`),
  KEY `idx_alerta_fecha` (`fecha_hora`),
  CONSTRAINT `fk_alerta_inyector` FOREIGN KEY (`inyector_id`) REFERENCES `inyectores` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_alerta_resolutor` FOREIGN KEY (`resuelta_por`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=295 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `auditoria`
--

DROP TABLE IF EXISTS `auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auditoria` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tabla_afectada` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `registro_id` bigint unsigned NOT NULL,
  `usuario_id` bigint unsigned DEFAULT NULL,
  `accion` enum('CREAR','ACTUALIZAR','ELIMINAR') COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `detalle_json` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_auditoria_usuario` (`usuario_id`),
  KEY `idx_auditoria_tabla_registro` (`tabla_afectada`,`registro_id`),
  CONSTRAINT `fk_auditoria_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=501 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `checklist_pre_inyeccion`
--

DROP TABLE IF EXISTS `checklist_pre_inyeccion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checklist_pre_inyeccion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `paciente_id` bigint unsigned NOT NULL,
  `sala_id` bigint unsigned DEFAULT NULL,
  `operador_id` bigint unsigned NOT NULL,
  `identidad_verificada` tinyint(1) NOT NULL DEFAULT '0',
  `gfr_revisado` tinyint(1) NOT NULL DEFAULT '0',
  `gfr_valor_momento` decimal(6,2) DEFAULT NULL,
  `riesgo_renal_momento` tinyint(1) NOT NULL DEFAULT '0',
  `alergias_revisadas` tinyint(1) NOT NULL DEFAULT '0',
  `alergias_momento` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `consentimiento_firmado` tinyint(1) NOT NULL DEFAULT '0',
  `observaciones` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firma_nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `firma_imagen_base64` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_checklist_sala` (`sala_id`),
  KEY `fk_checklist_operador` (`operador_id`),
  KEY `idx_checklist_paciente` (`paciente_id`,`fecha_hora`),
  CONSTRAINT `fk_checklist_operador` FOREIGN KEY (`operador_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_checklist_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `pacientes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checklist_sala` FOREIGN KEY (`sala_id`) REFERENCES `salas` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=22502 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `etiquetas_impresas`
--

DROP TABLE IF EXISTS `etiquetas_impresas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `etiquetas_impresas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyeccion_id` bigint unsigned NOT NULL,
  `fecha_hora_impresion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `contenido` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_etiqueta_inyeccion` (`inyeccion_id`),
  CONSTRAINT `fk_etiqueta_inyeccion` FOREIGN KEY (`inyeccion_id`) REFERENCES `inyecciones` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=30001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eventos_extravasacion`
--

DROP TABLE IF EXISTS `eventos_extravasacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventos_extravasacion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyeccion_id` bigint unsigned NOT NULL,
  `fecha_hora` datetime NOT NULL,
  `estado_eda` enum('SIN_REFERENCIA','EN_RANGO','FUERA_DE_RANGO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `revisado` tinyint(1) NOT NULL DEFAULT '0',
  `alerta_generada` tinyint(1) NOT NULL DEFAULT '0',
  `revisado_por` bigint unsigned DEFAULT NULL,
  `fecha_revision` datetime DEFAULT NULL,
  `accion_tomada` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notas` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `fk_eda_inyeccion` (`inyeccion_id`),
  KEY `fk_eda_revisor` (`revisado_por`),
  KEY `idx_eda_estado` (`estado_eda`),
  KEY `idx_eda_revisado` (`revisado`),
  CONSTRAINT `fk_eda_inyeccion` FOREIGN KEY (`inyeccion_id`) REFERENCES `inyecciones` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_eda_revisor` FOREIGN KEY (`revisado_por`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=3001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `historial_accesos`
--

DROP TABLE IF EXISTS `historial_accesos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historial_accesos` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned DEFAULT NULL,
  `email_usado` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exitoso` tinyint(1) NOT NULL,
  `metodo` enum('LOCAL','GOOGLE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOCAL',
  `ip_origen` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_historial_acceso_usuario` (`usuario_id`,`fecha_hora`),
  CONSTRAINT `fk_historial_acceso_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=725 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `identificadores_anatomicos`
--

DROP TABLE IF EXISTS `identificadores_anatomicos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `identificadores_anatomicos` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inventario_agentes`
--

DROP TABLE IF EXISTS `inventario_agentes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventario_agentes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sede_id` bigint unsigned NOT NULL,
  `agente_id` bigint unsigned NOT NULL,
  `stock_ml` decimal(9,2) NOT NULL DEFAULT '0.00',
  `stock_minimo_ml` decimal(9,2) NOT NULL DEFAULT '0.00',
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_inventario_sede_agente` (`sede_id`,`agente_id`),
  KEY `fk_inv_agente` (`agente_id`),
  CONSTRAINT `fk_inv_agente` FOREIGN KEY (`agente_id`) REFERENCES `agentes_contraste` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_inv_sede` FOREIGN KEY (`sede_id`) REFERENCES `sedes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inyeccion_fases`
--

DROP TABLE IF EXISTS `inyeccion_fases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inyeccion_fases` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyeccion_id` bigint unsigned NOT NULL,
  `numero_fase` smallint unsigned NOT NULL,
  `agente_id` bigint unsigned NOT NULL,
  `lote_agente_id` bigint unsigned DEFAULT NULL,
  `volumen_programado_ml` decimal(6,2) DEFAULT NULL,
  `volumen_real_ml` decimal(6,2) DEFAULT NULL,
  `velocidad_flujo_ml_s` decimal(5,2) DEFAULT NULL,
  `es_fase_contraste` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_inyeccion_fase` (`inyeccion_id`,`numero_fase`),
  KEY `fk_fase_iny_agente` (`agente_id`),
  KEY `idx_fase_lote` (`lote_agente_id`),
  CONSTRAINT `fk_fase_iny_agente` FOREIGN KEY (`agente_id`) REFERENCES `agentes_contraste` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_fase_iny_inyeccion` FOREIGN KEY (`inyeccion_id`) REFERENCES `inyecciones` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fase_lote` FOREIGN KEY (`lote_agente_id`) REFERENCES `lotes_agente_contraste` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=180001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inyeccion_fases_programadas`
--

DROP TABLE IF EXISTS `inyeccion_fases_programadas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inyeccion_fases_programadas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyeccion_id` bigint unsigned NOT NULL,
  `orden_fase` smallint unsigned NOT NULL,
  `agente_id` bigint unsigned NOT NULL,
  `velocidad_flujo_ml_s` decimal(5,2) NOT NULL,
  `volumen_ml` decimal(6,2) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_inyeccion_fase_programada` (`inyeccion_id`,`orden_fase`),
  KEY `fk_fase_prog_agente` (`agente_id`),
  CONSTRAINT `fk_fase_prog_agente` FOREIGN KEY (`agente_id`) REFERENCES `agentes_contraste` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_fase_prog_inyeccion` FOREIGN KEY (`inyeccion_id`) REFERENCES `inyecciones` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=36001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inyeccion_serie_flujo`
--

DROP TABLE IF EXISTS `inyeccion_serie_flujo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inyeccion_serie_flujo` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyeccion_id` bigint unsigned NOT NULL,
  `tiempo_seg` decimal(6,2) NOT NULL,
  `flujo_contraste_ml_s` decimal(6,2) NOT NULL,
  `flujo_salina_ml_s` decimal(6,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_serie_flujo_inyeccion` (`inyeccion_id`,`tiempo_seg`),
  CONSTRAINT `fk_serie_flujo_inyeccion` FOREIGN KEY (`inyeccion_id`) REFERENCES `inyecciones` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=143854 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inyeccion_serie_presion`
--

DROP TABLE IF EXISTS `inyeccion_serie_presion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inyeccion_serie_presion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyeccion_id` bigint unsigned NOT NULL,
  `tiempo_seg` decimal(6,2) NOT NULL,
  `presion_psi` decimal(6,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_serie_presion_inyeccion` (`inyeccion_id`,`tiempo_seg`),
  CONSTRAINT `fk_serie_presion_inyeccion` FOREIGN KEY (`inyeccion_id`) REFERENCES `inyecciones` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=719458 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inyecciones`
--

DROP TABLE IF EXISTS `inyecciones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inyecciones` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyector_id` bigint unsigned NOT NULL,
  `protocolo_id` bigint unsigned DEFAULT NULL,
  `identificador_anatomico_id` bigint unsigned DEFAULT NULL,
  `paciente_id` bigint unsigned DEFAULT NULL,
  `operador_id` bigint unsigned DEFAULT NULL,
  `fecha_hora_inicio` datetime NOT NULL,
  `fecha_hora_fin` datetime DEFAULT NULL,
  `duracion_seg` int unsigned DEFAULT NULL,
  `volumen_cargado_ml` decimal(6,2) DEFAULT NULL,
  `volumen_total_ml` decimal(6,2) DEFAULT NULL,
  `volumen_residual_ml` decimal(6,2) GENERATED ALWAYS AS ((`volumen_cargado_ml` - `volumen_total_ml`)) STORED,
  `presion_maxima_psi` decimal(6,2) DEFAULT NULL,
  `presion_promedio_psi` decimal(6,2) DEFAULT NULL,
  `presion_limite_psi` decimal(6,2) DEFAULT NULL,
  `eda_habilitado` tinyint(1) NOT NULL DEFAULT '1',
  `jeringa_nueva` tinyint(1) NOT NULL DEFAULT '0',
  `ctdi_vol_mgy` decimal(8,2) DEFAULT NULL,
  `dlp_mgy_cm` decimal(10,2) DEFAULT NULL,
  `estado` enum('COMPLETADA','ABORTADA','ERROR') COLLATE utf8mb4_unicode_ci NOT NULL,
  `motivo_aborto` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lote_sincronizacion_id` bigint unsigned DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `numero_accesion` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `procedimiento_programado` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `calibre_aguja` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `acceso_aguja` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avance_salina_ml` decimal(6,2) DEFAULT NULL,
  `salina_jump_usado` tinyint(1) NOT NULL DEFAULT '0',
  `scanner` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notas` text COLLATE utf8mb4_unicode_ci,
  `dosis_contraste_gl` decimal(6,2) DEFAULT NULL,
  `retraso_escaneo_seg` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_iny_protocolo` (`protocolo_id`),
  KEY `fk_iny_paciente` (`paciente_id`),
  KEY `fk_iny_operador` (`operador_id`),
  KEY `fk_iny_lote` (`lote_sincronizacion_id`),
  KEY `idx_iny_fecha` (`fecha_hora_inicio`),
  KEY `idx_iny_inyector_fecha` (`inyector_id`,`fecha_hora_inicio`),
  KEY `idx_iny_estado` (`estado`),
  KEY `idx_iny_identificador` (`identificador_anatomico_id`),
  CONSTRAINT `fk_iny_identificador` FOREIGN KEY (`identificador_anatomico_id`) REFERENCES `identificadores_anatomicos` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_iny_inyector` FOREIGN KEY (`inyector_id`) REFERENCES `inyectores` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_iny_lote` FOREIGN KEY (`lote_sincronizacion_id`) REFERENCES `lotes_sincronizacion` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_iny_operador` FOREIGN KEY (`operador_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_iny_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `pacientes` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_iny_protocolo` FOREIGN KEY (`protocolo_id`) REFERENCES `protocolos_inyeccion` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=90001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inyectores`
--

DROP TABLE IF EXISTS `inyectores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inyectores` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sala_id` bigint unsigned NOT NULL,
  `numero_serie` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `modelo` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EmpowerCTA',
  `fecha_instalacion` date DEFAULT NULL,
  `version_firmware` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('ACTIVO','MANTENIMIENTO','FUERA_DE_SERVICIO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  `fecha_ultimo_mantenimiento` date DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `numero_serie` (`numero_serie`),
  KEY `fk_inyectores_sala` (`sala_id`),
  CONSTRAINT `fk_inyectores_sala` FOREIGN KEY (`sala_id`) REFERENCES `salas` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lotes_agente_contraste`
--

DROP TABLE IF EXISTS `lotes_agente_contraste`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lotes_agente_contraste` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `agente_id` bigint unsigned NOT NULL,
  `sede_id` bigint unsigned NOT NULL,
  `numero_lote` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_caducidad` date NOT NULL,
  `cantidad_ml` decimal(9,2) NOT NULL,
  `recibido_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_lote_agente_sede` (`numero_lote`,`agente_id`,`sede_id`),
  KEY `fk_lote_agente` (`agente_id`),
  KEY `fk_lote_sede` (`sede_id`),
  KEY `idx_lote_caducidad` (`fecha_caducidad`),
  CONSTRAINT `fk_lote_agente` FOREIGN KEY (`agente_id`) REFERENCES `agentes_contraste` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_lote_sede` FOREIGN KEY (`sede_id`) REFERENCES `sedes` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lotes_sincronizacion`
--

DROP TABLE IF EXISTS `lotes_sincronizacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lotes_sincronizacion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `fuente` enum('IRIS_WORKSTATION','EXPORT_CSV','MANUAL','EMPOWERSYNC') COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `registros_importados` int unsigned NOT NULL DEFAULT '0',
  `estado` enum('EXITOSO','PARCIAL','FALLIDO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EXITOSO',
  `usuario_id` bigint unsigned DEFAULT NULL,
  `detalle` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `fk_lote_usuario` (`usuario_id`),
  CONSTRAINT `fk_lote_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=501 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mantenimientos_inyector`
--

DROP TABLE IF EXISTS `mantenimientos_inyector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mantenimientos_inyector` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyector_id` bigint unsigned NOT NULL,
  `tipo` enum('PREVENTIVO','CORRECTIVO','CALIBRACION') COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime DEFAULT NULL,
  `tecnico` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` longtext COLLATE utf8mb4_unicode_ci,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_mant_inyector` (`inyector_id`),
  CONSTRAINT `fk_mant_inyector` FOREIGN KEY (`inyector_id`) REFERENCES `inyectores` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=123 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `modulos`
--

DROP TABLE IF EXISTS `modulos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `modulos` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `orden` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pacientes`
--

DROP TABLE IF EXISTS `pacientes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pacientes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `identificador_externo` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_completo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sexo` enum('M','F','OTRO','NO_ESPECIFICADO') COLLATE utf8mb4_unicode_ci DEFAULT 'NO_ESPECIFICADO',
  `peso_kg` decimal(5,2) DEFAULT NULL,
  `gfr_ml_min` decimal(6,2) DEFAULT NULL,
  `creatinina_mg_dl` decimal(5,2) DEFAULT NULL,
  `alergias` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sincronizado_his_en` datetime DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `numero_expediente` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `talla_m` decimal(4,2) DEFAULT NULL,
  `grupo_etnico` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `identificador_externo` (`identificador_externo`)
) ENGINE=InnoDB AUTO_INCREMENT=6001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pedidos_reabastecimiento`
--

DROP TABLE IF EXISTS `pedidos_reabastecimiento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pedidos_reabastecimiento` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sede_id` bigint unsigned NOT NULL,
  `agente_id` bigint unsigned NOT NULL,
  `cantidad_solicitada_ml` decimal(9,2) NOT NULL,
  `estado` enum('PENDIENTE','ENVIADO','RECIBIDO','CANCELADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE',
  `generado_automaticamente` tinyint(1) NOT NULL DEFAULT '0',
  `fecha_solicitud` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_envio` datetime DEFAULT NULL,
  `fecha_recepcion` datetime DEFAULT NULL,
  `notas` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pedido_agente` (`agente_id`),
  KEY `idx_pedido_estado` (`estado`),
  KEY `idx_pedido_sede_agente` (`sede_id`,`agente_id`),
  CONSTRAINT `fk_pedido_agente` FOREIGN KEY (`agente_id`) REFERENCES `agentes_contraste` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_pedido_sede` FOREIGN KEY (`sede_id`) REFERENCES `sedes` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permisos`
--

DROP TABLE IF EXISTS `permisos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permisos` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `protocolo_fases`
--

DROP TABLE IF EXISTS `protocolo_fases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `protocolo_fases` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `protocolo_id` bigint unsigned NOT NULL,
  `numero_fase` smallint unsigned NOT NULL,
  `agente_id` bigint unsigned NOT NULL,
  `volumen_ml` decimal(6,2) NOT NULL,
  `velocidad_flujo_ml_s` decimal(5,2) NOT NULL,
  `es_fase_contraste` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_protocolo_fase` (`protocolo_id`,`numero_fase`),
  KEY `fk_fase_agente` (`agente_id`),
  CONSTRAINT `fk_fase_agente` FOREIGN KEY (`agente_id`) REFERENCES `agentes_contraste` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_fase_protocolo` FOREIGN KEY (`protocolo_id`) REFERENCES `protocolos_inyeccion` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `protocolos_inyeccion`
--

DROP TABLE IF EXISTS `protocolos_inyeccion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `protocolos_inyeccion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `identificador_anatomico_id` bigint unsigned NOT NULL,
  `fuente` enum('BRACCO','PERSONALIZADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PERSONALIZADO',
  `velocidad_flujo_min_ml_s` decimal(5,2) DEFAULT NULL,
  `velocidad_flujo_max_ml_s` decimal(5,2) DEFAULT NULL,
  `volumen_default_ml` decimal(6,2) DEFAULT NULL,
  `numero_fases_default` smallint unsigned NOT NULL DEFAULT '1',
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_protocolo_identificador` (`identificador_anatomico_id`),
  CONSTRAINT `fk_protocolo_identificador` FOREIGN KEY (`identificador_anatomico_id`) REFERENCES `identificadores_anatomicos` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rol_modulo_permiso`
--

DROP TABLE IF EXISTS `rol_modulo_permiso`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol_modulo_permiso` (
  `rol_id` bigint unsigned NOT NULL,
  `modulo_id` bigint unsigned NOT NULL,
  `permiso_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`rol_id`,`modulo_id`,`permiso_id`),
  KEY `fk_rmp_modulo` (`modulo_id`),
  KEY `fk_rmp_permiso` (`permiso_id`),
  CONSTRAINT `fk_rmp_modulo` FOREIGN KEY (`modulo_id`) REFERENCES `modulos` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rmp_permiso` FOREIGN KEY (`permiso_id`) REFERENCES `permisos` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rmp_rol` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` enum('ADMIN','TECNICO','RADIOLOGO','BIOMEDICA','VISUALIZADOR') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `salas`
--

DROP TABLE IF EXISTS `salas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sede_id` bigint unsigned NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_estudio` enum('TAC','RM','ANGIO','OTRO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TAC',
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_salas_sede` (`sede_id`),
  CONSTRAINT `fk_salas_sede` FOREIGN KEY (`sede_id`) REFERENCES `sedes` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sedes`
--

DROP TABLE IF EXISTS `sedes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sedes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `direccion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenant_branding`
--

DROP TABLE IF EXISTS `tenant_branding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_branding` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint unsigned NOT NULL,
  `nombre_comercial_mostrado` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_principal_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_claro_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_oscuro_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_login_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `favicon_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color_primario` varchar(7) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#1565C0',
  `color_error` varchar(7) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#dc2626',
  `esquema_tema` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SISTEMA',
  `email_soporte` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono_soporte` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direccion_fiscal` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_aviso_privacidad` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_terminos_condiciones` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `texto_pie_pagina` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zona_horaria` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'America/Mexico_City',
  `locale` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'es-MX',
  `unidad_medida` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'METRICO',
  `borrador_json` longtext COLLATE utf8mb4_unicode_ci,
  `tiene_cambios_sin_publicar` tinyint(1) NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '1',
  `publicado_en` datetime DEFAULT NULL,
  `publicado_por_usuario_id` bigint unsigned DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tenant_id` (`tenant_id`),
  KEY `fk_tenant_branding_publicado_por` (`publicado_por_usuario_id`),
  CONSTRAINT `fk_tenant_branding_publicado_por` FOREIGN KEY (`publicado_por_usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_tenant_branding_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenant_branding_historial`
--

DROP TABLE IF EXISTS `tenant_branding_historial`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_branding_historial` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint unsigned NOT NULL,
  `version` int NOT NULL,
  `snapshot_json` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `publicado_por_usuario_id` bigint unsigned DEFAULT NULL,
  `publicado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_tenant_branding_hist_usuario` (`publicado_por_usuario_id`),
  KEY `idx_tenant_branding_hist_tenant` (`tenant_id`,`version`),
  CONSTRAINT `fk_tenant_branding_hist_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tenant_branding_hist_usuario` FOREIGN KEY (`publicado_por_usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenant_dominios`
--

DROP TABLE IF EXISTS `tenant_dominios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_dominios` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint unsigned NOT NULL,
  `tipo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valor` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado_verificacion` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE',
  `es_principal` tinyint(1) NOT NULL DEFAULT '0',
  `notas_verificacion` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `valor` (`valor`),
  KEY `idx_tenant_dominio_tenant` (`tenant_id`),
  CONSTRAINT `fk_tenant_dominio_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenant_modulo_habilitado`
--

DROP TABLE IF EXISTS `tenant_modulo_habilitado`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_modulo_habilitado` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint unsigned NOT NULL,
  `modulo_id` bigint unsigned NOT NULL,
  `habilitado` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tenant_modulo` (`tenant_id`,`modulo_id`),
  KEY `fk_tenant_modulo_modulo` (`modulo_id`),
  CONSTRAINT `fk_tenant_modulo_modulo` FOREIGN KEY (`modulo_id`) REFERENCES `modulos` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tenant_modulo_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenant_planes`
--

DROP TABLE IF EXISTS `tenant_planes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_planes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `codigo` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `permite_dominio_personalizado` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenants`
--

DROP TABLE IF EXISTS `tenants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenants` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `slug` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_comercial` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `razon_social` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  `es_default` tinyint(1) NOT NULL DEFAULT '0',
  `plan_id` bigint unsigned DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `slug` (`slug`),
  KEY `fk_tenant_plan` (`plan_id`),
  CONSTRAINT `fk_tenant_plan` FOREIGN KEY (`plan_id`) REFERENCES `tenant_planes` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tickets_soporte`
--

DROP TABLE IF EXISTS `tickets_soporte`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tickets_soporte` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `inyector_id` bigint unsigned NOT NULL,
  `creado_por` bigint unsigned NOT NULL,
  `titulo` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prioridad` enum('BAJA','MEDIA','ALTA','CRITICA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIA',
  `estado` enum('ABIERTO','EN_PROCESO','ESPERANDO_FABRICANTE','CERRADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ABIERTO',
  `numero_ticket_fabricante` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `respuesta_fabricante` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_cierre` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_ticket_creador` (`creado_por`),
  KEY `idx_ticket_estado` (`estado`),
  KEY `idx_ticket_inyector` (`inyector_id`),
  CONSTRAINT `fk_ticket_creador` FOREIGN KEY (`creado_por`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ticket_inyector` FOREIGN KEY (`inyector_id`) REFERENCES `inyectores` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tokens_recuperacion_password`
--

DROP TABLE IF EXISTS `tokens_recuperacion_password`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tokens_recuperacion_password` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expira_en` datetime NOT NULL,
  `usado` tinyint(1) NOT NULL DEFAULT '0',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `idx_token_recuperacion_usuario` (`usuario_id`),
  KEY `idx_token_recuperacion_expira` (`expira_en`),
  CONSTRAINT `fk_token_recuperacion_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tokens_refresco`
--

DROP TABLE IF EXISTS `tokens_refresco`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tokens_refresco` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `token` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expira_en` datetime NOT NULL,
  `revocado` tinyint(1) NOT NULL DEFAULT '0',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_refresco_token` (`token`),
  KEY `idx_token_refresco_usuario` (`usuario_id`),
  CONSTRAINT `fk_token_refresco_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sede_id` bigint unsigned DEFAULT NULL,
  `nombre_completo` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `proveedor` enum('LOCAL','GOOGLE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOCAL',
  `proveedor_id` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email_verificado` tinyint(1) NOT NULL DEFAULT '0',
  `rol_id` bigint unsigned NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `bloqueado_hasta` datetime DEFAULT NULL COMMENT 'Si no es NULL y es futuro, la cuenta esta bloqueada por intentos fallidos (DEF-01, QA julio 2026)',
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `uq_usuarios_proveedor` (`proveedor`,`proveedor_id`),
  KEY `fk_usuario_sede` (`sede_id`),
  KEY `fk_usuario_rol` (`rol_id`),
  CONSTRAINT `fk_usuario_rol` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_usuario_sede` FOREIGN KEY (`sede_id`) REFERENCES `sedes` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'contrast_iq'
--

--
-- Dumping routines for database 'contrast_iq'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-16 17:10:32
