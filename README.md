VelvetAura

Sistema web para una tienda de cosméticos: catálogo de productos, clientes, pedidos con cola de prioridad e historial de eventos. Proyecto final de Estructuras de Datos 2026-2.

Autoras: Sofia Espinosa Bedoya y Nikol Mosquera Mosquera

1. Qué hace (MVP)
Código	Funcionalidad	Estructura de datos
F01	Registrar producto	HashMap por SKU
F02	Buscar producto por SKU	HashMap por SKU (O(1))
F03	Filtrar productos por categoría	Recorrido del HashMap
F04	Reponer stock	HashMap por SKU
F05	Registrar cliente	HashMap por identificación
F06	Crear pedido (valida cliente, productos y stock)	HashMap por id de pedido
F07	Ver la cola de atención	PriorityQueue (EXPRESS primero, luego por llegada)
F08	Atender el siguiente pedido	PriorityQueue (poll)
F09	Avanzar el estado de un pedido	Stack (se guarda el cambio)
F10	Deshacer el último cambio de estado	Stack (LIFO)
F11	Historial de eventos	Lista enlazada simple hecha a mano (Lista / Nodo)

No incluye: pagos, login, cancelar pedidos, editar o eliminar registros, imágenes, reportes.

2. Tecnologías
Java 21
Spring Boot (Spring MVC + Spring Data JPA)
Thymeleaf + HTML + CSS (sin JavaScript)
PostgreSQL
Gradle (se incluye el wrapper gradlew, no hace falta instalar Gradle)
JUnit 5 + Mockito para las pruebas

Arquitectura MVC organizada en capas: model, repository, service, controller y templates.

3. Requisitos previos

Instalar antes de empezar:

