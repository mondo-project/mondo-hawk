/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.greycat;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.rocksdb.RocksDBStorage;

/**
 * <p>Version of the Greycat backend, which uses the RocksDB storage layer
 * with Snappy compression on Linux/Mac, and no compression on Windows.</p>
 * 
 * <p>This storage layer has bad performance in the subtree tests for the
 * time being. The LevelDB storage layer has much better performance for
 * traversing the graph - please use that instead in those cases.</p>
 */
@Deprecated
public class RocksDBGreycatDatabase extends AbstractGreycatDatabase {

	@Override
	protected Graph createGraph() {
		return new GraphBuilder()
			.withMemorySize(1_000_000)
			.withStorage(new RocksDBStorage(storageFolder.getAbsolutePath()))
			.build();
	}

	@Override
	public final String getType() {
		// For backwards compatibiltiy only
		return "org.hawk.greycat.GreycatDatabase";
	}
	
	@Override
	public String getHumanReadableName() {
		return "RocksDB " + super.getHumanReadableName() + " (deprecated)";
	}

}
