package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.userinteraction.inputvalidation.InputValidator;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardTextInput;
import com.ur.urcap.api.domain.value.jointposition.JointPositions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Properties;

public class Common {
  // IDs of common data
  // -- server info
  public static final String SERVICE_HOSTNAME = "service_hostname";
  public static final String SERVICE_PORT_NUMBER = "service_port_number";
  // -- proxy info
  public static final String PROXY_HOSTNAME = "proxy_hostname";
  public static final String PROXY_PORT_NUMBER = "proxy_port_number";
  // -- services names
  public static final String SERVICE_RPC_PATH = "service_rpc_path";
  public static final String SERVICE_GENERATE = "service_generate";
  public static final String SERVICE_LOAD = "service_load";
  // -- trajectory generation data
  public static final String TRAJECTORY_MIN_WAYPOINTS = "trajectory_min_waypoints";
  public static final String TRAJECTORY_MAX_SPEED = "trajectory_max_speed";
  public static final String TRAJECTORY_MAX_ACCELERATION = "trajectory_max_acceleration";
  public static final String TRAJECTORY_SAMPLING_TIME = "trajectory_sampling_time";
  public static final String TRAJECTORY_EXECUTION_TIME = "trajectory_execution_time";
  public static final String TRAJECTORY_REGULARIZATION_FACTOR = "trajectory_regularization_factor";
  public static final String TRAJECTORY_BASIS_TYPE = "trajectory_basis_type";
  public static final String TRAJECTORY = "trajectory";
  // -- waypoint data
  public static final String WAYPOINT_POSITIONS = "waypoint_positions";
  // -- trajectory follower gains
  public static final String CONTROL_GAIN_PROPORTIONAL = "control_gain_proportional";
  public static final String CONTROL_GAIN_DERIVATIVE = "control_gain_derivative";
  public static final String CONTROL_EPS = "control_eps";
  // -- operator's data
  public static final String OPERATOR_VECTOR = "operator_vector";
  // -- resources
  public static final String LOGO_UNIBZ = "/media/unibz.png";
  public static final String LOGO_SMF = "/media/smf.png";
  public static final String URCAP_PROPERTIES = "/urcap.properties";
  public static final String URSCRIPT_INSTALLATION = "/scripts/InstallationNodeTime.script";
  public static final String URSCRIPT_TRAJECTORY = "/scripts/TrajectoryNodeTime.script";
  public static final String PROXY_RESOURCE_FOLDER = "/proxy/";
  public static final String PROXY_EXECUTABLE = "/proxy/XmlRpcProxy.py";
  // -- nodes data
  public static final String NODE_ID = "node_id";
  public static final String NODE_HASH = "node_hash";
  public static final String UNIQUE_ID = "unique_id";
  public static final String IS_DEFINED = "is_defined";

  public static interface ChildWaypointAPI {
    public JointPositions getJointPositions();
  }

  public static KeyboardTextInput getKeyboardInput(final KeyboardInputFactory keyboardInputFactory, final InputValidator validator) {
    return keyboardInputFactory.createStringKeyboardInput().setErrorValidator(validator);
  }

  public static BufferedReader loadResource(final InputStream src) {
    try {
      return new BufferedReader(new InputStreamReader(src, Charset.forName("UTF-8").name()));
    } catch (Exception e) {
      return null;
    }
  }

  public static BufferedReader loadInstallationScript() {
    return loadResource(Common.class.getClassLoader().getResourceAsStream(URSCRIPT_INSTALLATION));
  }

  public static BufferedReader loadTrajectoryScript() {
    return loadResource(Common.class.getClassLoader().getResourceAsStream(URSCRIPT_TRAJECTORY));
  }

  public static boolean copyTo(String resourcePath, String destinationPath) {
    try {
      BufferedReader src = loadResource(Common.class.getClassLoader().getResourceAsStream(resourcePath));
      BufferedWriter dst = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destinationPath), Charset.forName("UTF-8").name()));
      String line;
      while ((line = src.readLine()) != null) {
        dst.write(line);
        dst.newLine();
      }
      dst.flush();
      dst.close();
      return true;
    } catch (Exception e) {
      Swing.error("Resource copy", "Cannot copy resource: " + e.getMessage());
      return false;
    }
  }

  public static String getDefault(final String key) {
    try {
      InputStream src = Common.class.getClassLoader().getResourceAsStream(URCAP_PROPERTIES);
      Properties properties = new Properties();
      properties.load(src);
      String value = properties.getProperty(key);
      src.close();
      return value;
    } catch (Exception e) {
      Swing.error("Properties loader", "Cannot read property " + key + ": " + e.getMessage());
    }
    return new String();
  }

  public static Object getWithDefault(final DataModel model, final String key) {
    if (key == OPERATOR_VECTOR || key == TRAJECTORY_BASIS_TYPE) {
      return model.get(key, getDefault(key).split(","));
    } else {
      return model.get(key, getDefault(key));
    }
  }

  public static void setDefault(final DataModel model, final String key) {
    if (key == OPERATOR_VECTOR || key == TRAJECTORY_BASIS_TYPE) {
      model.set(key, getDefault(key).split(","));
    } else {
      model.set(key, getDefault(key));
    }
  }
}
