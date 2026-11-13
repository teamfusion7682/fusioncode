package edu.wpi.first.wpilibj.motorcontrol;

import edu.wpi.first.wpilibj.RobotController;

public interface MotorController {
  void set(double speed);

  default void setVoltage(double outputVolts) {
    set(outputVolts / RobotController.getBatteryVoltage());
  }

  double get();

  void setInverted(boolean isInverted);

  boolean getInverted();

  void disable();

  void stopMotor();
}
