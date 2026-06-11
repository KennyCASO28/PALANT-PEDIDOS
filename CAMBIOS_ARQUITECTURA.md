# Independencia de Arquero y Vectores - Cambios Implementados

## Resumen
Se ha refactorizado la arquitectura de personalización de prendas para hacer arquero completamente independiente del sistema de camiseta, y separar los vectores del arquero del boceto de la camiseta.

## Cambios Realizados

### 1. **Nuevo ArqueroRenderer**
- **Archivo**: `src/main/java/org/example/component/renderer/ArqueroRenderer.java`
- **Tipo**: Renderer independiente que extiende `BaseGarmentRenderer`
- **Característica Principal**: Carga vectores SOLO desde `/vectors/arquero/` usando la bandera `isArquero=true` en `GarmentAssetManager.getShirtPath()`
- **Estructura**: Idéntica a `ShortsRenderer` - completamente desacoplada de `ShirtRenderer`

### 2. **Refactorización de PrendaVisualizer**
- **Cambio de tipo**: `ShirtRenderer arqueroShirtRenderer` → `ArqueroRenderer arqueroShirtRenderer`
- **Método actualizado**: `ensureArqueroInitialized()` ahora instancia `ArqueroRenderer` en lugar de `ShirtRenderer`
- **Vectores separados**: Método `updateArqueroLayers()` usa vectores de `/vectors/arquero/` exclusivamente

### 3. **Separación de Vectores**
- **Camiseta**: Carga desde `/vectors/[genero]/[corte]/[largo].svg`
- **Arquero**: Carga desde `/vectors/arquero/[largo].svg` 
- **Stock**: Carga desde `/vectors/[genero]/shores/[corte]/...`
- **Medias**: Carga desde `/vectors/[genero]/medias/...`

Cada uno tiene su propio estado (`camisetaState` vs `arqueroState`) con vectores y capas completamente independientes.

## Ventajas

✅ **Arquero super independiente**: Como `ShortsRenderer`, con su propio renderer dedicado  
✅ **Vectores desacoplados**: Los cambios en arquero NO afectan la camiseta  
✅ **Boceto independiente**: Visualización de arquero completamente separada  
✅ **Escalabilidad**: Fácil agregar más tipos de prendas siguiendo este patrón  
✅ **Mantenibilidad**: Código más limpio sin acoplamiento

## Estructura de Archivos de Vectores

```
/vectors/
├── mujer/
│   ├── cuellos/
│   ├── cero/
│   ├── corta/
│   ├── larga/
│   ├── 3_4/
│   ├── redondo/
│   ├── cuadrado/
│   ├── extras/
│   ├── escudo/
│   ├── marca/
│   ├── medias/
│   ├── numeros/
│   └── shores/
├── varon/
│   ├── (misma estructura)
└── arquero/     ← NUEVO: Vectores específicos para arquero
    ├── cero.svg
    ├── corta.svg
    ├── larga.svg
    └── 3_4.svg
```

## Cómo Usar

### Para editar camiseta:
```java
visualizer.setActiveDesign(false);  // Usa ShirtRenderer + camisetaState
```

### Para editar arquero:
```java
visualizer.setActiveDesign(true);   // Usa ArqueroRenderer + arqueroState
```

Cada diseño mantiene:
- Capas de usuario independientes (compartidas por sincronización, no acopladas)
- Vectores de prenda independientes
- Estado de colores, números, etc. completamente separado

## Métodos clave

- `ArqueroRenderer.updateArqueroLayers()` - Carga vectores de `/vectors/arquero/`
- `PrendaVisualizer.setActiveDesign(boolean)` - Cambia entre camiseta/arquero
- `GarmentAssetManager.getShirtPath(genero, corte, largo, isArquero)` - Resuelve ruta de vectores
