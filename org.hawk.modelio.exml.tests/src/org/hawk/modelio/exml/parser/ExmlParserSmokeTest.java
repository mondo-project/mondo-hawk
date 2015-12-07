/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.parser;

import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Test;

/**
 * Smoke test for the {@link ExmlParser} that tries to parse collections of
 * <code>.exml</code> files and see if we run into any errors.
 */
public class ExmlParserSmokeTest {

	@Test
	public void zoo() throws Exception {
		final ExmlParser parser = new ExmlParser();
		final FileVisitor<Path> fv = new SimpleFileVisitor<Path>(){
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				final String fileName = path.toFile().getName();
				if (fileName.endsWith(".exml")) {
					try (final InputStream is = new BufferedInputStream(new FileInputStream(path.toFile()))) {
						parser.getObject(is);
					} catch (Exception e) {
						fail("Failed to parse " + fileName + ": " + e.getMessage());
					}
				}
				return FileVisitResult.CONTINUE;
			}
		};
		Files.walkFileTree(new File("resources").toPath(), fv);
	}
}
