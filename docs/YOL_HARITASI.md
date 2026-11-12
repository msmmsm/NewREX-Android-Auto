# NewREX Android Auto — Geliştirme Yol Haritası (Roadmap)

NewREX'i klasik bir medya uygulamasından yeni nesil bir **Otomobil Kokpit & Telemetri Merkezi**ne (Infotainment & Telemetry Cockpit) dönüştürme planıdır.

---

## 📌 Genel İlerleme Özeti
- **Mevcut Faz:** Tamamlandı 🎉 (Tüm Temel ve İleri Seviye Fazlar Tamamlandı)
- **Tamamlanan Fazlar:** 
  - Faz 1 (Altyapı & Ağ Güvenliği Sıkılaştırması)
  - Faz 2 (Kokpit Arayüzü, MBUX/Tesla Tasarımı, Mini Oynatıcı)
  - Faz 3 (OBD-II Bluetooth CAN-Bus Motoru & DTC Teşhis)
  - Faz 4 (3D/2.5D Toyota C-HR Modeli, Özel Plaka & Sketchfab Entegrasyonu)
  - Faz 5 (Modüler Mimari Refactoring, OBD UI & Otomatik CI/CD Release Hattı)
- **Son Güncelleme:** 2026-11-12

---

## 🚀 Faz 1: Güvenlik, Ağ Mimarisi ve Altyapı Temelleri (Öncelik: P0)
Uygulamanın güvenlik açıklarını kapatmak, veri katmanını çoklu kaynaklara (OBD / CarHardware / GPS) hazırlamak ve kesintisiz medya altyapısını kurmak.

- [x] **1.1. Ağ Güvenliği Sıkılaştırması (`mobile-security-coder`)**
  - [x] `network_security_config.xml` oluşturularak sınırsız HTTP (`usesCleartextTraffic=true`) açığının kapatılması.
  - [x] Yalnızca yerel ağ (IPTV, lokal stream, radyo HTTP sunucuları) ve izinli domainler için cleartext izni verilmesi; kalan tüm trafiğin HTTPS'e zorlanması.
- [x] **1.2. Çok Katmanlı Telemetri Modeli (`TelemetryHub.kt`)**
  - [x] Katman 1 (OBD-II), Katman 2 (Android Auto CarHardware), Katman 3 (GPS & Telefon Sensörleri) önceliklendirme mimarisi.
  - [x] Hız, devir (RPM), akü voltajı, motor harareti, yakıt seviyesi ve menzil için `StateFlow` modelleri.
  - [x] *Prensip:* Gerçek veri yoksa uydurma veri üretilmez, açıkça "Veri Yok / Bağlantı Bekleniyor" durumunu bildirir.
- [x] **1.3. Kalıcı Mini Oynatıcı (MiniPlayer) Altyapısı**
  - [x] TV veya Radyo çalarken kullanıcı diğer sayfalara (Araç, Hava, Ayarlar) geçtiğinde kesintisiz devam eden alt oynatıcı çubuğu veri modeli.

---

## 🎨 Faz 2: Yeni Kokpit Ana Ekran Düzeni (MBUX / Tesla Çizgisi) (Öncelik: P1)
Karşılama ekranı (`BoardView`) ile uygulama ekranını (`HomePage`) modern otomotiv standartlarında birleştirmek.

- [x] **2.1. Birleşik Kokpit Ana Ekranı**
  - [x] Üst Durum Çubuğu: Saat, sıcaklık, hava durumu, sürüş modu (PARK / SÜRÜŞTE), bağlantı ikonları (OBD / GPS).
  - [x] Merkez Araç Bölgesi: 3D model veya 2.5D araç perspektifi.
  - [x] Dinamik Telemetri Çubuğu: Hız, Devir/Voltaj, Yakıt/Menzil göstergesi.
- [x] **2.2. Akıllı Medya Kartları & Son Kullanılanlar (Dock)**
  - [x] [ TV ] [ YouTube ] [ Radyo ] [ Web ] hızlı başlatıcıları.
  - [x] "Son Kullanılanlar" (En son izlenen kanal, son dinlenen radyo veya podcast) tek dokunuşla başlatma şeridi.
- [x] **2.3. Kalıcı Mini Oynatıcı UI Entegrasyonu**
  - [x] Oynatılan medyanın logo/başlığı, oynat/duraklat, sessize alma ve hızlı kapatma düğmeleri (`MiniPlayerView.kt`).
- [x] **2.4. Güvenlik & Sürüş Kısıtlamaları**
  - [x] Araç hareket ettiği an (`DriveState.driving == true`) video gizleme, dikkat dağıtıcı animasyonları ve döndürmeyi otomatik dondurma.

---

## 🔌 Faz 3: OBD-II (ELM327 Bluetooth / BLE) Telemetri Motoru (Öncelik: P1)
Araçtan doğrudan CAN-Bus telemetri ve motor teşhis verilerini okuma.

- [x] **3.1. Bluetooth & BLE Bağlantı Yöneticisi (`ObdBluetoothManager` / `ObdManager.kt`)**
  - [x] Yakındaki ve eşleşmiş ELM327 / OBD2 cihazlarını listeleme, RFCOMM SPP soket bağlantısı ve yeniden bağlanma yönetimi.
