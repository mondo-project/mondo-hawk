/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - use Java 7 Path instead of File+string processing, use SHA1 to only include unique content
 ******************************************************************************/
package org.hawk.localfolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Repository that only includes one copy of each distinct file in a local folder.
 * Useful for model technologies that use UUID-based references and may keep multiple
 * copies of the same model fragment in different locations (e.g. Modelio).
 */
public class UniqueLocalFolder extends LocalFolder {

	@Override
	public String getHumanReadableName() {
		return super.getHumanReadableName() + " - unique by SHA1";
	}

	@Override
	protected void addAllFiles(File dir, Set<File> ret) {
		addAllFiles(dir, ret, new HashSet<String>());
	}

	private void addAllFiles(File dir, Set<File> ret, Set<String> knownChecksums) {
		File[] files = dir.listFiles();
		if (files == null) {
			// couldn't list files in that directory
			console.printerrln("Could not list the entries of " + dir);
			return;
		}
		for (File file : files) {
			if (!file.isDirectory()) {
				try {
					final String sha1 = computeSHA1(file);
					if (knownChecksums.add(sha1)) {
						ret.add(file);
					}
				} catch (Exception e) {
					e.printStackTrace();
					ret.add(file);
				}
			} else {
				addAllFiles(file, ret, knownChecksums);
			}
		}
	}

	private String computeSHA1(File file) throws NoSuchAlgorithmException, IOException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
	    try (InputStream input = new FileInputStream(file)) {
	        byte[] buffer = new byte[8192];
	        int len = input.read(buffer);

	        while (len != -1) {
	            sha1.update(buffer, 0, len);
	            len = input.read(buffer);
	        }

	        return new HexBinaryAdapter().marshal(sha1.digest());
	    }		
	}
	
}
