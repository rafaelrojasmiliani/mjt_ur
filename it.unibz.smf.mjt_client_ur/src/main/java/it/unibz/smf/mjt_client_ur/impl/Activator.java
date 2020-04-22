package it.unibz.smf.mjt_client_ur.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import com.ur.urcap.api.contribution.InstallationNodeService;
import com.ur.urcap.api.contribution.ProgramNodeService;

public class Activator implements BundleActivator {
	@Override
	public void start(final BundleContext context) throws Exception {
		context.registerService(InstallationNodeService.class, new Installation.Service(), null);
		context.registerService(ProgramNodeService.class, new Program.Service(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}
