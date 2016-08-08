package org.hawk.service.emf.impl;


import org.eclipse.emf.common.util.URI;

/**
 * Resource factory that forces Hawk resources to not split their contents
 * across surrogate fields. This is needed for transformation tools such
 * as CloudATL, for instance.
 */
public class UnsplitHawkResourceFactoryImpl extends HawkResourceFactoryImpl {

	@Override
	public HawkResourceImpl createResource(URI uri) {
		HawkResourceImpl r = super.createResource(uri);
		r.getDescriptor().setSplit(false);
		return r;
	}

}
