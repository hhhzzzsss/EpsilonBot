package com.github.hhhzzzsss.epsilonbot.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HashUtils {
	private static MessageDigest SHA256_DIGEST;

	private static String getHash(String raw) {
		byte[] hash = SHA256_DIGEST.digest(raw.getBytes(StandardCharsets.UTF_8));
		BigInteger big_int = new BigInteger(1, Arrays.copyOfRange(hash, 0, 4));
		return big_int.toString(Character.MAX_RADIX);
	}

	// /keyring add EpsilonBot sha-256 COMMAND;NAME;TIMESTAMP;KEY UTF-8 4 5 <KEY>
	// <text>;<username>;<unixMillis / 5000>;<key>
	public static boolean isValidHash(String text, String username, String key, String hash) {
		long time = System.currentTimeMillis() / 5000;
		String expectedHash = getHash(text + ";" + username + ";" + time + ";" + key);
		return hash.equals(expectedHash);
	}

	static {
		try {
			SHA256_DIGEST = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
