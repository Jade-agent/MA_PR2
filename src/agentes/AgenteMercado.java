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
    private int stock;
    private ArrayList<String> mensajesParaConsola;
    
    private boolean comprando;
    
    private AID consola;
    
    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        capital = 0;
        stock = 0;
        consola = null;
        mensajesParaConsola = new ArrayList();
        comprando = false;

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
        addBehaviour(new TareaEnvioConsola());
        addBehaviour(new LeerPeticionStock());
        addBehaviour(new LeerPeticionOfertas());
        addBehaviour(new LeerDecision());
        addBehaviour(new LeerFinal());
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
            if (capital < 0) {
                System.out.println("CAPITAL < 0");
                System.exit(-1);
            }
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
    
    public class LeerPeticionOfertas extends CyclicBehaviour {
        
        @Override
        public void action() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //Formato del mensaje: null,cantidad,nVenta,nombre
                String[] contenido = mensaje.getContent().split(",");
                if (!comprando) {
                    if (capital > 0) {
                        //Formato de la respuesta: nombre,precio,nVenta
                        ACLMessage respuesta = mensaje.createReply();
                        respuesta.setPerformative(ACLMessage.REQUEST);
                        float precioMax = (float) (capital * 0.6);
                        int oferta = (int) (Math.random() * precioMax)+1;
                        respuesta.setContent(this.getAgent().getName() + "," + oferta + "," + contenido[2]);
                        send(respuesta);
                        comprando = true;
                        mensajesParaConsola.add("Oferta de compra del agente " + contenido[0] + " vendo por " + oferta);
                    } else {
                        //enviar mensaje nulo
                        ACLMessage respuesta = mensaje.createReply();
                        respuesta.setPerformative(ACLMessage.REQUEST);
                        respuesta.setContent(this.getAgent().getName() + "," + -1 + "," + contenido[2]);
                        send(respuesta);
                    }
                } else {
                    //enviar mensaje nulo
                    mensajesParaConsola.add("Estoy negociando, no me molestes");
                    ACLMessage respuesta = mensaje.createReply();
                    respuesta.setPerformative(ACLMessage.REQUEST);
                    respuesta.setContent(this.getAgent().getName() + "," + -1 + "," + contenido[2]);
                    send(respuesta);
                }
            } else {
                block();
            }
        }
        
    }
    
    public class LeerDecision extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                String[] contenido = mensaje.getContent().split(",");
                if("Acepto".equals(contenido[0])){
                    mensajesParaConsola.add("Han aceptado mi oferta");
                    stock+=Integer.parseInt(contenido[1]);
                    capital-=Integer.parseInt(contenido[2]);
                }else{
                    mensajesParaConsola.add("Han rechazado mi oferta");
                }
                comprando=false;
            } else {
                block();
            }
        }

    }
    
    public class LeerFinal extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
               this.myAgent.doDelete();
            } else {
                block();
            }
        }
    }
}
