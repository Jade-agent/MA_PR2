/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilidad;

/**
 *
 * @author jcsp0003
 */
public class ObjetoContenedor implements Comparable<ObjetoContenedor> {

    private String nombre;
    private String valor;

    private int cosecha;
    private int oferta;
    private int venta;

    public ObjetoContenedor(String nombree, String valorr) {
        this.nombre = nombree;
        this.valor = valorr;
    }

    public ObjetoContenedor(String nombree, int cosechaa, int ofertaa, int ventaa) {
        this.nombre = nombree;
        this.cosecha = cosechaa;
        this.oferta = ofertaa;
        this.venta = ventaa;
    }

    @Override
    public int compareTo(ObjetoContenedor o) {
        if (Integer.parseInt(this.valor) < Integer.parseInt(o.getValor())) {
            return -1;
        } else {
            return 1;
        }
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getValor() {
        return this.valor;
    }

    void setNombre(String nombree) {
        this.nombre = nombree;
    }

    void setValor(String valorr) {
        this.valor = valorr;
    }

    private int getCosecha() {
        return this.cosecha;
    }

    private int getOferta() {
        return this.oferta;
    }

    private int getVenta() {
        return venta;
    }
}
