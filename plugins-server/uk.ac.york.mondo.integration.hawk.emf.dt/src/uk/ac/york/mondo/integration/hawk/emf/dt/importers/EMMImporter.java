/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.hawk.emf.dt.importers;

import uk.ac.york.mondo.integration.api.EffectiveMetamodelRuleset;

/**
 * Generic interface for an element that asks for some input and extends the
 * effective metamodel in a <code>.hawkmodel</code> file.
 */
public interface EMMImporter {

	/**
	 * Asks for some input and extends the provided effective metamodel.
	 */
	void importEffectiveMetamodelInto(EffectiveMetamodelRuleset targetEMM);
}
