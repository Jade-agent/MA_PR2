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
public class ObjetoContenedor {

    private String nombre;
    private String valor;



    public ObjetoContenedor(String nombree, String valorr) {
        this.nombre = nombree;
        this.valor = valorr;
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

}
