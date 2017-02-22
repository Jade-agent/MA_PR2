/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilidad;

import jade.lang.acl.ACLMessage;

/**
 *
 * @author jcsp0003
 */
public class ObjetoContenedor implements Comparable<ObjetoContenedor> {

    private String nombre;
    private String valor;

    private int cosecha;
    private int oferta;
    private ACLMessage mensaje;

    public ObjetoContenedor(String nombree, String valorr) {
        this.nombre = nombree;
        this.valor = valorr;
    }

    public ObjetoContenedor(String nombree, int cosechaa, int ofertaa, ACLMessage mensajee) {
        this.nombre = nombree;
        this.cosecha = cosechaa;
        this.oferta = ofertaa;
        this.mensaje = mensajee;
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

    public int getCosecha() {
        return this.cosecha;
    }

    public int getOferta() {
        return this.oferta;
    }

    public float getBeneficio() {
        if (cosecha == 0) {
            return -1;
        }
        return (float) cosecha / oferta;
    }

}
