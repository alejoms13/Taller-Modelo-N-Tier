# Café Overflow — Dev & Coffee Lounge

Aplicación web académica para el taller de Arquitectura de Software (modelo N-Tier / C4).

## Tecnologías
- Java 17 o superior, usando `com.sun.net.httpserver.HttpServer`.
- HTML, CSS y JavaScript sin frameworks.
- Persistencia local en archivos JSON.
- No utiliza frameworks ni dependencias externas.

## Ejecutar
Desde la raíz del proyecto:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out presentation.Server
```

Abra `http://localhost:8080` en el navegador.

En Windows PowerShell, compile con:
```powershell
Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName } | Set-Content sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp out presentation.Server
```

Los datos se almacenan en `data/productos.json`, `data/clientes.json` y `data/pedidos.json`. Se crean datos iniciales si los archivos no existen. Haga una copia de `data/` antes de pruebas que modifiquen información.

## Arquitectura
- `presentation`: servidor HTTP, rutas, controladores y archivos estáticos.
- `negocio`: entidades, reglas y servicios.
- `persistencia`: interfaces DAO e implementación basada en JSON.
- `web`: interfaz de usuario.

Las solicitudes web llegan a Java mediante HTTP. Los controladores llaman servicios; los servicios utilizan interfaces DAO; los DAO realizan la lectura/escritura JSON.

## Reglas implementadas
- Descuento: Junior 5 %, Mid 10 %, Senior 15 %.
- Ascenso: Mid desde $500.000 y Senior desde $1.500.000 de compras completadas acumuladas.
- DevPoints: un punto por cada $20.000 de consumo completado (división entera); cada punto redimido descuenta $200.
- No se aceptan cantidades superiores al stock.
- Un cliente no puede crear otro pedido mientras tenga uno en estado `PENDIENTE_PAGO`.
- Los puntos y el acumulado histórico se actualizan cuando el pedido se marca como `ENTREGADO`, que en esta implementación representa la compra completada.
- Los importes monetarios se representan en pesos enteros (COP).
- La redención se limita al saldo disponible y nunca puede producir un total negativo.

## Notas académicas y decisiones explícitas
El enunciado no especifica la forma de pago, cancelación ni devolución. Para mantener el alcance, no se integra una pasarela de pagos. La transición `PENDIENTE_PAGO -> EN_PREPARACION -> LISTO -> ENTREGADO` se realiza desde la interfaz de gestión de pedidos; el último estado marca la compra como completada. Los puntos se calculan sobre el valor efectivamente pagado después de descuentos y redención. El orden del cálculo es: subtotal, descuento por nivel, redención de puntos y total final. Estas son decisiones de implementación para cerrar ambigüedades del taller, no reglas textuales adicionales del docente.

## Pruebas manuales recomendadas
1. Crear un pedido con cantidades válidas.
2. Intentar pedir más unidades de las disponibles: debe rechazarse.
3. Crear otro pedido del mismo cliente mientras tiene uno pendiente de pago: debe rechazarse.
4. Avanzar un pedido por sus estados hasta `ENTREGADO`; comprobar acumulado y DevPoints.
5. Verificar el descuento aplicable a clientes Junior, Mid y Senior.
6. Reiniciar el servidor y comprobar que los datos siguen guardados en JSON.

## Limitaciones
- El servidor es de demostración académica y escucha en `localhost:8080`.
- Los archivos JSON no son una base de datos transaccional; las escrituras se sincronizan en proceso, pero no ofrecen concurrencia multiinstancia ni recuperación transaccional ante fallos.
- No incluye autenticación ni integración de pagos porque no son requisitos explícitos del taller.
