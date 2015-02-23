package org.hawk.epsilon.emc;

import java.util.HashSet;
import java.util.Set;

import org.hawk.core.query.IAccess;
import org.hawk.core.query.IAccessListener;

public class AccessListener implements IAccessListener {

	private Set<IAccess> accesses = new HashSet<>();

	private String sourceObject;

	public void accessed(String accessObject, String property) {
		accesses.add(new Access(sourceObject, accessObject, property));
		// System.err.println("access:" + query + "\non:" + sourceObject);
		// System.err.println(accessObject + " -- " + property);
	}

	public void setSourceObject(String s) {

		sourceObject = s;

	}

	public Set<IAccess> getAccesses() {
		return accesses;
	}

	public void resetAccesses() {
		accesses.clear();
	}

	@Override
	public void removeAccess(IAccess a) {
		accesses.remove(a);
	}

}
