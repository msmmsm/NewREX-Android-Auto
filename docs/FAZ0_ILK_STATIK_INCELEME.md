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
**Durum:** Kısmen düzeltildi  
Gizlilik metni, üçüncü taraf servislerin IP adresini görebildiğini ve hava durumu için yaklaşık konum gönderildiğini açıkça belirtecek şekilde güncellendi. Yeni yol haritasındaki bulut tabanlı AI, aile paylaşımı veya uzaktan takip gibi özellikler uygulanırsa veri akışı ve izinler yeniden değerlendirilmeli.

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
- [ ] Temiz checkout derleme, lint ve test raporu (otomatik testler eklendi; CI henüz çalıştırılmadı)
- [ ] Marka/paket kimliği kararı ve NOTICE/GPL uyum incelemesi
- [ ] PRIVACY.md veri akışı doğrulaması
- [ ] Gerçek OBD adaptörü ve araç test planı

## Test ve tamamlanma kuralı

Bu kayıttaki hiçbir özellik yalnızca kaynak kodunda sınıf veya ekran bulunduğu için "çalışıyor" sayılmaz. Otomatik test, gerçek cihaz testi ve donanım gerektiren maddelerde araç/adaptör doğrulaması gerekir. Test edilemeyen maddeler açıkça "test edilmedi" olarak kalır.


## 2026-11-15 — İlk düzeltmeler ve yeni doğrulama bulguları

### OmniDrive marka adı
- Uygulama kaynaklarındaki `app_name` zaten `OmniDrive` olarak tanımlıydı; Hakkında ekranı hâlâ NewREX gösteriyordu. Hakkında ekranı OmniDrive olarak düzeltildi.
- Özgün geliştirici atfı NewREX olarak korundu; atıf bağlantısı özgün projeye, uygulama içi GitHub bağlantısı ise msmmsm deposuna ayrıldı.
- README ve gizlilik belgesinin ürün adı OmniDrive olarak güncellendi.
- Paket kimliği `com.newrex.auto` henüz değiştirilmedi. NOTICE.md ek koşulu nedeniyle dağıtım öncesi ayrı paket kimliği kararı ve mevcut kurulumdan geçiş testi zorunlu.

### OBD-II kaynak kodu bulguları
- Mevcut `ObdManager` yalnızca Bluetooth Classic RFCOMM/SPP kullanıyor; BLE tarama/bağlantı kodu yok. README'deki BLE iddiası düzeltilerek desteklenmediği belirtildi.
- Polling döngüsü ile DTC okuma/silme işlemleri aynı Bluetooth akışını kullanıyordu. Komut/yanıt çiftlerinin karışmasını önlemek için Mutex ile seri hâle getirildi.
- DTC silme işlemi, ECU'nun Mode 04 olumlu yanıtı (`44`) alınmadan yerel hata listesini temizleyebiliyordu. Yerel liste artık yalnızca olumlu yanıt sonrası temizleniyor.
- DTC kategori kodlamasında C/B/U ailelerinin bazı alt kategorileri yanlış oluşturulabiliyordu. SAE iki kategori biti çözümlemesi düzeltildi.
- Adaptör bağlantısı kesilince önceki bağlantıdan kalan RPM, hız, sıcaklık, voltaj, yakıt ve DTC değerleri temizleniyor.
- Bu davranışlar için JUnit testleri eklendi; GitHub Actions test adımında başarısızlıkları gizleyen `|| true` kaldırıldı.

### Uygulama yapılandırması
- Manifest uygulama kategorisi `game` yerine `productivity` yapıldı.
- Cleartext HTTP izni henüz daraltılmadı. Kullanıcı tanımlı IPTV/radyo akışları ve yerel ağ kaynakları HTTP kullanabildiğinden, gerçek URL envanteri ve uyumluluk testi olmadan genel izni kapatmak medya işlevlerini bozabilir. Bu konu açık güvenlik işi olarak kalıyor.
- Depoda önceden `app/src/test` ve `app/src/androidTest` test dosyası bulunmuyordu. Yeni OBD parser testleri ilk otomatik test katmanıdır.

### Doğrulama durumu
- Kaynak değişiklikleri GitHub `master` dalına işlendi.
- Bu güncelleme için GitHub Actions çalışması henüz görünmüyor; commit durumlarında da raporlanmış kontrol yok. Dolayısıyla derleme ve testlerin başarılı olduğu iddia edilmiyor.
- Gerçek ELM327 adaptörü, araç ECU'su ve Toyota C-HR üzerinde fiziksel test yapılmadı.


### Marka tutarlılığı ve güncelleme kanalı
- Telefon ana ekranı, araç ekranı, Hakkında, Ayarlar, yardım metinleri ve sesli karşılama içindeki görünen ürün adı OmniDrive olarak düzenlendi.
- Uygulama içi güncelleme denetleyicisi artık özgün NewREX deposunu değil OmniDrive fork deposunu kontrol ediyor.
- Güncelleme denetleyicisi, OmniDrive deposunda sürüm bulunmadığında önceki upstream sürüm önbelleğini temizliyor; eski NewREX APK'sını OmniDrive güncellemesi gibi önermiyor.
- Self-install geçici APK dosya adı OmniDrive.apk olarak değiştirildi.
- OmniDrive deposunda şu anda yayımlanmış Release bulunmuyor. Bu nedenle README, APK'nın henüz yayımlanmadığını belirtiyor ve sürüm sayfasını işaret ediyor.
- Hakkında ekranında lisansın istediği tam `Original developer: NewREX` atfı ve özgün proje bağlantısı gösteriliyor.

### Açık dağıtım blokajı
- `applicationId` ve `namespace` hâlâ `com.newrex.auto`. `applicationId` değerini `com.omnidrive.auto` yapmak mevcut kurulumun üzerine normal güncelleme olarak kurulmayı engeller; yeni paket ayrı uygulama olur. Kullanıcı verilerinin taşınması ve self-install/kurulum aracı uyumu test edilmeden bu değişiklik yapılmadı.
- OmniDrive için ilk Release oluşturulmadığından otomatik güncelleme ve APK indirme akışı henüz uçtan uca doğrulanamaz.
