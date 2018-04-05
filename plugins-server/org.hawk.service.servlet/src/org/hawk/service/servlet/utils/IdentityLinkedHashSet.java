/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.servlet.utils;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * {@link LinkedHashSet} which uses {@link System#identityHashCode(Object)}
 * instead of {@link Object#hashCode()} for keys.
 */
public class IdentityLinkedHashSet<E> extends AbstractSet<E> {
	private static class IdentityWrapper {
		private final Object object;

		public IdentityWrapper(Object o) {
			this.object = o;
		}

		public Object getObject() {
			return this.object;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(object);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof IdentityWrapper)) {
				return false;
			}
			return this.object == ((IdentityWrapper)other).object;
		}
	}

	private final LinkedHashSet<IdentityLinkedHashSet.IdentityWrapper> internal = new LinkedHashSet<>();

	@Override
	public int size() {
		return internal.size();
	}

	@Override
	public Iterator<E> iterator() {
		final Iterator<IdentityLinkedHashSet.IdentityWrapper> itWrapper = internal.iterator();
		return new Iterator<E>() {
			@Override
			public boolean hasNext() {
				return itWrapper.hasNext();
			}

			@SuppressWarnings("unchecked")
			@Override
			public E next() {
				return (E)itWrapper.next().getObject();
			}

			@Override
			public void remove() {
				itWrapper.remove();
			}
		};
	}

	@Override
	public boolean contains(Object o) {
		return internal.contains(new IdentityWrapper(o));
	}

	@Override
	public boolean add(E e) {
		return internal.add(new IdentityWrapper(e));
	}

	@Override
	public boolean remove(Object o) {
		return internal.remove(new IdentityWrapper(o));
	}

	@Override
	public void clear() {
		internal.clear();
	}

}