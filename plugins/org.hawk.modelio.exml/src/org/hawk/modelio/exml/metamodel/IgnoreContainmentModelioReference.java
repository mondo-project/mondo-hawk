package org.hawk.modelio.exml.metamodel;

import org.hawk.modelio.exml.metamodel.mlib.MDependency;

/**
 * Variant of {@link ModelioReference} that ignores the composition flag of the MDependency.
 * Needed in most cases since Modelio has per-instance containment, which is not compatible
 * with EMF. The solution is to add an extra reference that is always containment, and leave
 * the existing ones as non-containment references.
 */
public class IgnoreContainmentModelioReference extends ModelioReference {
	public IgnoreContainmentModelioReference(ModelioClass mc, MDependency mdep) {
		super(mc, mdep);
	}

	@Override
	public boolean isContainment() {
		return false;
	}

	@Override
	public boolean isContainer() {
		return false;
	}
}
