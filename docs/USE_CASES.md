# Casos de uso principales

## Configuración inicial

La aplicación comprueba que tenga el rol de SMS predeterminado, el rol de filtrado de llamadas y el permiso de contactos. Desde `Estado`, la persona puede solicitar cada requisito y volver a la aplicación para actualizar su estado.

## Bandeja de entrada

1. La aplicación importa los SMS existentes una sola vez cuando los permisos están activos.
2. La bandeja agrupa los mensajes por remitente y muestra el último mensaje de cada conversación.
3. Al tocar una conversación se abre el hilo ordenado por fecha, con separadores de día y hora.
4. Desde el hilo se puede responder, borrar un SMS individual o borrar la conversación completa.
5. Una pulsación prolongada sobre una conversación permite borrarla desde la bandeja.

## Cuarentena

1. Los SMS que coinciden con una palabra configurada y las llamadas bloqueadas aparecen en esta pantalla.
2. El filtro conserva la selección entre el listado y el detalle; las llamadas son la selección inicial.
3. Cada tipo se ordena por recepción y se agrupa visualmente por fecha.
4. Los SMS se pueden abrir en un hilo de solo lectura, restaurar a la bandeja o descartar.
5. Las llamadas se pueden descartar.
6. El menú superior permite borrar todos los elementos del tipo activo después de confirmar.
7. Los listados vacíos muestran un estado explícito para distinguir una cuarentena sin datos de un filtro sin resultados.

## Palabras de filtrado

1. Se puede añadir una keyword desde el campo superior.
2. Las keywords se guardan normalizadas para que el filtrado no dependa de mayúsculas.
3. La lista muestra cada keyword capitalizada y permite borrarla desde su menú contextual.

## Arquitectura de la interfaz

- `ui/home/HomeScreen.kt` coordina navegación, estado de selección y acciones que necesitan la base de datos.
- `ui/home/InboxScreens.kt` contiene la bandeja y los hilos normales.
- `ui/home/QuarantineScreens.kt` contiene el listado, filtros y detalle de cuarentena.
- `ui/home/KeywordsStatusScreens.kt` contiene keywords y estado de permisos.
- `ui/home/HomeComponents.kt` contiene componentes visuales y utilidades compartidas.
- `ui/theme` contiene el tema Material 3 y sus tokens de color.

Las pantallas reciben `Flow` y callbacks; la capa de UI no crea ni conoce implementaciones concretas de Room fuera del coordinador de navegación.
