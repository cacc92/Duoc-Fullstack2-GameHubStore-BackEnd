# GameHub Store · Backend de microservicios

Ecosistema de **11 microservicios Spring Boot** más **Eureka** y **Spring Cloud Gateway**: el backend de referencia que el caso semestral llama «el ecosistema de microservicios construido en Desarrollo FullStack I». El frontend React habla solo con el Gateway; cada servicio tiene su propia base de datos y se comunica con los demás con **OpenFeign**.

## Arquitectura

```
                         ┌──────────────┐
  React (5173) ────────▶ │  api-gateway │ 8080   CORS · rutas /api/** · Swagger de todos
                         └──────┬───────┘
                                │ lb://<servicio>  (pregunta a Eureka dónde está cada uno)
          ┌─────────────────────┼──────────────────────────────────────────────┐
          ▼                     ▼                                              ▼
  auth-service 8081     category-service 8083 ◀──▶ product-service 8084 ──▶ inventory-service 8085
     │                  promotion-service 8086         ▲     ▲                    ▲
     ▼                                                 │     │                    │
  user-service 8082     order-service 8087 ───────────┘     │                    │
                          │  │  │  └──▶ promotion-service    │                    │
                          │  │  └─────────────────────────────────────────────────┘
                          │  └──▶ payment-service 8088
                          └─────▶ shipping-service 8089
  review-service 8090 ──▶ order-service      warranty-service 8091 ──▶ order-service

                    eureka-server 8761: registro de todos los servicios
```

Las flechas son llamadas con Feign a rutas `/internal/**`. El Gateway no expone esas rutas y cada una exige la cabecera `X-Clave-Interna`.

## Microservicios

