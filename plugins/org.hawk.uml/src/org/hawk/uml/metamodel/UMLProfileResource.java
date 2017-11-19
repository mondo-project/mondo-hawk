package org.hawk.uml.metamodel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.eclipse.uml2.uml.resource.UMLResource;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;

/**
 * TODO just a stub for now, need to complete implementation
 */
public class UMLProfileResource implements IHawkMetaModelResource {

	public UMLProfileResource(UMLResource r) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void unload() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<IHawkObject> getAllContents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMetaModelResourceFactory getMetaModelResourceFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void save(OutputStream output, Map<Object, Object> options) throws IOException {
		// TODO Auto-generated method stub

	}

	public UMLResource getResource() {
		// TODO Auto-generated method stub
		return null;
	}

}
