package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.DaemonContribution;
import com.ur.urcap.api.contribution.DaemonService;
import com.ur.urcap.api.contribution.installation.ContributionConfiguration;
import com.ur.urcap.api.contribution.installation.CreationContext;
import com.ur.urcap.api.contribution.installation.InstallationAPIProvider;
import com.ur.urcap.api.contribution.installation.swing.SwingInstallationNodeService;
import com.ur.urcap.api.contribution.InstallationNodeContribution;
import com.ur.urcap.api.contribution.ViewAPIProvider;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.script.ScriptWriter;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;

import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/*
 * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/installationnodecontribution.html
 */
public class ClientInstallation implements InstallationNodeContribution {
  /*
   * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/daemonservice.html
   */
  public static class ProxyDaemon implements DaemonService {
    /*
     * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/daemoncontribution.html
     */
    private DaemonContribution daemonContribution;

    @Override
    public void init(DaemonContribution daemonContribution) {
      this.daemonContribution = daemonContribution;
      try {
        installResource(new URL("file:" + Common.PROXY_RESOURCE_FOLDER));
      } catch (Exception e) {
      }
    }

    @Override
    public URL getExecutable() {
      try {
        return new URL("file:" + Common.PROXY_EXECUTABLE);
      } catch (Exception e) {
        return null;
      }
    }

    DaemonContribution.State getState() {
      return daemonContribution.getState();
    }

    void installResource(URL url) {
      daemonContribution.installResource(url);
    }

    void start() {
      if(getState() == DaemonContribution.State.STOPPED) {
        daemonContribution.start();
      } else {
        if(getState() == DaemonContribution.State.ERROR) {
          Swing.error("Proxy daemon service", "Proxy daemon in error state, it cannot be started");
        } else {
          Swing.info("Proxy daemon service", "Proxy daemon already started");
        }
      }
    }

    void stop() {
      if(getState() == DaemonContribution.State.RUNNING) {
        daemonContribution.stop();
      } else {
        if(getState() == DaemonContribution.State.ERROR) {
          Swing.error("Proxy daemon service", "Proxy daemon in error state, it cannot be stopped");
        } else {
          Swing.info("Proxy daemon service", "Proxy daemon already stopped");
        }
      }
    }
  }

  /*
   * https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installation/swing/swinginstallationnodeservice.html
   */
  public static class Service implements SwingInstallationNodeService<ClientInstallation, ClientInstallationView> {
    private ProxyDaemon proxyDaemon;

    public Service() {
      this.proxyDaemon = new ProxyDaemon();
    }

    @Override
    public String getTitle(Locale locale) {
      return "MJT Client";
    }

    @Override
    public void configureContribution(ContributionConfiguration configuration) {
    }

    @Override
    public ClientInstallationView createView(ViewAPIProvider apiProvider) {
      return new ClientInstallationView(new Style(apiProvider.getSystemAPI().getSoftwareVersion().getMajorVersion()));
    }

    @Override
    public ClientInstallation createInstallationNode(InstallationAPIProvider apiProvider, ClientInstallationView view, DataModel model, CreationContext context) {
      return new ClientInstallation(apiProvider, model, view, proxyDaemon);
    }

    ProxyDaemon getProxyDaemonService() {
      return proxyDaemon;
    }
  }

  private final InstallationAPIProvider apiProvider;
  private final ClientInstallationView view;
  private final ProxyDaemon proxyDaemon;
  private final DataModel model;

  public ClientInstallation(InstallationAPIProvider apiProvider, DataModel model, ClientInstallationView view, ProxyDaemon proxyDaemon) {
    this.apiProvider = apiProvider;
    this.view = view;
    this.proxyDaemon = proxyDaemon;
    this.model = model;
    Common.setDefault(this.model, Common.SERVICE_HOSTNAME);
    Common.setDefault(this.model, Common.SERVICE_PORT_NUMBER);
    Common.setDefault(this.model, Common.TRAJECTORY_MIN_WAYPOINTS);
    Common.setDefault(this.model, Common.TRAJECTORY_MAX_SPEED);
    Common.setDefault(this.model, Common.TRAJECTORY_MAX_ACCELERATION);
    Common.setDefault(this.model, Common.TRAJECTORY_SAMPLING_TIME);
    Common.setDefault(this.model, Common.CONTROL_GAIN_PROPORTIONAL);
    Common.setDefault(this.model, Common.CONTROL_GAIN_DERIVATIVE);
    Common.setDefault(this.model, Common.CONTROL_EPS);
    Common.setDefault(this.model, Common.OPERATOR_VECTOR);
  }

