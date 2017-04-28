package multiply.matrix;

import jade.core.AID;

public class VerifyData {

	private AID agent;
	private DataPackage operation;
	private int result;
	
	public VerifyData(){
		
	}
	
	public VerifyData(AID _agent, DataPackage _operation, int _result) {
		this.agent = _agent;
		this.operation = _operation;
		this.result = _result;
	}
	
	public AID getAgent() {
		return agent;
	}
	public void setAgent(AID agent) {
		this.agent = agent;
	}
	public DataPackage getOperation() {
		return operation;
	}
	public void setOperation(DataPackage operation) {
		this.operation = operation;
	}
	public int getResult() {
		return result;
	}
	public void setResult(int result) {
		this.result = result;
	}
	
	
}
