# Documento de Contexto Maestro: PALANT-PEDIDOS

### 1. Perfil del Proyecto
* **Nombre:** PALANT-PEDIDOS
* **Stack Tecnológico:** Java, JavaFX, Maven, Supabase (Cloud DB).
* **Objetivo:** Sistema de gestión de pedidos y personalización de indumentaria deportiva.
* **Patrón de Arquitectura:** MVC (Modelo-Vista-Controlador) con capas de servicio y DAO.

### 2. Mapa de Estructura (src/main/java/org/example)
| Capa | Responsabilidad | Archivos/Paquetes Clave |
| :--- | :--- | :--- |
| **Component** | Elementos visuales del lienzo. | `GraphicLayer`, `ImageLayer`, `TextLayer`. |
| **Controller** | Lógica de flujo y manejo de vistas (UI). | `PedidoController`, `ShellController`. |
| **DAO** | Acceso a Base de Datos (Supabase). | `PedidoDAO`, `ClienteDAO`, `ConexionDB`. |
| **DTO** | Objetos de datos para serialización. | `OrderRequestDTO`, `PrendaStateDTO`. |
| **Helper** | Lógica operativa (Cálculos, UI, Sincronización). | `BezierInteractionService`, `ShapeGeometryEngine`. |
| **Model** | Entidades y estados del dominio. | `PrendaState`, `ShapeType`, `DetallePedido`. |
| **Pattern** | Lógica de acciones (Patrón Comando). | `ICommand`, `TransformCommand`. |
| **Service** | Lógica de negocio pesada/exportación. | `PdfExportService`, `OrderService`, `SvgExportService`. |

### 3. Convenciones Técnicas
* **UI:** Definida en archivos `.fxml` ubicados en `src/main/resources`.
* **Persistencia:** La lógica de guardado de estado se centraliza en `service/save/StateMapper.java` y `ProjectManager.java`.
* **Assets:** Los recursos gráficos (SVG/PNG) están en `src/main/resources/vectors`, organizados por `genero` > `tipo`.

### 4. Instrucciones para la IA
* Cuando se trabaje en un error o nueva función, referenciar primero la capa correspondiente según la tabla anterior.
* No generar código completo si no es necesario; pedir específicamente el archivo que se debe modificar para optimizar el consumo de tokens.
* Priorizar la arquitectura actual (JavaFX + Maven).