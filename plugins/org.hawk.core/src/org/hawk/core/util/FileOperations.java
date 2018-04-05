/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     + http://stackoverflow.com/questions/106770/standard-concise-way-to
 *         -copy -a-file-in-java accessed: 8/12/2011, 13:15gmt
 ******************************************************************************/
package org.hawk.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileOperations {

	/**
	 * Copies a file (sourceFile) to another file (destFile)
	 * 
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile)
			throws IOException {
		if (destFile.isDirectory()) {
			System.err
					.println("Directory given to copyFile(File sourceFile, File destFile), returning with no copying");
			return;
		}

		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		try (final FileInputStream fisSource = new FileInputStream(sourceFile);
				final FileOutputStream fosDest = new FileOutputStream(destFile);) {
			final FileChannel source = fisSource.getChannel();
			final FileChannel destination = fosDest.getChannel();
			destination.transferFrom(source, 0, source.size());
		}
	}

	public static boolean deleteFiles(File file, Boolean prev) {

		boolean success = prev;

		if (file.isDirectory()) {

			if (file.list().length == 0)
				success = success && file.delete();
			else {
				File[] files = file.listFiles();

				for (File temp : files) {
					deleteFiles(temp, success);
				}

				if (file.list().length == 0)
					success = success && file.delete();
				else {
					success = false;
				}
			}

		} else
			success = success && file.delete();

		return success;

	}

}
