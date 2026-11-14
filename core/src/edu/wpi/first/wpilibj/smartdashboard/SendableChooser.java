package edu.wpi.first.wpilibj.smartdashboard;

import edu.wpi.first.util.sendable.Sendable;
import frcsim.hal.ChooserView;
import frcsim.hal.SimHal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Otonom secici. Secim web arayuzundeki acilir listeden gelir. */
public class SendableChooser<V> implements Sendable, ChooserView {
  private final Map<String, V> m_map = new LinkedHashMap<>();
  private String m_defaultChoice = "";
  private String m_key;

  public void addOption(String name, V object) {
    m_map.put(name, object);
  }

  public void setDefaultOption(String name, V object) {
    if (name == null) throw new NullPointerException("name");
    m_defaultChoice = name;
    addOption(name, object);
  }

  public V getSelected() {
    String selected = m_key == null ? null : SimHal.chooserSelection.get(m_key);
    if (selected == null || !m_map.containsKey(selected)) selected = m_defaultChoice;
    return m_map.get(selected);
  }

  void registerKey(String key) {
    m_key = key;
  }

  @Override
  public List<String> optionNames() {
    return new ArrayList<>(m_map.keySet());
  }

  @Override
  public String defaultName() {
    return m_defaultChoice;
  }
}
