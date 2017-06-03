package philosophers;

import java.util.List;
import java.util.ArrayList;

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
 * Waiter Agent class implementation.
 */
public class WaiterAgent extends Agent {

	private List<AID> philosophers, forks;
	private Integer kebabsNumber, kebabsLeft, expectedAgentsNumber;
	private List<Kebab> kebabs;
	
	/**
	 * Agent setup
	 */
	protected void setup() {
		// create new service
		ServiceDescription sd = new ServiceDescription();
		sd.setType("waiter-service");
		sd.setName("waiter-service");
		
		// register service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
					
		System.out.println(
			"Waiter Agent " + getAID().getName()
			+ " is ready.");
		
		// check if number of kebabs given as arg
		Object[] args = getArguments();
		
		if (args != null && args.length > 1) {
			expectedAgentsNumber = Integer.valueOf((String) args[0]);
			kebabsNumber = kebabsLeft = Integer.valueOf((String) args[1]);
			
			// initialize lists
			philosophers = new ArrayList<>();
			forks = new ArrayList<>();
			kebabs = new ArrayList<>();
			
			// generate kebabs
			for (int i = 0; i < kebabsNumber; ++i) {
				Kebab kebab = new Kebab();
				kebab.setName("Kebab_" + i);
				kebabs.add(kebab);
			}
			// add main behaviour
			addBehaviour(new WaiterBehaviour());
		}
		else {
			System.out.println("Number of kebabs or philosophers not specified!");
			doDelete();
		}
	}
	
