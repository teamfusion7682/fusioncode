# FRC Web Simülatör

Takımın **kendi WPILib Java robot kodunu** simüle eden ve sonucu tarayıcıda gösteren bir şablon:
robot sahada hareket eder, SmartDashboard değerleri grafik olur, **farklı senaryolar (ör. zayıf kod / PID) üst üste karşılaştırılır.**

İki kullanım şekli var:

| | **GitHub Pages (önerilen)** | **Yerel sunucu (atölye modu)** |
|---|---|---|
| Sunucu gerekir mi | Hayır. Statik site | Evet, `./run.sh` |
| Kodu nerede yazarsınız | Depoda (`github.dev` veya bilgisayarınız) | Sayfadaki editörde |
| Sonuç ne zaman görünür | Push'tan 1–2 dk sonra | Anında |
| Sayfa canlı kod çalıştırır mı | **Hayır**, yayın sırasında hesaplanmış sonuçları oynatır | Evet |
| Herkese açık yayın | Güvenli (sunucuda yabancı kod çalışmaz) | Önce "Güvenlik" bölümü |

> **Dürüst kapsam notu (sunumda da böyle söyleyin):** Bu proje gerçek WPILib'i çalıştırmaz. WPILib'in sık kullanılan
> sınıflarını **aynı paket ve aynı metot adlarıyla** yeniden yazar; bu sınıflarla sınırlı kod simülatörde de gerçek
> robotta da değişmeden derlenir. Desteklenen kapsam aşağıda listeli; listede olmayan API (Command tabanlı yapı, swerve,
> üretici kütüphaneleri vb.) henüz yok.

---

## 1) GitHub Pages ile yayınlama (sunucusuz)

### Nasıl çalışır

```
push → GitHub Actions
         ├─ robot/src içindeki Java kodunu derler  (hata varsa iş kırmızı biter, site güncellenmez)
         ├─ robot/scenarios.json'daki her senaryoyu 20 ms sabit adımla simüle eder
         └─ sonuçları (JSON) + arayüzü GitHub Pages'e yükler
ziyaretçi → statik sayfa sonuçları oynatır (kod ve sonuç yan yana, kimse sunucuya kod göndermez)
```

Simülasyon sabit adımlı ve deterministiktir: her ziyaretçi aynı sonucu görür.

### Adım adım kurulum

1. Bu depoyu GitHub'da kendi hesabınıza **fork'layın** (veya "Use this template"). Pages'in ücretsiz planda çalışması için
   depo genellikle **herkese açık (public)** olmalıdır; kodunuz herkese görünür olur.
2. Depoda **Settings → Pages → Build and deployment → Source: GitHub Actions** seçin.
3. `main` dalına bir değişiklik push'layın (veya **Actions → Siteyi yayınla → Run workflow**).
4. İş yeşil bitince site şurada: `https://<kullanıcı-adı>.github.io/<depo-adı>/`

### Kendi robotunuzu koymak

Yalnızca `robot/` klasörünü düzenlersiniz:

```
robot/src/frc/robot/*.java   robot kodunuz (WPILib Java, aşağıdaki listeyle sınırlı)
robot/robot-config.json      robotun fiziği: motor, dişli oranı, kütle, tekerlek, kablolama
robot/scenarios.json         sitede gösterilecek senaryolar (isteğe bağlı)
```

Depoyu tarayıcıda düzenlemek için GitHub'da depo sayfasında klavyeden **`.`** (nokta) tuşuna basın (github.dev),
dosyayı değiştirin, sol menüden **commit** edin. Site 1–2 dakikada güncellenir.

`scenarios.json` yoksa otonom seçicisindeki (`SendableChooser`) her seçenek için bir senaryo otomatik üretilir.
Örnek senaryo:

```json
{ "id": "pid", "label": "PID (encoder ile)", "mode": "autonomous", "duration": 6,
  "chooser": { "Otonom": "2 - PID (encoder ile)" } }
```

`mode`: `autonomous`, `teleop` veya `test`. Teleop için `"joystick": [{"t": 0, "axes": [0, -0.8, 0, 0, 0, 0]}, ...]`
(`t` saniye; eksenler: 0 solX, 1 solY, 4 sağX). En fazla 6 senaryo aynı anda gösterilir.

Sahada hedef çizgisi ve grafikte referans çizgisi için kodunuzdan şunu yazın:

```java
SmartDashboard.putNumber("Mesafe (m)", mesafe);
SmartDashboard.putNumber("Hedef (m)", hedef);   // adı "Hedef" ile başlayan, birimi (m) olan değer
```

### Hata olursa

Actions sekmesinde kırmızı çarpıya tıklayın. Derleme hataları dosya ve satır numarasıyla, senaryo hataları
senaryo adıyla yazılır. **Hata varken yayındaki site değişmez**, önceki çalışan sürüm görünmeye devam eder.

