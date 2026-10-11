package negocio.services;
import negocio.model.Cliente;
public class FidelidadService {
 public int porcentajeDescuento(String nivel){return switch(nivel){case "SENIOR"->15;case "MID"->10;default->5;};}
 public int descuentoNivel(int subtotal,Cliente c){return subtotal*porcentajeDescuento(c.nivel)/100;}
 public int puntosRedimibles(Cliente c,int solicitados){if(solicitados<0)throw new IllegalArgumentException("Puntos inválidos");return Math.min(c.devPoints,solicitados);}
 public int valorRedencion(int puntos){return puntos*200;}
 public int puntosGanados(int totalPagado){return Math.max(0,totalPagado)/20000;}
 public void registrarCompraCompletada(Cliente c,int total,int puntosRedimidos){c.comprasAcumuladas+=total;c.devPoints=Math.max(0,c.devPoints-puntosRedimidos)+puntosGanados(total);if(c.comprasAcumuladas>=1500000)c.nivel="SENIOR";else if(c.comprasAcumuladas>=500000)c.nivel="MID";else c.nivel="JUNIOR";}
}
