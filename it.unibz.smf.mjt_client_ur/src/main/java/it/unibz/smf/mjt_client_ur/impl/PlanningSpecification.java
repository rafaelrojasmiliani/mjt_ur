package it.unibz.smf.mjt_client_ur.impl;

import com.google.gson.Gson;

import java.util.List;
import java.util.ArrayList;


public class PlanningSpecification {
  private String unique_id = null;
  private double maximum_speed = Double.NaN;
  private double maximum_acceleration = Double.NaN;
  private double sampling_time = Double.NaN;
  private double[] operator_vector = new double[]{Double.NaN, Double.NaN, Double.NaN};
  private double execution_time = Double.NaN;
  private double regularization_factor = Double.NaN;
  private String basis_type = null;
  private List<double[]> waypoints = null;

  public String getAsJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  public Object[] getAsXmlRpcParam() {
    return new Object[]{getAsJson()};
  }

  public PlanningSpecification setUniqueId(String uniqueId) {
    this.unique_id = uniqueId;
    return this;
  }

  public PlanningSpecification setMaximumSpeed(double maximumSpeed) {
    this.maximum_speed = maximumSpeed;
    return this;
  }

  public PlanningSpecification setMaximumAcceleration(double maximumAcceleration) {
    this.maximum_acceleration = maximumAcceleration;
    return this;
  }

  public PlanningSpecification setSamplingTime(double samplingTime) {
    this.sampling_time = samplingTime;
    return this;
  }

  public PlanningSpecification setOperatorVector(double[] operatorVector) {
    this.operator_vector = operatorVector;
    return this;
  }

  public PlanningSpecification setExecutionTime(double executionTime) {
    this.execution_time = executionTime;
    return this;
  }

  public PlanningSpecification setRegularizationFactor(double regularizationFactor) {
    this.regularization_factor = regularizationFactor;
    return this;
  }

  public PlanningSpecification setBasisType(String basisType) {
    this.basis_type = basisType;
    return this;
  }

  public PlanningSpecification setWaypoints(List<double[]> waypoints) {
    this.waypoints = waypoints;
    return this;
  }
}
