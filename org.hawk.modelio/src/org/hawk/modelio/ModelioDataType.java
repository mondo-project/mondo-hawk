package org.hawk.modelio;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;

import org.hawk.core.model.*;

public class ModelioDataType extends ModelioObject implements IHawkDataType {

	private EDataType edatatype;

	// private String containingFeatureName = null;

	// private static HashMap<EClass, Collection<EClass>> eAllSubTypes;

	public ModelioDataType(EDataType eDataType) {

		super(eDataType);
		edatatype = ((EDataType) eDataType);

	}

	public EObject getEObject() {
		return edatatype;

	}

	@Override
	public String getName() {
		return edatatype.getName();
	}

	@Override
	public String getPackageNSURI() {
		return edatatype.getEPackage().getNsURI();
	}

}
