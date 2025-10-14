# Concurrency Monitor - Petri Nets

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=flat&logo=gradle&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white"/>
</p>

> Implementación de simulación de una Red de Petri, administrando la concurrencia de la red a través de un monitor.

---

## Descripción

En el presente proyecto se implementa la **ejecución y análisis de Redes de Petri**. Estas modelan sistemas que típicamente son concurrentes, ya que diversos procesos deben interactuar de manera coordinada al compartir recursos. Los recursos deben ser gestionados eficientemente para maximizar el rendimiento, por lo que el sistema planteado presenta desafíos típicos de la programación concurrente, requiriéndose al menos un mecanismo de sincronización para coordinar las diferentes etapas de los procesos, en este caso un **monitor de concurrencia**.

Tanto para el análisis como para la ejecución (disparos de transiciones) de la Red de Petri se utiliza álgebra lineal, por lo que la instancia de la red se define mediante la **matriz de incidencia de entrada** (Pre), **matriz de incidencia de salida** (Post) y la **marca inicial** (M0). Para evitar hardcodear estos valores, se implementó un archivo `config.properties` en el directorio `./src/main/resources`. En este archivo se especifica el parámetro `execution.max_invariants` para determinar cuántas invariantes se completan antes de detener la simulación. Además, admite transiciones temporales mediante el vector `temporary_transitions.vector` (si el valor es 0, la transición es instantánea).

Lo primero que ejecuta el código son diversos algoritmos para determinar cantidad y responsabilidad de hilos en el sistema:
* Algoritmo para la determinación de hilos máximos activos simultáneos
* Algoritmo para determinar la responsabilidad de los hilos
* Algoritmo para la determinación de hilos máximos por segmento

En resumen, el sistema calcula los **P-invariantes** (de los cuales se deduce qué plazas son recursos, restricciones o idle), los **T-invariantes**, el **árbol de alcanzabilidad**, la cantidad total de hilos, las **secuencias de disparo** y la cantidad de hilos por secuencia, detectando T-invariantes lineales (secuenciales), conflictos (forks) y uniones (joins).

Una vez determinada la cantidad de hilos y sus responsabilidades (secuencias de disparos), inicia la simulación de la red. Los disparos se realizan de manera concurrente en el monitor, asegurando que los recursos solo sean utilizados por un hilo a la vez. La simulación finaliza una vez alcanzado el máximo de invariantes permitidos (186 por defecto).

Finalmente, se procede a realizar un análisis temporal y diversas estadísticas referentes a cada hilo.

## Objetivos

---

## Tecnologías Utilizadas

### Lenguaje y Entorno de Ejecución
- **Java:** OpenJDK 21.0.8
  - OpenJDK Runtime Environment (build 21.0.8+9-Ubuntu-0ubuntu124.04.1)
  - OpenJDK 64-Bit Server VM (mixed mode, sharing)

### Herramientas de Build
- **Gradle:** 4.4.1
  - Groovy: 2.4.21
  - Apache Ant: 1.10.14

### Containerización
- **Docker:** 27.5.1

### Sistema Operativo de Desarrollo
- **OS:** Linux 6.14.0-33-generic amd64 (Ubuntu 24.04)

### Librerías y Dependencias
- Java Collections Framework
- Java Concurrency API (`java.util.concurrent`)
- Java Properties (Configuración)

---

## Características Principales
## Arquitectura del Sistema

## Documentación Adicional

## Autor

## Licencia
<p align="center">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg"/>
</p>

Copyright (c) 2025 Sassi Juan Ignacio