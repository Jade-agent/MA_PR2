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

/**
 *
 * @author jcsp0003
 */
public class AgenteAgricultor extends Agent {

    //Variables del agente
    private int cosecha, ganancias;

    private ArrayList<ACLMessage> ofertas;
    private boolean vendiendo;
    private int cVendo;
    private int nVenta;

    private AID[] agentesMercado;
    private AID consola;

    private ArrayList<String> mensajesParaConsola;

    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        vendiendo = false;
        cosecha = 0;
        nVenta = 0;
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
        addBehaviour(new LeerOfertas());
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
    public class TareaGenerarCosecha extends TickerBehaviour {

        public TareaGenerarCosecha(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (cosecha < 0) {
                System.out.println("COSECHA < 0");
                System.exit(-1);
            }
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

        public void timeOUT() {
            int idMejor = -1;
            int precio = -1;
            int ofer;
            for (int i = 0; i < ofertas.size(); i++) {
                String[] mens = ofertas.get(i).getContent().split(",");
                ofer = Integer.parseInt(mens[1]);
                if (ofer != -1 && ofer > precio) {
                    idMejor = i;
                    precio = ofer;
                }
            }
            if (idMejor != -1) {
                mensajesParaConsola.add("La mejor oferta ha sido " + precio);
                addBehaviour(new ComunicarDecision(idMejor));
            } else {
                mensajesParaConsola.add("No he recibido ninguna oferta aceptable.");
                vendiendo = false;
            }
            vendiendo = false;
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            mensaje.setSender(myAgent.getAID());
            for (int i = 0; i < ofertas.size(); i++) {
                String[] mens = ofertas.get(i).getContent().split(",");
                ofer = Integer.parseInt(mens[1]);
                if (ofer != -1 && i != idMejor) {
                    mensaje.addReceiver(ofertas.get(i).getSender());
                }
            }
            mensaje.setContent("Rechazo");
            send(mensaje);

            if (idMejor != -1) {
                String[] mens2 = ofertas.get(idMejor).getContent().split(",");
                cosecha -= cVendo;
                ganancias += Integer.parseInt(mens2[1]);

                ACLMessage mensaje2 = new ACLMessage(ACLMessage.INFORM);
                mensaje2.setSender(myAgent.getAID());
                mensaje2.addReceiver(ofertas.get(idMejor).getSender());
                mensaje2.setContent("Acepto," + cVendo + "," + mens2[1]);
                send(mensaje2);

                mensajesParaConsola.add("He ganado " + mens2[1] + " y ya he contestado a los mercados.");
            } else {
                System.out.println("Se ha colado una oferta no valida");
                System.exit(-1);
            }
        }

        @Override
        protected void onTick() {
            if (vendiendo) {
                mensajesParaConsola.add("Venta por time out");
                timeOUT();
            }

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
                    mensajesParaConsola.add("");
                    mensajesParaConsola.add("Se han localizado " + result.length + " mercados.");
                    addBehaviour(new PedirOferta());
                } else {
                    //No se han encontrado agentes Mercado
                    mensajesParaConsola.add("No se han encontardo mercados mercados.");
                    agentesMercado = null;
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
            vendiendo = true;
            ++nVenta;

            ACLMessage mensaje = new ACLMessage(ACLMessage.REQUEST);
            mensaje.setSender(myAgent.getAID());
            //Se añaden todos los agentes Agricultor
            for (AID agentesMercado1 : agentesMercado) {
                mensaje.addReceiver(agentesMercado1);
            }
            cVendo = cosecha;
            mensaje.setContent("Dime tu oferta" + "," + cVendo + "," + nVenta + "," + this.myAgent.getName());
            mensajesParaConsola.add("Dime tu oferta" + "," + cVendo + "," + nVenta + "," + this.myAgent.getName());
            send(mensaje);
        }
    }

    public class LeerOfertas extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //Formato de la respuesta: nombre,precio,nVenta
                String[] contenido = mensaje.getContent().split(",");
                mensajesParaConsola.add("Oferta de venta de " + contenido[0] + " compra por" + contenido[1] + "€");
                int n = Integer.parseInt(contenido[2]);
                if (n == nVenta) {
                    ofertas.add(mensaje);
                    if (ofertas.size() == agentesMercado.length) {
                        mensajesParaConsola.add("Me han contestado todos los Mercados");
                        int idMejor = -1;
                        int precio = -1;
                        int ofer;
                        for (int i = 0; i < ofertas.size(); i++) {
                            String[] mens = ofertas.get(i).getContent().split(",");
                            ofer = Integer.parseInt(mens[1]);
                            if (ofer != -1 && ofer > precio) {
                                idMejor = i;
                                precio = ofer;
                            }
                        }
                        if (idMejor != -1) {
                            mensajesParaConsola.add("La mejor oferta ha sido " + precio);
                            addBehaviour(new ComunicarDecision(idMejor));
                        } else {
                            mensajesParaConsola.add("No he recibido ninguna oferta aceptable.");
                            vendiendo = false;
                        }
                    }
                } else {
                    mensajesParaConsola.add("-----------ME HA LLEGADO UNA PETICION ANTIGUA------------");
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
            if (vendiendo) {
                vendiendo = false;
                ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
                mensaje.setSender(myAgent.getAID());
                int ofer;
                for (int i = 0; i < ofertas.size(); i++) {
                    String[] mens = ofertas.get(i).getContent().split(",");
                    ofer = Integer.parseInt(mens[1]);
                    if (ofer != -1 && i != this.mejor) {
                        mensaje.addReceiver(ofertas.get(i).getSender());
                    }
                }
                mensaje.setContent("Rechazo");
                send(mensaje);

                if (this.mejor != -1) {
                    String[] mens2 = ofertas.get(mejor).getContent().split(",");
                    cosecha -= cVendo;
                    ganancias += Integer.parseInt(mens2[1]);

                    ACLMessage mensaje2 = new ACLMessage(ACLMessage.INFORM);
                    mensaje2.setSender(myAgent.getAID());
                    mensaje2.addReceiver(ofertas.get(this.mejor).getSender());
                    mensaje2.setContent("Acepto," + cVendo + "," + mens2[1]);
                    send(mensaje2);

                    mensajesParaConsola.add("He ganado " + mens2[1] + " y ya he contestado a los mercados.");
                } else {
                    System.out.println("Se ha colado una oferta no valida");
                    System.exit(-1);
                }
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
