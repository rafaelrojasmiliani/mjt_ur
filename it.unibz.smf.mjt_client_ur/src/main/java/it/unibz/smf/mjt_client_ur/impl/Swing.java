package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.undoredo.UndoRedoManager;
import com.ur.urcap.api.domain.undoredo.UndoableChanges;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputCallback;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardTextInput;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Image;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ImageIcon;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.Arrays;

public class Swing {
  public static void dialog(String title, String message) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
  }

  public static void info(String title, String message) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
  }

  public static void warning(String title, String message) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
  }

  public static void error(String title, String message) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
  }

  public static class Button {
    private final JButton button;

    public Button(final String label, final Style style) {
      this.button = new JButton(label);
      this.button.setMinimumSize(style.getButtonSize());
      this.button.setPreferredSize(style.getButtonSize());
      this.button.setMaximumSize(style.getButtonSize());
    }

    public void update(String label, final ActionListener listener) {
      for (ActionListener old_listener : button.getActionListeners()) {
        button.removeActionListener(old_listener);
      }

      if (label != null) {
        button.setText(label);
      }

      if (listener != null) {
        button.addActionListener(listener);
      }
    }

    public JButton getButton() {
      return button;
    }

  }

  public static class Field {
    private final String key;
    private final JLabel label;
    private final JTextField field;
    private final Box box;

    public Field(final String key, final String label, final Style style) {
      this.key = key;

      this.label = new JLabel(label + ":");
      this.label.setMinimumSize(style.getFieldSize());
      this.label.setPreferredSize(style.getFieldSize());
      this.label.setMaximumSize(style.getFieldSize());
      this.label.setToolTipText(label);

      this.field = new JTextField();
      this.field.setFocusable(false);
      this.field.setMinimumSize(style.getFieldSize());
      this.field.setPreferredSize(style.getFieldSize());
      this.field.setMaximumSize(style.getFieldSize());

      this.box = Box.createHorizontalBox();
      this.box.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.box.add(this.label);
      this.box.add(style.hSpace(2.0));
      this.box.add(this.field);
    }

    public void update(final DataModel model, final KeyboardTextInput keyboardTextInput) {
      MouseListener[] listeners = field.getMouseListeners();
      for (MouseListener listener : listeners) {
        field.removeMouseListener(listener);
      }

      field.setText((String) Common.getWithDefault(model, key));

      if (keyboardTextInput != null) {
        field.addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            keyboardTextInput.setInitialValue(field.getText());
            keyboardTextInput.show(field, new KeyboardInputCallback<String>() {
              @Override
              public void onOk(String value) {
                field.setText(value);
                model.set(key, value);
              }
            });
          }
        });
      }
    }

    public Box getBox() {
      return box;
    }
  }

  public static class Image {
    private final JLabel label;

    public Image(final URL imageURL, final String description, final Style style, final double scale) {
      ImageIcon icon = new ImageIcon(imageURL, description);
      int width = (int) (scale * (double) icon.getIconWidth());
      int height = (int) (scale * (double) icon.getIconHeight());
      icon = new ImageIcon(icon.getImage().getScaledInstance(width, height, java.awt.Image.SCALE_DEFAULT));
      Dimension dimension = new Dimension(style.getFieldSize());
      dimension.setSize(dimension.getWidth(), height);
      this.label = new JLabel(icon);
      this.label.setMinimumSize(dimension);
      this.label.setPreferredSize(dimension);
      this.label.setMaximumSize(dimension);
    }

    public JLabel getImage() {
      return label;
    }
  }

  public static class Vector3 {
    private final String key;
    private final JLabel label;
    private final JTextField[] fields = new JTextField[3];
    private final Box box;
    private final Style style;

    public Vector3(final String key, final String label, final Style style) {
      this.key = key;
      this.style = style;

      this.label = new JLabel(label + ":");
      this.label.setMinimumSize(style.getFieldSize());
      this.label.setPreferredSize(style.getFieldSize());
      this.label.setMaximumSize(style.getFieldSize());
      this.label.setToolTipText(label);

      for (int i = 0; i < this.fields.length; i++) {
        this.fields[i] = new JTextField();
        this.fields[i].setFocusable(false);
        this.fields[i].setMinimumSize(style.getCoordinateSize());
        this.fields[i].setPreferredSize(style.getCoordinateSize());
        this.fields[i].setMaximumSize(style.getCoordinateSize());
      }

      this.box = Box.createHorizontalBox();
      this.box.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.box.add(this.label);
      this.box.add(style.hSpace(2.0));
      this.box.add(this.fields[0]);
      for (int i = 1; i < this.fields.length; i++) {
        this.box.add(style.hSpace(1.0));
        this.box.add(this.fields[i]);
      }
    }

    public void update(final DataModel model, final KeyboardTextInput keyboardTextInput) {
      final String[] values = (String[]) Common.getWithDefault(model, key);
      final String[] coordinates = {"x", "y", "z"};
      for (int i = 0; i < fields.length; i++) {
        final String coordinate = coordinates[i];
        final JTextField field = fields[i];

        MouseListener[] listeners = field.getMouseListeners();
        for (MouseListener listener : listeners) {
          field.removeMouseListener(listener);
        }

        field.setText(values[i]);

        if (keyboardTextInput != null) {
          field.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
              keyboardTextInput.setErrorValidator(new Validation.Real(coordinate + "-coordinate"));
              keyboardTextInput.setInitialValue(field.getText());
              keyboardTextInput.show(field, new KeyboardInputCallback<String>() {
                @Override
                public void onOk(String value) {
                  field.setText(value);
                  String[] values = (String[]) Common.getWithDefault(model, key);
                  for (int i = 0; i < fields.length; i++) {
                    values[i] = fields[i].getText();
                  }
                  model.set(key, values);

                  try {
                    double vectorNorm = 0.0;
                    for (int i = 0; i < fields.length; i++) {
                      vectorNorm += Math.pow(Double.parseDouble(values[i]), 2.0);
                    }
                    vectorNorm = Math.sqrt(vectorNorm);
                    if (vectorNorm < 1e-2) {
                      Swing.error("Operator's vector", "The norm of the operator's vector is close to zero");
                    }
                    for (int i = 0; i < fields.length; i++) {
                      fields[i].setBackground((vectorNorm < 1e-2) ? style.getBadValueColor() : Color.WHITE);
                    }
                  } catch (Exception e) {
                    Swing.error("Operator's vector", e.getMessage());
                    for (int i = 0; i < fields.length; i++) {
                      fields[i].setBackground(style.getBadValueColor());
                    }
                  }
                }
              });
            }
          });
        }
      }
    }

    public Box getBox() {
      return box;
    }
  }

  public static class Menu {
    private final String key;
    private final JLabel label;
    private final JComboBox menu;
    private final Box box;

    public Menu(final String key, final String label, final Style style) {
      this.key = key;

      this.label = new JLabel(label + ":");
      this.label.setMinimumSize(style.getFieldSize());
      this.label.setPreferredSize(style.getFieldSize());
      this.label.setMaximumSize(style.getFieldSize());
      this.label.setToolTipText(label);

      this.menu = new JComboBox();
      this.menu.setFocusable(false);
      this.menu.setMinimumSize(style.getFieldSize());
      this.menu.setPreferredSize(style.getFieldSize());
      this.menu.setMaximumSize(style.getFieldSize());

      this.box = Box.createHorizontalBox();
      this.box.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.box.add(this.label);
      this.box.add(style.hSpace(2.0));
      this.box.add(this.menu);
    }

    public void update(final DataModel model, final UndoRedoManager undoRedoManager) {
      for (ActionListener old_listener : menu.getActionListeners()) {
        menu.removeActionListener(old_listener);
      }

      if (model != null) {
        menu.removeAllItems();
        final String[] items = (String[]) Common.getWithDefault(model, key);
        for (int i = 0; i < items.length - 1; i++) {
          menu.addItem(items[i]);
          if (items[i].equals(items[items.length - 1])) {
            menu.setSelectedIndex(i);
          }
        }
      }

      menu.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          final String selectedItem = (String) ((JComboBox) e.getSource()).getSelectedItem();
          undoRedoManager.recordChanges(new UndoableChanges() {
            @Override
            public void executeChanges() {
              String[] items = (String[]) Common.getWithDefault(model, key);
              items[items.length - 1] = selectedItem;
              model.set(key, items);
            }
          });
        }
      });
    }

    public Box getBox() {
      return box;
    }
  }
}
