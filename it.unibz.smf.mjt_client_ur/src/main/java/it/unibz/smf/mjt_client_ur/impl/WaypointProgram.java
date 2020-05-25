package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.program.ContributionConfiguration;
import com.ur.urcap.api.contribution.program.CreationContext;
import com.ur.urcap.api.contribution.program.ProgramAPIProvider;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeService;
import com.ur.urcap.api.contribution.ProgramNodeContribution;
import com.ur.urcap.api.contribution.ViewAPIProvider;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.script.ScriptWriter;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;
import com.ur.urcap.api.domain.userinteraction.RobotPositionCallback;
import com.ur.urcap.api.domain.userinteraction.robot.movement.MovementCompleteEvent;
import com.ur.urcap.api.domain.userinteraction.robot.movement.MovementErrorEvent;
import com.ur.urcap.api.domain.userinteraction.robot.movement.RobotMovement;
import com.ur.urcap.api.domain.userinteraction.robot.movement.RobotMovementCallback;
import com.ur.urcap.api.domain.value.jointposition.JointPositions;
import com.ur.urcap.api.domain.value.jointposition.JointPositionFactory;
import com.ur.urcap.api.domain.value.simple.Angle.Unit;
import com.ur.urcap.api.domain.value.Pose;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

/*
 * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/programnodecontribution.html
 */
public class WaypointProgram implements ProgramNodeContribution, Common.ChildWaypointAPI {
  /*
   * https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/program/swing/swingprogramnodeservice.html
   */
  public static class Service implements SwingProgramNodeService<WaypointProgram, WaypointProgramView> {
    @Override
    public String getId() {
      return "MJWaypointUniqueID";
    }

    @Override
    public String getTitle(Locale locale) {
      return  "MJWaypoint";
    }

    @Override
    public void configureContribution(ContributionConfiguration configuration) {
      configuration.setChildrenAllowed(false);
    }

    @Override
    public WaypointProgramView createView(ViewAPIProvider apiProvider) {
      return new WaypointProgramView(new Style(apiProvider.getSystemAPI().getSoftwareVersion().getMajorVersion()));
    }

    @Override
    public WaypointProgram createNode(ProgramAPIProvider apiProvider, WaypointProgramView view, DataModel model, CreationContext context) {
      return new WaypointProgram(apiProvider, view, model);
    }
  }

  private final ProgramAPIProvider apiProvider;
  private final WaypointProgramView view;
  private final DataModel model;
  private final RobotRealtimeReader rtci;

  public WaypointProgram(ProgramAPIProvider apiProvider, WaypointProgramView view, DataModel model) {
    this.apiProvider = apiProvider;
    this.apiProvider.getProgramAPI().getProgramModel().getRootTreeNode(this).setChildSequenceLocked(true);
    this.view = view;
    this.model = model;
    this.model.set(Common.NODE_ID, "Waypoint");
    this.model.set(Common.IS_DEFINED, false);
    this.rtci = new RobotRealtimeReader();
  }

  @Override
  public void openView() {
    class WaypointCallback implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
        JointPositions jointPositions = model.get(Common.WAYPOINT_POSITIONS, (JointPositions)null);
        JointPositions robotJointPoitions = createJointPositions(rtci.readNow().getActualJointPose(), Unit.RAD);
        if (jointPositions != null && !jointPositions.epsilonEquals(robotJointPoitions, 0.001, Unit.RAD)) {
          getRequestUserToMoveRobot(jointPositions, new RobotMovementCallback() {
            @Override
            public void onComplete(MovementCompleteEvent event) {
              getUserDefinedRobotPosition(new RobotPositionCallback() {
                @Override
                public void onOk(Pose pose, JointPositions positions) {
                  model.set(Common.WAYPOINT_POSITIONS, positions);
                }
              });
            }

            @Override
            public void onError(MovementErrorEvent event) {
              Swing.error("Update waypoint", "Unexpected error while moving towards the waypoint pose" + event.getErrorType());
            }
          });
        } else {
          getUserDefinedRobotPosition(new RobotPositionCallback() {
            @Override
            public void onOk(Pose pose, JointPositions positions) {
              model.set(Common.WAYPOINT_POSITIONS, positions);
              model.set(Common.IS_DEFINED, true);
              view.updateGUI(getKeyboardInputFactory(), model, new WaypointCallback());
            }
          });
        }
      }
    };

    view.updateGUI(getKeyboardInputFactory(), model, new WaypointCallback());
  }

  @Override
  public void closeView() {
  }

  @Override
  public String getTitle() {
    return model.get(Common.NODE_ID, "Waypoint");
  }

  @Override
  public boolean isDefined() {
    return model.get(Common.IS_DEFINED, false);
  }

  @Override
  public void generateScript(ScriptWriter writer) {
    // nothing here, script generation is completely handled by the parent MJTrajectory node.
  }

  @Override
  public JointPositions getJointPositions() {
    return model.get(Common.WAYPOINT_POSITIONS, (JointPositions)null);
  }

  private JointPositions createJointPositions(double[] q, Unit unit) {
    return getJointPositionFactory().createJointPositions(q[0], q[1], q[2], q[3], q[4], q[5], unit);
  }

  private JointPositionFactory getJointPositionFactory() {
    return apiProvider.getProgramAPI().getValueFactoryProvider().getJointPositionFactory();
  }

  private KeyboardInputFactory getKeyboardInputFactory() {
    return apiProvider.getUserInterfaceAPI().getUserInteraction().getKeyboardInputFactory();
  }

  private void getUserDefinedRobotPosition(RobotPositionCallback callback) {
    apiProvider.getUserInterfaceAPI().getUserInteraction().getUserDefinedRobotPosition(callback);
  }

  private void getRequestUserToMoveRobot(JointPositions joints, RobotMovementCallback callback) {
    apiProvider.getUserInterfaceAPI().getUserInteraction().getRobotMovement().requestUserToMoveRobot(joints, callback);
  }
}
