package org.hawk.uml.model;

import java.util.Set;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.ProfileApplication;
import org.eclipse.uml2.uml.util.UMLUtil;
import org.hawk.core.model.IHawkClass;
import org.hawk.emf.EMFClass;
import org.hawk.emf.EMFWrapperFactory;
import org.hawk.uml.metamodel.UMLProfile;

/**
 * This decorates UML profiles so they will have a supertype reference
 * to the virtual profile classes of any profiles applied to them. That
 * makes it possible to find the profile through <code>X.all</code>.
 */
public class UMLProfiledPackageType extends EMFClass {

	private final Package umlPackage;

	public UMLProfiledPackageType(Package pkg, EMFWrapperFactory wf) {
		super(pkg.eClass(), wf);
		this.umlPackage = pkg;
	}

	@Override
	public Set<IHawkClass> getAllSuperTypes() {
		final Set<IHawkClass> ret = super.getAllSuperTypes();

		for (ProfileApplication app : umlPackage.getProfileApplications()) {
			final EAnnotation ann = app.getEAnnotation(UMLUtil.UML2_UML_PACKAGE_2_0_NS_URI);
			if (ann != null) {
				for (EObject ref : ann.getReferences()) {
					if (ref instanceof EPackage) {
						final EPackage appEPackage = (EPackage) ref;
						final UMLProfile umlProfile = new UMLProfile(appEPackage, wf, null);
						ret.add(umlProfile.getVirtualProfileClass());
					}
				}
			}
		}

		return ret;
	}
	
}
