package token.trade;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class Producer extends Agent {

	private Integer action_interval;
	private Integer tokens_number;
	private Queue<String> tokens;
	private Integer token_counter;
	private List<AID> consumerAgents;
	
	// Producer initialization
	protected void setup() {
		System.out.println(
			"Producer Agent " + getAID().getName()
			+ " is ready."
		);
		
		// set action_interval time
		Random r = new Random();
		action_interval = r.nextInt(800-400) + 400;
		
		// initialize list of tokens
		tokens = new LinkedList<>();
		token_counter = 0;
		consumerAgents = new ArrayList<>();
		
		// Register the token-generating service
		// in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("token-generating");
		sd.setName("JADE-token-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Get numbers of tokens to generate from args
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {
			tokens_number = Integer.valueOf((String) args[0]);
			
			// Add Generating Tokens as Ticker Behaviour
			// Token is generated every action_interval milliseconds
			addBehaviour(
				new TickerBehaviour(this, action_interval) {
				
				protected void onTick() {
					if (token_counter < tokens_number) {
						// Generate random string as token
						String token = new BigInteger(50, r).toString(32);
						token_counter++;
						
						// Add generated token to list of tokens
						tokens.add(token);
						
						System.out.println(
							"Producer Agent " + getAID().getName()
							+ " generated new token("+ token_counter.toString()
							+ "/" + tokens_number.toString() + "): " + token
						);
					
					}
					else {
						if (tokens.isEmpty()) {
							addBehaviour(new HandleGeneratingFinish());
						}
					}
				}
			});
			
			addBehaviour(new HandleTokenRequest());
		}
		else {
			// Producer terminate 
			// if number of tokens to generate not given
			System.out.println("Number of tokens to generate not specified");
			doDelete();
		}
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println(
			"Producer Agent " + getAID().getName()
			+ " generated " + tokens_number.toString() + " tokens and is terminating.");
	}
	
	private class HandleTokenRequest extends CyclicBehaviour {
		
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) {
				// updating consumers
				int index = consumerAgents.indexOf(msg.getSender());
				if (index == -1) {
					consumerAgents.add(msg.getSender());
				}
				// CFP Message received. Process it
				ACLMessage reply = msg.createReply();
				
				
				if (!tokens.isEmpty()) {
					reply.setPerformative(ACLMessage.PROPOSE);
					// send and remove token from queue
					reply.setContent(tokens.remove());
				}
				else {
					// token not available
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
	
	private class HandleGeneratingFinish extends CyclicBehaviour {
		private int repliesCounter = 0;
		private int step = 0;
		private MessageTemplate mt;
		
		public void action() {
			switch (step) {
			case 0:
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				
				for (AID cAgent: consumerAgents) {
					cfp.addReceiver(cAgent);
				}
				cfp.setContent("production-finished");
				cfp.setConversationId("token-production-finished");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("token-production-finished"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				
				step = 1;
				break;
				
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					repliesCounter++;
					
					if (repliesCounter >= consumerAgents.size()) {
						myAgent.doDelete(); 
					}
				}
				else {
					block();
				}
				break;
			}
		}
	}
}
