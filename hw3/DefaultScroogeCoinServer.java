package hw3;

import java.security.*;
import java.util.*;

//Scrooge creates coins by adding outputs to a transaction to his public key.
//In ScroogeCoin, Scrooge can create as many coins as he wants.
//No one else can create a coin.
//A user owns a coin if a coin is transfer to him from its current owner
public class DefaultScroogeCoinServer implements ScroogeCoinServer {

	private KeyPair scroogeKeyPair;
	private ArrayList<Transaction> ledger = new ArrayList();

	// Set scrooge's key pair
	@Override
	public synchronized void init(KeyPair scrooge) {
		this.scroogeKeyPair = scrooge;
	}

	// For every 10 minute epoch, this method is called with an unordered list
	// of proposed transactions
	// submitted during this epoch.
	// This method goes through the list, checking each transaction for
	// correctness, and accepts as
	// many transactions as it can in a "best-effort" manner, but it does not
	// necessarily return
	// the maximum number possible.
	// If the method does not accept an valid transaction, the user must try to
	// submit the transaction
	// again during the next epoch.
	// Returns a list of hash pointers to transactions accepted for this epoch

	public synchronized List<HashPointer> epochHandler(List<Transaction> txs) {
		List<HashPointer> list = new ArrayList<HashPointer>();
		while (!txs.isEmpty()) {
			List<Transaction> temp = new ArrayList<Transaction>();
			for (Transaction ts : txs) {
				if (isValid(ts)) {
					ledger.add(ts);
					HashPointer hp = new HashPointer(ts.getHash(), ledger.size() - 1);
					list.add(hp);
				} else {
					temp.add(ts);
				}
			}
			if (temp.size() == txs.size())
				break;
			txs = temp;
		}
		return list;
	}

	// Returns true if and only if transaction tx meets the following
	// conditions:
	// CreateCoin transaction
	// (1) no inputs
	// (2) all outputs are given to Scrooge's public key
	// (3) all of tx’s output values are positive
	// (4) Scrooge's signature of the transaction is included

	// PayCoin transaction
	// (1) all inputs claimed by tx are in the current unspent (i.e. in
	// getUTOXs()),
	// (2) the signatures on each input of tx are valid,
	// (3) no UTXO is claimed multiple times by tx,
	// (4) all of tx’s output values are positive, and
	// (5) the sum of tx’s input values is equal to the sum of its output
	// values;
	@Override
	public synchronized boolean isValid(Transaction tx) {
		Transaction trans = tx;
		switch (trans.getType()) {
		case Create:
			if (trans.numInputs() > 0)
				return false; // no inputs
			for (Transaction.Output op : trans.getOutputs()) {
				if (op.getValue() <= 0)
					return false; // outputs are positive
				if (op.getPublicKey() != scroogeKeyPair.getPublic())
					return false; // all outputs are s pk
			}
			// verify
			try {
				Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
				signature.initVerify(scroogeKeyPair.getPublic());
				signature.update(tx.getRawBytes());
				if (!signature.verify(trans.getSignature())) {
					return false;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return true;
		case Pay:
			Set<UTXO> utxo = getUTXOs();

			double ipsum = 0;
			for (int i = 0; i < trans.numInputs(); i++) {
				// verify input=>utxo &check whether same hash use twice
				Transaction.Input ip = trans.getInputs().get(i);
				int opindex = ip.getIndexOfTxOutput();
				int indexofledger = getLedgerIndex(ip.getHashOfOutputTx(), utxo, opindex, ip);
				if (indexofledger == -1)
					return false;
				// check input sig and get input value
				Transaction.Output ipop = ledger.get(indexofledger).getOutput(opindex);
				ipsum += ipop.getValue();
				PublicKey pk = ipop.getPublicKey();
				try {
					Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
					signature.initVerify(pk);
					signature.update(trans.getRawDataToSign(i));
					if (!signature.verify(ip.getSignature())) {
						return false;
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			double opsum = 0;
			for (Transaction.Output op : trans.getOutputs()) {
				if (op.getValue() <= 0)
					return false;
				opsum += op.getValue();
			}
			if (Math.abs(ipsum - opsum) < .000001) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	private int getLedgerIndex(byte[] hashOfOutputTx, Set<UTXO> utxo, int opindex, Transaction.Input ip) {
		for (int i = 0; i < ledger.size(); i++) {
			if (Arrays.equals(ledger.get(i).getHash(), hashOfOutputTx)) {// !
				HashPointer iphp = new HashPointer(ip.getHashOfOutputTx(), i);
				UTXO iputxo = new UTXO(iphp, opindex);
				if (utxo.contains(iputxo)) {
					return i;
				}
			}
		}
		return -1;
	}

	// Returns the complete set of currently unspent transaction outputs on the
	// ledger
	@Override
	public synchronized Set<UTXO> getUTXOs() {
		Set<UTXO> utxo = new HashSet<UTXO>();
		for (int ledgerindex = 0; ledgerindex < ledger.size(); ledgerindex++) {
			Transaction trans = ledger.get(ledgerindex);
			switch (trans.getType()) {
			case Create:
				for (Transaction.Output create : trans.getOutputs()) {
					int index = trans.getIndex(create);
					HashPointer createhp = new HashPointer(trans.getHash(), ledgerindex);
					UTXO createutxo = new UTXO(createhp, index);
					utxo.add(createutxo);
				}
				break;
			case Pay:
				// output=>utxo
				// input->utxo
				for (int i = 0; i < trans.numInputs(); i++) {
					Transaction.Input ip = trans.getInputs().get(i);
					int opindex = ip.getIndexOfTxOutput();
					HashPointer iphp = new HashPointer(ip.getHashOfOutputTx(),
							getLedgerIndex(ip.getHashOfOutputTx(), utxo, opindex, ip));

					UTXO iputxo = new UTXO(iphp, opindex);
					utxo.remove(iputxo);
				}
				for (Transaction.Output op : trans.getOutputs()) {
					int index = trans.getIndex(op);
					HashPointer ophp = new HashPointer(trans.getHash(), ledgerindex);
					UTXO oputxo = new UTXO(ophp, index);
					utxo.add(oputxo);
				}
				break;
			}

		}
		return utxo;
	}

}
