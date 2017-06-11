package hw5;

import java.text.NumberFormat;
import java.util.Locale;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

public class CustomAddressGenerator {

	protected final static Logger log = LoggerFactory.getLogger(CustomAddressGenerator.class);
	private static final NetworkParameters NET_PARAMS = MainNetParams.get();
	private static final int BTC_ADDRESS_MAX_LENGTH = 35;
	private static long attempts;

	private static boolean isValidBTCAddressSubstring(final String substring) {
		boolean validity = true;
		if (!CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(substring) || substring.length() > BTC_ADDRESS_MAX_LENGTH
				|| CharMatcher.anyOf("OIl0").matchesAnyOf(substring)) {
			validity = false;
		}
		return validity;
	}

	private static void logAttempts() {
		if (attempts % 100000 == 0) {
			log.info("Thread " + Thread.currentThread().getName() + " is still working, # of attempts: "
					+ NumberFormat.getNumberInstance(Locale.US).format(attempts));
		}
	}

	/*
	 * @param prefix string of letters
	 * 
	 * @returns key whose Bitcoin address on mainnet starts with 1 followed
	 * prefix.
	 */
	public static ECKey get(String prefix) {
		if (isValidBTCAddressSubstring(prefix)) {
			log.info("Searching for a bitcoin address that starts with: " + prefix);
			ECKey key;
			do {
				key = new ECKey();
				attempts++;
				logAttempts();
			} while (!(key.toAddress(NET_PARAMS).toString().startsWith(prefix)));
			log.info("Exiting thread " + Thread.currentThread().getName() + ", Attempts made: "
					+ NumberFormat.getNumberInstance(Locale.US).format(attempts));
			return key;
		} else {
			System.out.println("Your prefix contains illegal characters!");
			return null;
		}
	}

	public static void main(String args[]) {
		ECKey key = get("1ZHEN");
		System.out.println(key.toAddress(NET_PARAMS));
		System.out.println("Private Key: " + key.getPrivKey());
	}

}
