package persistencia.dao;
import negocio.model.Pedido;
import java.util.List;
public interface PedidoDAO { List<Pedido> listar(); Pedido buscar(String id); void guardar(Pedido p); void actualizar(Pedido p); }
