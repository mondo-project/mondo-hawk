package org.hawk.uml.metamodel;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.hawk.emf.EMFClass;
import org.hawk.emf.EMFWrapperFactory;

/**
 * Small wrapper over UML stereotypes mapped to EMF EClasses to use the same
 * /X.Y.Z as for profile EPackages.
 */
public class UMLStereotype extends EMFClass {
	public UMLStereotype(EClass o, EMFWrapperFactory wf) {
		super(o, wf);
	}

	@Override
	public String getPackageNSURI() {
		final EPackage pkg = eClass.getEPackage();
		return new UMLProfile(pkg, wf, null).getNsURI();
	}
}