JDK 21 (por ejemplo Temurin 21: https://adoptium.net). Durante la instalación marcar las opciones de agregar al PATH y definir JAVA_HOME.
PostgreSQL (probado con la versión 18) y, opcionalmente, pgAdmin para manejar la base de datos.
Git.
Opcional: IntelliJ IDEA (la Community sirve).

Comprobar que Java quedó bien instalado:

java -version

Debe mostrar la versión 21.

4. Instalación paso a paso (desde cero)
4.1 Descargar el proyecto
git clone https://github.com/sofiaespinosab/velvetaura.git
cd velvetaura

La rama con el proyecto completo es main.

4.2 Crear la base de datos

La aplicación crea las tablas sola; solo hay que crear una base de datos vacía llamada velvetaura.

Con pgAdmin: clic derecho en Databases > Create > Database... > nombre velvetaura > Save.

O con la terminal:

psql -U postgres -c "CREATE DATABASE velvetaura;"
4.3 Configurar la conexión

Abrir el archivo src/main/resources/application.properties y dejarlo así, cambiando la contraseña por la de su PostgreSQL:

properties
spring.application.name=velvetaura

spring.datasource.url=jdbc:postgresql://localhost:5432/velvetaura
spring.datasource.username=postgres
spring.datasource.password=SU_CONTRASEÑA

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
Si PostgreSQL usa otro puerto o usuario, cambiar 5432 y postgres.
ddl-auto=update hace que Hibernate cree las tablas (producto, cliente, pedido, detalle_pedido, evento) la primera vez que se ejecuta. No hace falta importar ningún script SQL.

La contraseña real no se sube a GitHub. El repositorio trae application.properties con la contraseña vacía o de ejemplo: hay que escribir la suya.

4.4 Dependencias

Todas las dependencias se descargan solas al compilar, porque están declaradas en build.gradle. No hay que instalar nada a mano. Las que usa el proyecto:

spring-boot-starter-web
spring-boot-starter-thymeleaf
spring-boot-starter-data-jpa
spring-boot-starter-validation
org.postgresql:postgresql
spring-boot-starter-test   (JUnit 5 y Mockito, solo para pruebas)

Para descargarlas y compilar de una vez:

Windows: .\gradlew.bat build -x test
Linux / macOS: ./gradlew build -x test

(Se necesita conexión a internet la primera vez.)

5. Ejecutar la aplicación

Desde la terminal (en la carpeta del proyecto):

Windows: .\gradlew.bat bootRun
Linux / macOS: ./gradlew bootRun

Desde IntelliJ: abrir el proyecto (File > Open > carpeta velvetaura), esperar a que Gradle termine de cargar, abrir VelvetauraApplication.java y pulsar el triángulo verde.

Cuando en la consola aparezca Started VelvetauraApplication, abrir en el navegador:

http://localhost:8080/productos

Páginas disponibles:

Ruta	Qué muestra
/productos	F01 a F04: registrar, buscar, filtrar y reponer stock
/clientes	F05: registrar y listar clientes
/pedidos	F06 a F10: crear pedidos, cola de atención, avanzar estado y deshacer
/eventos	F11: historial de eventos

Para detener la aplicación: Ctrl + C en la terminal, o el cuadro rojo en IntelliJ.

6. Cómo probarla (orden sugerido)
En /productos registrar un producto (ej. SKU LAB01, Labial rojo, MAQUILLAJE, 25000, stock 10) y probar buscar, filtrar y reponer stock.
En /clientes registrar un cliente (ej. identificación 123).
En /pedidos crear pedidos con prioridad NORMAL y EXPRESS y ver cómo el EXPRESS sale primero en la cola.
Pulsar Atender siguiente pedido, luego Avanzar estado y Deshacer último cambio de estado.
En /eventos ver el historial.
7. Pruebas unitarias
.\gradlew.bat test        (Windows)
./gradlew test            (Linux / macOS)

Se ejecutan 6 pruebas de PedidoServiceTest (creación de pedido, stock insuficiente, orden de la cola, deshacer, deshacer sin cambios y avance de estado inválido). Usan Mockito, así que no necesitan la base de datos. El reporte queda en build/reports/tests/test/index.html.

8. Archivos adicionales y base de datos
No hay scripts .sql que importar: las tablas las crea la aplicación con spring.jpa.hibernate.ddl-auto=update. Lo único manual es crear la base vacía velvetaura (paso 4.2).
El .zip de la entrega contiene el proyecto completo y este README. Si se desea, se puede incluir una copia de application.properties con la contraseña vacía, que se debe copiar en src/main/resources/ y completar como en el paso 4.3.
Los recursos estáticos (hoja de estilos) están en src/main/resources/static/css/estilo.css y se cargan automáticamente.
9. Estructura del proyecto
src/main/java/com/example/velvetaura/
├── model/          Producto, Cliente, Pedido, DetallePedido, Evento y enums
├── estructuras/    Nodo y Lista (lista enlazada hecha a mano)
├── repository/     Interfaces JPA que hablan con PostgreSQL
├── service/        Lógica de negocio y estructuras de datos en memoria
└── controller/     Controladores web (reciben los formularios)

src/main/resources/
├── templates/      Páginas Thymeleaf (productos, clientes, pedidos, eventos)
├── static/css/     estilo.css
└── application.properties

src/test/java/      PedidoServiceTest
10. Cómo funciona (resumen)

PostgreSQL guarda todo de forma permanente. Al iniciar, cada servicio carga sus datos en estructuras en memoria (@PostConstruct) y trabaja con ellas:

ProductoService: HashMap<String, Producto> con el SKU como llave. Registrar y buscar es O(1).
ClienteService: HashMap con la identificación como llave.
PedidoService: HashMap<Long, Pedido>, una PriorityQueue<Pedido> con los pendientes (EXPRESS primero, luego fecha y luego id) y una Stack<CambioEstado> para deshacer.
EventoService: lista enlazada propia; cada evento nuevo se inserta en la cabeza (O(1)), así el más reciente aparece primero.
11. Limitaciones conocidas
La pila de "deshacer" vive solo en memoria: al reiniciar la aplicación queda vacía.
Un solo usuario administrador, sin login y sin manejo de concurrencia.
Solo se puede avanzar un pedido que esté EN_PREPARACION o ENVIADO.
12. Problemas frecuentes
Problema	Solución
Cannot find a Java installation ... languageVersion=21	Instalar JDK 21 y reiniciar IntelliJ / la terminal.
Connection refused o password authentication failed	Revisar que PostgreSQL esté encendido y que username y password de application.properties sean correctos.
database "velvetaura" does not exist	Crear la base vacía (paso 4.2).
Port 8080 was already in use	Cerrar la otra aplicación que usa el puerto, o agregar server.port=8081 en application.properties.
Página Whitelabel Error Page (404)	Verificar que existan los 4 controladores en controller/ y volver a ejecutar.
La página se ve sin estilos	Verificar que exista src/main/resources/static/css/estilo.css y recargar con Ctrl + F5.
