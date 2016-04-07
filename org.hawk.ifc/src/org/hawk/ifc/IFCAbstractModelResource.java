package org.hawk.ifc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.MetaDataManager;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepDeserializer;
import org.bimserver.ifc.step.deserializer.Ifc4StepDeserializer;
import org.bimserver.ifc.xml.deserializer.Ifc2x3tc1XmlDeserializer;
import org.bimserver.ifc.xml.deserializer.Ifc4XmlDeserializer;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.models.ifc2x3tc1.IfcPlaneAngleMeasure;
import org.bimserver.models.ifc4.Ifc4Package;
import org.bimserver.plugins.Plugin;
import org.bimserver.plugins.PluginDescriptor;
import org.bimserver.plugins.PluginImplementation;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.PluginSourceType;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.Deserializer;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.ifc.IFCModelFactory.IFCModelType;

public abstract class IFCAbstractModelResource implements IHawkModelResource {

	private final IFCModelFactory factory;
	private final IFCModelType ifcModelType;
	private Set<IHawkObject> allElements;

	public IFCAbstractModelResource(IFCModelFactory p, IFCModelType type) {
		this.factory = p;
		this.ifcModelType = type;
	}

	protected abstract IfcModelInterface readModel(Deserializer d)
			throws DeserializeException, IOException;

	@Override
	public Iterable<IHawkObject> getAllContents() {
		return getAllContentsSet();
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		if (allElements == null) {
			allElements = new HashSet<IHawkObject>();

			try {
				Deserializer d = createDeserializer();
				IfcModelInterface s = readModel(d);
				for (IdEObject eo : s.getValues()) {
					allElements.add(new IFCObject(eo));
					addFloating(allElements, eo);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
		return allElements;
	}

	/**
	 * IFC {@link EObject}s sometimes refer to "floating" {@link EObject}s that
	 * are not returned by {@link IfcModelInterface#getValues()} and are not
	 * part of the IFC Resource either (their URI is "#//").
	 * 
	 * For instance, in <code>samples/WallOnly.ifc</code>, this was happening
	 * with the {@link IfcPlaneAngleMeasure} in this line:
	 * 
	 * <pre>
	 * #13 = IFCMEASUREWITHUNIT(IFCPLANEANGLEMEASURE(1.745E-2), #14);
	 * </pre>
	 * 
	 * We need to traverse all references manually and add those values back
	 * into the set. To ensure termination, we only proceed recursively if the
	 * added value was not already part of the set.
	 */
	private void addFloating(Set<IHawkObject> allElements, EObject eo) {
		for (EReference eref : eo.eClass().getEAllReferences()) {
			final Object refValue = eo.eGet(eref);
			if (refValue instanceof Iterable<?>) {
				for (Object o : (Iterable<?>)refValue) {
					final EObject eoRef = (EObject)o;
					if (eoRef.eResource() == null && allElements.add(new IFCObject(eoRef))) {
						addFloating(allElements, eoRef);
					}
				}
			} else if (refValue != null) {
				final EObject eoRef = (EObject)refValue;
				if (eoRef.eResource() == null && allElements.add(new IFCObject(eoRef))) {
					addFloating(allElements, eoRef);
				}
			}
		}
	}

	private Deserializer createDeserializer() throws Exception {

		Deserializer d;
		String packageLowerCaseName;

		switch (ifcModelType) {
		case IFC2X3_STEP:
			d = new Ifc2x3tc1StepDeserializer(Schema.IFC2X3TC1);
			packageLowerCaseName = Ifc2x3tc1Package.eINSTANCE.getName().toLowerCase();
			break;
		case IFC2X3_XML:
			d = new Ifc2x3tc1XmlDeserializer();
			packageLowerCaseName = Ifc2x3tc1Package.eINSTANCE.getName().toLowerCase();
			break;
		case IFC4_STEP:
			d = new Ifc4StepDeserializer(Schema.IFC4);
			packageLowerCaseName = Ifc4Package.eINSTANCE.getName().toLowerCase();
			break;
		case IFC4_XML:
			d = new Ifc4XmlDeserializer();
			packageLowerCaseName = Ifc4Package.eINSTANCE.getName().toLowerCase();
			break;
		default:
			throw new IllegalArgumentException("Unsupported IFC model type " + ifcModelType);
		}

		PluginManager bimPluginManager = createPluginManager();
		MetaDataManager bimMetaDataManager = new MetaDataManager(bimPluginManager);
		bimMetaDataManager.init();
		final PackageMetaData packageMetaData = bimMetaDataManager.getPackageMetaData(packageLowerCaseName);
		d.init(packageMetaData);
	
		return d;
	}

	@SuppressWarnings("unchecked")
	private PluginManager createPluginManager() throws Exception {
		final PluginManager bimPluginManager = new PluginManager();
		final InputStream isBIMPluginXML = IFCModelResource.class.getResourceAsStream("/plugin/plugin.xml");
		final PluginDescriptor desc = readPluginDescriptor(isBIMPluginXML);
		for (PluginImplementation impl : desc.getImplementations()) {
			final Class<? extends Plugin> interfaceClass = (Class<? extends Plugin>) Class.forName(impl.getInterfaceClass());
			final Class<?> implClass = Class.forName(impl.getImplementationClass());
			final Plugin plugin = (Plugin) implClass.newInstance();
			bimPluginManager.loadPlugin(interfaceClass, "", "", plugin,
					this.getClass().getClassLoader(),
					PluginSourceType.INTERNAL, impl);
		}
		return bimPluginManager;
	}

	@Override
	public String getType() {
		return factory.getType();
	}

	public IFCModelType getIFCModelType() {
		return ifcModelType;
	}

	private PluginDescriptor readPluginDescriptor(InputStream is) throws JAXBException {
		JAXBContext ctx = JAXBContext.newInstance(PluginDescriptor.class);
		Unmarshaller unm = ctx.createUnmarshaller();
		return (PluginDescriptor) unm.unmarshal(is);
	}

}