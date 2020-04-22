package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.ProgramNodeContribution;
import com.ur.urcap.api.contribution.ProgramNodeService;

import com.ur.urcap.api.domain.URCapAPI;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.program.nodes.builtin.configurations.assignmentnode.AssignmentNodeConfigFactory;
import com.ur.urcap.api.domain.program.nodes.builtin.AssignmentNode;
import com.ur.urcap.api.domain.program.nodes.ProgramNodeFactory;
import com.ur.urcap.api.domain.program.ProgramModel;
import com.ur.urcap.api.domain.program.structure.TreeNode;
import com.ur.urcap.api.domain.program.structure.TreeStructureException;
import com.ur.urcap.api.domain.script.ScriptWriter;
import com.ur.urcap.api.domain.userinteraction.RobotPositionCallback;
import com.ur.urcap.api.domain.value.expression.ExpressionBuilder;
import com.ur.urcap.api.domain.value.expression.InvalidExpressionException;
import com.ur.urcap.api.domain.value.jointposition.JointPosition;
import com.ur.urcap.api.domain.value.jointposition.JointPositions;
import com.ur.urcap.api.domain.value.simple.Angle.Unit;
import com.ur.urcap.api.domain.value.Pose;
import com.ur.urcap.api.domain.variable.Variable;
import com.ur.urcap.api.domain.variable.VariableException;
import com.ur.urcap.api.domain.variable.VariableFactory;

