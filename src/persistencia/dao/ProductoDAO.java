package persistencia.dao;
import negocio.model.Producto;
import java.util.List;
public interface ProductoDAO { List<Producto> listar(); Producto buscar(String id); void guardar(Producto p); void actualizar(Producto p); }
