/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.ecoregen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.modelio.exml.metamodel.ModelioAttribute;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioPackage;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.hawk.modelio.model.util.RegisterMeta;

/**
 * Generates an <code>.ecore</code> file that mimics the Modelio metamodel, in
 * order to make it possible to load Modelio instances through
 * <code>.hawkmodel</code> and <code>.localhawkmodel</code> files.
 *
 * Also generates the appropriate plugin.xml entries to have these metamodels
 * included automatically in the default EMF EPackage registry.
 */
public class EcoreGenerator {

	protected static final class ClassifierNameComparator implements Comparator<IHawkClassifier> {
		@Override
		public int compare(IHawkClassifier o1, IHawkClassifier o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	private final class Tuple<L, R> {
		private final L left;
		private final R right;

		private Tuple(L key, R value) {
			this.left = key;
			this.right = value;
		}

		public L getLeft() {
			return left;
		}

		public R getRight() {
			return right;
		}
	}

	private final EcoreFactory factory;
	private final List<EPackage> packages = new ArrayList<>();
	private final Map<String, Tuple<EClass, ModelioClass>> mClasses = new LinkedHashMap<>();
	private int nPackage = 0;

	public static void main(String[] args) {
		try {
			final EcoreGenerator generator = new EcoreGenerator();
			final File f = new File("model/modelio-v3.ecore");
			final Resource r = generator.generate(f);
			System.out.println("plugin.xml fragment:\n\n" + generator.generatePluginXml(r));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String generatePluginXml(Resource r) {
		final StringBuffer sbuf = new StringBuffer();
		for (EPackage pkg : packages) {
			/*
			 * <resource location="model/modelio.ecore#/"
			 * uri="org.hawk.modelio.exml.ecoregen.resource3">
			 */
			sbuf.append("    <resource uri=\"");
			sbuf.append(pkg.getNsURI());
			sbuf.append("\" location=\"");
			sbuf.append(r.getURI().toString() + "#" + r.getURIFragment(pkg));
			sbuf.append("\"/>\n");
		}
		return sbuf.toString();
	}

	public EcoreGenerator() {
		factory = EcorePackage.eINSTANCE.getEcoreFactory();
	}

	public Resource generate(File file) throws Exception {
		Resource r = new XMIResourceImpl(URI.createFileURI(file.getPath()));

		// Do a first pass to create the structure
		final Collection<ModelioPackage> mps = RegisterMeta.getRegisteredPackages();//metamodel.getMPackages();
		for (ModelioPackage mp : mps) {
			addPackageContents(r, mp);
		}

		// On the second pass, create features and references (sorted by names, to avoid unwanted diffs when regenerating)
		for (Tuple<EClass, ModelioClass> entry : mClasses.values()) {
			final EcorePackage ecorePkg = EcorePackage.eINSTANCE;
			final EClass ec = entry.getLeft();
			final ModelioClass mc = entry.getRight();

			final List<IHawkClass> superMClasses = new ArrayList<>(mc.getOwnSuperTypes());
			Collections.sort(superMClasses, new ClassifierNameComparator());
			for (IHawkClass superMClass : superMClasses) {
				EClass eSuper = mClasses.get(superMClass.getName()).getLeft();
				ec.getESuperTypes().add(eSuper);
			}

			final List<ModelioAttribute> mattrs = new ArrayList<>(mc.getOwnAttributesMap().values());
			Collections.sort(mattrs, new Comparator<IHawkAttribute>(){
				@Override
				public int compare(IHawkAttribute o1, IHawkAttribute o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (IHawkAttribute mattr : mattrs) {
				EDataType edt = ecorePkg.getEString();
				switch (((ModelioAttribute)mattr).getRawAttribute().getMDataType().getJavaEquivalent()) {
				case "Short":
					edt = ecorePkg.getEShort();
					break;
				case "Long":
					edt = ecorePkg.getELong();
					break;
				case "Integer":
					edt = ecorePkg.getEInt();
					break;
				case "Float":
					edt = ecorePkg.getEFloat();
					break;
				case "Double":
					edt = ecorePkg.getEDouble();
					break;
				case "Character":
					edt = ecorePkg.getEChar();
					break;
				case "Byte":
					edt = ecorePkg.getEByte();
					break;
				case "Boolean":
					edt = ecorePkg.getEBoolean();
					break;
				}

				final EAttribute eattr = factory.createEAttribute();
				eattr.setName(mattr.getName());
				eattr.setEType(edt);
				eattr.setOrdered(mattr.isOrdered());
				eattr.setUnique(mattr.isUnique());
				eattr.setUpperBound(mattr.isMany() ? EAttribute.UNBOUNDED_MULTIPLICITY : 1);
				ec.getEStructuralFeatures().add(eattr);
			}

			final List<ModelioReference> mdeps = new ArrayList<>(mc.getOwnReferencesMap().values());
			Collections.sort(mdeps, new Comparator<IHawkReference>(){
				@Override
				public int compare(IHawkReference o1, IHawkReference o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (IHawkReference mdep : mdeps) {
				final EClass targetEClass = mClasses.get(mdep.getType().getName()).getLeft();

				final EReference eref = factory.createEReference();
				eref.setName(mdep.getName());
				eref.setOrdered(mdep.isOrdered());
				eref.setUnique(mdep.isUnique());
				eref.setUpperBound(mdep.isMany() ? EReference.UNBOUNDED_MULTIPLICITY : 1);
				eref.setEType(targetEClass);
				eref.setContainment(mdep.isContainment());
				ec.getEStructuralFeatures().add(eref);
			}

			if (mc.getAllSuperTypes().isEmpty()) {
				final EReference eRefParent = (EReference) ec.getEStructuralFeature(ModelioClass.REF_PARENT);

				final EReference eRefChildren = factory.createEReference();
				eRefChildren.setName(ModelioClass.REF_CHILDREN);
				eRefChildren.setOrdered(true);
				eRefChildren.setUnique(true);
				eRefChildren.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);

				// The hawkParent reference's eType should be the root class that defines the reference itself
				eRefChildren.setEType(eRefParent.getEType());
				eRefChildren.setContainment(true);

				eRefChildren.setEOpposite(eRefParent);
				eRefParent.setEOpposite(eRefChildren);

				ec.getEStructuralFeatures().add(eRefChildren);
			}
		}

		r.save(null);
		return r;
	}

	private void addPackageContents(Resource r, final ModelioPackage pkg) throws Exception {
		EPackage ep = factory.createEPackage();
		ep.setName(pkg.getName());
		ep.setNsPrefix("m" + nPackage++);
		ep.setNsURI(pkg.getNsURI());
		r.getContents().add(ep);
		this.packages.add(ep);

		// Sort by name (getClasses returns a set, not a list)
		List<IHawkClassifier> classes = new ArrayList<>(pkg.getClasses());
		Collections.sort(classes, new ClassifierNameComparator());

		for (final IHawkClassifier mc : classes) {
			final EClass ec = factory.createEClass();
			ec.setName(mc.getName());
			ep.getEClassifiers().add(ec);

			final Tuple<EClass, ModelioClass> previousEntry = mClasses.put(mc.getName(), new Tuple<>(ec, (ModelioClass) mc));
			if (previousEntry != null) {
				throw new Exception("More than one class named " + mc.getName() + ": aborting");
			}
		}
		for (ModelioPackage subpkg : pkg.getPackages()) {
			addPackageContents(r, subpkg);
		}
	}
}
