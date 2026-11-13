package frcsim.hal;

import java.util.List;

/** SendableChooser'in simulatore gosterdigi gorunum (UI'da acilir liste yapmak icin). */
public interface ChooserView {
  List<String> optionNames();

  String defaultName();
}
