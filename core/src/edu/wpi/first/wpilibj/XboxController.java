package edu.wpi.first.wpilibj;

/** Xbox kol. Eksenler: 0 solX, 1 solY, 2 sol tetik, 3 sag tetik, 4 sagX, 5 sagY. */
public class XboxController extends GenericHID {
  public XboxController(int port) {
    super(port);
  }

  public double getLeftX() {
    return getRawAxis(0);
  }

  public double getLeftY() {
    return getRawAxis(1);
  }

  public double getRightX() {
    return getRawAxis(4);
  }

  public double getRightY() {
    return getRawAxis(5);
  }

  public double getLeftTriggerAxis() {
    return getRawAxis(2);
  }

  public double getRightTriggerAxis() {
    return getRawAxis(3);
  }

  public boolean getAButton() {
    return getRawButton(1);
  }

  public boolean getAButtonPressed() {
    return getRawButtonPressed(1);
  }

  public boolean getAButtonReleased() {
    return getRawButtonReleased(1);
  }

  public boolean getBButton() {
    return getRawButton(2);
  }

  public boolean getBButtonPressed() {
    return getRawButtonPressed(2);
  }

  public boolean getBButtonReleased() {
    return getRawButtonReleased(2);
  }

  public boolean getXButton() {
    return getRawButton(3);
  }

  public boolean getXButtonPressed() {
    return getRawButtonPressed(3);
  }

  public boolean getXButtonReleased() {
    return getRawButtonReleased(3);
  }

  public boolean getYButton() {
    return getRawButton(4);
  }

  public boolean getYButtonPressed() {
    return getRawButtonPressed(4);
  }

  public boolean getYButtonReleased() {
    return getRawButtonReleased(4);
  }

  public boolean getLeftBumper() {
    return getRawButton(5);
  }

  public boolean getRightBumper() {
    return getRawButton(6);
  }

  public boolean getBackButton() {
    return getRawButton(7);
  }

  public boolean getStartButton() {
    return getRawButton(8);
  }

  public boolean getLeftStickButton() {
    return getRawButton(9);
  }

  public boolean getRightStickButton() {
    return getRawButton(10);
  }
}
