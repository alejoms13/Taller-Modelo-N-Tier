package negocio.services;

import negocio.model.*;
import persistencia.dao.*;
import java.util.*;

public class PedidoService {
 private final ProductoDAO productos; private final ClienteDAO clientes; private final PedidoDAO pedidos;
 private final FidelidadService fidelidad; private final InventarioService inventario;
 public PedidoService(ProductoDAO p,ClienteDAO c,PedidoDAO o,FidelidadService f,InventarioService i){productos=p;clientes=c;pedidos=o;fidelidad=f;inventario=i;}
 public List<Pedido> listar(){return pedidos.listar();}
 public synchronized Pedido crear(String clienteId,List<Map<String,Object>> lineas,int puntos,String modalidad){
  Cliente c=clientes.buscar(clienteId);if(c==null)throw new IllegalArgumentException("Cliente no encontrado");
  if(pedidos.listar().stream().anyMatch(o->o.clienteId.equals(clienteId)&&o.estado.equals("PENDIENTE_PAGO")))throw new IllegalArgumentException("El cliente ya tiene un pedido pendiente de pago");
  if(lineas==null||lineas.isEmpty())throw new IllegalArgumentException("El pedido debe tener productos");
  Pedido o=new Pedido();o.id="O"+UUID.randomUUID().toString().substring(0,8).toUpperCase();o.clienteId=clienteId;o.modalidad="LLEVAR".equals(modalidad)?"LLEVAR":"MESA";
  Set<String> ids=new HashSet<>();int subtotal=0;
  for(Map<String,Object> linea:lineas){String pid=String.valueOf(linea.get("productoId"));Object q=linea.get("cantidad");int qty=q instanceof Number?((Number)q).intValue():Integer.parseInt(String.valueOf(q));if(!ids.add(pid))throw new IllegalArgumentException("No repita productos en líneas separadas");Producto p=productos.buscar(pid);if(p==null)throw new IllegalArgumentException("Producto no encontrado: "+pid);inventario.validarStock(p,qty);o.items.add(new ItemPedido(p.id,p.nombre,qty,p.precio));subtotal+=p.precio*qty;}
  o.subtotal=subtotal;o.descuentoNivel=fidelidad.descuentoNivel(subtotal,c);int afterLevel=subtotal-o.descuentoNivel;
  o.puntosRedimidos=fidelidad.puntosRedimibles(c,puntos);o.descuentoPuntos=Math.min(afterLevel,fidelidad.valorRedencion(o.puntosRedimidos));o.total=Math.max(0,afterLevel-o.descuentoPuntos);
  // Reserve stock when the order is accepted, preventing overselling in this single-process application.
  for(ItemPedido item:o.items){Producto p=productos.buscar(item.productoId);inventario.descontar(p,item.cantidad);productos.actualizar(p);}
  pedidos.guardar(o);return o;
 }
 public synchronized Pedido cambiarEstado(String id,String nuevo){
  Pedido o=pedidos.buscar(id);if(o==null)throw new IllegalArgumentException("Pedido no encontrado");
  List<String> estados=List.of("PENDIENTE_PAGO","EN_PREPARACION","LISTO","ENTREGADO");
  int actual=estados.indexOf(o.estado),destino=estados.indexOf(nuevo);
  if(destino<0||destino!=actual+1)throw new IllegalArgumentException("Transición inválida. Avance al siguiente estado: "+String.join(" → ",estados));
  o.estado=nuevo;
  if(nuevo.equals("ENTREGADO")){Cliente c=clientes.buscar(o.clienteId);if(c==null)throw new IllegalStateException("Cliente no encontrado");fidelidad.registrarCompraCompletada(c,o.total,o.puntosRedimidos);o.puntosGanados=fidelidad.puntosGanados(o.total);clientes.actualizar(c);}
  pedidos.actualizar(o);return o;
 }
}
