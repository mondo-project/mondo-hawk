package org.hawk.uml.metamodel;

import org.eclipse.emf.ecore.EClass;
import org.hawk.emf.EMFClass;
import org.hawk.emf.EMFWrapperFactory;

/**
 * Virtual class that represents the profile itself, to be used to find profile
 * applications through <code>X.all</code>.
 */
public class UMLProfileVirtualClass extends EMFClass {

	private UMLProfile profile;

	public UMLProfileVirtualClass(EClass eClass, UMLProfile umlProfile, EMFWrapperFactory wf) {
		super(eClass, wf);
		this.profile = umlProfile;
	}

	@Override
	public String getPackageNSURI() {
		return profile.getNsURI();
	}
	
}
