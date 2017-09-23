package org.hawk.modelio.exml.metamodel;

import org.hawk.modelio.exml.metamodel.mlib.MDependency;

/**
 * Variant of {@link ModelioReference} that always reports itself to be a
 * container reference. Useful for the simulated containment reference.
 */
class AlwaysContainerModelioReference extends ModelioReference{

	public AlwaysContainerModelioReference(ModelioClass mc, MDependency mdep) {
		super(mc, mdep);
	}

	@Override
	public boolean isContainment() {
		return false;
	}

	@Override
	public boolean isContainer() {
		return true;
	}
}