| Mesaj | Anlamı |
|---|---|
| `incompatible types ...` | Derleme hatası; satır numarası notta |
| `Senaryo 'x' basarisiz` + `...Exception` | Robot kodu o senaryoda çöktü |
| `... saniyede bitmedi` | Robot kodunda sonsuz döngü / çok ağır hesap |
| `izin verilmiyor` | Kodunuz dosya/ağ/süreç gibi kısıtlı bir işlem yapmaya çalıştı |
| `robot-config.json ... hatali` | Yapılandırma değeri geçersiz (mesaj hangisi olduğunu söyler) |

### Kendi alan adınız (örn. `simulator.takimadi.org`)

Doğrulanmış GitHub dokümanına göre ([custom domain](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site)):

1. **Önce GitHub'a ekleyin, sonra DNS'i değiştirin:** Settings → Pages → **Custom domain** alanına alan adını yazıp **Save**.
   (Sıra önemli: aksi halde başkası alt alan adınızda site yayınlayabilir. Mümkünse alan adınızı GitHub'da doğrulatın.)
2. DNS sağlayıcınızda kayıtları girin:
   - **Alt alan adı** (`simulator.takimadi.org`): `CNAME` kaydı → `<kullanıcı-adı>.github.io` (depo adı yazılmaz).
   - **Kök alan adı** (`takimadi.org`): dört `A` kaydı: `185.199.108.153`, `185.199.109.153`, `185.199.110.153`, `185.199.111.153`
     (isteğe bağlı IPv6 için `AAAA`: `2606:50c0:8000::153`, `2606:50c0:8001::153`, `2606:50c0:8002::153`, `2606:50c0:8003::153`).
3. DNS yayılınca Settings → Pages'te **Enforce HTTPS**'i işaretleyin (seçenek açılması 24 saati bulabilir).
4. `*.takimadi.org` gibi **wildcard DNS kaydı kullanmayın** (GitHub bunu alan adı ele geçirme riski olarak uyarıyor).

Bu depodaki gibi **Actions ile yayınlandığında `CNAME` dosyası gerekmez** (yok sayılır); alan adı yalnızca Settings'ten girilir.

---

## 2) Yerel sunucu modu (atölye / geliştirme)

Gereksinim: **JDK 17–23** (`java -version`; JDK 24+ desteklenmez, bkz. "Güvenlik").

```bash
bash build.sh          # çekirdek + sunucuyu derler
./run.sh               # http://127.0.0.1:8080
```

Sayfadaki editörde kodu yazıp **Çalıştır**'a basarsınız; sunucu derler, ayrı bir JVM sürecinde simüle eder ve sonucu oynatır.
Aynı ağdaki başkaları için `./run.sh --host 0.0.0.0 --port 8080` (önce "Güvenlik" bölümünü okuyun).

```
Tarayıcı ──POST /api/run──▶ Sunucu: javac → ayrı JVM (temiz ortam, 256 MB, 20 sn, SecurityManager) → JSON ──▶ oynatma
```

---

## Desteklenen WPILib API'si

| Alan | Var |
|---|---|
| Yaşam döngüsü | `TimedRobot` (init/periodic/exit, `addPeriodic`), `RobotBase.startRobot`, `Timer`, `RobotController`, `DriverStation` (durum) |
| Motor | `PWMSparkMax`, `PWMVictorSPX`, `PWMTalonSRX`, `MotorController` |
| Sürüş | `DifferentialDrive` (arcade, tank, curvature) |
| Sensör | `Encoder`, `AnalogGyro`, `ADXRS450_Gyro` |
| Kontrol | `PIDController` (WPILib ile aynı algoritma), `MathUtil`, `Units`, `Rotation2d` |
| Giriş | `XboxController`, `Joystick`, `GenericHID` (senaryo girişi) |
| Panel | `SmartDashboard.putNumber/putBoolean/putString`, `SendableChooser` |

**Henüz yok:** Command tabanlı yapı (`CommandScheduler`), swerve, kinematik/odometri sınıfları, `Field2d`,
NetworkTables, CAN motor sürücüleri (REV/CTRE kütüphaneleri), asansör/kol fiziği, oyun parçaları.

### robot-config.json

`drivetrain`: `motor` (NEO, NEO550, Vortex, CIM, MiniCIM, Falcon500, KrakenX60), `motorsPerSide`, `gearing`,
`massKg`, `momentOfInertiaKgM2`, `wheelRadiusMeters`, `trackWidthMeters`, `efficiency`, `rollingFriction`,
`turnFriction`, `tractionCoefficient`, `currentLimitAmps`, `idleMode` (brake/coast).
`wiring`: sol/sağ için PWM kanalları ve `reversed`. `encoders`: kanallar, taraf, `countsPerRev`. `gyro.channel`.
Hatalı değerler çalıştırmadan önce açık bir mesajla reddedilir.

---

## Testler

```bash
bash build.sh
python3 tests/test_site.py   # ~20 sn: site üretici (GitHub Actions hattı): başarı, hata yolları, saldırgan kod
python3 tests/test_api.py    # ~1 dk: yerel sunucu: çalışma, hatalar, 17 saldırı senaryosu, zaman aşımı, bellek bombası
```

Ağın kapalı olduğu, bir yerel dinleyiciye bağlantı gelip gelmediğine bakılarak doğrulanır. Arayüz (iki mod) ayrıca gerçek
bir tarayıcıda uçtan uca denenmiştir; bu tarayıcı testleri depoda yoktur.

**Test edilmemiş:** GitHub'ın kendi ortamında (Actions + Pages + özel alan adı) bir çalıştırma. Workflow geçerli YAML'dir
ve aynı komutlar yerelde çalıştırılmıştır, fakat ilk gerçek yayın sizin hesabınızda olacak. Takılırsanız Actions günlüğünü
bakın; sorun çoğunlukla Pages kaynağının "GitHub Actions" seçilmemesidir (2. adım).

## Güvenlik

- **GitHub Pages modunda** sunucuda yabancı kod çalışmaz. Robot kodunuz yalnızca sizin deponuzun Actions
  çalıştırıcısında, sizin push'ladığınız haliyle çalışır. Fork'lardan gelen PR'larda gizli anahtarlar verilmez;
  bu depoda hiç gizli anahtar kullanılmıyor.
- **Yerel sunucu modu** başkasının Java kodunu çalıştırır; varsayılan olarak yalnızca `127.0.0.1`'de dinler.
  Önlemler: ayrı JVM süreci, temiz ortam, 256 MB / 20 sn sınırı, 8 KB çıktı sınırı, `-proc:none`, `SecurityManager`
  (dosya yazma, `exec`, ağ, `System.exit`, ortam okuma, `setAccessible` vb. engellenir).
  `SecurityManager` JDK'de kullanımdan kalkmıştır ve tek başına **tam bir güvenlik sınırı değildir**; JDK 24+ sürümünde
  kaldırıldığı için sunucu o sürümlerde başlamayı reddeder. Kapalı bir ağda (atölye) makuldür; **internete açacaksanız**
  konteyner/VM içinde (ağsız, salt okunur dosya sistemi, CPU/bellek/PID sınırı, hız sınırı) çalıştırın. Bu proje bunları sağlamaz.

## Jüri için ölçüm şablonu

Sunumdaki sayılar tahmin değil ölçüm olmalı. Her denemeyi kaydedin:

| Tarih | Kişi | Yöntem (Simülatör / Robot) | Süre (dk) | Sonuç (hedefe ulaştı mı, hata m) | Not |
|---|---|---|---|---|---|

1. **Zaman:** Aynı otonomu simülatörle ve robota çıkarak yazma/test süresi → "robot başına test süresi X saat azaldı".
2. **Sim–gerçek farkı:** Aynı PID ile simde ve robotta ulaşılan mesafe. `reference-wpilib-project/` aynı kodun gerçek
   WPILib ile çalışan sürümüdür (derlenmemiş; AdvantageScope'a log yazar). İki grafiği yan yana koyun.
   Fizik parametreleri (kütle, sürtünme) kendi robotunuza göre kalibre edilmelidir; kalibre etmeden "gerçeğe yakın" demeyin.
3. **Dışarıdan kullanıcı:** 5–10 kişi, ilk çalışan otonomu kaç dakikada yazdı.

## Klasör yapısı

```
robot/                   TAKIMIN düzenlediği tek klasör: src/, robot-config.json, scenarios.json
.github/workflows/       pages.yml (yayın), tests.yml (simülatör testleri)
tools/build_site.py      derle + simüle et + statik siteyi üret (Actions bunu çalıştırır)
web/                     arayüz (bağımlılıksız HTML/CSS/JS; iki modu da destekler)
core/src/edu/wpi/...     WPILib uyumlu sınıflar (takım kodunun import ettiği)
core/src/frcsim/         sanal HAL, fizik, SimRunner, Guard (kısıtlayıcı), Json
server/src/frcsim/server yerel sunucu modu
tests/                   test_site.py, test_api.py
reference-wpilib-project gerçek WPILib ile çalışan eşdeğer robot projesi (derlenmemiş)
```

## Yol haritası

1. **Tarayıcıda canlı çalıştırma** (TeaVM ile çekirdeği JS'e derleyip sayfada çalıştırmak): sunucusuz *ve* anında geri bildirim
2. Command tabanlı yapı (`CommandScheduler`, `Subsystem`, `Command`): çoğu takım bunu kullanır
3. Asansör/kol fiziği ve config'e parça ekleme
4. Swerve + kinematik/odometri, `Field2d`
5. Saha elemanları, oyun parçası, puanlı görevler
6. Robot kurma arayüzü (robot-config.json üreten)
7. Yerel sunucu için konteyner tabanlı güvenli barındırma şablonu
