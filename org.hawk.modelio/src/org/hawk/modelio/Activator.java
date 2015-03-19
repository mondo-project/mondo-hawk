package org.hawk.modelio;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

	private static BundleContext context;
	private static Plugin instance;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		//System.out.println("kjsdbgksdjgbsdkgjbsdgkjsdbgkjsdbgksdbg");
		super.start(bundleContext);
		Activator.context = bundleContext;
		Activator.instance = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		//System.out.println("I AM STOPPED");
		super.stop(bundleContext);
		Activator.context = null;
		Activator.instance = null;
	}

	public static Plugin getInstance() {
		//System.out.println("getIntance() : " + (instance == null));
		return instance;

	}

}
