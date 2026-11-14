package edu.wpi.first.wpilibj2.command.button;

import edu.wpi.first.wpilibj.GenericHID;

public class JoystickButton extends Trigger {
  public JoystickButton(GenericHID joystick, int buttonNumber) {
    super(() -> joystick.getRawButton(buttonNumber));
  }
}
