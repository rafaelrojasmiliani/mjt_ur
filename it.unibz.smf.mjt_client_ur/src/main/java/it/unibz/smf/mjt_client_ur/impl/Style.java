package it.unibz.smf.mjt_client_ur.impl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;

public class Style {
  private static final int HORIZONTAL_SPACING = 10;
  private static final int VERTICAL_SPACING = 10;
  private static final int LARGE_VERTICAL_SPACING = 25;
  private static final int XLARGE_VERTICAL_SPACING = 50;
  private static final int SMALL_HEADER_FONT_SIZE = 16;

  private Dimension field_size_;
  private Dimension button_size_;
  private Dimension coordinate_size_;

  public Style(int version) {
    if (version >= 5) {
       field_size_ = new Dimension(212, 30);
       button_size_ = new Dimension(140, 30);
       coordinate_size_ = new Dimension((212 - 2 * HORIZONTAL_SPACING) / 3, 30);
    } else {
       field_size_ = new Dimension(271, 30);
       button_size_ = new Dimension(180, 30);
       coordinate_size_ = new Dimension((271 - 2 * HORIZONTAL_SPACING) / 3, 30);
    }
  }

  public int getHorizontalSpacing() {
    return HORIZONTAL_SPACING;
  }

  public int getVerticalSpacing() {
    return VERTICAL_SPACING;
  }

  public int getExtraLargeVerticalSpacing() {
      return XLARGE_VERTICAL_SPACING;
  }

  public int getLargeVerticalSpacing() {
    return LARGE_VERTICAL_SPACING;
  }

  public int getSmallHeaderFontSize() {
    return SMALL_HEADER_FONT_SIZE;
  }

  public Dimension getButtonSize() {
    return button_size_;
  }

  public Dimension getFieldSize() {
    return field_size_;
  }

  public Dimension getCoordinateSize() {
    return coordinate_size_;
  }

  public Component hSpace(double ratio) {
    Dimension dimension = new Dimension();
    dimension.setSize(ratio * getHorizontalSpacing(), 0.0);
    return Box.createRigidArea(dimension);
  }

  public Component vSpace(double ratio) {
    Dimension dimension = new Dimension();
    dimension.setSize(0.0, ratio * getVerticalSpacing());
    return Box.createRigidArea(dimension);
  }

  public Color getBadValueColor() {
    return new Color(255, 152, 152);
  }
}
