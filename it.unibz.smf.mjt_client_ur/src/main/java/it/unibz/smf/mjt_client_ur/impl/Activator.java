package it.unibz.smf.mjt_client_ur.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ur.urcap.api.contribution.installation.swing.SwingInstallationNodeService;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeService;

public class Activator implements BundleActivator {
  @Override
  public void start(final BundleContext context) throws Exception {
    context.registerService(SwingInstallationNodeService.class, new ClientInstallation.Service(), null);
    context.registerService(SwingProgramNodeService.class, new TrajectoryProgram.Service(), null);
    context.registerService(SwingProgramNodeService.class, new WaypointProgram.Service(), null);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
  }
}
