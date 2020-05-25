package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.program.ContributionConfiguration;
import com.ur.urcap.api.contribution.program.CreationContext;
import com.ur.urcap.api.contribution.program.ProgramAPIProvider;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeService;
import com.ur.urcap.api.contribution.ProgramNodeContribution;
import com.ur.urcap.api.contribution.ViewAPIProvider;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.program.nodes.contributable.URCapProgramNode;
import com.ur.urcap.api.domain.program.structure.TreeNode;
import com.ur.urcap.api.domain.script.ScriptWriter;
import com.ur.urcap.api.domain.undoredo.UndoRedoManager;
import com.ur.urcap.api.domain.undoredo.UndoableChanges;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;
import com.ur.urcap.api.domain.value.jointposition.JointPositions;
import com.ur.urcap.api.domain.value.jointposition.JointPosition;
import com.ur.urcap.api.domain.value.simple.Angle.Unit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/*
 * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/programnodecontribution.html
 */
public class TrajectoryProgram implements ProgramNodeContribution {
  /*
   * https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/program/swing/swingprogramnodeservice.html
   */
  public static class Service implements SwingProgramNodeService<TrajectoryProgram, TrajectoryProgramView> {
    @Override
    public String getId() {
      return "MJTrajectoryUniqueID";
    }

    @Override
    public String getTitle(Locale locale) {
      return  "MJTrajectory";
    }

    @Override
    public void configureContribution(ContributionConfiguration configuration) {
      configuration.setChildrenAllowed(true);
    }

    @Override
    public TrajectoryProgramView createView(ViewAPIProvider apiProvider) {
      return new TrajectoryProgramView(new Style(apiProvider.getSystemAPI().getSoftwareVersion().getMajorVersion()));
    }

    @Override
    public TrajectoryProgram createNode(ProgramAPIProvider apiProvider, TrajectoryProgramView view, DataModel model, CreationContext context) {
      return new TrajectoryProgram(apiProvider, view, model);
    }
  }

  private final ProgramAPIProvider apiProvider;
  private final UndoRedoManager undoRedoManager;
  private final TrajectoryProgramView view;
  private final DataModel model;

  public TrajectoryProgram(ProgramAPIProvider apiProvider, TrajectoryProgramView view, DataModel model) {
    this.apiProvider = apiProvider;
    this.undoRedoManager = this.apiProvider.getProgramAPI().getUndoRedoManager();
    this.view = view;
    this.model = model;
    this.model.set(Common.NODE_ID, "Trajectory");
    this.model.set(Common.NODE_HASH, UUID.randomUUID().toString());
    this.model.set(Common.UNIQUE_ID, UUID.randomUUID().toString());
    this.model.set(Common.IS_DEFINED, false);
    this.model.set(Common.TRAJECTORY, (String) null);
    Common.setDefault(this.model, Common.TRAJECTORY_EXECUTION_TIME);
    Common.setDefault(this.model, Common.TRAJECTORY_REGULARIZATION_FACTOR);
    Common.setDefault(this.model, Common.TRAJECTORY_BASIS_TYPE);
  }

