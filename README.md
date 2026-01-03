# ServEat – Despliegue en Render

Este proyecto implementa el despliegue automático de la aplicación **ServEat** en la nube utilizando la plataforma **Render**, integrando control de versiones con GitHub y contenedores Docker.

El objetivo es demostrar un flujo completo de despliegue continuo (CI/CD) como parte del proyecto de Ingeniería Web.

---

## 🌐 URL del despliegue

> La aplicación estará disponible en:
>
> https://iw2025-2026-proyecto-serveat.onrender.com
>
> *(La primera carga puede tardar unos segundos debido al plan gratuito de Render)*

---

## ⚙️ Tecnologías utilizadas

- Java 21
- Spring Boot
- Vaadin
- PostgreSQL
- Docker
- Render
- GitHub

---

## 🏗️ Arquitectura de despliegue

- Repositorio GitHub como origen del código
- Render como plataforma PaaS
- Aplicación Spring Boot empaquetada en Docker
- Base de datos PostgreSQL gestionada por Render
- Variables sensibles gestionadas mediante Environment Variables


## 🔁 Despliegue automático (CI/CD)

El proyecto está configurado para que cada `push` a la rama `main` active automáticamente un nuevo despliegue en Render:

1. Render clona el repositorio desde GitHub.
2. Se construye la imagen Docker definida en el `Dockerfile`.
3. Se arrancan los servicios necesarios.
4. La aplicación queda accesible públicamente.

No se requiere intervención manual para desplegar nuevas versiones.

---

## 🗄️ Base de datos

La aplicación utiliza **PostgreSQL** como sistema gestor de base de datos en el entorno cloud.

- En local: MySQL o PostgreSQL (según configuración del perfil)
- En Render: PostgreSQL gestionado por Render

El esquema de la base de datos se genera automáticamente a partir de las entidades JPA del proyecto (`ddl-auto=update`).

Los datos de demostración se inicializan automáticamente mediante un `DataInitializer` cuando se utiliza el perfil `dev`.


## 🔐 Variables de entorno requeridas

Para el correcto funcionamiento de la aplicación, deben definirse las siguientes variables de entorno:

### Base de datos
- `DB_URL`
- `DB_USER`
- `DB_PASS`

### Perfiles
- `SPRING_PROFILES_ACTIVE`
    - `dev` → desarrollo y datos de demostración

### Seguridad
- `DEMO_PASSWORD`

### Email
- `SERVEAT_MAIL_PASSWORD`

Estas variables se configuran en Render mediante el panel de configuración del servicio (Environment Variables).

---

## 🐳 Ejecución en local con Docker

### Requisitos
- Docker
- Docker Compose

### Pasos
1. Construir la imagen:
```bash
   docker build -t serveat .
```
2.Ejecutar el contenedor:
```bash
    docker run -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=dev \
    -e DB_URL=jdbc:mysql://localhost:3306/serveat_db \
    -e DB_USER=root \
    -e DB_PASS= \
    -e DEMO_PASSWORD=demo \
    -e ADMIN_PASSWORD=admin \
    serveat
```
3.Acceder a la aplicación:
http://localhost:8080

---

## 🧪 Entornos

| Entorno | Perfil | Uso |
|------|------|----|
| Local | dev | Desarrollo y pruebas |
| Render | dev | Entorno remoto de demostración |

---

## 📚 Notas

- El despliegue en Render se utiliza como entorno remoto de desarrollo.
- El plan gratuito de Render puede suspender la aplicación tras periodos de inactividad.
- La primera petición tras la suspensión puede tardar unos segundos.

---

## 👨‍💻 Proyecto académico

Este despliegue forma parte del proyecto de la asignatura **Ingeniería Web**.

