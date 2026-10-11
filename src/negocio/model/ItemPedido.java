package negocio.model;

public class ItemPedido {
    public String productoId;
    public String nombreProducto;
    public int cantidad;
    public int precioUnitario;

    public ItemPedido() {}
    public ItemPedido(String productoId, String nombreProducto, int cantidad, int precioUnitario) {
        this.productoId=productoId; this.nombreProducto=nombreProducto;
        this.cantidad=cantidad; this.precioUnitario=precioUnitario;
    }
    public int subtotal() { return cantidad * precioUnitario; }
}
