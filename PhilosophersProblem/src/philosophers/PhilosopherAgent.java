package philosophers;

import java.util.Random;

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

public class PhilosopherAgent extends Agent {

	private Integer eatenKebs, interval;
	private Boolean[] forks;
	private AID waiter;
	
	protected void setup() {
		// initial log
		System.out.println(
			"Philosopher Agent " + getAID().getName()
			+ " is ready.");
		
		eatenKebs = 0;
		
		// initialize interval time
		Random r = new Random();
		interval = r.nextInt(1000) + 500;
		
		// initialize array with fork flags
		forks = new Boolean[2];
		forks[0] = false; // left fork
		forks[1] = false; // right fork
		
		// add indirect behaviour which add main behaviour for philosopher agent
		addBehaviour(new Register());
		
		// add secondary behaviour
		// listening for end of program msg
		addBehaviour(new Finish());
	}
	
	protected void takeDown() {
		System.out.println(
			"Philosopher Agent " + getAID().getName()
			+ " with interval " + interval
			+ " ate " + eatenKebs
			+ " kebabs and is terminating.");
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
				System.out.println("Philosopher Agent "
						+ myAgent.getAID().getName()
						+ " trying to register");
				
				// searching for waiter agent
				while(waiter == null) {
					waiter = searchWaiterAgent("waiter-service");
				}
				
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				cfp.addReceiver(waiter);
				cfp.setContent("philosopher-agent");
				cfp.setConversationId("philosopher-waiter-fork");
				cfp.setReplyWith("cfp"+System.currentTimeMillis());
				System.out.println("Philosopher Agent "
						+ myAgent.getAID().getName()
						+ " send registration request " + cfp);
				myAgent.send(cfp);
				
				// Prepare template to get response
				//mt = MessageTemplate.and(MessageTemplate.MatchConversationId("philosopher-waiter-fork"),
				//		MessageTemplate.MatchInReplyTo(request.getReplyWith()));
				mt = MessageTemplate.MatchConversationId("philosopher-waiter-fork");
				state = State.REGISTERED;
				break;
				
			case REGISTERED:
				ACLMessage response = myAgent.receive();
				if (response != null && response.getConversationId().equals("philosopher-waiter-fork")) {
					if (response.getPerformative() == ACLMessage.SUBSCRIBE) {
						// behaviour can be finished
						registered = true;
						System.out.println("Philosopher Agent "
								+ myAgent.getAID().getName()
								+ " registered Successfully");
					}
					else {
						registered = false;
						System.out.println("Philosopher Agent "
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
				myAgent.addBehaviour(new PhilosopherBehaviour());
				System.out.println("\n\nPhilosopher " + myAgent.getAID().getName() + " finished registration\n\n");
			}
			return registered;
		}
		
	}
	/**
	 * Implementation of behaviour in which Philosopher Agent
	 * pick up fork by sending requests to Waiter Agent
	 * 
	 * @done start new behaviour with kebab requests
	 */
//	private class RequestFork extends Behaviour {
//
//		// start behaviour with fork request
//		private State state = State.REQUEST_FORK;
//		private MessageTemplate mt = null;
//		
//		@Override
//		public void action() {
//			switch (state) {
//			case REQUEST_FORK:
//				System.out.println("Philosopher Agent "
//						+ myAgent.getAID().getName()
//						+ " trying to get fork");
//				myAgent.doWait(interval);
//				
//				// prepare and send request
//				ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
//				request.addReceiver(waiter);
//				request.setContent(pickFork());
//				request.setConversationId("philosopher-waiter-fork");
//				request.setReplyWith("request"+System.currentTimeMillis());
//				myAgent.send(request);
//				
//				// Prepare template to get response
//				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("philosopher-waiter-fork"),
//						MessageTemplate.MatchInReplyTo(request.getReplyWith()));
//				state = State.RECEIVE_FORK;
//				break;
//				
//			case RECEIVE_FORK:
//				// receive msg matching with template
//				ACLMessage response = myAgent.receive(mt);
//				
//				if (response != null) {// && response.getConversationId().equals("philosopher-waiter-fork")) {
//					switch (response.getPerformative()) {
//					// fork is free
//					case ACLMessage.CONFIRM:
//						if (response.getContent().equals("right-fork")) {
//							forks[1] = true;
//						}
//						if (response.getContent().equals("left-fork")) {
//							forks[0] = true;
//						}
//						break;
//						
//					// fork in use
//					case ACLMessage.DISCONFIRM:
//						// requested fork was free
//						if(fork_request_result) {
//							// if got two forks request for kebab
//							if (forks[0] && forks[1]) {
//								state = State.REQUEST_KEBAB;
//								
//							} // otherwise request for another fork
//							else {
//								state = State.REQUEST_FORK;
//							}
//						} // requested fork is in use
//						else {
//							// if have one fork it need to be free
//							if (forks[0] || forks[1]) {
//								state = State.FREE_FORKS;
//								
//							} // if have no forks request again
//							else {
//								state = State.REQUEST_FORK;
//							}
//						}
//						break;
//						
//					default:
//						break;
//					}
//				}
//				else {
//					block();
//				}
//				break;
//			
//			default:
//				break;
//			}
//		}
//
//		@Override
//		public boolean done() {
//			System.out.println(myAgent.getAID().getName() + " got left " + forks[0] + ", got right " + forks[1] + "\n\n");
//			Boolean _done = forks[0] && forks[1];
//			if (_done) {
//				myAgent.addBehaviour(new RequestKebab());
//			}
//			return _done;
//		}
//		
//	}
	
//	private class RequestKebab extends Behaviour {
//
//		private Boolean gotKebab = false;
//		// start behaviour with kebab request
//		private State state = State.REQUEST_KEBAB;
//		private MessageTemplate mt = null;
//		@Override
//		public void action() {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public boolean done() {
//			if (gotKebab) {
//				++eatenKebs;
//				myAgent.addBehaviour(new FreeForks());
//			}
//			return false;
//		}
//		
//	}
	
	/**
	 * Select random fork to pick in have none, else pick missing
	 * @return string with fork which should be picked up
	 */
//	protected String pickFork() {
//		String fork = "right-fork";
//		// pick random fork if don't have any
//		if (!forks[0] && !forks[1]) {
//			Random r = new Random();
//			if (r.nextInt(9) + 1 <= 5) {
//				fork = "left-fork";
//			}
//			else {
//				fork = "right-fork";
//			}
//		} else if (!forks[0]) {
//			fork = "left-fork";
//		}
//		return fork;
//	}
	
	// PhilosopherBehaviour implementation
	private class PhilosopherBehaviour extends CyclicBehaviour {

		// start behaviour with fork request
		private State state = State.REQUEST_FORK;
		private MessageTemplate mt = MessageTemplate.MatchConversationId("philosopher-waiter-fork");//null;
		
		@Override
		public void action() {
			switch (state) {
			
			// send request for fork (when got 0 or 1 fork) doWait(interval) before send request
			case REQUEST_FORK:
				doWait(interval);
				// prepare and send request
				ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.addReceiver(waiter);
				request.setContent(pickFork());
				request.setConversationId("philosopher-waiter-fork");
				request.setReplyWith("request"+System.currentTimeMillis());
				myAgent.send(request);
				state = State.RECEIVE_FORK;
				break;
				
			// receive response for fork request
			case RECEIVE_FORK:
				state = receiveFork();
				break;
				
			// send request for kebab (when got 2 forks) doWait(interval) before send request
			case REQUEST_KEBAB:
				doWait(interval);
				// prepare and send propose
				ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
				propose.addReceiver(waiter);
				propose.setConversationId("philosopher-waiter-fork");
				propose.setReplyWith("propose"+System.currentTimeMillis());
				myAgent.send(propose);
				state = State.RECEIVE_KEBAB;
				break;
				
			// receive response for kebab request
			case RECEIVE_KEBAB:
				Boolean kebab_request_result = receiveKebab();
				if (kebab_request_result) {
					++eatenKebs;
					state = State.FREE_FORKS;
				}
				else {
					block();
				}
				break;

			// send free forks request (when got kebab or second fork is in use)
			// if (forks[0] == forks[1] == true) doWait(interval) before free both forks else free instantly
			case FREE_FORKS:
				if (forks[0] && forks[1]) {
					doWait(interval);
				}
				freeForks();
				state = State.REQUEST_FORK;
				break;
				
			default:
				break;
			}
		}
		
		/**
		 * Method select random fork which Philosopher should request for.
		 * If Philosopher got any fork then missing is requested.
		 * @return String used as message content with picked fork.
		 */
		public String pickFork() {
			String fork = "right-fork";
			// pick random fork if don't have any
			if (!forks[0] && !forks[1]) {
				Random r = new Random();
				if (r.nextInt(9) + 1 <= 5) {
					fork = "left-fork";
				}
				else {
					fork = "right-fork";
				}
			} else if (!forks[0]) {
				fork = "left-fork";
			}
			return fork;
		}
		
		/**
		 * Method receive responses to fork requests sent to Waiter Agent
		 * and set next state for behaviour depending on response type and content.
		 * @return State which will be next in Philosopher Behaviour execution.
		 */
		public State receiveFork() {
			// receive msg matching with template
			ACLMessage response = myAgent.receive(mt);
			State result = State.RECEIVE_FORK;
			
			if (response != null) {
				switch (response.getPerformative()) {
				// fork is free
				case ACLMessage.CONFIRM:
					if (response.getContent().equals("right-fork")) {
						forks[1] = true;
					}
					if (response.getContent().equals("left-fork")) {
						forks[0] = true;
					}
					
					// if got two forks request for kebab
					if (forks[0] && forks[1]) {
						result = State.REQUEST_KEBAB;
						
					} // otherwise request for another fork
					else {
						result = State.REQUEST_FORK;
					}
					break;
					
				// fork in use
				case ACLMessage.DISCONFIRM:
					// if have one fork it need to be free
					if (forks[0] || forks[1]) {
						result = State.FREE_FORKS;
						
					} // if have no forks request again
					else {
						result = State.REQUEST_FORK;
					}
					break;
					
				default:
					break;
				}
			}
			else {
				block();
			}
			return result;
		}

		/**
		 * Method receive responses for sent kebab reqests.
		 * @return result Boolean value depending on received message type.
		 */
		public Boolean receiveKebab() {
			// receive msg matching with template
			ACLMessage response = myAgent.receive(mt);
			Boolean result = false;
			//System.out.println("\n\n\n" + myAgent.getAID().getName() + " receiveKebab " + response + "\n\n\n");
			if (response != null) {
				switch (response.getPerformative()) {
				// obtained kebab
				case ACLMessage.ACCEPT_PROPOSAL:
					// TODO if all will work add unpacking object from content
					System.out.println(myAgent.getAID().getName() + " got kebab.");
					result = true;
					break;
					
				// no kebab available
				case ACLMessage.REFUSE:
					result = false;
					break;
					
				default:
					break;
				}
			}
			else {
				block();
			}
			return result;
		}

		/**
		 * Method sending free forks request depending on forks array values
		 * free right fork if forks[1] == true,
		 * free left fork if forks[0] == true
		 * @send Message with free forks request.
		 */
		public void freeForks() {
			// send message to free forks
			String msg = "right-fork";
			if (forks[0] && forks[1]) {
				msg = "both-forks";
				forks[0] = false;
				forks[1] = false;
			}
			else if (forks[0]) {
				msg = "left-fork";
				forks[0] = false;
			}
			else {
				msg = "right-fork";
				forks[1] = false;
			}
			ACLMessage request = new ACLMessage(ACLMessage.CANCEL);
			request.addReceiver(waiter);
			request.setContent(msg);
			request.setConversationId("philosopher-waiter-fork");
			request.setReplyWith("cancel"+System.currentTimeMillis());
			myAgent.send(request);
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