import com.ur.urcap.api.ui.annotation.Input;
import com.ur.urcap.api.ui.annotation.Label;
import com.ur.urcap.api.ui.component.InputButton;
import com.ur.urcap.api.ui.component.InputEvent;
import com.ur.urcap.api.ui.component.InputTextField;
import com.ur.urcap.api.ui.component.LabelComponent;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Program implements ProgramNodeContribution {
	// API required for adding a new type of program node to PolyScope
	public static class Service implements ProgramNodeService {
		public Service() {
		}

		// unique identifier for this kind of program node
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodeservice.html#getId--
		@Override
		public String getId() {
			return "mjt-client-ur";
		}

		// text displayed in the Structure Tab for program nodes created by this factory
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodeservice.html#getTitle--
		@Override
		public String getTitle() {
			return "MJT planner";
		}

		// return an input stream with the HTML contents of the node
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodeservice.html#getHTML--
		@Override
		public InputStream getHTML() {
			InputStream is = this.getClass().getResourceAsStream("/programnode.html");
			return is;
		}

		// by returning true it is not possible for the user to create new
		// program nodes of this type. Loading of existing programs will
		// however still be supported for this program node. NOTE: This
		// method is only called once when the URCap is activated and never
		// called again afterwards
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodeservice.html#isDeprecated--
		@Override
		public boolean isDeprecated() {
			return false;
		}

		// by returning true it is possible for the program node to have
		// child nodes. NOTE: this method is only called once when the URCap
		// is activated and never called again afterwards
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodeservice.html#isChildrenAllowed--
		@Override
		public boolean isChildrenAllowed() {
			return true;
		}

		// creates a new program node contribution instance. the returned
		// node must use the supplied data model object to retrieve and
		// store the data contained in it. every change to the model object
		// is registered as a separate undo/redo event in the program tree
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodeservice.html#createNode-com.ur.urcap.api.domain.URCapAPI-com.ur.urcap.api.domain.data.DataModel-
		@Override
		public ProgramNodeContribution createNode(URCapAPI api, DataModel model) {
			return new Program(api, model);
		}
	}

	// callback to add waypoint nodes (see below, where defined the
	// object addWaypoint and the method onAddWaypointPressed())
	private class AddWaypointCallback extends RobotPositionCallback {
		// add waypoint (joint positions) to the program tree
		@Override
		public void onOk(Pose _pose, JointPositions _positions) {
			// extract joint positions
			JointPosition[] positions = _positions.getAllJointPositions();

			// generate waypoint and prepare the variable value builder
			double[] waypoint = new double[positions.length];
			ExpressionBuilder valueBuilder = api.getValueFactoryProvider().createExpressionBuilder().append("[");

			int i = 0;
			for (; waypoint.length - 1 > i; i++) {
				waypoint[i] = positions[i].getPosition(Unit.RAD);
				valueBuilder.append(String.format("%.6f, ",waypoint[i]));
			}
			waypoint[i] = positions[i].getAngle(Unit.RAD);
			valueBuilder.append(String.format("%.6f]",waypoint[i]));

			try {
				// get the actual number of waypoints
				int N = model.get(Common.WAYPOINTS, 0);

				// generate variable name
				Variable variable = variableFactory.createGlobalVariable(String.format(Common.TEMPLATE, N));

				// create the waypoint node
				AssignmentNode node = nodeFactory.createAssignmentNode();
				AssignmentNodeConfigFactory config = node.getConfigFactory();

				try {
					// configure the waypoint node
					node.setConfig(config.createExpressionConfig(variable, valueBuilder.build()));

					try {
						// add waypoint node to the tree
						tree.addChild(node);

						// store waypoint on the dictionary
						model.set(String.format(Common.TEMPLATE,N), waypoint);

						// update number of waypoints
						model.set(Common.WAYPOINTS, ++N);

						// enable generate and reset plan buttons
						generatePlan.setEnabled(Common.MIN_WAYPOINTS <= N);
						resetPlan.setEnabled(0 < N);
					} catch (TreeStructureException e) {
						e.printStackTrace();
					}
				} catch(InvalidExpressionException e) {
					e.printStackTrace();
				}
			} catch (VariableException e) {
				e.printStackTrace();
			}
		}
	}

	// DataModel is an interface that provides methods for adding,
	// removing, retrieving, and changing values in a dictionary. Seems
	// that the scope of the model variable is to share data between
	// different program instances of the same node
	// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/domain/data/datamodel.html
	private DataModel model;

	// provides access to functionality available from within PolyScope,
	// as well as creating additional model elements to be used within
	// PolyScope. NOTE: this interface is only relevant for URCap
	// program and installation nodes with an HTML-based user interface
	private final URCapAPI api;

	// program node tree, builders and factories
	private TreeNode tree;
	ProgramNodeFactory nodeFactory;
	VariableFactory variableFactory;

	// **interactive HTML element** to handle the node name
	@Input(id = Common.PROGRAM_NAME)
	private InputTextField nodeName;

	// **interactive HTML element callback** to update the node name
	@Input(id = Common.PROGRAM_NAME)
	public void onNameChanged(InputEvent event) {
		Common.setModelValue(model, event, Common.PROGRAM_NAME, nodeName);
	}

	// **interactive HTML element** to handle waypoints
	@Input(id = Common.ADD_WAYPOINT)
	private InputButton addWaypoint;

	// **interactive HTML element callback** to add a waypoint
	@Input(id = Common.ADD_WAYPOINT)
	public void onAddWaypointPressed(InputEvent event) {
		if (event.getEventType() == InputEvent.EventType.ON_PRESSED) {
			api.getUserInteraction().getUserDefinedRobotPosition(new AddWaypointCallback());
		}
	}

	// **interactive HTML element** handle the node trajectory
	@Input(id = Common.GENERATE_PLAN)
	private InputButton generatePlan;

	// **interactive HTML element callback** to generate the node trajectory
	@Input(id = Common.GENERATE_PLAN)
	public void onGenerateTrajectoryPressed(InputEvent event) {
		if (event.getEventType() == InputEvent.EventType.ON_PRESSED) {
			// get number of waypoints
			int N = model.get(Common.WAYPOINTS, 0);

			// validate number of waypoints
			if (Common.MIN_WAYPOINTS > N) {
				return;
			}

			// get server information from installation
			Installation installation = api.getInstallationNode(Installation.class);
			String serverIP = installation.getValue(Common.SERVER_IP);
			int serverPort = Integer.parseInt(installation.getValue(Common.SERVER_PORT));
			double Kp = Double.parseDouble(installation.getValue(Common.CONTROL_KP));
			double Kv = Double.parseDouble(installation.getValue(Common.CONTROL_KV));
			double Ka = Double.parseDouble(installation.getValue(Common.CONTROL_KA));
			double T = Double.parseDouble(installation.getValue(Common.SAMPLING_TIME));

			try {
				// open socket
				Socket client = new Socket(serverIP, serverPort);

				// initialize input and output streams
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				PrintWriter out = new PrintWriter(client.getOutputStream(), false);

				// request
				out.printf("%d\n", N);
				out.flush();
				out.printf("%.6f\n", T);
				out.flush();
				for (int i = 0; N > i; i++) {
					// retrieve waypoint
					double[] waypoint = new double[1];
					waypoint = model.get(String.format(Common.TEMPLATE,i), waypoint);
					int j = 0;
					for ( ; waypoint.length - 1 > j; j++) {
						out.printf("%.6f ", waypoint[j]);
					}
					out.printf("%.6f\n", waypoint[waypoint.length - 1]);
					out.flush();
				}

				// prepare temporary file for URScript code
				File tmpFile = File.createTempFile("mjt-client-", ".script");
				PrintWriter tmpStream = new PrintWriter(tmpFile);

				// determine number or points to be read
				int M = Integer.parseInt(in.readLine());

				// read points while generating the URScript code
				tmpStream.printf("Kp = %.6f\n", Kp);
				tmpStream.printf("Kv = %.6f\n", Kv);
				tmpStream.printf("Ka = %.6f\n", Ka);
				tmpStream.println("a = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0]");
				tmpStream.println("u = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0]");
				for (int i = 0; M > i; i++) {
					String[] parts = in.readLine().split(",");
					String[] q = parts[0].trim().split(" ");
					String[] qp = parts[1].trim().split(" ");
					if (0 == i) {
						for (int j = 0; qp.length > j; j++ ) {
							tmpStream.printf("u[%d] = %s\n", j, q[j]);
						}
						tmpStream.println("movej(u)");
						tmpStream.println("sync()");
					}
					tmpStream.println("q = get_actual_joint_positions()");
					tmpStream.println("qp = get_actual_joint_speeds()");
					for (int j = 0; qp.length > j; j++ ) {
						tmpStream.printf("a[%d] = %s-qp[%d]\n", j, qp[j], j);
						tmpStream.printf("u[%d] = Kp*(%s-q[%d]) + Kv*(%s-qp[%d]) + %s\n", j, q[j], j, qp[j], j, qp[j]);
					}
					tmpStream.printf("speedj(u,Ka*norm(a),%s)\n", T);
				}

				// close output stream
				out.close();

				// close input stream
				in.close();

				// close socket
				client.close();

				// store path of the temporary file
				model.set(Common.PATH, tmpFile.getPath());

				// close file
				tmpStream.close();

				// set the trajectory flag
				model.set(Common.TRAJECTORY, true);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// **interactive HTML element** to handle waypoints
	@Input(id = Common.RESET_PLAN)
	private InputButton resetPlan;

	// **interactive HTML element callback** to clear all waypoints
	@Input(id = Common.RESET_PLAN)
	public void onResetPlanPressed(InputEvent event) {
		if (event.getEventType() == InputEvent.EventType.ON_PRESSED) {
			try {
				Iterator<TreeNode> it = tree.getChildren().iterator();
				while (it.hasNext()) {
					TreeNode child = it.next();
					tree.removeChild(child);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// get current number of waypoints
			int N = model.get(Common.WAYPOINTS, 0);

			// delete all waypoints
			for (int i = 0; N > i; i++) {
				model.remove(String.format(Common.TEMPLATE,i));
			}

			// clear number of waypoints
			model.set(Common.WAYPOINTS, 0);

			// clear the trajectory flag
			model.set(Common.TRAJECTORY, false);

			// disable generate and reset plan buttons
			generatePlan.setEnabled(false);
			resetPlan.setEnabled(false);
		}
	}

	// **interactive HTML element** to show the error message
	@Label(id = Common.PROGRAM_DISABLED)
	private LabelComponent progamDisabled;

	// class constructor, here we obtain the dictionary model and the
	// api object (see the above Server.createNode() method definition)
	public Program(URCapAPI _api, DataModel _model) {
		this.api = _api;
		this.model = _model;

		// create dummy move node
		ProgramModel programModel = api.getProgramModel();

		// get the node factory
		nodeFactory = programModel.getProgramNodeFactory();

		// get the variable factory
		variableFactory = api.getVariableModel().getVariableFactory();

		// get and lock the program tree
		tree = programModel.getRootTreeNode(Program.this);
		tree.setChildSequenceLocked(true);
	}

	// called when this node is selected in the program tree
	// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodecontribution.html#openView--
	@Override
	public void openView() {
		// default or previously defined node name
		nodeName.setText(Common.getModelValue(model, Common.PROGRAM_NAME));
		model.set(Common.PROGRAM_NAME, nodeName.getText());

		// if the client is enabled, then
		if (api.getInstallationNode(Installation.class).isDefined()) {
			// get number of waypoints
			int N = model.get(Common.WAYPOINTS, 0);

			// enable the program node
			progamDisabled.setVisible(false);
			addWaypoint.setEnabled(true);
			generatePlan.setEnabled(Common.MIN_WAYPOINTS <= N);
			resetPlan.setEnabled(0 < N);
		} else {
			// otherwise disable the program waiting for client activation
			progamDisabled.setVisible(true);
			addWaypoint.setEnabled(false);
			generatePlan.setEnabled(false);
			resetPlan.setEnabled(false);
		}
	}

	// called when this node is unselected in the program tree or when navigating to another view
	// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodecontribution.html#closeView--
	@Override
	public void closeView() {
	}

	// the text displayed in the Program Tree for the program node
	// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/programnodecontribution.html#getTitle--
	@Override
	public String getTitle() {
		return Common.getModelValue(model, Common.PROGRAM_NAME);
	}

	@Override
	public boolean isDefined() {
		boolean trajectory = model.get(Common.TRAJECTORY, false);
		return api.getInstallationNode(Installation.class).isDefined() && trajectory;
	}

	@Override
	public void generateScript(ScriptWriter writer) {
		// get path of the temporary file
		String tmpPath = model.get(Common.PATH, (String)null);
		if (null == tmpPath ) {
			writer.appendLine("popup(\"Cannot generate plan, please try again.\", \"Minimum-jerk tajectory planner\", False, False, blocking=True)");
			writer.end();
			return;
		}

		File tmpFile = null;
		BufferedReader tmpStream = null;
		try {
			// open the temporary file
			tmpFile = new File(tmpPath);
			tmpStream = new BufferedReader(new FileReader(tmpFile));

			// write lines into the URScript
			String line = tmpStream.readLine();
			while (null != line) {
				writer.appendLine(line);
				line = tmpStream.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != tmpStream) {
					tmpStream.close();
				}

				if (null != tmpFile) {
					tmpFile.delete();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
