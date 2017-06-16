package hw6;

import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import static org.bitcoinj.script.ScriptOpCodes.*;

import java.util.ArrayList;
import java.util.Random;

public class GroupMultiSigTransaction extends ScriptTester {

	private DeterministicKey keyBank;
	private DeterministicKey keyCus1;
	private DeterministicKey keyCus2;
	private DeterministicKey keyCus3;

	public GroupMultiSigTransaction(WalletAppKit kit) {
		super(kit);
		keyBank = kit.wallet().freshReceiveKey();
		keyCus1 = kit.wallet().freshReceiveKey();
		keyCus2 = kit.wallet().freshReceiveKey();
		keyCus3 = kit.wallet().freshReceiveKey();
	}

	@Override
	public Script createLockingScript() {
		ScriptBuilder reedemBuilder = new ScriptBuilder();

		reedemBuilder.op(OP_2);
		reedemBuilder.data(keyBank.getPubKey());
		reedemBuilder.data(keyCus1.getPubKey());
		reedemBuilder.data(keyCus2.getPubKey());
		reedemBuilder.data(keyCus3.getPubKey());
		reedemBuilder.op(OP_4);
		reedemBuilder.op(OP_CHECKMULTISIG);

		return reedemBuilder.build();
	}

	@Override
	public Script createUnlockingScript(Transaction unsignedTransaction) {
		Random rn = new Random();
		int num = (rn.nextInt(3 - 1 + 1) + 1);
		ArrayList<DeterministicKey> keys = new ArrayList<DeterministicKey>();
		keys.add(keyCus1);
		keys.add(keyCus2);
		keys.add(keyCus3);
		TransactionSignature txSigBank = sign(unsignedTransaction, keyBank);
		TransactionSignature txSigC = sign(unsignedTransaction, keys.get(num - 1));
		ScriptBuilder builder = new ScriptBuilder();
		builder.smallNum(OP_0);
		builder.smallNum(OP_0);
		builder.data(txSigBank.encodeToBitcoin());
		builder.data(txSigC.encodeToBitcoin());
		return builder.build();
	}

	public static void main(String[] args) throws InsufficientMoneyException, InterruptedException {
		WalletInitTest wit = new WalletInitTest();
		new GroupMultiSigTransaction(wit.getKit()).run();
		wit.monitor();
	}

}
