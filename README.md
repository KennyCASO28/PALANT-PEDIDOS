# 👕 PALANT Pedidos (Desktop)

**PALANT Pedidos** es una aplicación de escritorio avanzada para la gestión de órdenes y visualización dinámica de prendas deportivas. 
Esta herramienta está diseñada específicamente para la administración interna y el diseño visual interactivo de los productos de confección deportiva.

---

## ✨ Características Principales

*   **Visualizador Vectorial Dinámico (JavaFX):** Un potente motor de renderizado propio basado en capas SVG. Permite visualizar en tiempo real diferentes cortes (cuadrado, ranglan, licra, pantaloneta), cuellos (V, redondo), largos de manga, colores de tela, estampados de marcas y dorsales.
*   **Gestor de Órdenes Internas:** Herramientas para la revisión y el procesamiento de pedidos, diseñadas para agilizar el flujo de trabajo de la empresa.
*   **Empaquetado y Despliegue:** Incluye scripts (`CREAR_INSTALADOR.bat`, `installer_palant.iss`) para compilar fácilmente la aplicación y generar un instalador nativo `.exe` para Windows.

---

## 🛠️ Tecnologías Utilizadas

*   **Lenguaje:** Java (JDK 17+)
*   **Interfaz Gráfica (UI):** JavaFX
*   **Procesamiento y Configuración:** Jackson (Parseo JSON para la configuración de assets gráficos)
*   **Gestor de Dependencias y Construcción:** Maven (`pom.xml`)
*   **Base de Datos:** SQL Server / MySQL (Conexión JDBC al esquema `utpPRUEBA`)
*   **Instaladores:** Inno Setup Compiler

---

## 📂 Estructura del Proyecto

```text
PALANT-PEDIDOS/
├── src/main/java/org/example/  # Lógica del renderizador SVG, Controladores JavaFX, Modelos
├── src/main/resources/         # Archivos FXML (Vistas), JSON de configuración y vectores SVG
├── installer_palant.iss        # Script de empaquetado para Inno Setup
├── restore_schema.sql          # Script de restauración de la Base de Datos
└── pom.xml                     # Dependencias y configuración de Maven
```

---

## 🚀 Instalación y Uso

### 1. Base de Datos
1. Abre tu gestor de base de datos (por ejemplo, SQL Server Management Studio o MySQL Workbench).
2. Ejecuta el archivo `restore_schema.sql` para crear las tablas necesarias e insertar la data maestra inicial.

### 2. Ejecución desde el Entorno de Desarrollo
Para compilar y ejecutar la aplicación localmente usando Maven:
```bash
mvn clean install
mvn javafx:run
```

### 3. Generación del Instalador y Lanzadores
* Puedes usar el archivo `PalantLauncher.bat` para iniciar rápidamente la aplicación si ya ha sido compilada.
* Para generar el instalador final de Windows, abre el archivo `installer_palant.iss` con **Inno Setup** y presiona "Compile" para obtener tu archivo ejecutable (`.exe`).

---

## 🎓 Contexto Académico

Este proyecto ha sido desarrollado como parte de un proyecto académico en la **Universidad Tecnológica del Perú (UTP)**. Aplica patrones de arquitectura de software y metodologías ágiles enfocadas en la optimización tecnológica de una empresa de confección deportiva.
