package negocio.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Pedido {
    public String id;
    public String clienteId;
    public List<ItemPedido> items = new ArrayList<>();
    public String estado = "PENDIENTE_PAGO";
    public int subtotal;
    public int descuentoNivel;
    public int descuentoPuntos;
    public int total;
    public int puntosRedimidos;
    public int puntosGanados;
    public String fecha = Instant.now().toString();
    public String modalidad = "MESA";

    public Pedido() {}
}
