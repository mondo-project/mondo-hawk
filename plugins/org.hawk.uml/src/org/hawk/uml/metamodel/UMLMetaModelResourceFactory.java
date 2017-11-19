package org.hawk.uml.metamodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.uml2.types.TypesPackage;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;
import org.hawk.emf.metamodel.EMFMetaModelResource;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;

/**
 * Adds support for the UML metamodel.
 *
 * TODO: profile support
 */
@SuppressWarnings("restriction")
public class UMLMetaModelResourceFactory implements IMetaModelResourceFactory {

	private static final String MM_EXTENSION = ".profile.uml";
	private EMFMetaModelResourceFactory emfMMFactory = new EMFMetaModelResourceFactory();
	private ResourceSet resourceSet;

	public UMLMetaModelResourceFactory() {
		if (EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI) == null) {
			EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI,
					EcorePackage.eINSTANCE);
		}

		resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("uml", new UMLResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("ecore", new EcoreResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("*", new XMIResourceFactoryImpl());
	}

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public String getHumanReadableName() {
		return "UML Metamodel Resource Factory";
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		UMLResource r = (UMLResource) resourceSet.createResource(URI.createFileURI(f.getAbsolutePath()));
		r.load(null);
		return new UMLProfileResource(r);
	}

	@Override
	public Set<IHawkMetaModelResource> getStaticMetamodels() {
		Set<IHawkMetaModelResource> resources = new HashSet<>();
		resources.add(new EMFMetaModelResource(EcorePackage.eINSTANCE.eResource(), this));
		resources.add(new EMFMetaModelResource(TypesPackage.eINSTANCE.eResource(), this));
		resources.add(new EMFMetaModelResource(UMLPackage.eINSTANCE.eResource(), this));
		return resources;
	}

	@Override
	public void shutdown() {
		// nothing to do for now
	}

	@Override
	public boolean canParse(File f) {
		return f.getName().toLowerCase().endsWith(MM_EXTENSION);
	}

	@Override
	public Collection<String> getMetaModelExtensions() {
		return Collections.singleton(MM_EXTENSION);
	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents) throws Exception {
		if (name == null || contents == null) {
			return null;
		}

		Resource r = resourceSet.createResource(URI.createURI(name));
		InputStream input = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		r.load(input, null);

		if (r instanceof UMLResource) {
			return new UMLProfileResource((UMLResource) r);
		} else {
			return new EMFMetaModelResource(r, this);
		}
	}

	@Override
	public String dumpPackageToString(IHawkPackage ePackage) throws Exception {
		// TODO add UML-specific logic here (profiles?)
		return emfMMFactory.dumpPackageToString(ePackage);
	}

}
