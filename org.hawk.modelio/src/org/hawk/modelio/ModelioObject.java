package org.hawk.modelio;

import java.util.HashSet;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;

import org.hawk.core.model.*;

public class ModelioObject implements IHawkObject {

	protected EObject eob;

	public ModelioObject(EObject o) {
		eob = o;
	}

	public EObject getEObject() {
		return eob;

	}

	@Override
	public boolean isProxy() {
		return eob.eIsProxy();
	}

	@Override
	public String proxyURI() {
		return ((InternalEObject) eob).eProxyURI().fragment();
	}

	@Override
	public String getUri() {
		String uri = org.eclipse.emf.ecore.util.EcoreUtil.getURI(eob)
				.toString();
		if (uri == null || uri == "" || uri == "/" || uri == "//")
			System.err.println("URI error on: " + eob);
		return uri;

	}

	@Override
	public String getUriFragment() {
		String frag = org.eclipse.emf.ecore.util.EcoreUtil.getURI(eob)
				.fragment();
		if (frag == null || frag == "" || frag == "/")
			System.err.println("fragment error on: "
					+ org.eclipse.emf.ecore.util.EcoreUtil.getURI(eob)
							.toString());

		return frag;
	}

	@Override
	public IHawkClassifier getType() {

		return new ModelioClass(eob.eClass());
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		return eob.eIsSet(eob.eClass().getEStructuralFeature(hsf.getName()));
	}

	@Override
	public Object get(IHawkAttribute attribute) {
		return eob
				.eGet(eob.eClass().getEStructuralFeature(attribute.getName()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object get(IHawkReference reference, boolean b) {

		Object ret;

		Object source = eob.eGet(
				eob.eClass().getEStructuralFeature(reference.getName()), b);

		if (source instanceof Iterable<?>) {
			ret = new HashSet<EObject>();
			for (EObject e : ((Iterable<EObject>) source)) {
				((HashSet<ModelioObject>) ret).add(new ModelioObject(e));
			}
		} else
			ret = new ModelioObject((EObject) source);

		return ret;

	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ModelioObject)
			return eob.equals(((ModelioObject) o).getEObject());
		else
			return false;
	}

	@Override
	public int hashCode() {
		return eob.hashCode();
	}

	@Override
	public String toString() {

		String ret = "";

		ret += ">" + eob + "\n";

		for (EAttribute e : eob.eClass().getEAllAttributes())
			ret += e + " : "+ eob.eGet(e);
		ret += "\n";

		return ret;

	}



}
