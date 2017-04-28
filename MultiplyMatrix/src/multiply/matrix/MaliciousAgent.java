package multiply.matrix;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MaliciousAgent extends CountingAgent {
	
	protected void setup() {
        // set action_interval time
        Random r = new Random();
        setInterval(r.nextInt(1500 - 500) + 500);

        addBehaviour(new MaliciousCount());
        addBehaviour(new HandleFinalResult());
	}

	private class MaliciousCount extends CyclicBehaviour {

		private int step = 0;
        private MessageTemplate mt;
        private ACLMessage msg = null;
        private ObjectMapper mapper = new ObjectMapper();
        private String result;

        @Override
        public void action() {
            // TODO Auto-generated method stub
            switch (step) {

                case 0:
                    // Searching for Distrubutor Agent
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("matrix-distributor");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] distributors = DFService.search(myAgent, template);

                        if (distributors.length == 1) {
                            setDistributorAgent(distributors[0].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Sending ready message
                    System.out.println(
                        "Malicious Agent " + myAgent.getAID().getName()
                        + " is ready."
                    );
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(getDistributorAgent());
                    cfp.setContent("multiply-matrix");
                    cfp.setConversationId("multiply-matrix");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare template to get response
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("multiply-matrix"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;

                    break;

                case 1:
                    // waiting for data from distributor
                    msg = myAgent.receive(mt);
                    // check if reply received
                    if (msg != null) {
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            String data = msg.getContent();
                            // deserialize data and go to counting

                            try {
                                // check if agent is not broken
                            	if(worksFine()) {
                            		// convert Json to DataPackage object
	                                DataPackage dpkg = mapper.readValue(data, DataPackage.class);
	                                // set multiply result in DataPackage object
	                                Random r = new Random();
	                                dpkg.setResult(r.nextInt(1000 + 500) - 500);
	                                // convert DataPackage object with result to Json
	                                result = mapper.writeValueAsString(dpkg);
	                                System.out.println(
                                        "Malicious Agent " + myAgent.getAID().getName()
                                        + " finished counting and is waiting "
                                        + getInterval().toString()
                                        + " before sending result."
	                                );
	                                // waiting before result send
	                                myAgent.doWait(getInterval());
	                                step = 3;
                            	}
                            	else {
                            		// agent is broken so it need to send back
                            		// data to distributor
                            		result = data;
                            		step = 2;
                            	}
                            } catch (JsonMappingException e) {
                                e.printStackTrace();
                                
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                        	step = 0;
                        }
                    }
                    else {
                        block();
                    }
                    break;

                case 2:
                	// send failure message to distributor when agent is broken
                    ACLMessage failMsg = msg.createReply();
                    failMsg.setContent(result);
                    failMsg.setPerformative(ACLMessage.FAILURE);
                    myAgent.send(failMsg);
                    System.out.println(
                        "Malicious Agent " + myAgent.getAID().getName()
                        + " is broken! Sending failure message and wait 2sec"
                    );
                    // waiting 2 seconds
                    myAgent.doWait(2000);
                    // set as ready to new count
                    step = 0;
                    break;

                case 3:
                    // send result to distributor
                    ACLMessage reply = msg.createReply();
                    reply.setContent(result);
                    reply.setPerformative(ACLMessage.CONFIRM);
                    myAgent.send(reply);
                    System.out.println(
                        "Malicious Agent " + myAgent.getAID().getName()
                        + " sent result to Distributor. " + result
                    );
                    // set as ready to new count
                    step = 0;
                    break;
            }
        }

        private boolean worksFine() {
			boolean works = true;
			Random r = new Random();
			
	        if(r.nextInt(10 - 1) + 1 < 3) {
	        	works = false;
	        }
	        
			return works;
		}
		
	}
}
