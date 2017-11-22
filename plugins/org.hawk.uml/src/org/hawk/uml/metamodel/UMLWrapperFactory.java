package org.hawk.uml.metamodel;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.emf.EMFClass;
import org.hawk.emf.EMFPackage;
import org.hawk.emf.EMFWrapperFactory;

public class UMLWrapperFactory extends EMFWrapperFactory {

	@Override
	public EMFPackage createPackage(EPackage pkg, IHawkMetaModelResource res) {
		if (isProfile(pkg)) {
			return new UMLProfile(pkg, this, res);
		} else {
			return new EMFPackage(pkg, this, res);
		}
	}

	@Override
	public EMFClass createClass(EClass cls) {
		if (isProfile(cls.getEPackage())) {
			return new UMLStereotype(cls, this);
		} else {
			return new EMFClass(cls, this);
		}
	}

	public boolean isProfile(EPackage pkg) {
		return pkg.getEAnnotation("PapyrusVersion") != null;
	}

}
