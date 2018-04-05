/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.emfresource.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;

import net.sf.cglib.proxy.CallbackHelper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;

/**
 * Factory for instrumented {@link EObject}s with support for lazy loading.
 */
public class LazyEObjectFactory {

	private Registry packageRegistry;
	private MethodInterceptor methodInterceptor;

	/** Map from classes to factories of instances instrumented by CGLIB for lazy loading. */
	private Map<Class<?>, net.sf.cglib.proxy.Factory> factories = null;

	public LazyEObjectFactory(Registry registry, MethodInterceptor interceptor) {
		this.packageRegistry = registry;
		this.methodInterceptor = interceptor;
	}

	public EObject createInstance(final EClass eClass) {
		final String nsURI = eClass.getEPackage().getNsURI();
		final EFactory factory = packageRegistry.getEFactory(nsURI);
		if (factory == null) {
			throw new NoSuchElementException(String.format("Could not find the EFactory for nsURI '%s' in the resource set's package registry", nsURI));
		}

		final EObject obj = factory.create(eClass);
		return createLazyLoadingInstance(eClass, obj.getClass());
	}

	private EObject createLazyLoadingInstance(final EClass eClass, final Class<?> klass) {
		/*
		 * We need to create a proxy to intercept eGet calls for lazy loading,
		 * but we need to use a subclass of the *real* implementation, or we'll
		 * have all kinds of issues with static metamodels (e.g. not using
		 * DynamicEObjectImpl).
		 */
		if (factories == null) {
			factories = new HashMap<>();
		}

		final net.sf.cglib.proxy.Factory factory = factories.get(klass);
		EObject o;
		if (factory == null) {
			
			final Enhancer enh = new Enhancer();
			final CallbackHelper helper = new CallbackHelper(klass, new Class[0]) {
				@Override
				protected Object getCallback(final Method m) {
					if ("eGet".equals(m.getName())
							&& m.getParameterTypes().length > 0
							&& EStructuralFeature.class.isAssignableFrom(m.getParameterTypes()[0])) {
						return methodInterceptor;
					} else if ("eContents".equals(m.getName()) && m.getParameterTypes().length == 0) {
						return methodInterceptor;
					} else if ("eContainer".equals(m.getName()) && m.getParameterTypes().length == 0) {
						return methodInterceptor;
					} else if ("eInternalContainer".equals(m.getName()) && m.getParameterTypes().length == 0) {
						return methodInterceptor;
					} else if ("eContainerFeatureID".equals(m.getName()) && m.getParameterTypes().length == 0) {
						return methodInterceptor;
					} else if ("eContainingFeature".equals(m.getName()) && m.getParameterTypes().length == 0) {
						return methodInterceptor;
					} else if ("eContainmentFeature".equals(m.getName()) && m.getParameterTypes().length == 0) {
						return methodInterceptor;
					} else if ("eResource".equals(m.getName()) && m.getParameterTypes().length == 0) {
						return methodInterceptor;
					} else if ("eIsSet".equals(m.getName()) && m.getParameterTypes().length == 1 && EStructuralFeature.class.isAssignableFrom(m.getParameterTypes()[0])) {
						return methodInterceptor;
					} else if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
						EReference eRef = guessEReferenceFromGetter(eClass, m.getName());
						if (eRef != null) {
							return methodInterceptor;
						} else {
							return NoOp.INSTANCE;
						}
					}
					else {
						return NoOp.INSTANCE;
					}
				}
			};
			enh.setSuperclass(klass);

			/*
			 * We need both classloaders: the classloader of the class to be
			 * enhanced, and the classloader of this plugin (which includes
			 * CGLIB). We want the CGLIB classes to always resolve to the same
			 * Class objects, so this plugin's classloader *has* to go first.
			 */
			enh.setClassLoader(new BridgeClassLoader(
				this.getClass().getClassLoader(),
				klass.getClassLoader()));

			/*
			 * The objects created by the Enhancer implicitly implement the
			 * CGLIB Factory interface as well. According to CGLIB, going
			 * through the Factory is faster than recreating or reusing the
			 * Enhancer.
			 */
			enh.setCallbackFilter(helper);
			enh.setCallbacks(helper.getCallbacks());
			o = (EObject)enh.create();
			factories.put(klass, (net.sf.cglib.proxy.Factory)o);
		} else {
			o = (EObject) factory.newInstance(factory.getCallbacks());
		}

		/*
		 * A newly created and instrumented DynamicEObjectImpl won't have the
		 * eClass set. We need to redo that here.
		 */
		if (o instanceof DynamicEObjectImpl) {
			((DynamicEObjectImpl)o).eSetClass(eClass);
		}
		return o;
	}

	public EReference guessEReferenceFromGetter(EClass eClass, String methodName) {
		assert methodName.startsWith("get") : "method name should start with get";

		String referenceName = methodName.substring("get".length());
		if (referenceName.length() == 0) {
			return null;
		}

		// For getFirstElement, try both "FirstElement" and "firstElement" 
		EStructuralFeature sf = eClass.getEStructuralFeature(referenceName);
		if (sf == null) {
			referenceName = Character.toLowerCase(referenceName.charAt(0)) + referenceName.substring(1);
			sf = eClass.getEStructuralFeature(referenceName);
		}
		if (sf instanceof EReference) {
			return (EReference)sf;
		} else {
			return null;
		}
	}
}
