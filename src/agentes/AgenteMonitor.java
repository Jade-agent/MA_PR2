/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import GUI.ConsolaJFrame;
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
import java.util.Arrays;
import utilidad.ObjetoPair;

/**
 *
 */
public class AgenteMonitor extends Agent {

    //Variables del agente
    private TablaJFrame guiAgricultores;
    private TablaJFrame guiMercados;

    private ObjetoPair[] pairAgricultores;
    private int pairAgricultoresLleno;
    private int totalAgricultores;

    private ArrayList<String> mensajesParaConsola;

    private AID consola;
    private AID[] agentesAgricultor;

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
        addBehaviour(new TareaBuscarAgricultores(this, 12000));
        addBehaviour(new TareaRecepcionOperacion());

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

    public class PedirGanancias extends OneShotBehaviour {

        @Override
        public void action() {
            //Se envía la operación a todos los agentes Agricultor
            pairAgricultores = new ObjetoPair[agentesAgricultor.length];
            totalAgricultores = agentesAgricultor.length;
            System.out.println("El vector tiene tam " + agentesAgricultor.length);
            pairAgricultoresLleno = 0;
            totalAgricultores = agentesAgricultor.length;

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

    public class TareaRecepcionOperacion extends CyclicBehaviour {

        @Override
        public void action() {
            //Recepción de la información para realizar la operación
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //procesamos el mensaje
                String[] contenido = mensaje.getContent().split(",");
                if ("Agricultor".equals(contenido[0])) {
                    ObjetoPair o = new ObjetoPair(contenido[1], contenido[2]);
                    pairAgricultores[pairAgricultoresLleno] = o;
                    ++pairAgricultoresLleno;
                    addBehaviour(new RellenarTabla("Agricultor"));
                } else {

                }
            } else {
                block();
            }
        }
    }

    public class RellenarTabla extends OneShotBehaviour {

        private String tipo;

        public RellenarTabla(String tipoo) {
            this.tipo = tipoo;
        }

        private void burbuja() {
            int i, j;
            ObjetoPair aux;
            for (i = 0; i < pairAgricultores.length - 1; i++) {
                for (j = 0; j < pairAgricultores.length - i - 1; j++) {
                    if (pairAgricultores[j + 1] != null && pairAgricultores[j] != null) {
                        if (Integer.parseInt(pairAgricultores[j + 1].getValor()) < Integer.parseInt(pairAgricultores[j].getValor())) {
                            aux = pairAgricultores[j + 1];
                            pairAgricultores[j + 1] = pairAgricultores[j];
                            pairAgricultores[j] = aux;
                        }
                    }
                }
            }
        }

        @Override
        public void action() {
            burbuja();
            if ("Agricultor".equals(this.tipo)) {
                guiAgricultores.dispose();
                guiAgricultores = new TablaJFrame("Agricultor");
                for (int z = totalAgricultores - 1; z >= 0; z--) {
                    if (pairAgricultores[z] != null) {
                        guiAgricultores.insertarFila(pairAgricultores[z].getNombre(), pairAgricultores[z].getValor());
                    }
                }
            }
        }
    }

}
