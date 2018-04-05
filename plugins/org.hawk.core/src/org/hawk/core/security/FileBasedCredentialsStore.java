/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core.security;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.hawk.core.ICredentialsStore;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Default implementation that uses a local file and encrypts passwords using
 * built-in ciphers in Java with a random 8-byte salt. Does not require any
 * external dependencies.
 */
public class FileBasedCredentialsStore implements ICredentialsStore {

	private final File store;

	private char[] encryptionKey; 
	private byte[] salt;

	private Map<String, Credentials> credentials;

	public FileBasedCredentialsStore(File store, char[] encryptionKey) {
		this.store = store;
		this.encryptionKey = encryptionKey;
	}

	@Override
	public void put(String repositoryKey, Credentials creds) throws IOException, GeneralSecurityException {
		checkOpen();
		credentials.put(repositoryKey, creds);
	}

	@Override
	public Credentials get(String repositoryKey) throws IOException, GeneralSecurityException {
		checkOpen();
		return credentials.get(repositoryKey);
	}

	@Override
	public void remove(String repositoryKey) throws IOException, GeneralSecurityException {
		checkOpen();
		credentials.remove(repositoryKey);
	}

	private void checkOpen() throws IOException, GeneralSecurityException {
		if (credentials != null) return;

		credentials = new HashMap<>();
		if (store.exists()) {
			load();
		} else {
			store.createNewFile();
			SecureRandom random = new SecureRandom();
			salt = random.generateSeed(8); 
			save();
		}
	}

	private void load() throws IOException, GeneralSecurityException {
		XStream stream = getXStream();

		CredentialsFile credsFile = null;
		try (BufferedReader br = new BufferedReader(new FileReader(store))) {
			credsFile = (CredentialsFile)stream.fromXML(br);
		}

		salt = base64Decode(credsFile.getBase64Salt());
		credentials.clear();
		for (CredentialsFileEntry fileEntry : credsFile.getEntries()) {
			credentials.put(fileEntry.getRepositoryKey(),
					new Credentials(fileEntry.getUsername(), decrypt(fileEntry.getPassword())));
		}
	}

	private void save() throws IOException, GeneralSecurityException {
		final CredentialsFile credsFile = new CredentialsFile();
		credsFile.setBase64Salt(base64Encode(salt));

		final List<CredentialsFileEntry> fileEntries = new ArrayList<>();
		for (Entry<String, Credentials> credEntry : credentials.entrySet()) {
			final Credentials creds = credEntry.getValue();
			final CredentialsFileEntry fileEntry = new CredentialsFileEntry(
					credEntry.getKey(), creds.getUsername(), encrypt(creds.getPassword()));
			fileEntries.add(fileEntry);
		}
		credsFile.setEntries(fileEntries);

		XStream stream = getXStream();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(store))) {
			stream.toXML(credsFile, bw);
		}
	}

	private XStream getXStream() {
		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(CredentialsFile.class);
		stream.processAnnotations(CredentialsFileEntry.class);
		stream.setClassLoader(getClass().getClassLoader());
		return stream;
	}

	@Override
	public void shutdown() throws IOException, GeneralSecurityException {
		if (credentials != null) {
			save();
			encryptionKey = null;
			credentials = null;
			salt = null;
		}
	}

	private String encrypt(String property)
			throws GeneralSecurityException, UnsupportedEncodingException {

		SecretKeyFactory keyFactory = SecretKeyFactory
				.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(encryptionKey));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher
				.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, 20));
		return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
	}

	private String decrypt(String property)
			throws GeneralSecurityException, IOException {
		SecretKeyFactory keyFactory = SecretKeyFactory
				.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(encryptionKey));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher
				.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, 20));
		return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
	}

	private static byte[] base64Decode(String property) throws IOException {
		// NB: This class is internal, and you probably should use another impl
		return new Base64().decode(property);
	}

	private static String base64Encode(byte[] bytes) {
		// NB: This class is internal, and you probably should use another impl
		return new Base64().encodeAsString(bytes);
	}
}
