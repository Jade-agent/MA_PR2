/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

/**
 *
 * @author jcsp0003 Esqueleto de agente para la estructura general que deben
 * tener todos los agentes
 */
public class AgenteAgricultor extends Agent {

    //Variables del agente
    private int cosecha, ganancias;
    private boolean negociando;
    private AID consola;
    private ArrayList<String> mensajesParaConsola;

    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        cosecha = 0;
        ganancias = 0;
        negociando = false;
        consola = null;
        mensajesParaConsola = new ArrayList();

        //Registro del agente en las Páginas Amarrillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Productor");
        sd.setName("Agricultor");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
        }

        System.out.println("Se inicia la ejecución del agente: " + this.getName());
        //Añadir las tareas principales
        addBehaviour(new TareaGenerarCosecha(this, 5000));
        addBehaviour(new TareaBuscarConsola(this, 5000));
        addBehaviour(new TareaEnvioConsola(this,5000));
    }

    @Override
    protected void takeDown() {
        //Desregristo del agente de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }
        //Liberación de recursos, incluido el GUI

        //Despedida
        System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }

    //Métodos de trabajo del agente
    //Clases internas que representan las tareas del agente
    public class TareaGenerarCosecha extends TickerBehaviour {

        public TareaGenerarCosecha(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            ++cosecha;
            System.out.println("El agente " + myAgent.getName() + " ha recogido la cosecha, ahora tiene " + cosecha);
            mensajesParaConsola.add("El agente " + myAgent.getName() + " ha recogido la cosecha, ahora tiene " + cosecha);
        }
    }

    public class TareaBuscarConsola extends TickerBehaviour {

        //Se buscarán agentes consola y operación
        public TareaBuscarConsola(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            DFAgentDescription template;
            ServiceDescription sd;
            DFAgentDescription[] result;

            //Busca agente consola
            template = new DFAgentDescription();
            sd = new ServiceDescription();
            sd.setName("Consola");
            template.addServices(sd);

            try {
                result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    consola = result[0].getName();
                } else {
                    //No se ha encontrado agente consola
                    consola = null;
                }
            } catch (FIPAException fe) {
            }
        }
    }
    
    public class TareaEnvioConsola extends TickerBehaviour {

        public TareaEnvioConsola(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            ACLMessage mensaje;
            if (consola != null) {
                if (!mensajesParaConsola.isEmpty()) {
                    mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(consola);
                    mensaje.setContent(mensajesParaConsola.remove(0));
            
                    myAgent.send(mensaje);
                }
                else {
                    //Si queremos hacer algo si no tenemos mensajes
                    //pendientes para enviar a la consola
                }
            }
        }
    }

}
