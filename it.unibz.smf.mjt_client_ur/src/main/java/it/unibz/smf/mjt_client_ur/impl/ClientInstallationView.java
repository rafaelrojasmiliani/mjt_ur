package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.installation.swing.SwingInstallationNodeView;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;

/*
 * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/installation/swing/swinginstallationnodeview.html
 */
public class ClientInstallationView implements SwingInstallationNodeView<ClientInstallation> {
  private final Style style;
  private final Swing.Field hostname;
  private final Swing.Field portNumber;
  private final Swing.Field minNumberOfWayoints;
  private final Swing.Field maximumSpeed;
  private final Swing.Field maximumAcceleration;
  private final Swing.Field samplingTime;
  private final Swing.Field proportionalGain;
  private final Swing.Field derivativeGain;
  private final Swing.Field controlEps;
  private final Swing.Vector3 operatorVector;
  private final Swing.Image logoUniBZ;
  private final Swing.Image logoSMF;

  public ClientInstallationView(Style style) {
    this.style = style;
    this.hostname = new Swing.Field(Common.SERVICE_HOSTNAME, "Service hostname or IP", this.style);
    this.portNumber = new Swing.Field(Common.SERVICE_PORT_NUMBER, "Service port number", this.style);
    this.minNumberOfWayoints = new Swing.Field(Common.TRAJECTORY_MIN_WAYPOINTS, "Minimum number of waypoints", this.style);
    this.maximumSpeed = new Swing.Field(Common.TRAJECTORY_MAX_SPEED, "Maximum joints speed [rad/sec]", this.style);
    this.maximumAcceleration = new Swing.Field(Common.TRAJECTORY_MAX_ACCELERATION, "Maximum joints acceleration [rad/sec^2]", this.style);
    this.samplingTime = new Swing.Field(Common.TRAJECTORY_SAMPLING_TIME, "Sampling time [sec]", this.style);
    this.proportionalGain = new Swing.Field(Common.CONTROL_GAIN_PROPORTIONAL, "Proportional control gain", this.style);
    this.derivativeGain = new Swing.Field(Common.CONTROL_GAIN_DERIVATIVE, "Derivative control gain", this.style);
    this.controlEps = new Swing.Field(Common.CONTROL_EPS, "Control eps", this.style);
    this.operatorVector = new Swing.Vector3(Common.OPERATOR_VECTOR, "Operator's direction (in base frame)", this.style);
    this.logoUniBZ = new Swing.Image(getClass().getResource(Common.LOGO_UNIBZ), "Free University of Bozen-Bolzano", this.style, 0.80);
    this.logoSMF = new Swing.Image(getClass().getResource(Common.LOGO_SMF), "Smart Mini-Factory Lab", this.style, 0.80);
  }

  @Override
  public void buildUI(JPanel jPanel, final ClientInstallation inode) {
    jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
    jPanel.add(hostname.getBox());
    jPanel.add(style.vSpace(0.3));
    jPanel.add(portNumber.getBox());
    jPanel.add(style.vSpace(0.6));
    jPanel.add(minNumberOfWayoints.getBox());
    jPanel.add(style.vSpace(0.3));
    jPanel.add(maximumSpeed.getBox());
    jPanel.add(style.vSpace(0.3));
    jPanel.add(maximumAcceleration.getBox());
    jPanel.add(style.vSpace(0.3));
    jPanel.add(samplingTime.getBox());
    jPanel.add(style.vSpace(0.6));
    jPanel.add(proportionalGain.getBox());
    jPanel.add(style.vSpace(0.3));
    jPanel.add(derivativeGain.getBox());
    jPanel.add(style.vSpace(0.3));
    jPanel.add(controlEps.getBox());
    jPanel.add(style.vSpace(0.6));
    jPanel.add(operatorVector.getBox());
    jPanel.add(style.vSpace(1.5));
    jPanel.add(getLogosBox());
  }

  private Box getLogosBox() {
    Box box = Box.createHorizontalBox();
    box.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(logoUniBZ.getImage());
    box.add(style.hSpace(1.0));
    box.add(logoSMF.getImage());
    return box;
  }

  public void updateGUI(final KeyboardInputFactory keyboardInputFactory, final DataModel model) {
    hostname.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.Hostnane()));
    portNumber.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.PortNumber()));
    minNumberOfWayoints.update(model, null);
    maximumSpeed.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.PositiveReal("maximum velocity")));
    maximumAcceleration.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.PositiveReal("maximum acceleration")));
    samplingTime.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.PositiveReal("sampling time")));
    proportionalGain.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.NonNegativeReal("proportional control gain")));
    derivativeGain.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.NonNegativeReal("derivative control gain")));
    controlEps.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.BoundedReal("control eps", 0.0, 1.0)));
    operatorVector.update(model, keyboardInputFactory.createStringKeyboardInput());
  }
}