	/**
	 * Agent dismissal message with service deregistration.
	 */
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		System.out.println(
			"Waiter Agent " + getAID().getName()
			+ " generated " + (kebabsNumber - kebabsLeft)
			+ "/" + kebabsNumber + " kebabs and is terminating.");
	}
	
	/**
	 * Waiter Agent main behaviour.
	 * It handle incoming messages from Philosophers and Forks.
	 * Messages from philosophers except kebab request, are redirected to fork.
	 * Messages from forks are redirected to philosophers.
	 * 
	 * @done When all kebabs was sent to philosophers
	 */
	private class WaiterBehaviour extends Behaviour {
		
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("philosopher-waiter-fork");
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null && msg.getConversationId().equals("philosopher-waiter-fork")) {
				switch (msg.getPerformative()) {
				// received regisration msg from forks and philosophers
				// until got expected value (given as arg)
				case ACLMessage.CFP:
					registerSender(msg);
					break;
					
				// received request for fork, redirect to fork
				case ACLMessage.REQUEST:
					redirectRequestToFork(msg);
					break;
					
				// fork confirmed request (is free), redirect to philosopher
				case ACLMessage.CONFIRM:
					redirectToPhilosopher(msg);
					break;
					
				// fork is in use, redirect to philosopher
				case ACLMessage.DISCONFIRM:
					redirectToPhilosopher(msg);
					break;
					
				// request for kebab
				case ACLMessage.PROPOSE:
					// send accept if kebabs available
					// send refuse if not and add finish program behaviour
					sendKebab(msg);
					break;

				case ACLMessage.CANCEL:
					redirectCancelToFork(msg);
					break;
					
				default:
					break;
				}
			}
			else {
				block();
			}
		}
		
		/**
		 * Method handling registration messages from philosophers and forks
		 * at start of the program before main part with kebab consumption start.
		 * It prevent starting main part before expected number of philosophers and forks
		 * will be available.
		 * 
		 * @param msg Received registration message.
		 */
		private void registerSender(ACLMessage msg) {
			if (msg.getContent().equals("philosopher-agent")) {
				// register philosopher
				int index = philosophers.indexOf(msg.getSender());
				if (index == -1) {
					philosophers.add(msg.getSender());
				}
			}
			else {
				// register fork
				int index = forks.indexOf(msg.getSender());
				if (index == -1) {
					forks.add(msg.getSender());
				}
			}
			// check if got all expected agents
			if (philosophers.size() == forks.size()
					&& philosophers.size() == expectedAgentsNumber) {
				
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.SUBSCRIBE);
				// add registered agents to recipients
				for (AID p: philosophers) {
					reply.addReceiver(p);
				}
				for (AID f: forks) {
					reply.addReceiver(f);
				}
				myAgent.send(reply);
			}
			else {
				System.out.println("\n\nNot all agents registered yet " + philosophers.size() + " " + forks.size() + " " + expectedAgentsNumber);
			}
		}
		
		/**
		 * Method redirect message received from Fork Agent
		 * to Philosopher Agent which was requesting fork.
		 * 
		 * @param msg Received message from Fork Agent.
		 */
		private void redirectToPhilosopher(ACLMessage msg) {
			// send msg to philosopher depending on content and sender
			// left = fork index + 1, right = fork index
			int index = forks.indexOf(msg.getSender());
			// if fork exist on the list
			if (index != -1) {
				if (msg.getContent().equals("left-fork")) {
					if (index == forks.size() - 1) {
						index = 0;
					}
					else {
						index += 1;
					}
				}
				ACLMessage newMsg = new ACLMessage(msg.getPerformative());
				AID philosopher = philosophers.get(index);
				System.out.println("Waiter redirect message from Fork "
						+ msg.getSender().getName() + " to Philosopher "
						+ philosopher.getName());
				newMsg.addReceiver(philosopher);
				newMsg.setContent(msg.getContent());
				newMsg.setConversationId(msg.getConversationId());
				myAgent.send(newMsg);
			}
			else {
				System.out.println("FORK not on the list " + msg.getSender().getName());
			}
		}
		
		/**
		 * Method redirect requests incoming from Philosophers to Forks.
		 * @param msg Received from Philosopher with request for fork.
		 */
		private void redirectRequestToFork(ACLMessage msg) {
			ACLMessage newMsg = null;
			// get fork depending on msg sender and content
			// left = philosopher index -1, right = philosopher index
			int index = philosophers.indexOf(msg.getSender());
			// if philosopher on the list
			if (index != -1) {
				if (msg.getContent().equals("left-fork")) {
					if (index == 0) {
						index = philosophers.size() - 1;
					}
					else {
						index -= 1;
					}
				}
				AID fork = forks.get(index);
				System.out.println("Waiter redirect message from Philosopher "
						+ msg.getSender().getName() + " to Fork "
						+ fork.getName());
				newMsg = new ACLMessage(msg.getPerformative());
				newMsg.addReceiver(fork);
			}
			else {
				// philosopher is not on the list
				newMsg = msg.createReply();
				newMsg.setPerformative(ACLMessage.DISCONFIRM);
			}

			newMsg.setContent(msg.getContent());
			newMsg.setConversationId(msg.getConversationId());
			myAgent.send(newMsg);
		}
		
		/**
		 * Method redirect message received from Philosopher
		 * with free forks request.
		 * @param msg Received from Philosopher free forks request.
		 */
		private void redirectCancelToFork(ACLMessage msg) {
			// get fork depending on msg sender and content
			// left = philosopher index -1, right = philosopher index
			int index = philosophers.indexOf(msg.getSender());
			
			// if philosopher on the list
			if (index != -1) {
				ACLMessage newMsg = new ACLMessage(msg.getPerformative());
				newMsg.setConversationId(msg.getConversationId());
				newMsg.setContent(msg.getContent());
				
				if (msg.getContent().equals("both-forks") || msg.getContent().equals("left-fork")) {
					AID leftFork = (index == 0) ? forks.get(philosophers.size() - 1) : forks.get(index - 1);
					newMsg.addReceiver(leftFork);
					System.out.println("Waiter redirect message from Philosopher "
							+ msg.getSender().getName() + " to Fork "
							+ leftFork.getName());
				}
				
				if (msg.getContent().equals("both-forks") || msg.getContent().equals("right-fork")) {
					AID rightFork = forks.get(index);
					newMsg.addReceiver(rightFork);
					System.out.println("Waiter redirect message from Philosopher "
							+ msg.getSender().getName() + " to Fork "
							+ rightFork.getName());
				}
				
				myAgent.send(newMsg);
			}
		}
		
		/**
		 * Method sending message with kebab taken from list
		 * as reply to Philosopher Agent kebab request.
		 * Requests are refused when no kebab available.
		 * 
		 * @param msg Received from Philosopher kebab request.
		 */
		private void sendKebab(ACLMessage msg) {
			ACLMessage reply = msg.createReply();
			
			if (kebabs != null && !kebabs.isEmpty()) {
				reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				kebabs.remove(kebabs.size() - 1);
				--kebabsLeft;
			}
			else {
				reply.setPerformative(ACLMessage.REFUSE);
			}
			
			myAgent.send(reply);
		}

		@Override
		public boolean done() {
			Boolean res = false;
			// no kebs left, finish this behaviour and inform agents
			if (kebabsLeft == 0) {
				System.out.println("\n\n\n waiter behviour done \n\n\n");
				myAgent.addBehaviour(new FinishProgram());
				res = true;
			}
			return res;
		}
	}
	
	/**
	 * Implementation of behaviour in which waiter inform all agents
	 * about program finish when list of kebabs is empty
	 */
	private class FinishProgram extends CyclicBehaviour {

		private int repliesCounter = 0;
		private int step = 0;
		private MessageTemplate mt;
		
		@Override
		public void action() {
			// TODO Auto-generated method stub
			switch (step) {
			case 0:
				ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
				
				for (AID philosopher: philosophers) {
					inform.addReceiver(philosopher);
				}
				for (AID fork: forks) {
					inform.addReceiver(fork);
				}
				inform.setContent("end-of-program");
				inform.setConversationId("end-of-program");
				inform.setReplyWith("inform"+System.currentTimeMillis()); // Unique value
				myAgent.send(inform);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("end-of-program"),
						MessageTemplate.MatchInReplyTo(inform.getReplyWith()));
				
				step = 1;
				break;
				
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					repliesCounter++;
					
					if (repliesCounter >= philosophers.size() + forks.size()) {
						System.out.println("Waiter agent got " + repliesCounter
								+ " replies for end-of-program message");
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
