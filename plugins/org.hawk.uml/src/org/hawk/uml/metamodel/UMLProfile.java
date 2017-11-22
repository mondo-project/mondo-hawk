package org.hawk.uml.metamodel;

import org.eclipse.emf.ecore.EPackage;
import org.hawk.core.model.IHawkAnnotation;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.emf.EMFPackage;
import org.hawk.emf.EMFWrapperFactory;

/**
 * Tweaks a UML profile mapped to an EMF package to add a suffix to its nsURI
 * with the version.
 */
public class UMLProfile extends EMFPackage {

	public UMLProfile(EPackage o, EMFWrapperFactory wf, IHawkMetaModelResource umlProfileResource) {
		super(o, wf, umlProfileResource);
	}

	@Override
	public String getNsURI() {
		final String baseURI = super.getNsURI();

		for (IHawkAnnotation ann : getAnnotations()) {
			if ("PapyrusVersion".equals(ann.getSource())) {
				return String.format("%s/%s", baseURI, ann.getDetails().get("Version"));
			}
		}

		return baseURI;
	}
}
