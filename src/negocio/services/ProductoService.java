package negocio.services;
import negocio.model.Producto; import persistencia.dao.ProductoDAO; import java.util.*;
public class ProductoService {
 private final ProductoDAO dao; public ProductoService(ProductoDAO dao){this.dao=dao;}
 public List<Producto> listar(){return dao.listar();}
 public Producto buscar(String id){Producto p=dao.buscar(id);if(p==null)throw new IllegalArgumentException("Producto no encontrado");return p;}
}
