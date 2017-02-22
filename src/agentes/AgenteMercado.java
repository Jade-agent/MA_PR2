/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import utilidad.ObjetoContenedor;

/**
 *
 * @author pedroj Esqueleto de agente para la estructura general que deben tener
 * todos los agentes
 */
public class AgenteMercado extends Agent {

    //Variables del agente
    private int capital;
    private int nVenta;
    private int stock;
    private ArrayList<String> mensajesParaConsola;

    private ArrayList<ObjetoContenedor> ofertas;

    private AID consola;
    private AID[] agentesAgricultor;

    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        capital = 0;
        nVenta = 0;
        stock = 0;
        consola = null;
        mensajesParaConsola = new ArrayList();

        //Registro del agente en las Páginas Amarrillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Productor");
        sd.setName("Mercado");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
        }

        System.out.println("Se inicia la ejecución del agente: " + this.getName());
        //Añadir las tareas principales
        addBehaviour(new TareaRecibirInversion(this, 3000));
        addBehaviour(new TareaBuscarConsola(this, 5000));
        addBehaviour(new TareaBuscarAgricultores(this, 5000));
        addBehaviour(new TareaEnvioConsola());
        addBehaviour(new LeerPeticionStock());
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
    public class TareaRecibirInversion extends TickerBehaviour {

        public TareaRecibirInversion(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            int x = (int) (Math.random() * 7) + 2;
            capital += x;
            mensajesParaConsola.add("Ha recibido una inversion de " + x + " ahora tiene " + capital);
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

    public class LeerPeticionStock extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                ACLMessage respuesta = mensaje.createReply();
                respuesta.setPerformative(ACLMessage.QUERY_IF);
                respuesta.setContent("Mercado," + this.getAgent().getName() + "," + capital);
                send(respuesta);
            } else {
                block();
            }
        }
    }

    public class TareaBuscarAgricultores extends TickerBehaviour {

        public TareaBuscarAgricultores(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            DFAgentDescription template;
            ServiceDescription sd;
            DFAgentDescription[] result;

            //Busca agentes Agricultor
            template = new DFAgentDescription();
            sd = new ServiceDescription();
            sd.setName("Agricultor");
            template.addServices(sd);

            try {
                result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    agentesAgricultor = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agentesAgricultor[i] = result[i].getName();
                    }
                    ++nVenta;
                    addBehaviour(new PedirOferta());
                } else {
                    //No se han encontrado agentes Agricultor
                    agentesAgricultor = null;
                }
            } catch (FIPAException fe) {
            }
        }
    }

    public class PedirOferta extends OneShotBehaviour {

        @Override
        public void action() {
            //Se envía la operación a todos los agentes Agricultor
            ofertas = new ArrayList();

            ACLMessage mensaje = new ACLMessage(ACLMessage.REQUEST);
            mensaje.setSender(myAgent.getAID());
            //Se añaden todos los agentes Agricultor
            for (AID agentesAgricultor1 : agentesAgricultor) {
                mensaje.addReceiver(agentesAgricultor1);
            }
            mensaje.setContent("Dime tu oferta" + "," + capital + "," + nVenta + "," + this.myAgent.getName());
            send(mensaje);
        }
    }

}