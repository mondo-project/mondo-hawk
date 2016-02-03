/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.ui.emfresource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory;
import org.hawk.emfresource.impl.LocalHawkResourceImpl;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;

public class LocalHawkResourceFactoryImpl implements Factory {
	public static final String FILE_ENCODING = "UTF-8";
	public static final String OPTION_UNSPLIT = "unsplit";
	public static final String OPTION_RPATTERNS = "repos";
	public static final String OPTION_FPATTERNS = "files";
	public static final String KEYVAL_SEPARATOR = "=";
	public static final String PATTERN_SEPARATOR = ",";

	@Override
	public Resource createResource(URI uri) {
		String hawkInstance;
		boolean isSplit = true;
		List<String> repoPatterns = Arrays.asList("*");
		List<String> filePatterns = repoPatterns;

		if ("hawk+local".equals(uri.scheme())) {
			hawkInstance = uri.host();
		} else {
			final String filePath = CommonPlugin.resolve(uri).toFileString();
			try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), FILE_ENCODING))) {
				hawkInstance = br.readLine();

				String optionLine;
				while ((optionLine = br.readLine()) != null) {
					optionLine = optionLine.trim();
					String[] parts = optionLine.split(KEYVAL_SEPARATOR, 2);
					if (parts.length == 1 && parts[0].equals(OPTION_UNSPLIT)) {
						isSplit = false;
					} else if (parts.length == 2 && parts[0].equals(OPTION_RPATTERNS)) {
						repoPatterns = Arrays.asList(parts[1].split(PATTERN_SEPARATOR));
					} else if (parts.length == 2 && parts[0].equals(OPTION_FPATTERNS)) {
						filePatterns = Arrays.asList(parts[1].split(PATTERN_SEPARATOR));
					}
				}
			} catch (IOException e) {
				Activator.logError("Could not read " + filePath, e);
				return new LocalHawkResourceImpl(uri, null, isSplit, repoPatterns, filePatterns);
			}
		}

		final HUIManager manager = HUIManager.getInstance();
		final HModel hawkModel = manager.getHawkByName(hawkInstance);
		if (hawkModel == null) {
			return new LocalHawkResourceImpl(uri, null, isSplit, repoPatterns, filePatterns);
		}
		if (!hawkModel.isRunning()) {
			hawkModel.start(manager);
		}
		return new LocalHawkResourceImpl(uri, hawkModel.getIndexer(), isSplit, repoPatterns, filePatterns);
	}
}
;