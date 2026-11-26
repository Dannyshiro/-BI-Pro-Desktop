# BI Pro Desktop

JavaFX 21 + Spring Boot 3.3 + JPA + Lombok + SQLite. Requiere JDK 17 o superior y Maven.

## Ejecutar
```
mvn clean javafx:run
```
En IntelliJ: abre la carpeta como proyecto Maven y ejecuta `Launcher`
(activa *Annotation Processing* para Lombok).

## Usuarios de demostración (clave 1234)
- atorres: Administrador
- lramos: Vendedor
- cmedina: Administrador

La base de datos se crea en `~/.bi-pro/bipro.db` con datos de demostración
(120 días de ventas). Se restablece desde Configuración.

## Flujo
Abrir caja → Vender → Reponer (orden de compra sugerida → recibir) → Cerrar caja.

## Estructura
- `model/` entidades JPA y enums
- `repository/` Spring Data
- `service/` reglas de negocio
- `controller/` pantallas JavaFX (+ `dialogo/`)
- `ui/` utilidades visuales
- `resources/view/` FXML, `resources/css/app.css` tema rojo/negro/blanco
