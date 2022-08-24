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
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

/*
 * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/installationnodecontribution.html
 */
public class ClientInstallation implements InstallationNodeContribution {
  /*
   * https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installation/swing/swinginstallationnodeservice.html
   */
  public static class Service implements SwingInstallationNodeService<ClientInstallation, ClientInstallationView> {
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
      return new ClientInstallation(apiProvider, model, view);
    }
  }

  private static class ProxyDaemon {
    private static class DaemonRunnable implements Runnable {
      /*
       * https://www.javaworld.com/article/2071275/when-runtime-exec---won-t.html?page=2
       */
      private static class StreamGobbler extends Thread {
        final InputStream is;
        final String type;

        StreamGobbler(InputStream is, String type) {
          this.is = is;
          this.type = type;
        }

        public void run() {
          try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ( (line = br.readLine()) != null) {
              System.out.println(type + " " + line);
            }
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }
      }
      private Process process = null;
      private String pathToDaemon;

      public DaemonRunnable(String pathToDaemon) {
        this.pathToDaemon = pathToDaemon;
      }

      public void stop() {
        process.destroy();
      }

      @Override
      public void run() {
        try {
          process = Runtime.getRuntime().exec("python " + pathToDaemon);

          StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "[proxy daemon STDERR]");
          StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "[proxy daemon STDOUT]");
          errorGobbler.start();
          outputGobbler.start();

          System.out.println("[proxy daemon EXIT VALUE] " + process.waitFor());
        } catch (Exception e) {
          Swing.error("Proxy daemon service", "Unexpected error while running the proxy daemon: " + e.getMessage());
        }
      }
    }

    private Thread daemonThread = null;
    private DaemonRunnable daemonRunnable = null;

    public void start(String resourcePath, String propertiesPath) {
      stop();
      try {
        final String rootPath = "/tmp/mjt";
        new File(rootPath + "/proxy").mkdirs();
        final String pathToDaemon = rootPath + "/proxy/" + UUID.randomUUID().toString() + ".py";
        final String pathToProperties = rootPath + "/urcap.properties";
        Common.copyTo(resourcePath, pathToDaemon);
        Common.copyTo(propertiesPath, pathToProperties);
        daemonRunnable = new DaemonRunnable(pathToDaemon);
        daemonThread = new Thread(daemonRunnable);
        daemonThread.start();
      } catch (Exception e) {
        Swing.error("Proxy daemon service", "Unexpected error while starting the proxy daemon: " + e.getMessage());
      }
    }

    public boolean isRunning() {
      return daemonRunnable != null && daemonThread != null;
    }

    public void stop() {
      if (daemonThread != null) {
        if (daemonRunnable != null) {
          daemonRunnable.stop();
        }
        try {
          daemonThread.join();
        } catch (Exception e) {
          Swing.error("Proxy daemon service", "Unexpected error while stopping the proxy daemon: " + e.getMessage());
        }
      }
      daemonRunnable = null;
      daemonThread = null;
    }
  }

  private final InstallationAPIProvider apiProvider;
  private final ClientInstallationView view;
  private final ProxyDaemon proxyDaemon;
  private final DataModel model;

  public ClientInstallation(InstallationAPIProvider apiProvider, DataModel model, ClientInstallationView view) {
    this.apiProvider = apiProvider;
    this.view = view;
    this.proxyDaemon = new ProxyDaemon();
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

    proxyDaemon.start(Common.PROXY_EXECUTABLE, Common.URCAP_PROPERTIES);

    Common.setDefault(this.model, Common.PROXY_HOSTNAME);
    Common.setDefault(this.model, Common.PROXY_PORT_NUMBER);
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
      if(service.equals(Common.getDefault(Common.SERVICE_LOAD))){
        return XMLRPCClient.request(
            (String) Common.getWithDefault(model, Common.PROXY_HOSTNAME),
            (String) Common.getWithDefault(model, Common.PROXY_PORT_NUMBER),
            service, params);
      }
    return XMLRPCClient.request(
        (String) Common.getWithDefault(model, Common.SERVICE_HOSTNAME),
        (String) Common.getWithDefault(model, Common.SERVICE_PORT_NUMBER),
        service, params);
  }

  public String getProxyURL() throws MalformedURLException {
    return XMLRPCClient.getServerURL(
        Common.getDefault(this.view.hostname),
        Common.getDefault(this.view.portNumber)).toString();
  }

  private KeyboardInputFactory getKeyboardInputFactory() {
    return apiProvider.getUserInterfaceAPI().getUserInteraction().getKeyboardInputFactory();
  }
}
