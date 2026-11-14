package edu.wpi.first.wpilibj2.command.button;

import edu.wpi.first.wpilibj.XboxController;

/** XboxController + dugmeleri dogrudan Trigger olarak veren kisayollar. */
public class CommandXboxController extends XboxController {
  public CommandXboxController(int port) {
    super(port);
  }

  public XboxController getHID() {
    return this;
  }

  public Trigger a() { return new Trigger(this::getAButton); }
  public Trigger b() { return new Trigger(this::getBButton); }
  public Trigger x() { return new Trigger(this::getXButton); }
  public Trigger y() { return new Trigger(this::getYButton); }
  public Trigger leftBumper() { return new Trigger(this::getLeftBumper); }
  public Trigger rightBumper() { return new Trigger(this::getRightBumper); }
  public Trigger back() { return new Trigger(this::getBackButton); }
  public Trigger start() { return new Trigger(this::getStartButton); }
  public Trigger leftStick() { return new Trigger(this::getLeftStickButton); }
  public Trigger rightStick() { return new Trigger(this::getRightStickButton); }
}
