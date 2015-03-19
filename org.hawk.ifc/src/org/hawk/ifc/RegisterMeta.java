package org.hawk.ifc;

import java.util.Arrays;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * 
 * @author kb
 * 
 */
public class RegisterMeta {

	private static int registered = 0;

	public void clean() {

		Object[] packages = EPackage.Registry.INSTANCE.keySet().toArray();
		System.err.println(Arrays.toString(packages));

		for (Object s : packages)
			if ((!((String) s).contains("Ecore") && !((String) s)
					.contains("XMLType")))
				EPackage.Registry.INSTANCE.remove(s);

	}

	// registers metamodel
	/**
	 * Iterates through the descendants of a package and registers all the
	 * subpackages (as well as the root)
	 * 
	 * @param root
	 */
	public static int registerPackages(EPackage root) {
		if (!root.getNsURI().equals(EcorePackage.eNS_URI)) {
			if (EPackage.Registry.INSTANCE.put(root.getNsURI(), root) == null) {
				System.err.println("registering package: " + root.getName()
						+ "(" + root.getNsURI() + ") ["+root.eResource().getURI()+"]");
				registered++;
			}
			for (EPackage pkg : root.getESubpackages()) {
				registerPackages(pkg);
			}
		}
		return registered;
	}

	public static void registerPackages(Resource r) {

		for (EObject e : r.getContents())
			if (e instanceof EPackage)
				registerPackages((EPackage) e);

	}

}
