package org.hawk.uml.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.hawk.core.IFileImporter;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.emf.model.EMFModelResource;
import org.hawk.uml.metamodel.UMLWrapperFactory;

public class UMLModelResourceFactory implements IModelResourceFactory {

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public String getHumanReadableName() {
		return "UML Model Resource Factory";
	}

	@Override
	public IHawkModelResource parse(IFileImporter importer, File changedFile) throws Exception {
		ResourceSet rset = new ResourceSetImpl();
		UMLResourcesUtil.init(rset);
		Resource r = rset.createResource(URI.createFileURI(changedFile.getAbsolutePath()));
		r.load(null);

		return new EMFModelResource(r, new UMLWrapperFactory(), this);
	}

	@Override
	public void shutdown() {
		// nothing to do for now
	}

	@Override
	public boolean canParse(File f) {
		return f.getName().toLowerCase().endsWith(".uml");
	}

	@Override
	public Collection<String> getModelExtensions() {
		return Collections.singleton(".uml");
	}

}
