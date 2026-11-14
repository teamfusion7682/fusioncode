package edu.wpi.first.wpilibj;

import frcsim.hal.SimHal;

public class GenericHID {
  public enum RumbleType { kLeftRumble, kRightRumble, kBothRumble }

  private final int m_port;
  private final boolean[] m_prevPressed = new boolean[SimHal.MAX_BUTTONS + 1];
  private final boolean[] m_prevReleased = new boolean[SimHal.MAX_BUTTONS + 1];

  public GenericHID(int port) {
    if (port < 0 || port >= SimHal.JOYSTICK_PORTS) {
      throw new IllegalArgumentException("Joystick portu 0-" + (SimHal.JOYSTICK_PORTS - 1) + " arasinda olmali");
    }
    m_port = port;
  }

  public int getPort() {
    return m_port;
  }

  public double getRawAxis(int axis) {
    return SimHal.getAxis(m_port, axis);
  }

  public boolean getRawButton(int button) {
    return SimHal.getButton(m_port, button);
  }

  public boolean getRawButtonPressed(int button) {
    boolean now = getRawButton(button);
    boolean was = button >= 1 && button <= SimHal.MAX_BUTTONS && m_prevPressed[button];
    if (button >= 1 && button <= SimHal.MAX_BUTTONS) m_prevPressed[button] = now;
    return now && !was;
  }

  public boolean getRawButtonReleased(int button) {
    boolean now = getRawButton(button);
    boolean was = button >= 1 && button <= SimHal.MAX_BUTTONS && m_prevReleased[button];
    if (button >= 1 && button <= SimHal.MAX_BUTTONS) m_prevReleased[button] = now;
    return !now && was;
  }

  public int getPOV() {
    return SimHal.getPov(m_port);
  }

  public boolean isConnected() {
    return true;
  }

  public void setRumble(RumbleType type, double value) {}
}
