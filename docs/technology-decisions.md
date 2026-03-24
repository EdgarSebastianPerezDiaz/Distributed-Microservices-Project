# Documento de Decisiones Tecnológicas

**Proyecto:** Sistema de Gestión de Contratos y Proveedores  
**Tipo:** Backend — Arquitectura de Microservicios  
**Sprint:** 1  
**Versión:** 1.0  
**Fecha:** 2025-01-15  

---

## Tabla de Contenido

1. [Contexto y Alcance](#1-contexto-y-alcance)  
2. [Estructura del Documento](#2-estructura-del-documento)  
3. [Nivel de Sistema — Arquitectura de Microservicios](#3-nivel-de-sistema--arquitectura-de-microservicios)  
4. [Nivel de Aplicación — Frameworks y Lenguajes](#4-nivel-de-aplicación--frameworks-y-lenguajes)  
5. [Nivel de Datos — Persistencia](#5-nivel-de-datos--persistencia)  
6. [Nivel de Comunicación — API y Seguridad](#6-nivel-de-comunicación--api-y-seguridad)  
7. [Nivel de Red — Infraestructura de Microservicios](#7-nivel-de-red--infraestructura-de-microservicios)  
8. [Resumen de Decisiones](#8-resumen-de-decisiones)  
9. [Referencias Bibliográficas](#9-referencias-bibliográficas)  

---

## 1. Contexto y Alcance

El sistema tiene como objetivo gestionar el ciclo de vida de contratos con proveedores, con trazabilidad de auditoría completa. Está compuesto por cuatro servicios de negocio (Usuarios, Proveedores, Contratos, Auditoría), un API Gateway y un Service Registry, todos corriendo localmente. La solución es exclusivamente backend; las pruebas se realizan mediante Postman.

Los requisitos no funcionales más relevantes que guían las decisiones tecnológicas son:

- Tiempo de respuesta inferior a 2 ms en condiciones locales.
- Autenticación JWT obligatoria en todos los endpoints (excepto `/auth/login`).
- Cada microservicio con su propia base de datos (aislamiento de datos).
- Comunicación exclusivamente por REST (HTTP/JSON).
- Control de acceso basado en roles: ADMINISTRADOR, FUNCIONARIO, AUDITOR (sin multirol).

---

## 2. Estructura del Documento

Las decisiones se organizan siguiendo los cinco niveles de arquitectura propios de los sistemas distribuidos, de acuerdo con Tanenbaum (2002) y Coulouris (2001):

| Nivel | Alcance |
|---|---|
| **Sistema** | Estilo arquitectónico general (microservicios) |
| **Aplicación** | Frameworks, lenguajes y herramientas de desarrollo |
| **Datos** | Motores de base de datos y estrategias de persistencia |
| **Comunicación** | Protocolo de comunicación, formato de mensajes y seguridad |
| **Red** | Descubrimiento de servicios, enrutamiento y balanceo |

---

## 3. Nivel de Sistema — Arquitectura de Microservicios

### Decisión: Arquitectura de Microservicios

**Alternativas consideradas:**  
- Monolito modular  
- Arquitectura de microservicios  
- SOA (Service Oriented Architecture)

**Decisión adoptada:** Arquitectura de microservicios con despliegue local.

**Justificación:**

Los sistemas distribuidos, según Tanenbaum (2002), son colecciones de computadores independientes que se muestran al usuario como un único sistema coherente. El objetivo primario es la transparencia de distribución: que los componentes puedan evolucionar, desplegarse y escalarse de forma independiente, sin afectar la disponibilidad del sistema completo.

Coulouris (2001) refuerza esta perspectiva al destacar que la separación de responsabilidades entre componentes —lo que en la arquitectura actual se materializa como servicios independientes— reduce el acoplamiento estructural y aumenta la tolerancia a fallos. Un error en el servicio de Auditoría, por ejemplo, no afecta la operación de Contratos.

La granularidad elegida (un servicio por dominio de negocio: Usuarios, Proveedores, Contratos, Auditoría) sigue el principio de separación de preocupaciones (*Separation of Concerns*), permitiendo que cada equipo o desarrollador trabaje sobre un servicio con ciclo de vida propio. Liu (2004) denomina a este tipo de sistemas *loosely coupled distributed systems*, donde los componentes interactúan mediante interfaces bien definidas y pueden reemplazarse sin afectar al sistema global.

**Consecuencias de la decisión:**
- Cada servicio tiene su propia base de datos (no se comparten esquemas).
- La comunicación entre servicios es únicamente REST/HTTP.
- Se requiere un mecanismo de descubrimiento de servicios (Eureka) y enrutamiento centralizado (API Gateway).

---

## 4. Nivel de Aplicación — Frameworks y Lenguajes

### 4.1 Decisión: Spring Boot (Java) para Usuarios, Proveedores y Contratos

**Alternativas consideradas:**  
- Spring Boot (Java)  
- Quarkus (Java)  
- Node.js + Express  
- .NET Core

**Decisión adoptada:** Spring Boot 3.x con Java 17+.

**Justificación:**

Spring Boot provee un entorno de autoconfiguración para aplicaciones Java que reduce significativamente el tiempo de arranque del proyecto. Para sistemas distribuidos, la relevancia de Spring Boot radica en su integración nativa con Spring Cloud, el ecosistema que incluye Eureka (Service Discovery), Spring Cloud Gateway y Spring Security para JWT.

Desde la perspectiva de Tanenbaum (2002), la portabilidad y transparencia de acceso son propiedades fundamentales de los sistemas distribuidos. Spring Boot cumple ambas: el mismo artefecto `.jar` puede ejecutarse en cualquier JVM sin modificaciones, y la abstracción de Spring Security permite configurar la autenticación JWT sin acoplar la lógica de seguridad a la lógica de negocio de cada servicio.

Schäfer (2010) señala que los modelos de programación concurrente y distribuida requieren mecanismos de abstracción que desacoplen el manejo de hilos del código de dominio. Spring Boot, a través de su integración con el modelo de servlets y el contenedor embebido Tomcat, gestiona la concurrencia internamente, permitiendo que los servicios de negocio se concentren exclusivamente en la lógica de dominio.

**Aplicabilidad al proyecto:**  
- Los tres servicios (Usuarios, Proveedores, Contratos) comparten el mismo stack tecnológico, lo que reduce la curva de aprendizaje y facilita el mantenimiento.
- Spring Data JPA simplifica la interacción con PostgreSQL mediante repositorios tipados.
- Spring Security gestiona la validación de tokens JWT en un filtro centralizado por servicio.

### 4.2 Decisión: Python + FastAPI para el Servicio de Auditoría

**Alternativas consideradas:**  
- Spring Boot (Java) — mismo stack que los demás servicios  
- Python + FastAPI  
- Python + Django REST Framework  
- Node.js + Fastify

**Decisión adoptada:** Python 3.11+ con FastAPI.

**Justificación:**

La elección de un segundo lenguaje para el servicio de Auditoría responde a dos razones complementarias: demostrar la interoperabilidad propia de los sistemas distribuidos y aprovechar el ecosistema Python para el procesamiento de documentos NoSQL.

Coulouris (2001) establece que la heterogeneidad (*heterogeneity*) es una de las características definitivas de los sistemas distribuidos modernos: componentes implementados en diferentes lenguajes, sobre diferentes sistemas operativos, deben coexistir e interactuar de manera transparente mediante protocolos estándar. REST/JSON actúa aquí como el protocolo de interoperabilidad, permitiendo que el servicio Python se integre sin fricciones con los servicios Java.

FastAPI, en concreto, fue seleccionado sobre Django REST Framework por su rendimiento (basado en Starlette y `asyncio`) y su generación automática de documentación OpenAPI 3.0, coherente con el contrato de API definido en el Sprint 1. La naturaleza asíncrona de FastAPI es especialmente adecuada para el servicio de Auditoría, que realiza escrituras en MongoDB y puede atender múltiples eventos concurrentes sin bloqueo.

Motor (el driver async de MongoDB para Python) se complementa naturalmente con FastAPI, permitiendo operaciones de inserción no bloqueantes sobre la colección de eventos.

---

## 5. Nivel de Datos — Persistencia

### 5.1 Decisión: PostgreSQL para Usuarios, Proveedores y Contratos

**Alternativas consideradas:**  
- PostgreSQL (SQL relacional)  
- MySQL / MariaDB  
- H2 (en memoria, solo para pruebas)  

**Decisión adoptada:** PostgreSQL 15+ como motor relacional para los tres servicios de negocio.

**Justificación:**

Los datos de Usuarios, Proveedores y Contratos son de naturaleza altamente estructurada, con relaciones bien definidas (un usuario tiene exactamente un rol; un contrato referencia un proveedor) y con requisitos estrictos de integridad referencial e integridad transaccional (ACID). Liu (2004) precisa que en los sistemas distribuidos, cuando se requieren garantías de consistencia fuerte dentro de un componente, las bases de datos relacionales siguen siendo la elección más adecuada, dado que implementan las propiedades ACID de manera nativa.

La restricción de no multirol para los usuarios se implementa directamente en el esquema SQL: la tabla `usuarios` tiene una columna `rol_id` NOT NULL con clave foránea a la tabla `roles`, y no existe tabla intermedia de relación usuario-rol. Esto garantiza, a nivel de motor de base de datos, que un usuario no puede tener más de un rol, incluso si la capa de aplicación falla.

PostgreSQL se selecciona sobre MySQL por su soporte robusto de tipos (UUID nativos, JSONB, arrays), su motor de índices avanzados (GIN para búsquedas de texto), y su estricto cumplimiento del estándar SQL, que facilita la portabilidad. Adicionalmente, PostgreSQL soporta `TIMESTAMPTZ` (timestamp con zona horaria), esencial para un sistema distribuido donde la consistencia temporal de los registros es crítica (Tanenbaum, 2002, aborda la sincronización de relojes en sistemas distribuidos como un problema fundamental).

**Aislamiento de datos:**  
Cada microservicio tiene su propia base de datos PostgreSQL independiente (`db_usuarios`, `db_proveedores`, `db_contratos`). Las referencias entre servicios (por ejemplo, `proveedor_id` en la tabla `contratos`) son UUIDs lógicos, sin claves foráneas físicas entre bases de datos. La validación de la existencia del proveedor se realiza mediante llamadas REST al servicio de Proveedores desde la capa de aplicación, antes de la inserción.

### 5.2 Decisión: MongoDB para el Servicio de Auditoría

**Alternativas consideradas:**  
- PostgreSQL con columna JSONB  
- MongoDB (NoSQL documental)  
- Elasticsearch  
- Amazon DynamoDB (descartado, no disponible localmente)

**Decisión adoptada:** MongoDB 6.0+ como motor documental para el servicio de Auditoría.

**Justificación:**

Los eventos de auditoría tienen una característica que los distingue de las entidades de negocio: su estructura varía según el tipo de operación registrada. Un evento de autenticación tiene campos distintos a un evento de cambio de estado de contrato. En un modelo relacional, esta variabilidad requeriría o bien tablas muy amplias con muchos campos nulos, o bien una jerarquía de tablas compleja. MongoDB, al ser un motor orientado a documentos, permite almacenar subdocumentos heterogéneos (`datos_anteriores`, `datos_nuevos`) sin penalizar el rendimiento.

Coulouris (2001) señala que en los sistemas distribuidos, la escalabilidad horizontal es una propiedad deseable cuando el volumen de datos crece de manera impredecible. MongoDB está diseñado para escalar horizontalmente mediante *sharding*, lo que lo hace apropiado para una colección de auditoría que puede crecer a miles de eventos por hora en ambientes productivos.

La política de inmutabilidad de los eventos de auditoría es especialmente relevante: una vez insertado, ningún evento puede modificarse ni eliminarse. Esto se garantiza en la capa de aplicación (FastAPI) al exponer únicamente operaciones de inserción en el repositorio, y se refuerza en MongoDB mediante la validación de esquema (`validationLevel: "strict"`) que rechaza documentos malformados en el momento de la inserción.

---

## 6. Nivel de Comunicación — API REST, JWT y SHA-512

### 6.1 Decisión: REST/HTTP como protocolo de comunicación

**Alternativas consideradas:**  
- REST/HTTP  
- gRPC  
- GraphQL  
- AMQP / mensajería asíncrona (RabbitMQ, Kafka)

**Decisión adoptada:** REST/HTTP con JSON como único protocolo de comunicación entre servicios y con clientes externos.

**Justificación:**

El requisito explícito del proyecto establece que la comunicación debe ser exclusivamente REST. Desde el punto de vista teórico, Coulouris (2001) clasifica REST dentro del paradigma de comunicación *request-reply* sobre HTTP, el más adecuado cuando se requiere sincronicidad, trazabilidad y facilidad de prueba (Postman en este caso). REST/JSON cumple además con el principio de transparencia de acceso de Tanenbaum (2002): el cliente no necesita conocer la ubicación física del servicio, solo la URL del endpoint expuesto por el API Gateway.

La elección de REST sobre gRPC responde a la naturaleza de las pruebas (Postman soporta REST nativamente) y a la prioridad de simplicidad en el Sprint 1. REST con OpenAPI 3.0 provee además documentación automática legible por máquinas y humanos.

### 6.2 Decisión: JSON Web Tokens (JWT) para Autenticación y Autorización

**Alternativas consideradas:**  
- Sesiones del lado del servidor (server-side sessions)  
- JWT (stateless)  
- OAuth 2.0 con servidor de autorización externo  
- API Keys

**Decisión adoptada:** JWT con algoritmo HMAC-SHA256 (HS256) para firma.

**Justificación:**

Liu (2004) caracteriza los sistemas distribuidos por la ausencia de estado compartido entre componentes: cada nodo debe ser capaz de tomar decisiones autónomas basándose en información local. Los tokens JWT son idealmente adecuados para este modelo: contienen en sí mismos toda la información necesaria para la autenticación (identidad del usuario) y la autorización (rol), sin que el servicio receptor necesite consultar un almacén de sesiones centralizado.

La estructura del JWT en este proyecto incluye los siguientes *claims*: `sub` (UUID del usuario), `username`, `rol` (uno de los tres roles), `iat` (emisión) y `exp` (expiración). Al recibir una solicitud, cada microservicio valida la firma del token y extrae el rol directamente del payload, sin llamadas adicionales al servicio de Usuarios. Esto contribuye directamente al requisito de tiempo de respuesta inferior a 2 ms.

Schäfer (2010) menciona que en sistemas de objetos distribuidos, los mecanismos de control de acceso deben integrarse de forma transparente al flujo de comunicación. En Spring Boot esto se implementa mediante un `JwtAuthenticationFilter` que intercepta cada request antes de que llegue al controlador, validando el token y estableciendo el `SecurityContext` con la información del usuario.

### 6.3 Decisión: SHA-512 para Hash de Contraseñas

**Alternativas consideradas:**  
- SHA-512 (función hash criptográfica)  
- bcrypt (función de hash adaptativa)  
- Argon2 (ganador de la competencia PHC)  
- MD5 (descartado por obsoleto e inseguro)

**Decisión adoptada:** SHA-512 como función de hash para contraseñas, aplicado en la capa de servicio antes de la persistencia.

**Justificación:**

SHA-512 es una función de hash criptográfica de la familia SHA-2, que produce un resumen de 512 bits (128 caracteres hexadecimales). Aunque algoritmos adaptativos como bcrypt o Argon2 son preferibles en ambientes productivos por su resistencia a ataques de fuerza bruta mediante hardware especializado (GPUs), SHA-512 fue definido como requisito explícito del proyecto.

Su implementación en el esquema de base de datos se refleja en la columna `password_hash CHAR(128)` de la tabla `usuarios`, con una restricción CHECK que valida el formato hexadecimal (`^[a-f0-9]{128}$`). Esto garantiza que ningún valor que no sea un hash SHA-512 válido pueda persistirse, ni siquiera desde herramientas de administración de base de datos externas a la aplicación.

La responsabilidad del hashing recae en la capa de servicio de Spring Boot (antes de cualquier operación de base de datos), siguiendo el principio de defensa en profundidad.

---

## 7. Nivel de Red — Infraestructura de Microservicios

### 7.1 Decisión: Eureka como Service Registry

**Alternativas consideradas:**  
- Eureka (Netflix OSS / Spring Cloud)  
- Consul (HashiCorp)  
- ZooKeeper  
- DNS estático (registro manual)

**Decisión adoptada:** Eureka Server de Spring Cloud Netflix.

**Justificación:**

Tanenbaum (2002) define el servicio de nombres (*naming service*) como uno de los componentes fundamentales de los sistemas distribuidos: permite que los procesos se localicen entre sí sin conocer de antemano sus ubicaciones físicas (transparencia de ubicación). Eureka implementa este principio mediante un registro dinámico: cada microservicio se registra al arrancar con su nombre lógico (ej: `servicio-contratos`) e IP/puerto actuales, y se da de baja al detenerse.

La integración de Eureka con Spring Boot es nativa a través de Spring Cloud Netflix, requiriendo únicamente la anotación `@EnableEurekaClient` en cada servicio y la anotación `@EnableEurekaServer` en el servidor de registro. Esta baja fricción de configuración es coherente con el alcance del Sprint 1.

En un entorno local (todos los servicios en la misma máquina), Eureka provee principalmente el beneficio de desacoplamiento de configuración: los servicios no necesitan conocer el puerto de sus dependencias en el código, sino que los resuelven en tiempo de ejecución a través del registro.

### 7.2 Decisión: Spring Cloud Gateway como API Gateway

**Alternativas consideradas:**  
- Spring Cloud Gateway  
- Netflix Zuul (legacy)  
- Kong  
- Nginx (proxy inverso simple)

**Decisión adoptada:** Spring Cloud Gateway.

**Justificación:**

Coulouris (2001) describe el patrón de *proxy* en sistemas distribuidos como un componente intermediario que centraliza aspectos transversales de la comunicación: enrutamiento, seguridad, logging y limitación de tasa. El API Gateway materializa este patrón en la arquitectura del proyecto.

Spring Cloud Gateway fue seleccionado sobre Zuul por su arquitectura reactiva (basada en Project Reactor), que lo hace no bloqueante y, por tanto, más eficiente bajo carga. Sus responsabilidades en este proyecto son: enrutamiento de peticiones hacia el microservicio correcto según el path (ej: `/api/usuarios/**` → servicio-usuarios), validación central del token JWT antes de reenviar la petición al servicio destino, y adición de cabeceras de contexto (información del usuario autenticado) en las peticiones reenviadas.

La validación JWT en el Gateway evita que peticiones no autenticadas lleguen a los servicios internos, reduciendo la superficie de ataque.

---

## 8. Resumen de Decisiones

| Dimensión | Tecnología elegida | Nivel |
|---|---|---|
| Estilo arquitectónico | Microservicios (6 componentes) | Sistema |
| Servicios de negocio (3) | Spring Boot 3.x + Java 17 | Aplicación |
| Servicio de Auditoría | Python 3.11 + FastAPI | Aplicación |
| BD relacional (3 instancias) | PostgreSQL 15+ | Datos |
| BD documental | MongoDB 6.0+ | Datos |
| Protocolo de comunicación | REST / HTTP + JSON | Comunicación |
| Autenticación | JWT (HMAC-SHA256) | Comunicación |
| Hash de contraseñas | SHA-512 (128 hex chars) | Comunicación |
| Service Registry | Eureka (Spring Cloud) | Red |
| API Gateway | Spring Cloud Gateway | Red |

---

## 9. Referencias Bibliográficas

**[1]** Tanenbaum, A. S. (2002). *Sistemas Distribuidos: Principios y Paradigmas*. Prentice Hall. — Referencia principal para los principios de transparencia de distribución, servicio de nombres y sincronización temporal en sistemas distribuidos. Fundamenta las decisiones de arquitectura de microservicios, Eureka y PostgreSQL con `TIMESTAMPTZ`.

**[2]** Coulouris, G., Dollimore, J., & Kindberg, T. (2001). *Sistemas Distribuidos: Conceptos y Diseño* (3.ª ed.). Addison Wesley. — Fundamenta la heterogeneidad del sistema (Java + Python), el patrón de proxy (API Gateway) y la escalabilidad horizontal de MongoDB. Base teórica del principio de separación de responsabilidades entre microservicios.

**[3]** Liu, M. L. (2004). *Computación Distribuida: Fundamentos y Aplicaciones*. Pearson Education. — Soporte teórico para los sistemas *loosely coupled*, la elección de JWT como mecanismo stateless coherente con la ausencia de estado compartido, y la validez de PostgreSQL para componentes con requisitos ACID estrictos.

**[4]** Schäfer, J. (2010). *A Programming Model and Language for Concurrent and Distributed Object-Oriented Systems*. Creative Commons. — Fundamenta el diseño del modelo de concurrencia en Spring Boot (filtro JWT como aspecto transversal) y la separación entre lógica de dominio y mecanismos de comunicación/seguridad.

---

*Documento elaborado para el Sprint 1 del proyecto de Sistemas Distribuidos.*  
*Todos los artefactos técnicos (OpenAPI YAML, DDL SQL, esquema MongoDB) son consistentes con las decisiones registradas en este documento.*
