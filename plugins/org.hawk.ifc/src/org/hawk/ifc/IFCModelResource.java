/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - updates and maintenance
 ******************************************************************************/
package org.hawk.ifc;

import java.io.File;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.Deserializer;
import org.hawk.ifc.IFCModelFactory.IFCModelType;

public class IFCModelResource extends IFCAbstractModelResource {

	protected File ifc;

	public IFCModelResource(File f, IFCModelFactory p, IFCModelType type) {
		super(p, type);
		ifc = f;
	}

	@Override
	public void unload() {
		ifc = null;
	}

	@Override
	protected IfcModelInterface readModel(Deserializer d) throws DeserializeException {
		return d.read(ifc);
	}

	@Override
	public boolean providesSingletonElements() {
		return false;
	}
}
