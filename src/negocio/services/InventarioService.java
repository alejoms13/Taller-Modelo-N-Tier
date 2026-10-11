package negocio.services;
import negocio.model.Producto;
public class InventarioService {
 public void validarStock(Producto p,int cantidad){if(cantidad<=0)throw new IllegalArgumentException("La cantidad debe ser mayor que cero");if(cantidad>p.stock)throw new IllegalArgumentException("Stock insuficiente para "+p.nombre+" (disponible: "+p.stock+")");}
 public void descontar(Producto p,int cantidad){validarStock(p,cantidad);p.stock-=cantidad;}
}
