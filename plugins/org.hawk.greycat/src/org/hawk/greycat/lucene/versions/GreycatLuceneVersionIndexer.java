/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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
package org.hawk.greycat.lucene.versions;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndex;
import org.hawk.greycat.AbstractGreycatDatabase;
import org.hawk.greycat.lucene.AbstractLuceneIndexer;
import org.hawk.greycat.lucene.versions.GreycatLuceneVersionIndexer.GreycatLuceneVersionIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lucene-based index of relevant versions of nodes in the graph. It follows a very different
 * structure to the usual node indices, so it made more sense to keep it in its own class. Version
 * indices are much simpler than node indices: they are simply a set of time + node ID pairs. 
 */
public class GreycatLuceneVersionIndexer extends AbstractLuceneIndexer<GreycatLuceneVersionIndex> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatLuceneVersionIndexer.class);

	protected class GreycatLuceneVersionIndex implements ITimeAwareGraphNodeVersionIndex {

		private final String name;

		public GreycatLuceneVersionIndex(String name) {
			this.name = name;
		}

		@Override
		public void add(ITimeAwareGraphNode n) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void remove(ITimeAwareGraphNode n) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Iterator<ITimeAwareGraphNode> getAllVersions(ITimeAwareGraphNode n) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Iterator<ITimeAwareGraphNode> getVersionsSince(ITimeAwareGraphNode n) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Iterator<ITimeAwareGraphNode> getVersionsAfter(ITimeAwareGraphNode n) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Iterator<ITimeAwareGraphNode> getVersionsUntil(ITimeAwareGraphNode n) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Iterator<ITimeAwareGraphNode> getVersionsBefore(ITimeAwareGraphNode n) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void flush() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void delete() {
			// TODO Auto-generated method stub
			
		}
		
	}

	public GreycatLuceneVersionIndexer(AbstractGreycatDatabase db, File dir) throws IOException {
		super(db, dir);
	}

	@Override
	protected GreycatLuceneVersionIndex createIndexInstance(String name) {
		return new GreycatLuceneVersionIndex(name);
	}

	public void remove(ITimeAwareGraphNode n) {
		// TODO Auto-generated method stub
		
	}

}
