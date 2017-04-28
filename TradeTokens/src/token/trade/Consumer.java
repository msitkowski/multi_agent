package token.trade;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Consumer extends Agent {

	private Integer action_interval;
	private Integer collected_tokens;
	private AID producerAgent;
	private boolean isWaiting;
	
	// Initialize Consumer Agent
	protected void setup() {
		System.out.println(
			"Consumer Agent " + getAID().getName()
			+ " is ready."
		);
		
		// set action_interval time
		Random r = new Random();
		action_interval = r.nextInt(800-400) + 400;
		collected_tokens = 0;
		isWaiting = false;
		
		addBehaviour(
			new TickerBehaviour(this, action_interval) {
			
			protected void onTick() {
				if (!isWaiting) {
					System.out.println(
						"Consumer Agent " + getAID().getName()
						+ "Trying to get token."
					);
					
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("token-generating");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						if (result.length == 1) {
							producerAgent = result[0].getName();
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
					
					// Perform the request
					myAgent.addBehaviour(new RequestToken());
			
				}
				else {
					System.out.println(
						"Consumer Agent " + getAID().getName()
						+ " waiting."
					);
				}
				isWaiting = !isWaiting;
			}
		});
		
		addBehaviour(new HandleProductionFinished());
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println(
			"Consumer Agent " + getAID().getName()
			+ " with action interval: " 
			+ action_interval.toString()
			+ " collected " + collected_tokens.toString()
			+ " tokens and is terminating."
		);
	}
	
	// Requesting token Behaviour
	private class RequestToken extends Behaviour {
		
		private MessageTemplate mt;
		private int step = 0;
		private boolean obtainedToken = false;
		private String token = "";
		
		public void action() {
			switch (step) {
			case 0:
				// Send request to producer
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				cfp.addReceiver(producerAgent);
				cfp.setContent("request-token");
				cfp.setConversationId("trade-token");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare template to get response
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("trade-token"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
				
			case 1:
				// Receive Producer response
				ACLMessage reply = myAgent.receive(mt);
				// check if reply received
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						token = reply.getContent();
						if (!token.equals("")) {
							collected_tokens++;
							obtainedToken = true;
						}
					}
					step = 2;
				}
				else {
					block();
				}
				break;
			}
		}
		
		public boolean done() {
			if (step == 2 && obtainedToken) {
				System.out.println(
					"Consumer Agent " + myAgent.getAID().getName()
					+ " obtained token: " + token
				);
			}
			return (step == 2);
		}
	}
	
	private class HandleProductionFinished extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) {
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.CONFIRM);
				myAgent.send(reply);
				myAgent.doDelete();
			}
			else {
				block();
			}
		}
		
	}
}
