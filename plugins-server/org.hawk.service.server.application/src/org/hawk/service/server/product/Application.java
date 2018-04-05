/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.server.product;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.hawk.service.server.cli.ServerCommandProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application implements IApplication {

	private static final String TEST_NODE = "mondo.test";
	private static final String TEST_KEY = "testvalue";
	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		// We need to test the secure store, so the user will get a warning
		// if they haven't set up a proper password.
		ISecurePreferences factory = SecurePreferencesFactory.getDefault();
		final ISecurePreferences node = factory.node(TEST_NODE);
		node.put(TEST_KEY, "1", true);
		if (!node.isEncrypted(TEST_KEY)) {
			LOGGER.error("Secure store not encrypted: please revise your setup!");
		} else {
			LOGGER.info("Secure store encrypted: setup is OK");
		}
		factory.flush();

		System.out.println("\n"
				+ "Welcome to the Hawk Server!\n"
				+ "List available commands with '" + ServerCommandProvider.HSERVER_HELP_CMD + "'.\n"
				+ "Stop the server with 'shutdown'.\n");

		// We don't really do anything at the moment for the application:
		// we just want a working Equinox instance for now		
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// nothing to do!
	}

}
