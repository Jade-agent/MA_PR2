/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import GUI.TablaJFrame;
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
 */
public class AgenteMonitor extends Agent {

    //Variables del agente
    private TablaJFrame guiAgricultores;
    private TablaJFrame guiMercados;

    private ObjetoContenedor[] pairAgricultores;
    private int pairAgricultoresLleno;
    private int totalAgricultores;

    private ObjetoContenedor[] pairMercados;
    private int pairMercadosLleno;
    private int totalMercados;

    private ArrayList<String> mensajesParaConsola;

    private AID consola;
    private AID[] agentesAgricultor;
    private AID[] agentesMercado;

    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        mensajesParaConsola = new ArrayList();

        //Configuración del GUI
        guiAgricultores = new TablaJFrame("Agricultor");
        guiMercados = new TablaJFrame("Mercado");

        System.out.println("Se inicia la ejecución del agente: " + this.getName());
        //Añadir las tareas principales
        addBehaviour(new TareaBuscarConsola(this, 5000));
        addBehaviour(new TareaEnvioConsola(this, 5000));
        addBehaviour(new TareaBuscarAgricultores(this, 15000));
        addBehaviour(new TareaBuscarMercado(this, 15000));
        addBehaviour(new TareaRecepcionContestacion());

    }

    @Override
    protected void takeDown() {
        //Liberación de recursos, incluido el GUI
        guiAgricultores.dispose();
        guiMercados.dispose();

        //Despedida
        System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }

    //Métodos de trabajo del agente
    //Clases internas que representan las tareas del agente
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
                } else {
                    //Si queremos hacer algo si no tenemos mensajes
                    //pendientes para enviar a la consola
                }
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
                    addBehaviour(new PedirGanancias());
                } else {
                    //No se han encontrado agentes Agricultor
                    agentesAgricultor = null;
                }
            } catch (FIPAException fe) {
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
                    addBehaviour(new PedirStock());
                } else {
                    //No se han encontrado agentes Mercado
                    agentesMercado = null;
                }
            } catch (FIPAException fe) {
            }
        }
    }

    public class PedirGanancias extends OneShotBehaviour {

        @Override
        public void action() {
            //Se envía la operación a todos los agentes Agricultor
            pairAgricultores = new ObjetoContenedor[agentesAgricultor.length];
            totalAgricultores = agentesAgricultor.length;
            System.out.println("El vector tiene tam " + agentesAgricultor.length);
            pairAgricultoresLleno = 0;

            ACLMessage mensaje = new ACLMessage(ACLMessage.QUERY_IF);
            mensaje.setSender(myAgent.getAID());
            //Se añaden todos los agentes Agricultor
            for (AID agentesAgricultor1 : agentesAgricultor) {
                mensaje.addReceiver(agentesAgricultor1);
            }
            mensaje.setContent("Dime tus ganancias.");
            send(mensaje);
        }
    }

    public class PedirStock extends OneShotBehaviour {

        @Override
        public void action() {
            //Se envía la operación a todos los agentes Mercado
            pairMercados = new ObjetoContenedor[agentesMercado.length];
            totalMercados = agentesMercado.length;
            System.out.println("El vector tiene tam " + agentesMercado.length);
            pairMercadosLleno = 0;

            ACLMessage mensaje = new ACLMessage(ACLMessage.QUERY_IF);
            mensaje.setSender(myAgent.getAID());
            //Se añaden todos los agentes Mercado
            for (AID agentesMercado1 : agentesMercado) {
                mensaje.addReceiver(agentesMercado1);
            }
            mensaje.setContent("Dime tu stock.");
            send(mensaje);
        }
    }

    public class TareaRecepcionContestacion extends CyclicBehaviour {

        @Override
        public void action() {
            //Recepción de la información para realizar la operación
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //procesamos el mensaje
                String[] contenido = mensaje.getContent().split(",");
                if ("Agricultor".equals(contenido[0])) {
                    pairAgricultores[pairAgricultoresLleno] = new ObjetoContenedor(contenido[1], contenido[2]);
                    ++pairAgricultoresLleno;
                    addBehaviour(new RellenarTabla("Agricultor"));
                } else {
                    pairMercados[pairMercadosLleno] = new ObjetoContenedor(contenido[1], contenido[2]);
                    ++pairMercadosLleno;
                    addBehaviour(new RellenarTabla("Mercado"));
                }
            } else {
                block();
            }
        }
    }

    public class RellenarTabla extends OneShotBehaviour {

        private final String tipo;

        public RellenarTabla(String tipoo) {
            this.tipo = tipoo;
        }

        @Override
        public void action() {
            if ("Agricultor".equals(this.tipo)) {
                for (int z = totalAgricultores - 1; z >= 0; z--) {
                    if (pairAgricultores[z] != null) {
                        guiAgricultores.insertarFila(pairAgricultores[z].getNombre(), pairAgricultores[z].getValor());
                    }
                }
                guiAgricultores.burbuja();
            } else {
                for (int z = totalMercados - 1; z >= 0; z--) {
                    if (pairMercados[z] != null) {
                        guiMercados.insertarFila(pairMercados[z].getNombre(), pairMercados[z].getValor());
                    }
                }
                guiMercados.burbuja();
            }
        }
    }

}
