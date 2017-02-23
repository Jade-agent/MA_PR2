/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;

/**
 *
 * @author jcsp0003
 */
public class AgenteAgricultor extends Agent {

    //Variables del agente
    private int cosecha, ganancias;

    private AID[] agentesMercado;
    private AID consola;

    private ArrayList<String> mensajesParaConsola;

    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        cosecha = 0;
        ganancias = 0;
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
        addBehaviour(new TareaBuscarMercado(this, 15000));
        addBehaviour(new TareaEnvioConsola());
        addBehaviour(new LeerPeticionGanancias());
    }

    @Override
    protected void takeDown() {
        //Desregristo del agente de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }

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

    public class TareaEnvioConsola extends CyclicBehaviour {

        @Override
        public void action() {
            if (consola != null && !mensajesParaConsola.isEmpty()) {
                ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
                mensaje.setSender(myAgent.getAID());
                mensaje.addReceiver(consola);
                mensaje.setContent(mensajesParaConsola.remove(0));

                myAgent.send(mensaje);
            } else {
                block();
            }
        }
    }

    public class LeerPeticionGanancias extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //System.out.println("Soy el agricultor me estan preguntando mis ganancias");
                ACLMessage respuesta = mensaje.createReply();
                respuesta.setPerformative(ACLMessage.QUERY_IF);
                respuesta.setContent("Agricultor," + this.getAgent().getName() + "," + ganancias);
                send(respuesta);
            } else {
                block();
            }
        }

    }

    public class TareaBuscarMercado extends TickerBehaviour {

        public TareaBuscarMercado(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            DFAgentDescription template;
            ServiceDescription sd;
            DFAgentDescription[] result;

            //Busca agentes Mercado
            template = new DFAgentDescription();
            sd = new ServiceDescription();
            sd.setName("Mercado");
            template.addServices(sd);

            try {
                result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    agentesMercado = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agentesMercado[i] = result[i].getName();
                    }
                    mensajesParaConsola.add("Se han localizado " + result.length + " mercados.");
                } else {
                    //No se han encontrado agentes Mercado
                    mensajesParaConsola.add("No se han encontardo mercados mercados.");
                    agentesMercado = null;
                }
            } catch (FIPAException fe) {
            }
        }
    }

}
