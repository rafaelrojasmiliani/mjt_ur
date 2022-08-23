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

  private static class TreeWaypoints {
    private final List<double[]> waypoints = new ArrayList<double[]>();
    private int invalidChildren = 0;
    private int uninitializedChildren = 0;
    private int validChildren = 0;

    public TreeWaypoints(final TreeNode root) {
      try {
        for (TreeNode child : root.getChildren()) {
          if (child.getProgramNode() instanceof URCapProgramNode) {
            URCapProgramNode urcap = (URCapProgramNode) child.getProgramNode();
            if (urcap.canGetAs(Common.ChildWaypointAPI.class)) {
              Common.ChildWaypointAPI waypointNode = urcap.getAs(Common.ChildWaypointAPI.class);
              if (waypointNode.getJointPositions() != null) {
                JointPosition[] jointPosition = waypointNode.getJointPositions().getAllJointPositions();
                double[] waypoint = new double[jointPosition.length];
                for (int i = 0; jointPosition.length > i; i++) {
                  waypoint[i] = jointPosition[i].getPosition(Unit.RAD);
                }
                waypoints.add(waypoint);
                validChildren++;
              } else {
                uninitializedChildren++;
              }
            } else {
              invalidChildren++;
            }
          } else {
            invalidChildren++;
          }
        }
      } catch (Exception e) {
        invalidChildren++;
        Swing.error("Waypoint tree search", "Unexpected error while traversing the trajectory tree: " + e.getMessage());
      }
    }

    public boolean isInvalid() {
      return (invalidChildren > 0) || (uninitializedChildren > 0) || validChildren == 0;
    }

    public int getNumberOfInvalidChildren() {
      return invalidChildren;
    }

    public int getNumberOfUninitializedChildren() {
      return uninitializedChildren;
    }

    public int getNumberOfValidChildren() {
      return validChildren;
    }

    public double[] getFirstWaypoint() {
      return waypoints.get(0);
    }

    public double[] getLastWaypoint() {
      return waypoints.get(waypoints.size() - 1);
    }

    public List<double[]> getWaypoints() {
      return waypoints;
    }
  };

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
      final String service = Common.getDefault(Common.SERVICE_LOAD);
      final Object[] params = new Object[]{model.get(Common.UNIQUE_ID, ""), model.get(Common.TRAJECTORY, "")};
      final Boolean response = (Boolean) getInstallation().xmlRpcRequest(service, params);
      if (response != null && response.booleanValue()) {
        writer.appendLine("textmsg(\"mjt_trajectory_load: " + model.get(Common.UNIQUE_ID, "") + "\")");

        final TreeWaypoints treeWaypoints = new TreeWaypoints(getRootTreeNode());

        String line;
        final BufferedReader scriptTrajectory = Common.loadTrajectoryScript();
        while ((line = scriptTrajectory.readLine()) != null) {
          line = line.replace("${UNIQUE_ID}", model.get(Common.UNIQUE_ID, ""));
          line = line.replace("${TRAJECTORY_MAX_ACCELERATION}", getInstallation().getMaximumAccelerationStr());
          line = line.replace("${TRAJECTORY_SAMPLING_TIME}", getInstallation().getSamplingTimeStr());
          line = line.replace("${TRAJECTORY_FIRST_WAYPOINT}", Arrays.toString(treeWaypoints.getFirstWaypoint()));
          line = line.replace("${TRAJECTORY_LAST_WAYPOINT}", Arrays.toString(treeWaypoints.getLastWaypoint()));
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
    final TreeWaypoints treeWaypoints = new TreeWaypoints(getRootTreeNode());
    if (treeWaypoints.isInvalid()) {
      return false;
    }
    final String hash = model.get(Common.NODE_HASH, UUID.randomUUID().toString());
    final String currentHash = getPlanningSpecification(treeWaypoints.getWaypoints()).getAsJson();
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
      if (regularizationFactor < 0.0) {
        Swing.error("Generate trajectory", "Invalid regularization factor: must be a positive number");
        return;
      }
    } catch(Exception e) {
      Swing.error("Generate trajectory", "Invalid regularization factor: must be a positive number");
      return;
    }

    final TreeWaypoints treeWaypoints = new TreeWaypoints(getRootTreeNode());
    int invalidChildren = treeWaypoints.getNumberOfInvalidChildren();
    int uninitializedChildren = treeWaypoints.getNumberOfUninitializedChildren();
    int validChildren = treeWaypoints.getNumberOfValidChildren();

    if (invalidChildren > 0) {
      boolean one = (invalidChildren == 1);
      Swing.error("Generate trajectory", "There " + ((one) ? "is " : "are ") + invalidChildren + " invalid child" + ((one) ? "" : "ren"));
      return;
    }
    if (uninitializedChildren > 0) {
      boolean one = (uninitializedChildren == 1);
      Swing.error("Generate trajectory", "There " + ((one) ? "is " : "are ") + uninitializedChildren + " uninitialized waypoint" + ((one) ? "" : "s"));
      return;
    }
    int minNumberOfWayoints = getInstallation().getMinNumberOfWaypoints();
    if (validChildren < minNumberOfWayoints) {
      boolean one = (validChildren == 1);
      boolean zero = (validChildren == 0);
      String msg = "There " + ((one) ? "is " : "are ") + ((zero)? "no" : ("only " + validChildren)) + " available waypoint" +
                   ((one) ? "" : "s") + ", at least " + minNumberOfWayoints + " are required to compute a trajectory";
      Swing.error("Generate trajectory", msg);
      return;
    }

    undoRedoManager.recordChanges(new UndoableChanges() {
      @Override
      public void executeChanges() {
        PlanningSpecification planningSpecification = getPlanningSpecification(treeWaypoints.getWaypoints());
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

  private TreeNode getRootTreeNode() {
    return apiProvider.getProgramAPI().getProgramModel().getRootTreeNode(this);
  }

  private KeyboardInputFactory getKeyboardInputFactory() {
    return apiProvider.getUserInterfaceAPI().getUserInteraction().getKeyboardInputFactory();
  }
}
