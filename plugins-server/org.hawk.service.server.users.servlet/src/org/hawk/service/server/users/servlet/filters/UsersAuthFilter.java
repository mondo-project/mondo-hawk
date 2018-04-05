/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.server.users.servlet.filters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;

import org.apache.shiro.web.servlet.IniShiroFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("deprecation")
public class UsersAuthFilter extends IniShiroFilter {

	public UsersAuthFilter() throws URISyntaxException, IOException {
		final Bundle bundle = FrameworkUtil.getBundle(UsersAuthFilter.class);
		final File shiroFile = bundle.getDataFile("shiro.ini");
		if (!shiroFile.exists()) {
			final URL urlDefaultShiro = bundle.getEntry("resources/shiro.ini.default");
			try (final InputStream is = urlDefaultShiro.openConnection().getInputStream()) {
				Files.copy(is, shiroFile.toPath());
			}
		}

		setConfigPath(URLDecoder.decode(shiroFile.toURI().toASCIIString(), "US-ASCII"));
	}

}
