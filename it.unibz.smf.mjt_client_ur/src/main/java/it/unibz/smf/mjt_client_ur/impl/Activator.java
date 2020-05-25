package it.unibz.smf.mjt_client_ur.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ur.urcap.api.contribution.installation.swing.SwingInstallationNodeService;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeService;
import com.ur.urcap.api.contribution.DaemonService;

public class Activator implements BundleActivator {
  @Override
  public void start(final BundleContext context) throws Exception {
    ClientInstallation.Service clientInstallationService = new ClientInstallation.Service();
    context.registerService(SwingInstallationNodeService.class, clientInstallationService, null);
    context.registerService(SwingProgramNodeService.class, new TrajectoryProgram.Service(), null);
    context.registerService(SwingProgramNodeService.class, new WaypointProgram.Service(), null);
    context.registerService(DaemonService.class, clientInstallationService.getProxyDaemonService(), null);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
  }
}
