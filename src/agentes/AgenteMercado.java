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

    private boolean comprando;

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
        comprando = false;
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
        addBehaviour(new TareaBuscarAgricultores(this, 6000));
        addBehaviour(new TareaEnvioConsola());
        addBehaviour(new LeerPeticionStock());
        addBehaviour(new LeerOfertas());
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
                respuesta.setContent("Mercado," + this.getAgent().getName() + "," + stock);
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

        public void compraDeEmergencia() {
            int mejor = -1;
            float beneficio = -1;
            for (int i = 0; i < ofertas.size(); i++) {
                if (ofertas.get(i).getBeneficio() != -1 && beneficio < ofertas.get(i).getBeneficio()) {
                    beneficio = ofertas.get(i).getBeneficio();
                    mejor = i;
                }
            }
            if (mejor != -1) {
                mensajesParaConsola.add("Acepto la oferta Cosecha: " + ofertas.get(mejor).getCosecha() + " Oferta: " + ofertas.get(mejor).getOferta());
            } else {
                mensajesParaConsola.add("NO acepto ninguna oferta");
            }
            addBehaviour(new ComunicarDecision(mejor));
        }

        @Override
        protected void onTick() {
            DFAgentDescription template;
            ServiceDescription sd;
            DFAgentDescription[] result;

            if (comprando) {//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<---------------------------- HACER COMPRA DE EMERGENCIA
                System.out.println("Compra de emergencia");
                compraDeEmergencia();
            }
            mensajesParaConsola.add("");
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
            comprando = true;

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

    public class LeerOfertas extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //Formato del mensaje: nombre,unidades,precio,nVenta
                String[] contenido = mensaje.getContent().split(",");
                mensajesParaConsola.add("Oferta de venta de " + contenido[0] + " vende " + contenido[1] + " por " + contenido[2] + "€");
                int n = Integer.parseInt(contenido[3]);
                if (n == nVenta) {
                    int cantidad = Integer.parseInt(contenido[1]);
                    int ofer = Integer.parseInt(contenido[2]);
                    ofertas.add(new ObjetoContenedor(contenido[0], cantidad, ofer, mensaje));
                    if (agentesAgricultor.length == ofertas.size()) {
                        mensajesParaConsola.add("Me han contestado todos los agricultores");
                        int mejor = -1;
                        float beneficio = -1;
                        for (int i = 0; i < ofertas.size(); i++) {
                            if (ofertas.get(i).getBeneficio() != -1 && beneficio < ofertas.get(i).getBeneficio()) {
                                beneficio = ofertas.get(i).getBeneficio();
                                mejor = i;
                            }
                        }
                        if (mejor != -1) {
                            mensajesParaConsola.add("Acepto la oferta Cosecha: " + ofertas.get(mejor).getCosecha() + " Oferta: " + ofertas.get(mejor).getOferta());
                        } else {
                            mensajesParaConsola.add("NO acepto ninguna oferta");
                        }
                        addBehaviour(new ComunicarDecision(mejor));
                    }
                } else {
                    mensajesParaConsola.add("ME HA LLEGADO UNA PETICION ANTIGUA");
                }

            } else {
                block();
            }
        }
    }

    public class ComunicarDecision extends OneShotBehaviour {

        private final int mejor;

        public ComunicarDecision(int mejorr) {
            this.mejor = mejorr;
        }

        @Override
        public void action() {
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            mensaje.setSender(myAgent.getAID());
            for (int i = 0; i < ofertas.size(); i++) {
                if (i != mejor) {
                    mensaje.addReceiver(ofertas.get(i).getMensaje().getSender());
                }
            }
            mensaje.setContent("Rechazo");
            send(mensaje);
            if (mejor != -1) {
                ACLMessage mensaje2 = new ACLMessage(ACLMessage.INFORM);
                mensaje2.setSender(myAgent.getAID());
                mensaje2.addReceiver(ofertas.get(mejor).getMensaje().getSender());
                mensaje2.setContent("Acepto," + ofertas.get(mejor).getCosecha() + "," + ofertas.get(mejor).getOferta());
                send(mensaje2);
                stock += ofertas.get(mejor).getCosecha();
                capital -= ofertas.get(mejor).getOferta();
            }
            mensajesParaConsola.add("He respondido a los agricultores mi decision");
        }
    }

}
