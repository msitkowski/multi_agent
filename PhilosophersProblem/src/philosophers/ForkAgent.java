package philosophers;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Fork Agent class implementation.
 */
public class ForkAgent extends Agent {

	private Boolean inUse;
	private AID waiter;
	
	/**
	 * Agent setup
	 */
	protected void setup() {
		System.out.println(
			"Fork Agent " + getAID().getName()
			+ " is ready.");
		
		inUse = false;
		
		// add indirect behaviour which add main behaviour
		// in which fork respond to requests
		addBehaviour(new Register());
		
		// add secondary behaviour
		// listening for end of program msg
		addBehaviour(new Finish());
	}
	
	/**
	 * Agent dismissal message.
	 */
	protected void takeDown() {
		System.out.println(
			"Fork Agent " + getAID().getName()
			+ " is terminating.");
	}
	
	/**
	 * Implementation of behaviour in which Agent
	 * is registered in Waiter agent (added to specific list)
	 * and can start working (when forks == philosophers == waiter expected agents)
	 */
	private class Register extends Behaviour {

		private Boolean registered = false;
		private State state = State.REGISTER;
		MessageTemplate mt = null;
		
		@Override
		public void action() {
			// TODO Auto-generated method stub
			switch (state) {
			case REGISTER:
				System.out.println("Fork Agent "
						+ myAgent.getAID().getName()
						+ " trying to register");
				
				// searching for waiter agent
				while(waiter == null) {
					waiter = searchWaiterAgent("waiter-service");
				}
				
				ACLMessage request = new ACLMessage(ACLMessage.CFP);
				request.addReceiver(waiter);
				request.setContent("fork-agent");
				request.setConversationId("philosopher-waiter-fork");
				request.setReplyWith("cfp"+System.currentTimeMillis());
				myAgent.send(request);
				
				// Prepare template to get response
//				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("philosopher-waiter-fork"),
//						MessageTemplate.MatchInReplyTo(request.getReplyWith()));
				mt = MessageTemplate.MatchConversationId("philosopher-waiter-fork");
				
				state = State.REGISTERED;
				break;
				
			case REGISTERED:
				ACLMessage response = myAgent.receive();
				if (response != null && response.getConversationId().equals("philosopher-waiter-fork")) {
					if (response.getPerformative() == ACLMessage.SUBSCRIBE) {
						// behaviour can be finished
						registered = true;
						System.out.println("Fork Agent "
								+ myAgent.getAID().getName()
								+ " trying to register Successfully");
					}
					else {
						registered = false;
						System.out.println("Fork Agent "
								+ myAgent.getAID().getName()
								+ " registration in progress");
						state = State.REGISTER;
					}
				}
				else {
					System.out.println(myAgent.getAID().getName() + " blocked");
					block();
				}
				break;

			default:
				break;
			}
		}

		@Override
		public boolean done() {
			if (registered) {
				System.out.println("\n\nFork " + myAgent.getAID().getName() + " finished registration\n\n");
				myAgent.addBehaviour(new ForkBehaviour());
			}
			return registered;
		}
		
	}
	
	/**
	 * Fork Behaviour implementation
	 * Fork respond on waiter requests
	 * response type depends on inUse flag
	 */
	private class ForkBehaviour extends CyclicBehaviour {
		
		@Override
		public void action() {

			MessageTemplate mt = MessageTemplate.MatchConversationId("philosopher-waiter-fork");
			ACLMessage msg = myAgent.receive();
			
			if (msg != null && msg.getConversationId().equals("philosopher-waiter-fork")) {
				switch (msg.getPerformative()) {
				// request fork message
				case ACLMessage.REQUEST:
					ACLMessage response = msg.createReply();
					response.setContent(msg.getContent());
					//response.addReceiver(msg.getSender());
					response.setConversationId(msg.getConversationId());
					
					if (inUse) {
						response.setPerformative(ACLMessage.DISCONFIRM);
						System.out.println("Fork agent "
								+ myAgent.getAID().getName() + " is already in use.");
					}
					else {
						inUse = true;
						response.setPerformative(ACLMessage.CONFIRM);
						System.out.println("Fork agent "
								+ myAgent.getAID().getName() + " picked up.");
					}
					myAgent.send(response);
					break;

				// free fork message
				case ACLMessage.CANCEL:
					inUse = false;
					System.out.println("Fork agent "
							+ myAgent.getAID().getName() + " is free.");
					break;

				default:
					break;
				}
			}
			else {
				block();
			}
		}
	}
	
	/**
	 * Implementation of behaviour handling
	 * information from waiter about end of program
	 */
	private class Finish extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("end-of-program");
			ACLMessage program_end_msg = myAgent.receive(mt);
			
			// send aggreement and terminate if received msg
			if (program_end_msg != null) {
				ACLMessage reply = program_end_msg.createReply();
				reply.setPerformative(ACLMessage.AGREE);
				myAgent.send(reply);
				// terminate
				myAgent.doDelete();
			}
			else {
				block();
			}
		}
		
	}
	
	/** 
	 * Method search agent which provide given service name
	 */
	protected AID searchWaiterAgent(String service_name) {
		
		AID waiterAgent = null;
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(service_name);
		template.addServices(sd);
		
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			
			if (result.length == 1) {
				waiterAgent = result[0].getName();
				System.out.println(
						getAID().getName()
						+ " found waiter "
						+ waiterAgent.getName());
			}
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		return waiterAgent;
	}
}
