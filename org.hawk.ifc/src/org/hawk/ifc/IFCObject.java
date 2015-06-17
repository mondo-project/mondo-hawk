package org.hawk.ifc;

import java.util.HashSet;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.hawk.core.model.*;

public class IFCObject implements IHawkObject {

	protected EObject eob;

	public IFCObject(EObject o) {

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
	public String proxyURIFragment() {
		return ((InternalEObject) eob).eProxyURI().fragment();
	}

	@Override
	public String proxyURI() {
		return ((InternalEObject) eob).eProxyURI().toString();
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

		return new IFCClass(eob.eClass());
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
				((HashSet<IFCObject>) ret).add(new IFCObject(e));
			}
		} else
			ret = new IFCObject((EObject) source);

		return ret;

	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IFCObject)
			return eob.equals(((IFCObject) o).getEObject());
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
			ret += e + " : " + eob.eGet(e);
		ret += "\n";

		return ret;

	}

}
