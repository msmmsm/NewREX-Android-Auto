# Faz 0 — İlk Statik İnceleme Kaydı

Tarih: 2026-11-15  
İncelenen depo: msmmsm/NewREX-Android-Auto  
İncelenen dal: master  
İncelenen son bilinen commit: c5d202848fc4b72868c5b2ed4ecbf7c706f8557d  
İnceleme türü: Kaynak kodu statik incelemesi. APK, gerçek telefon, Android Auto araç ekranı, OBD adaptörü veya Toyota C-HR üzerinde bu kayıtta test yapılmamıştır.

## Bulgular

### F0-01 — Genel cleartext ağ izni
**Dosya:** app/src/main/AndroidManifest.xml  
**Durum:** İncelenmeli  
Uygulama düzeyinde `android:usesCleartextTraffic="true"` tanımlı. Aynı manifestte `@xml/network_security_config` de kullanılıyor. Ağ güvenliği kararı verilmeden önce XML yapılandırmasının tamamı, tüm HTTP kaynakları ve yerel ağ/stream gereksinimleri çıkarılmalı. Hedef, tüm uygulamaya genel HTTP izni vermek yerine yalnızca zorunlu ve gerekçeli istisnaları tanımlamak.

### F0-02 — Marka, paket kimliği ve ek lisans koşulları
**Dosyalar:** README.md, app/build.gradle.kts, NOTICE.md  
**Durum:** Yayın öncesi blokaj  
README ve son commit uygulama markasının OmniDrive yönünde değiştiğini gösteriyor. Gradle yapılandırmasında namespace ve applicationId hâlâ `com.newrex.auto`. NOTICE.md, değiştirilmiş sürüm için farklı uygulama adı ve farklı uygulama kimliği şartı içeriyor. Bu koşulların hukuki yorumu uzman incelemesi gerektirebilir; teknik olarak ise yeni dağıtım kimliği seçilmeden ve atıf/marka ekranı doğrulanmadan yeni sürüm yayımlanmamalı.

### F0-03 — Gizlilik beyanının doğrulanması
**Dosya:** PRIVACY.md  
**Durum:** Kod ve bağımlılık denetimi gerekli  
Gizlilik metni verilerin geliştiriciye veya başka bir şirkete gönderilmediğini belirtiyor. Yeni yol haritasındaki bulut tabanlı AI, aile paylaşımı veya uzaktan takip gibi özellikler bu beyanı değiştirebilir. Her ağ isteği, konum aktarımı, ses işleme ve saklama davranışı veri akışı envanterine eklenmeli; metin yalnızca fiilî davranışla uyumlu olacak şekilde güncellenmeli.

### F0-04 — Uygulama kategorisi
**Dosya:** app/src/main/AndroidManifest.xml  
**Durum:** Kontrol edilmeli  
`android:appCategory="game"` tanımlı. Uygulamanın gerçek işleviyle uyumlu kategori seçilmeli.

### F0-05 — README özellik iddiaları
**Dosya:** README.md  
**Durum:** Doğrulanmadı  
README; 3D C-HR modeli, OBD-II, DTC, performans sayacı, hız uyarısı, araç takibi ve çoklu medya özellikleri listeliyor. Bu incelemede bunların gerçek cihaz/araç üzerinde çalıştığı doğrulanmadı. Her özellik kaynak kodu, otomatik test ve fiziksel test kanıtına göre sınıflandırılmalı.

### F0-06 — Mevcut veri kaynakları
**Dosyalar:** DriveState.kt, PlayerHub.kt, data/Prefs.kt  
**Durum:** Kısmi statik inceleme  
- DriveState, araç hız verisini Android Car API üzerinden almayı deniyor; veri yoksa GPS hızına düşüyor. Bu iki kaynağın UI'da ayırt edilmesi ve GPS doğruluğunun saha testleriyle değerlendirilmesi gerekir.
- PlayerHub, TV/radyo/podcast için ortak Media3 oynatıcı kullanıyor; hata kurtarma ve uzun süreli oynatma gerçek ağ koşullarında test edilmeli.
- Prefs, birçok ayarı SharedPreferences içinde saklıyor. Yolculuk geçmişi, sensör telemetrisi ve bakım kayıtları genişletilirken veri şeması, saklama süresi, yedekleme ve dışa aktarma politikası belirlenmeli.

## Sonraki teknik işler

- [ ] network_security_config.xml tam incelemesi ve HTTP kaynak envanteri
- [ ] Manifest izinleri için özellik-izin eşleştirmesi
- [ ] Kaynak kodu özellik envanteri ve README iddia matrisi
- [ ] Gradle bağımlılıkları, lisanslar ve kullanılmayan bağımlılıklar denetimi
- [ ] Temiz checkout derleme, lint ve test raporu
- [ ] Marka/paket kimliği kararı ve NOTICE/GPL uyum incelemesi
- [ ] PRIVACY.md veri akışı doğrulaması
- [ ] Gerçek OBD adaptörü ve araç test planı

## Test ve tamamlanma kuralı

Bu kayıttaki hiçbir özellik yalnızca kaynak kodunda sınıf veya ekran bulunduğu için "çalışıyor" sayılmaz. Otomatik test, gerçek cihaz testi ve donanım gerektiren maddelerde araç/adaptör doğrulaması gerekir. Test edilemeyen maddeler açıkça "test edilmedi" olarak kalır.
