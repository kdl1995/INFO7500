package hw4.node;

import hw4.Transaction;

public class Candidate {
	public final Transaction tx;
	public final int sender;
	
	public Candidate(Transaction tx, int sender) {
		this.tx = tx;
		this.sender = sender;
	}
}