  @Override
  public void openView() {
    class GenerateListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
        generateTrajectory();
      }
    };

    class ClearListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearTrajectory();
      }
    };

    ActionListener[] listeners = {new GenerateListener(), new ClearListener()};
    view.updateGUI(getKeyboardInputFactory(), model, undoRedoManager, listeners);
  }

  @Override
  public void closeView() {
  }

  @Override
  public String getTitle() {
    return model.get(Common.NODE_ID, "Trajectory");
  }

  @Override
  public void generateScript(ScriptWriter writer) {
    try {
      String service = Common.getDefault(Common.SERVICE_LOAD);
      Object[] params = new Object[]{model.get(Common.UNIQUE_ID, ""), model.get(Common.TRAJECTORY, "")};
      Boolean response = (Boolean) getInstallation().xmlRpcRequest(service, params);
      if (response != null && response.booleanValue()) {
        writer.appendLine("textmsg(\"mjt_trajectory_load: " + model.get(Common.UNIQUE_ID, "") + "\")");

        List<double[]> waypoints = new ArrayList<double[]>();
        getWaypoints(waypoints, null, null, null);
        String firstWaypoint = Arrays.toString(waypoints.get(0));
        String lastWaypoint = Arrays.toString(waypoints.get(waypoints.size() - 1));

        String line;
        BufferedReader scriptTrajectory = Common.loadTrajectoryScript();
        while ((line = scriptTrajectory.readLine()) != null) {
          line = line.replace("${UNIQUE_ID}", model.get(Common.UNIQUE_ID, ""));
          line = line.replace("${TRAJECTORY_MAX_ACCELERATION}", getInstallation().getMaximumAccelerationStr());
          line = line.replace("${TRAJECTORY_SAMPLING_TIME}", getInstallation().getSamplingTimeStr());
          line = line.replace("${TRAJECTORY_FIRST_WAYPOINT}", firstWaypoint);
          line = line.replace("${TRAJECTORY_LAST_WAYPOINT}", lastWaypoint);
          //System.out.println(line);
          writer.appendLine(line);
        }
      } else {
        writer.appendLine("textmsg(\"mjt_trajectory_load: failed\"");
        writer.appendLine("halt");
      }
    } catch (Exception e) {
    }
  }

  @Override
  public boolean isDefined() {
    final List<double[]> waypoints = new ArrayList<double[]>();
    if (!getWaypoints(waypoints, null, null, null)) {
      return false;
    }
    String hash = model.get(Common.NODE_HASH, UUID.randomUUID().toString());
    String currentHash = getPlanningSpecification(waypoints).getAsJson();
    return getInstallation().isDefined() && model.get(Common.IS_DEFINED, false) && hash.equals(currentHash);
  }

  public void generateTrajectory() {
    if (!getInstallation().isDefined()) {
      Swing.error("Generate trajectory", "Please verify the installation settings before generating the trajectory");
      return;
    }

    try {
      double executionTime = Double.parseDouble(model.get(Common.TRAJECTORY_EXECUTION_TIME, ""));
      if (executionTime <= 0) {
        Swing.error("Generate trajectory", "Invalid execution time: must be a positive real number");
        return;
      }
    } catch(Exception e) {
      Swing.error("Generate trajectory", "Invalid execution time: must be a positive real number");
      return;
    }

    try {
      double regularizationFactor = Double.parseDouble(model.get(Common.TRAJECTORY_REGULARIZATION_FACTOR, ""));
      if (regularizationFactor < 0.0 || regularizationFactor > 1.0) {
        Swing.error("Generate trajectory", "Invalid regularization factor: must be a positive number in range [0.0, 1.0]");
        return;
      }
    } catch(Exception e) {
      Swing.error("Generate trajectory", "Invalid regularization factor: must be a positive number in range [0.0, 1.0]");
      return;
    }

    final List<double[]> waypoints = new ArrayList<double[]>();
    int[] invalidChildren = new int[1];
    int[] uninitializedChildren = new int[1];
    int[] validChildren = new int[1];
    getWaypoints(waypoints, invalidChildren, uninitializedChildren, validChildren);

    if (invalidChildren[0] > 0) {
      boolean one = (invalidChildren[0] == 1);
      Swing.error("Generate trajectory", "There " + ((one) ? "is " : "are ") + invalidChildren[0] + " invalid child" + ((one) ? "" : "ren"));
      return;
    }
    if (uninitializedChildren[0] > 0) {
      boolean one = (uninitializedChildren[0] == 1);
      Swing.error("Generate trajectory", "There " + ((one) ? "is " : "are ") + uninitializedChildren[0] + " uninitialized waypoint" + ((one) ? "" : "s"));
      return;
    }
    int minNumberOfWayoints = getInstallation().getMinNumberOfWaypoints();
    if (validChildren[0] < minNumberOfWayoints) {
      boolean one = (validChildren[0] == 1);
      boolean zero = (validChildren[0] == 0);
      String msg = "There " + ((one) ? "is " : "are ") + ((zero)? "no" : ("only " + validChildren[0])) + " available waypoint" +
                   ((one) ? "" : "s") + ", at least " + minNumberOfWayoints + " are required to compute a trajectory";
      Swing.error("Generate trajectory", msg);
      return;
    }

    undoRedoManager.recordChanges(new UndoableChanges() {
      @Override
      public void executeChanges() {
        PlanningSpecification planningSpecification = getPlanningSpecification(waypoints);
        model.set(Common.NODE_HASH, planningSpecification.getAsJson());

        String service = Common.getDefault(Common.SERVICE_GENERATE);
        String response = (String) getInstallation().xmlRpcRequest(service, planningSpecification.getAsXmlRpcParam());
        model.set(Common.IS_DEFINED, (response != null));
        model.set(Common.TRAJECTORY, response);
      }
    });
  }

  public void clearTrajectory() {
    final TreeNode root = apiProvider.getProgramAPI().getProgramModel().getRootTreeNode(this);
    undoRedoManager.recordChanges(new UndoableChanges() {
      @Override
      public void executeChanges() {
        try {
          for (TreeNode child : root.getChildren()) {
            root.removeChild(child);
          }
        } catch (Exception e) {
          Swing.error("Clear trajectory", "Unexpected error while traversing the trajectory tree: " + e.getMessage());
        }
        model.set(Common.IS_DEFINED, false);
        model.set(Common.TRAJECTORY, (String) null);
      }
    });
  }

  private ClientInstallation getInstallation() {
    return apiProvider.getProgramAPI().getInstallationNode(ClientInstallation.class);
  }

  private boolean getWaypoints(final List<double[]> waypoints, final int[] invalid, final int uninitialized[], final int[] valid) {
    try {
      if (waypoints != null) {
        waypoints.clear();
      }
      if (invalid != null) {
        invalid[0] = 0;
      }
      if (uninitialized != null) {
        uninitialized[0] = 0;
      }
      if (valid != null) {
        valid[0] = 0;
      }
      final TreeNode root = apiProvider.getProgramAPI().getProgramModel().getRootTreeNode(this);
      for (TreeNode child : root.getChildren()) {
        if (child.getProgramNode() instanceof URCapProgramNode) {
          URCapProgramNode urcap = (URCapProgramNode) child.getProgramNode();
          if (urcap.canGetAs(Common.ChildWaypointAPI.class)) {
            Common.ChildWaypointAPI waypointNode = urcap.getAs(Common.ChildWaypointAPI.class);
            if (waypointNode.getJointPositions() != null) {
              if (waypoints != null) {
                JointPosition[] jointPosition = waypointNode.getJointPositions().getAllJointPositions();
                double[] waypoint = new double[jointPosition.length];
                for (int i = 0; jointPosition.length > i; i++) {
                  waypoint[i] = jointPosition[i].getPosition(Unit.RAD);
                }
                waypoints.add(waypoint);
              }
              if (valid != null) {
                valid[0]++;
              }
            } else {
              if (uninitialized != null) {
                uninitialized[0]++;
              }
            }
          } else {
            if (invalid != null) {
              invalid[0]++;
            }
          }
        }
      }
      return true;
    } catch (Exception e) {
      if (invalid != null) {
        invalid[0]++;
      }
      Swing.error(getTitle(), "Unexpected error while traversing the trajectory tree: " + e.getMessage());
      return false;
    }
  }

  private PlanningSpecification getPlanningSpecification(List<double[]> waypoints) {
    String executionTime = (String) Common.getWithDefault(model, Common.TRAJECTORY_EXECUTION_TIME);
    String regularizationFactor = (String) Common.getWithDefault(model, Common.TRAJECTORY_REGULARIZATION_FACTOR);
    String[] basisType = (String[]) Common.getWithDefault(model, Common.TRAJECTORY_BASIS_TYPE);
    return getInstallation().getPlanningSpecification()
        .setUniqueId(model.get(Common.UNIQUE_ID, UUID.randomUUID().toString()))
        .setExecutionTime(Double.parseDouble(executionTime))
        .setRegularizationFactor(Double.parseDouble(regularizationFactor))
        .setBasisType(basisType[basisType.length - 1])
        .setWaypoints(waypoints);
  }

  private KeyboardInputFactory getKeyboardInputFactory() {
    return apiProvider.getUserInterfaceAPI().getUserInteraction().getKeyboardInputFactory();
  }
}