  @Override
  public void openView() {
    view.updateGUI(getKeyboardInputFactory(), model);
  }

  @Override
  public void closeView() {
  }

  @Override
  public void generateScript(ScriptWriter writer) {
    try {
      String line;
      String maximumSpeed = (String) Common.getWithDefault(model, Common.TRAJECTORY_MAX_SPEED);
      String proportionalGain = (String) Common.getWithDefault(model, Common.CONTROL_GAIN_PROPORTIONAL);
      String derivativeGain = (String) Common.getWithDefault(model, Common.CONTROL_GAIN_DERIVATIVE);
      String eps = (String) Common.getWithDefault(model, Common.CONTROL_EPS);
      BufferedReader scriptInstallation = Common.loadInstallationScript();
      while ((line = scriptInstallation.readLine()) != null) {
        line = line.replace("${PROXY_URL}", getProxyURL());
        line = line.replace("${TRAJECTORY_MAX_SPEED}", maximumSpeed);
        line = line.replace("${CONTROL_GAIN_PROPORTIONAL}", proportionalGain);
        line = line.replace("${CONTROL_GAIN_DERIVATIVE}", derivativeGain);
        line = line.replace("${CONTROL_EPS}", eps);
        //System.out.println(line);
        writer.appendLine(line);
      }
    } catch (Exception e) {
    }
  }

  public boolean isDefined() {
    try {
      String[] operatorVectorStr = (String[]) Common.getWithDefault(model, Common.OPERATOR_VECTOR);
      if (operatorVectorStr.length != 3) {
        return false;
      }
      double operatorVectorNorm = 0.0;
      for (int i = 0; i < operatorVectorStr.length; i++) {
        operatorVectorNorm += Math.pow(Double.parseDouble(operatorVectorStr[i]), 2.0);
      }
      if (operatorVectorNorm < 1e-2) {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public int getMinNumberOfWaypoints() {
    return Integer.parseInt((String) Common.getWithDefault(model, Common.TRAJECTORY_MIN_WAYPOINTS));
  }

  public String getMaximumAccelerationStr() {
    return (String) Common.getWithDefault(model, Common.TRAJECTORY_MAX_ACCELERATION);
  }

  public String getSamplingTimeStr() {
    return (String) Common.getWithDefault(model, Common.TRAJECTORY_SAMPLING_TIME);
  }

  public PlanningSpecification getPlanningSpecification() {
    String maximumSpeed = (String) Common.getWithDefault(model, Common.TRAJECTORY_MAX_SPEED);
    String maximumAcceleration = (String) Common.getWithDefault(model, Common.TRAJECTORY_MAX_ACCELERATION);
    String samplingTime = (String) Common.getWithDefault(model, Common.TRAJECTORY_SAMPLING_TIME);
    String[] operatorVectorStr = (String[]) Common.getWithDefault(model, Common.OPERATOR_VECTOR);
    double[] operatorVector = new double[3];
    for (int i = 0; i < operatorVector.length; i++) {
      operatorVector[i] = Double.parseDouble(operatorVectorStr[i]);
    }
    return new PlanningSpecification()
        .setMaximumSpeed(Double.parseDouble(maximumSpeed))
        .setMaximumAcceleration(Double.parseDouble(maximumAcceleration))
        .setSamplingTime(Double.parseDouble(samplingTime))
        .setOperatorVector(operatorVector);
  }

  public Object xmlRpcRequest(String service, Object[] params) {
    return XMLRPCClient.request(
        (String) Common.getWithDefault(model, Common.SERVICE_HOSTNAME),
        (String) Common.getWithDefault(model, Common.SERVICE_PORT_NUMBER),
        service, params);
  }

  public String getProxyURL() throws MalformedURLException {
    return XMLRPCClient.getServerURL(
        Common.getDefault(Common.PROXY_HOSTNAME),
        Common.getDefault(Common.PROXY_PORT_NUMBER)).toString();
  }

  private KeyboardInputFactory getKeyboardInputFactory() {
    return apiProvider.getUserInterfaceAPI().getUserInteraction().getKeyboardInputFactory();
  }
}
