# Ticketeo

Aplicación de gestión de eventos desarrollada con **Spring Boot, Spring Data JPA y PostgreSQL**.

## 📋 Requisitos Previos

Antes de comenzar, asegúrate de tener instalados los siguientes componentes en tu sistema local:

- **Java 21** o superior: [Descargar JDK 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) (o versiones posteriores).
- **PostgreSQL**: [Descargar PostgreSQL](https://www.postgresql.org/download/).
- **Git** para clonar el código.
- Un IDE o editor de texto compatible (IntelliJ IDEA, Eclipse, VS Code).
- *Nota: No es estrictamente necesario instalar Maven en el sistema, ya que el proyecto incluye el ejecutable de Maven Wrapper (`mvnw` y `mvnw.cmd`).*

---

## ⚙️ Configuración de la Base de Datos (PostgreSQL)

El proyecto hace uso de **Spring Data JPA** (Hibernate) para interactuar automáticamente con PostgreSQL. Para que la aplicación funcione, es necesario inicializar la base de datos localmente.

Sigue estos pasos:

1. Abre tu cliente de **PostgreSQL** de preferencia (por ejemplo pgAdmin, DBeaver o el terminal `psql`).
2. Crea una base de datos nueva con el siguiente nombre:
   ```sql
   CREATE DATABASE "TicketeoDB";
   ```
3. Verifica las credenciales de conexión en el proyecto. Abre el archivo localizado en `src/main/resources/application.properties` y asegúrate de que el usuario, contraseña y puerto coinciden con los que configuraste al instalar PostgreSQL:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/TicketeoDB
   spring.datasource.username=postgres  # <- Cambia por tu usuario local de postgres
   spring.datasource.password=12345     # <- Cambia por tu contraseña local de postgres
   ```

> **Nota importante:** La propiedad `spring.jpa.hibernate.ddl-auto=update` se encarga de que Spring Data JPA infiera y construya (o actualice) las tablas en PostgreSQL a partir de tus entidades de forma automática en cuanto despliegues la aplicación.

---

## 🚀 Instalación y Ejecución

Sigue las siguientes instrucciones una vez tengas la base de datos configurada:

1. **Clona el repositorio:**
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd Ticketeo
   ```

2. **Compila y ejecuta la aplicación:**
   Puedes levantar el proyecto usando el propio entorno y utilizando el Maven Wrapper.

   *En terminal de Windows:*
   ```cmd
   mvnw.cmd spring-boot:run
   ```

   *En terminal de Linux / macOS:*
   ```bash
   ./mvnw spring-boot:run
   ```
   *(Si el script no tiene permisos, puedes otorgárselos ejecutando `chmod +x mvnw` previamente).*

3. **Acceso a la plataforma:**
   Una vez el servidor tomcat y Spring arranquen de forma exitosa, abre cualquier navegador web y dirígete a:
   👉 **[http://localhost:8080](http://localhost:8080)**

---

## Credenciales de Administrador por Defecto

Para pruebas, hay un usuario de Spring Security por defecto con rol de administrador:

- **Usuario:** `admin`
- **Contraseña:** `admin`
- **Rol:** `ADMIN`