- [x] **3.2. Standart PID Ayrıştırıcı & Sorgulama Döngüsü**
  - [x] Motor Devri (RPM `010C`), Gerçek Hız (`010D`), Hararet (`0105`), Akü Voltajı (`AT RV`), Depo Seviyesi (`012F`).
- [x] **3.3. Arıza Teşhis Kodları (DTC) & Check Engine**
  - [x] Arıza kodlarını okuma (`Mode 03`), Türkçe hata ayrıştırması ve sürücü onaylı kod silme (`Mode 04`).
- [x] **3.4. 0-100 km/s Performans & Hızlanma Telemetrisi**
  - [x] TelemetryHub üzerinden tekerlek hızı tabanlı anlık telemetri beslemesi.

---

## 🚘 Faz 4: 3D Araç Modeli & Etkileşimli Gövde (Öncelik: P1)
Google Filament / GLB tabanlı, mobil GPU dostu canlı araç modeli.

- [x] **4.1. Filament / GLB Render Bileşeni (`CarModelView.kt`)**
  - [x] GLB model arama/yükleme altyapısı ve güvenli geri dönüş (ChassisRendererView) entegrasyonu.
  - [x] Sketchfab model ekleme rehberi ve format standartlarının belirlenmesi (`docs/SKETCHFAB_MODEL_REHBERI.md`).
- [x] **4.2. Park & Sürüş Durum Yönetimi (`diagnose-android-overheating`)**
  - [x] Park halinde 360° parmakla inceleme; sürüşe geçildiğinde açıyı otomatik sabitleme, dokunmayı kilitleme ve GPU/pil yükünü sıfırlama.
  - [x] `Saver.kt` tasarruf modunda gereksiz render döngülerinin kapatılması.
- [x] **4.3. Dinamik Araç Rengi**
  - [x] Kullanıcının seçtiği gövde rengini (`Palette.accent`) araç şasisine ve materyaline canlı yansıtma.
- [x] **4.4. Etkileşimli Araç Noktaları (Hotspots)**
  - [x] Kaput Callout: Motor harareti, akü voltajı, devir telemetrisi.
  - [x] Depo Callout: Gerçek yakıt %, kalan menzil.
  - [x] Dokunulduğunda doğrudan Araç Teşhis Paneline (`VehiclePage`) yönlendirme.

---

## 🛠️ Faz 5: Modüler Mimari, Güvenlik & CI/CD (Öncelik: P1)
- [x] **5.1. Büyük Dosyaların Parçalanması & Modüler Mimari**
  - [x] `MainActivity.kt`'nin telefon yardımcı modüllerine ayrıştırılması (`PhoneCarHelper`, `PhoneMaintenanceHelper`, `PhoneThemeHelper`, `PhoneTroubleshootHelper`).
  - [x] `InfoPages.kt` dosyasının bağımsız sayfalara bölünmesi (`FavoritesPage`, `SettingsPage`, `AboutPage`, `ColorPickerPage`).
  - [x] `VehiclePage.kt` alt panellerinin ayrıştırılması (`VehiclePanels.kt`).
- [x] **5.2. OBD-II CAN-Bus Ayarları & DTC Kontrol UI**
  - [x] Ayarlar ekranında eşleşmiş ELM327 Bluetooth cihazlarını listeleme, bağlanma ve arıza kodlarını tarama/silme arayüzü.
- [x] **5.3. GitHub Actions Otomasyonu & Release Derleme**
  - [x] Birim testleri, otomatik APK üretimi ve git tag (v*) ile GitHub Release yayınlama hattı (`android-apk.yml`).

---

## ⚡ Faz 6: İleri Seviye Otomotiv Telemetrisi & Akıllı Kokpit (P1)
- [x] **6.1. 0-100 km/s ve 0-50 km/s Performans & Hızlanma Telemetrisi (`PerformanceTimer.kt`)**
  - [x] Dragy / RaceBox stili otonom sayaç: Araç durduğunda (0 km/s) otomatik "HAZIR" beklemesi, gaza basıldığı an başlayan hassas kronometre.
  - [x] 100 km/s'ye ulaşıldığında süreyi kilitleme, 0-50 km/s ara zamanı ve en iyi dereceleri (`CarLog.bestZeroHundred`) yerel olarak kaydetme.
  - [x] `VehiclePanels.kt` içine "Performans & Hızlanma" paneli entegrasyonu.
- [x] **6.2. Hıza Duyarlı Akıllı Ses Dengeleme (`SpeedVolumeCompensator.kt`)**
  - [x] Otomobil hızlandıkça oluşan yol/rüzgar gürültüsünü telafi etmek için ses seviyesini dinamik ölçekleme.
  - [x] Kırmızı ışıkta ve dur-kalklarda sesi orijinal seviyesine yumuşakça çekme.
  - [x] `SettingsPage.kt` üzerinden tek dokunuşla açılıp kapatılabilen ayar anahtarı.
