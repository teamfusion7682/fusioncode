package frcsim.hal;

/** Komut zaman cizelgesi icin bir kayit: bir komutun ne zaman basladigi/bittigi. */
public final class CommandEvent {
  public final String name;
  public final int depth;
  public final double start;
  public double end = -1.0;
  public boolean interrupted;

  public CommandEvent(String name, int depth, double start) {
    this.name = name;
    this.depth = depth;
    this.start = start;
  }
}
