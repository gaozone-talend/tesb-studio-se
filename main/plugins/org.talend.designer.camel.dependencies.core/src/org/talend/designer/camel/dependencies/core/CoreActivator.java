package org.talend.designer.camel.dependencies.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CoreActivator implements BundleActivator {

	public static final String PLUGIN_ID = "org.talend.designer.camel.dependencies.core";
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		CoreActivator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		CoreActivator.context = null;
	}

}
