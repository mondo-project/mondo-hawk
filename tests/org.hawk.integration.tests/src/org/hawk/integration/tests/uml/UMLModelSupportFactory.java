package org.hawk.integration.tests.uml;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.integration.tests.ModelIndexingTest.IModelSupportFactory;
import org.hawk.uml.metamodel.UMLMetaModelResourceFactory;
import org.hawk.uml.model.UMLModelResourceFactory;

public class UMLModelSupportFactory implements IModelSupportFactory {

	@Override
	public IMetaModelResourceFactory createMetaModelResourceFactory() {
		return new UMLMetaModelResourceFactory();
	}

	@Override
	public IModelResourceFactory createModelResourceFactory() {
		return new UMLModelResourceFactory();
	}

}