| Servicio | Puerto | Responsabilidad | Llama a |
| --- | --- | --- | --- |
| `eureka-server` | 8761 | Registro de servicios (panel en http://localhost:8761) | — |
| `api-gateway` | 8080 | Única entrada: enruta `/api/**`, aplica CORS y junta la documentación | todos, por Eureka |
| `auth-service` | 8081 | Credenciales, login, registro, renovación del JWT y activación de cuentas | user-service |
| `user-service` | 8082 | Perfiles: datos de contacto y dirección de despacho | — |
| `category-service` | 8083 | Categorías del catálogo | product-service |
| `product-service` | 8084 | Productos, precios, ofertas y especificaciones | category-service, inventory-service |
| `inventory-service` | 8085 | Stock por producto y movimientos (ingreso, salida, ajuste) | — |
| `promotion-service` | 8086 | Cupones con vigencia, tope de descuento y tope de usos | — |
| `order-service` | 8087 | Carrito, órdenes y bandeja del operador: coordina la compra | product, inventory, promotion, payment, shipping |
| `payment-service` | 8088 | Pagos simulados (débito, crédito y transferencia) | — |
| `shipping-service` | 8089 | Despachos, número de seguimiento y eventos de entrega | — |
| `review-service` | 8090 | Reseñas y su moderación | order-service |
| `warranty-service` | 8091 | Solicitudes de garantía | order-service |

`comun/` no es un servicio: es una librería con lo que los 11 comparten (formato de errores, validación del JWT, clave interna, configuración de Feign, Swagger y los datos de ejemplo).

Cada servicio sigue el patrón **Controller → Service → Repository**, con sus paquetes `model`, `repository`, `dto`, `client` (Feign), `service`, `controller` y `config`.

## Requisitos

- JDK 21.
- Para levantarlo todo con contenedores: Docker Desktop (o Colima) con Docker Compose.
- No hace falta instalar Maven: el proyecto trae el wrapper (`./mvnw`).

> Trabaja en una carpeta local, no dentro de Google Drive: las carpetas `target/` y la caché de Maven generan miles de archivos que Drive intenta sincronizar.

## Ejecutar

### Opción 1: Docker Compose (todo de una vez)

```bash
docker compose up --build
```

La primera vez compila los 13 módulos (unos minutos). Después:

- Gateway: http://localhost:8080
- Swagger de todos los servicios: http://localhost:8080/swagger-ui.html (elige el servicio arriba a la derecha)
- Eureka: http://localhost:8761

### Opción 2: desde el IDE o la terminal

Primero compila todo una vez:

```bash
./mvnw install -DskipTests
```

Luego levanta en este orden, cada uno en su terminal (o con el botón Run del IDE sobre cada clase `*Application`):

1. `eureka-server` y espera a que responda http://localhost:8761.
2. Los 11 servicios, en cualquier orden. Por ejemplo:

```bash
./mvnw -pl product-service spring-boot:run
```

3. `api-gateway`.

Cada servicio aparece en el panel de Eureka a los pocos segundos de arrancar, y desde ese momento el Gateway lo encuentra.

Para cambiar un puerto, usa la variable `PORT` (por ejemplo `PORT=8092`). Eureka avisa la dirección nueva y no hay que tocar nada más.

## Datos y cuentas de prueba

En local, cada servicio usa **H2 en memoria** y se llena al arrancar con su clase `config/DatosIniciales`. Los datos de los servicios calzan entre sí: los toman de `comun/.../semilla/DatosDeEjemplo.java`, que define 17 cuentas, 7 categorías, 13 productos y 18 órdenes en todos los estados. Al detener un servicio, sus datos se borran.

Contraseña de todas las cuentas: **`clave123`**.

| Correo | Rol |
| --- | --- |
| `ana@gamehub.test` | ADMIN |
| `luis@gamehub.test` | OPERADOR |
| `camila@gamehub.test` | CLIENTE (tiene órdenes en todos los estados) |
| `pedro@gamehub.test` | CLIENTE desactivado (el login responde 403) |

Cupones: `GAMER10` (vigente), `SETUP15` (programado) y `CYBER25` (vencido).

## Rutas del Gateway

| Prefijo | Servicio |
| --- | --- |
| `/api/auth/**`, `/api/admin/usuarios/**` | auth-service |
| `/api/usuarios/**` | user-service |
| `/api/categorias/**`, `/api/admin/categorias/**` | category-service |
| `/api/productos/**`, `/api/admin/productos/**` | product-service |
| `/api/productos/{id}/resenas`, `/api/resenas/**`, `/api/admin/resenas/**` | review-service |
| `/api/inventario/**`, `/api/operador/inventario/**` | inventory-service |
| `/api/promociones/**`, `/api/admin/promociones/**` | promotion-service |
| `/api/carrito/**`, `/api/ordenes/**`, `/api/operador/ordenes/**`, `/api/operador/resumen` | order-service |
| `/api/pagos/**`, `/api/operador/pagos/**` | payment-service |
| `/api/despachos/**`, `/api/operador/despachos/**` | shipping-service |
| `/api/garantias/**`, `/api/operador/garantias/**` | warranty-service |
| `/docs/<servicio>/v3/api-docs` | documentación OpenAPI de cada servicio |

Las rutas están en `api-gateway/src/main/resources/application.yml`; las de la documentación, en `DocumentacionConfig.java`. El filtro global `RegistroSolicitudes` deja en el log cada petición con el servicio que respondió, el código y el tiempo, y le asigna un `X-Request-Id`.

## Seguridad

- `auth-service` firma el JWT con `JWT_SECRETO`. Los demás servicios **no** le preguntan nada a auth-service: validan el token con el mismo secreto y leen el id, el nombre y el rol del token (`JwtFiltro` en `comun`).
- Cada servicio vuelve a revisar el rol en su `SecurityConfig`, aunque el frontend ya oculte lo que un rol no puede hacer.
- El CORS se configura **solo en el Gateway** (`CORS_ORIGENES`). Si un servicio también agregara cabeceras CORS, el navegador las vería duplicadas y bloquearía la respuesta.
- Las rutas `/internal/**` exigen `X-Clave-Interna` (`CLAVE_INTERNA`). Feign la agrega sola.

Todos los errores tienen la misma forma: `{ "mensaje": "...", "errores": { "campo": "..." } }`.

| Código | Cuándo |
| --- | --- |
| 400 | Datos inválidos o una regla del negocio no se cumple (cupón vencido, stock negativo, garantía fuera de plazo) |
| 401 | Falta el token, venció o las credenciales son incorrectas |
| 403 | El rol no tiene permiso, la cuenta está desactivada o falta la clave interna |
| 404 | El recurso o la ruta no existen |
| 409 | Conflicto con el estado actual (despachar sin pago, SKU o correo repetido, sin stock) |
| 503 | Otro microservicio no respondió a tiempo |

## Comunicación entre servicios

- **OpenFeign:** cada servicio declara una interfaz `@FeignClient(name = "<servicio>")` en su paquete `client`. Feign pregunta a Eureka dónde está ese servicio. Timeouts: 2 s para conectar y 5 s para responder (`application.yml`).
- **Errores remotos:** si el otro servicio responde con error, `FeignInternoConfig` (en `comun`) lo convierte en una excepción con el mismo código y mensaje. Por ejemplo, «El cupón CYBER25 venció el…» llega intacto desde promotion-service hasta el navegador. Si el otro servicio no responde, la respuesta es 503.
- **Compra repartida entre servicios:** order-service revisa el stock, valida el cupón, registra el pago en payment-service y, si quedó aprobado, descuenta el stock en inventory-service. Si el stock se acabó entre medio, **revierte el pago** (compensación) y responde 409.
- **Composición en el frontend:** algunas vistas juntan datos de varios servicios, como pide el caso. El catálogo toma el producto de product-service, el stock de inventory-service y el puntaje de review-service. Mis órdenes junta order-service, review-service y warranty-service. Esa composición vive en `frontend/src/services/`.

## Pruebas

```bash
./mvnw verify
```

Cada servicio tiene pruebas unitarias de su capa `service` con **JUnit 5 y Mockito** (los repositorios y los clientes Feign son dobles de prueba, con la estructura Given / When / Then) y una prueba de integración con **MockMvc** que levanta el servicio con su base H2 y revisa seguridad, validaciones y la clave interna. La cobertura de cada módulo queda en `<servicio>/target/site/jacoco/index.html`; se mide sobre la lógica (se excluyen `config/` y la clase `*Application`).

Última ejecución: **286 pruebas, todas correctas**.

| Servicio | Pruebas | Cobertura (instrucciones) |
| --- | --- | --- |
| auth-service | 25 | 100 % |
| user-service | 14 | 100 % |
| category-service | 11 | 89 % |
| product-service | 39 | 99 % |
| inventory-service | 33 | 99 % |
| promotion-service | 29 | 100 % |
| order-service | 41 | 94 % |
| payment-service | 23 | 100 % |
| shipping-service | 22 | 100 % |
| review-service | 23 | 100 % |
| warranty-service | 26 | 100 % |

Todas superan el 80 % que pide la pauta de FullStack I.

## Configuración y perfiles

Cada servicio tiene `application.yml` (local, con H2 y Eureka) y `application-prod.yml` (se activa con `SPRING_PROFILES_ACTIVE=prod`).

| Variable | Por defecto (local) | Uso |
| --- | --- | --- |
| `PORT` | el del servicio | Puerto HTTP |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |
| `EUREKA_HOSTNAME` | `localhost` | Nombre con que el servicio se anota en Eureka (en Docker, el nombre del contenedor) |
| `JWT_SECRETO` | secreto de desarrollo | Clave del JWT, igual en todos los servicios (al menos 32 caracteres) |
| `CLAVE_INTERNA` | clave de desarrollo | Clave de las rutas `/internal/**`, igual en todos los servicios |
| `CORS_ORIGENES` | `http://localhost:5173` | Solo en el Gateway: orígenes del frontend, separados por coma |
| `DATOS_INICIALES` | `true` | Carga los datos de ejemplo si la base está vacía |
| `DB_URL`, `DB_USUARIO`, `DB_CLAVE` | — | PostgreSQL (solo perfil `prod`) |
| `<SERVICIO>_SERVICE_URL` | — | Perfil `prod`: URL pública de cada servicio que se llama, por ejemplo `PRODUCT_SERVICE_URL` |

## Despliegue en la nube

El perfil `prod` está pensado para servicios con URL fija, como Render.com, donde no hay Eureka: cada servicio y el Gateway reciben las URL de los otros por variables de entorno, y `lb://<servicio>` sigue funcionando gracias al *simple discovery* de Spring Cloud.

- Cada servicio se publica como un *Web Service* con entorno Docker, usando el `Dockerfile` de esta carpeta y el argumento `MODULO=<servicio>`.
- La base de datos puede ser una sola de Supabase: cada servicio usa su propio esquema (`hibernate.default_schema`) y como máximo 2 conexiones.
- Son 13 servicios: en planes gratuitos se suspenden con la inactividad y tardan en despertar. Para la defensa conviene la ejecución local con Docker Compose.

## Estructura

```
backend/
├── pom.xml                 Proyecto padre: Spring Boot 4.0.9, Spring Cloud 2025.1.4 y los 14 módulos
├── comun/                  Librería compartida (no arranca sola)
├── eureka-server/
├── api-gateway/
├── auth-service/  …  warranty-service/
│   └── src/main/java/cl/gamehub/<servicio>/
│       ├── controller/     Rutas REST: públicas, de administración, del operador e internas
│       ├── service/        Reglas del negocio
│       ├── repository/     Acceso a datos (JpaRepository)
│       ├── model/          Entidades JPA
│       ├── dto/            Lo que entra y sale de la API, con Bean Validation
│       ├── client/         Clientes Feign hacia otros servicios
│       └── config/         Seguridad y datos de ejemplo
├── Dockerfile              Una receta para los 13 servicios (argumento MODULO)
└── docker-compose.yml      El ecosistema completo
```
