package multiply.matrix;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class DistributorAgent extends Agent {

    private Queue<DataPackage> operationsQueue;
    private List<String> onGoingOperations;
    private List<AID> countingAgents;//, blackList;
    private Matrix M1, M2, resM;
//    private boolean verifyResult;
//    private List<VerifyData> operationsToVerify;

    // Producer initialization
    protected void setup() {
        // Register the matrix-distributor service
        // in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("matrix-distributor");
        sd.setName("JADE-matrix-distributor");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        operationsQueue = new LinkedList<>();
        onGoingOperations = new ArrayList<>();
        countingAgents = new ArrayList<>();
//        blackList = new ArrayList<>();
//        verifyResult = false;
//        operationsToVerify = new ArrayList<>();

        int n = 3;
        int m = 3;
        // generate two matrixes
        M1 = new Matrix(n, m);
        M1.generateValues();
        M2 = new Matrix(m, n);
        M2.generateValues();

        // initialize result Matrix
        resM = new Matrix(n, n);
        Matrix M = resM.multiplyMatrixes(M1, M2);

        System.out.println("M1:");
        M1.displayMatrix();
        System.out.println("M2:");
        M2.displayMatrix();
        System.out.println("Expected result:");
        M.displayMatrix();

        // add operations to queue
        for (int i = 0; i < n; ++i) {
            List<Integer> rowData = M1.getRowData(i);

            for (int j = 0; j < n; ++j) {
                DataPackage dpkg = new DataPackage(i, j);
                dpkg.setRowData(rowData);
                dpkg.setColData(M2.getColData(j));
                operationsQueue.add(dpkg);
            }
        }

        System.out.println(
            "Distributor Agent " + getAID().getName()
			+ " is ready."
        );

        // add custom Cyclic behaviuor which manage matrix multiplying
        addBehaviour(new ManageMatrixMultiply());
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
            "Distributor Agent " + getAID().getName()
            + " got final result and is terminating.");
        System.out.println("Obrained result:");
        resM.displayMatrix();
    }

    private class ManageMatrixMultiply extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId("multiply-matrix");
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                // updating counting Agents
                int index = countingAgents.indexOf(msg.getSender());
                //int bListIndex = blackList.indexOf(msg.getSender());
                if (index == -1) {// && bListIndex == -1) {
                    countingAgents.add(msg.getSender());
                }

                int msgType = msg.getPerformative();
                ObjectMapper mapper = new ObjectMapper();

                // handle message types
                switch (msgType) {
                    case ACLMessage.CFP:
                        ACLMessage reply = msg.createReply();
                        // additionally check if sender is on the black list
                        if (!operationsQueue.isEmpty()) {// && bListIndex == -1) {
                        	System.out.println(
                                "Distributor Agent " + getAID().getName()
                                + " is sending data to " + msg.getSender().getName());
                            reply.setPerformative(ACLMessage.PROPOSE);
                            
                            // handling result verification
//                            if (verifyResult) { // verification workflow
//                            	// always get operation from first element
//                            	// and do not send data to verify to same agent
//                            	VerifyData vData = operationsToVerify.get(0);
//                            	AID cAgent = vData.getAgent();
//                            	if (cAgent != msg.getSender()) {
//                            		// get operation to verify
//                            		DataPackage dpkg = vData.getOperation();
//                            		// serialize DataPackage to Json
//                                    try {
//                                    	// add element to on going operations
//                                        String operationRC = dpkg.getRowIndex() + "," + dpkg.getColIndex();
//                                        // remove element from on going operations
//                                    	if(!onGoingOperations.contains(operationRC)) {
//                                    		onGoingOperations.add(operationRC);
//                                    		System.out.println("CFP: add operation to verify to queue: " + operationRC);
//                                    	}
//                                        String operation = mapper.writeValueAsString(dpkg);
//                                        reply.setContent(operation);
//                                        // verification sent
//                                        verifyResult = false;
//
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }
//                            	}
//                            	else {
//                            		// agent must wait until verification finish
//                            		reply.setPerformative(ACLMessage.REFUSE);
//                                    reply.setContent("not-available");
//                            	}
//                            }
//                            else { // normal workflow
                        	// remove operation from queue
                            DataPackage dpkg = operationsQueue.remove();
                            
                            // serialize DataPackage from operations queue to Json
                            try {
                            	// add element to on going operations
                                String operationRC = dpkg.getRowIndex() + "," + dpkg.getColIndex();
                                // remove element from on going operations
                            	if(!onGoingOperations.contains(operationRC)) {
                            		onGoingOperations.add(operationRC);
                            		System.out.println("CFP: add operation to queue: " + operationRC);
                            	}
                                String operation = mapper.writeValueAsString(dpkg);
                                reply.setContent(operation);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
//                            }

                        } else {
                            // operations not available or sender is on the black list
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("not-available");
                        }
                        myAgent.send(reply);
                        break;

                    case ACLMessage.FAILURE:
                    	System.out.println(
                            "Distributor Agent " + getAID().getName()
                            + " obtained failure message from "
                            + msg.getSender().getName()
                            + " and add data back to operations queue.");
                    	String failureData = msg.getContent();
                        // convert Json to DataPackage object
                        try {
                            DataPackage dpkg = mapper.readValue(failureData, DataPackage.class);
                            // check if operation have ongoing verification
//                            if (operationsToVerify.size() >= 2) {
//                            	for (int i = 1; i < operationsToVerify.size(); ++i) {
//	                            	VerifyData vData = operationsToVerify.get(i);
//	                            	DataPackage vDpkg = vData.getOperation();
//	                            	
//	                            	if (vDpkg.getRowIndex() == dpkg.getRowIndex()
//	                            			&& vDpkg.getColIndex() == dpkg.getColIndex()) {
//	                            		// search agent
//	                            		if (msg.getSender() == vData.getAgent()) {
//	                            			// remove from list and set verification on
//	                            			operationsToVerify.remove(i);
//	                            			verifyResult = true;
//	                            			break;
//	                            		}
//	                            	}
//	                            	else {
//	                            		// if operation not on the list,
//	                            		// do not continue search
//	                            		break;
//	                            	}
//                            	}
//                            }
                            // add operation again to operations queue
                            operationsQueue.add(dpkg);
                            String operationRC = dpkg.getRowIndex() + "," + dpkg.getColIndex();
                            // remove element from on going operations
                        	if(onGoingOperations.contains(operationRC)) {
                        		onGoingOperations.remove(operationRC);
                        		System.out.println("Failure: Removing operation from queue: " + operationRC);
                        	}

                        } catch (JsonParseException e) {
                            e.printStackTrace();

                        } catch (JsonMappingException e) {
                            e.printStackTrace();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    	break;
                    	
                    case ACLMessage.CONFIRM:
                    	System.out.println(
                            "Distributor Agent " + getAID().getName()
                            + " obtained result from "
                            + msg.getSender().getName());
                    	String data = msg.getContent();
                        // convert Json to DataPackage object
                        try {
                            DataPackage dpkg = mapper.readValue(data, DataPackage.class);
                            // get random number and decide if result should be verified
                            Random r = new Random();
                            
//                            if(1 / (r.nextInt(10 - 1) + 1) == 1) {
//                            	verifyResult = true;
//                            	VerifyData vData = new VerifyData(msg.getSender(), dpkg, dpkg.getResult());
//                            	operationsToVerify.add(vData);
//                            }
//                            else {
                            resM.setValue(dpkg.getRowIndex(), dpkg.getColIndex(),
                                    dpkg.getResult());
                            String operationRC = dpkg.getRowIndex() + "," + dpkg.getColIndex();
                            // remove element from on going operations
                        	if(onGoingOperations.contains(operationRC)) {
                        		onGoingOperations.remove(operationRC);
                        		System.out.println("Confirm: Removing operation from queue: " + operationRC);
                        	}
                            // finish program when operations queue and on going are empty
                            if (operationsQueue.isEmpty() && onGoingOperations.isEmpty()) {
                            	System.out.println("onGoing: " + onGoingOperations.size());
                                myAgent.addBehaviour(new GotFinalResult());
                            }
//                            }

                        } catch (JsonParseException e) {
                            e.printStackTrace();

                        } catch (JsonMappingException e) {
                            e.printStackTrace();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;

                    default:
                        break;
                }

            } else {
                block();
            }
        }
    }

    // behaviour informing all counting agents about finish
    private class GotFinalResult extends CyclicBehaviour {

        private int repliesCounter = 0;
        private int step = 0;
        private MessageTemplate mt;

        @Override
        public void action() {
            // TODO Auto-generated method stub
            switch (step) {
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (AID cAgent : countingAgents) {
                        cfp.addReceiver(cAgent);
                    }
                    cfp.setContent("got-final-result");
                    cfp.setConversationId("got-final-result");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("got-final-result"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    step = 1;
                    break;

                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCounter++;

                        if (repliesCounter >= countingAgents.size()) {
                            myAgent.doDelete();
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

    }
}
