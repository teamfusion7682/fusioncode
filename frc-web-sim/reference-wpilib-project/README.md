# FRC Robot Simülatörü: İlk Çekirdek (Aşama 1)

Gerçek WPILib robot kodunu **hiç değiştirmeden** simülasyonda çalıştıran proje iskeleti.
Bu aşamada: tank sürüş + `robot-config.json` ile yapılandırma + "zayıf kod / PID" demosu.

> **Durum:** Dosyalar yazıldı fakat **henüz derlenip çalıştırılmadı** (bu ortamda WPILib yok).
> İlk iş bilgisayarınızda derleyip çalıştırmak. Sınıf/import adları WPILib sürümüne göre
> küçük düzeltme isteyebilir (özellikle `PWMSparkMax` import yolu).

## Kurulum (bir kez)

1. [WPILib](https://docs.wpilib.org) kurun (VS Code'lu paket).
2. VS Code'da `Ctrl+Shift+P` → **WPILib: Create a new project** → Java → Template → **Timed Robot**.
   Takım numarası yerine kendi numaranızı yazın. Bu adım doğru `build.gradle` dosyasını üretir.
3. Bu klasördeki dosyaları yeni projeye kopyalayın:
   - `src/main/java/frc/robot/*.java` → projedeki aynı klasöre (mevcut `Robot.java`'nın üzerine yazın, `Main.java` zaten var)
   - `src/main/deploy/robot-config.json` → projedeki `src/main/deploy/` (klasör yoksa oluşturun)
4. `Ctrl+Shift+P` → **WPILib: Simulate Robot Code** → **Sim GUI** seçin.

## Demo nasıl yapılır

1. Sim GUI'de **Robot State → Autonomous**'u seçin (önce Disabled iken).
2. **Otonom/Secim** listesinden `1 - Zayıf kod`'u seçip Autonomous'a geçin. Robot hedefi (3 m) aşar.
3. Disabled'a dönüp `2 - PID`'yi seçin, tekrar Autonomous. Robot 3 m'de durur.
4. **Saha** (Field2d) ve **Otonom/MesafeM** grafiğini AdvantageScope'ta açın; iki çalışmayı yan yana gösterin.

## Farklı robotları denemek

`src/main/deploy/robot-config.json` içindeki kütle, dişli oranı, motor tipi (`NEO`, `CIM`, `MiniCIM`, `Falcon500`),
tekerlek yarıçapı ve iz genişliğini değiştirip yeniden çalıştırın. Robot kurma arayüzü (Aşama 3)
bu dosyayı üretecek; kod tarafı hazır.

## Gerçek robotta çalıştırma

Aynı kod yüklenir. `simulationUpdate()` gerçek robotta çağrılmaz. Farklı motor sürücüleri (REV, CTRE vb.)
kullanıyorsanız yalnızca `Drivetrain.java` içindeki motor/encoder/gyro nesnelerini değiştirin; `Robot.java` aynı kalır.

## Ölçüm şablonu (jüri için kanıt)

Her denemeyi kaydedin; sunumdaki sayılar buradan gelecek.

| Tarih | Kişi | Yöntem (Simülatör / Robot) | Süre (dk) | Sonuç (hedefe ulaştı mı, hata m) | Not |
|---|---|---|---|---|---|

Üç ölçüm yapın:
1. **Zaman:** Aynı otonomu simülatörle ve robota çıkarak yazma/test süresi.
2. **Sim–gerçek farkı:** Aynı PID ile simde ve robotta ulaşılan mesafe; AdvantageScope'ta yan yana grafik.
3. **Dışarıdan kullanıcı:** 5–10 kişi, ilk çalışan otonomu kaç dakikada yazdı.

## Sonraki adımlar

- **Aşama 2:** Asansör / kol (`ElevatorSim`, `SingleJointedArmSim`), config'e parça ekleme.
- **Aşama 3:** Saha + oyun parçası (maple-sim), robot kurma arayüzü (config üreten).
- **Aşama 4:** Kendi görsel simülatörünüz (HALSim WebSocket) ve tarayıcı arayüzü.
