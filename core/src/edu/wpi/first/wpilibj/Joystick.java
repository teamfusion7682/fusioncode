package edu.wpi.first.wpilibj;

/** Klasik joystick. Eksenler: 0 X, 1 Y, 2 Z/Twist, 3 Throttle. */
public class Joystick extends GenericHID {
  public Joystick(int port) {
    super(port);
  }

  public double getX() {
    return getRawAxis(0);
  }

  public double getY() {
    return getRawAxis(1);
  }

  public double getZ() {
    return getRawAxis(2);
  }

  public double getTwist() {
    return getRawAxis(2);
  }

  public double getThrottle() {
    return getRawAxis(3);
  }

  public boolean getTrigger() {
    return getRawButton(1);
  }

  public boolean getTop() {
    return getRawButton(2);
  }
}
