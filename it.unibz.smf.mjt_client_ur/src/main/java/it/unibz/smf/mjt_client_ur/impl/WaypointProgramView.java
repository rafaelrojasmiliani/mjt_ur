package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.ContributionProvider;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeView;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.event.ActionListener;

/*
 * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/program/swing/swingprogramnodeview.html
 */
public class WaypointProgramView implements SwingProgramNodeView<WaypointProgram>{
  private final Style style;
  private final Swing.Field nodeName;
  private final Swing.Button updateWaypoint;

  public WaypointProgramView(Style style) {
    this.style = style;
    this.nodeName = new Swing.Field(Common.NODE_ID, "Waypoint node name", this.style);
    this.updateWaypoint = new Swing.Button("Set waypoint", this.style);
  }

  @Override
  public void buildUI(JPanel jPanel, final ContributionProvider<WaypointProgram> provider) {
    jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
    jPanel.add(nodeName.getBox());
    jPanel.add(style.vSpace(1.5));
    jPanel.add(getButtonsBox());
  }

  private Box getButtonsBox() {
    Box box = Box.createHorizontalBox();
    box.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(updateWaypoint.getButton());
    return box;
  }

  public void updateGUI(final KeyboardInputFactory keyboardInputFactory, final DataModel model, final ActionListener waypointCallback) {
    nodeName.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.NonEmpty("program node name")));
    updateWaypoint.update(model.get(Common.IS_DEFINED, false)? "Update waypoint" : "Set waypoint", waypointCallback);
  }
}
