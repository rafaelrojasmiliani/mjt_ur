package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.ContributionProvider;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeView;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.undoredo.UndoRedoManager;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;

import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;

/*
 * https://plus.universal-robots.com/apidoc/70014/com/ur/urcap/api/contribution/program/swing/swingprogramnodeview.html
 */
public class TrajectoryProgramView implements SwingProgramNodeView<TrajectoryProgram>{
  private final Style style;
  private final Swing.Field nodeName;
  private final Swing.Field executionTime;
  private final Swing.Field regularizationFactor;
  private final Swing.Menu basisType;
  private final Swing.Button generateTrajectory;
  private final Swing.Button clearTrajectory;

  public TrajectoryProgramView(Style style) {
    this.style = style;
    this.nodeName = new Swing.Field(Common.NODE_ID, "Trajectory node name", this.style);
    this.executionTime = new Swing.Field(Common.TRAJECTORY_EXECUTION_TIME, "Indicative execution time (25 is in general good) [sec]", this.style);
    this.regularizationFactor = new Swing.Field(Common.TRAJECTORY_REGULARIZATION_FACTOR, "Regularization factor (greater than 0.001)", this.style);
    this.basisType = new Swing.Menu(Common.TRAJECTORY_BASIS_TYPE, "Polynomial basis type", this.style);
    this.generateTrajectory = new Swing.Button("Generate trajectory", this.style);
    this.clearTrajectory = new Swing.Button("Clear trajectory", this.style);
  }

  @Override
  public void buildUI(JPanel jPanel, final ContributionProvider<TrajectoryProgram> provider) {
    jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
    jPanel.add(nodeName.getBox());
    jPanel.add(style.vSpace(0.5));
    jPanel.add(executionTime.getBox());
    jPanel.add(style.vSpace(0.5));
    jPanel.add(regularizationFactor.getBox());
    jPanel.add(style.vSpace(0.5));
    jPanel.add(basisType.getBox());
    jPanel.add(style.vSpace(1.5));
    jPanel.add(getButtonsBox());
  }

  private Box getButtonsBox() {
    Box box = Box.createHorizontalBox();
    box.setAlignmentX(Component.CENTER_ALIGNMENT);
    box.add(generateTrajectory.getButton());
    box.add(style.hSpace(1.0));
    box.add(clearTrajectory.getButton());
    return box;
  }

  public void updateGUI(final KeyboardInputFactory keyboardInputFactory, final DataModel model, final UndoRedoManager undoRedoManager, final ActionListener[] listeners) {
    nodeName.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.NonEmpty("program node name")));
    executionTime.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.PositiveReal("execution time")));
    regularizationFactor.update(model, Common.getKeyboardInput(keyboardInputFactory, new Validation.BoundedReal("regularization factor", 0.001, 1.0e20)));
    basisType.update(model, undoRedoManager);
    generateTrajectory.update(null, listeners[0]);
    clearTrajectory.update(null, listeners[1]);
  }
}
