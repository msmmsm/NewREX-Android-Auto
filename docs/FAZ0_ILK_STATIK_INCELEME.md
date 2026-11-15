# Faz 0 — İlk Statik İnceleme Kaydı

Tarih: 2026-11-15  
İncelenen depo: msmmsm/NewREX-Android-Auto  
İncelenen dal: master  
İnceleme başlangıcındaki dal commit'i: 28ed7c34d23e22aa51f86a1b6a891308312b7dec  
İnceleme türü: Kaynak kodu statik incelemesi. APK, gerçek telefon, Android Auto araç ekranı, OBD adaptörü veya Toyota C-HR üzerinde bu kayıtta test yapılmamıştır.

## Bulgular

### F0-01 — Genel cleartext ağ izni
**Dosyalar:** app/src/main/AndroidManifest.xml, app/src/main/res/xml/network_security_config.xml  
**Durum:** Kısmen düzeltildi; açık risk mevcut  
- HTTPS sertifika hatasında otomatik HTTP'ye düşme ve HTTPS→HTTP çapraz protokol yönlendirmesi kaldırıldı.
- Kullanıcı tarafından yüklenmiş CA sertifikalarına genel güven kaldırıldı; sistem CA deposu kullanılıyor.
- Kullanıcı tanımlı IPTV/radyo listeleri ve yerel ağ kaynakları için temel ağ yapılandırmasında cleartext HTTP hâlâ açık. Dinamik kullanıcı URL'leri nedeniyle alan adı bazında statik istisna listesi yeterli değildir. Bu nedenle HTTP kaynakları kullanıcı tarafından açıkça girilen adreslerle sınırlı tutulmalı; hassas hesap/kimlik bilgileri HTTP üzerinden kullanılmamalıdır.
- Sonraki iş: HTTP kaynak envanteri, kullanıcıya HTTP kaynak risk uyarısı ve mümkünse kullanıcı bazında güvenli tercih eklenmesi.

### F0-02 — Marka, paket kimliği ve ek lisans koşulları
**Dosyalar:** README.md, app/build.gradle.kts, NOTICE.md  
**Durum:** Yayın öncesi blokaj  
Uygulama adı OmniDrive ve Gradle `applicationId` değeri `com.omnidrive.auto` olarak değiştirildi. Kotlin `namespace` değeri `com.newrex.auto` bırakıldı; bu, Android kurulum kimliği değildir. NOTICE.md'nin farklı ad ve farklı application ID koşulu teknik olarak karşılanıyor. Hakkında ekranında `Original developer: NewREX` atfı ve özgün proje bağlantısı korunuyor. Hukuki uygunluk hakkında ayrıca hukuk görüşü verilmiş değildir.

### F0-03 — Gizlilik beyanının doğrulanması
**Dosya:** PRIVACY.md  
**Durum:** Kısmen düzeltildi  
Gizlilik metni, üçüncü taraf servislerin IP adresini görebildiğini ve hava durumu için yaklaşık konum gönderildiğini açıkça belirtecek şekilde güncellendi. Yeni yol haritasındaki bulut tabanlı AI, aile paylaşımı veya uzaktan takip gibi özellikler uygulanırsa veri akışı ve izinler yeniden değerlendirilmeli.

### F0-04 — Uygulama kategorisi
**Dosya:** app/src/main/AndroidManifest.xml  
**Durum:** Kontrol edilmeli  
`android:appCategory="productivity"` olarak değiştirildi.

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
- [ ] Temiz checkout yerel derleme, lint ve test raporu (Android SDK/Gradle ortamı sağlandığında)
- [x] Marka/paket kimliği teknik değişikliği ve Hakkında atfı
- [ ] NOTICE/GPL koşullarının hukuki incelemesi
- [ ] PRIVACY.md veri akışı doğrulaması
- [ ] Gerçek OBD adaptörü ve araç test planı

## Test ve tamamlanma kuralı

Bu kayıttaki hiçbir özellik yalnızca kaynak kodunda sınıf veya ekran bulunduğu için "çalışıyor" sayılmaz. Otomatik test, gerçek cihaz testi ve donanım gerektiren maddelerde araç/adaptör doğrulaması gerekir. Test edilemeyen maddeler açıkça "test edilmedi" olarak kalır.


## 2026-11-15 — İlk düzeltmeler ve yeni doğrulama bulguları

### OmniDrive marka adı
- Uygulama kaynaklarındaki `app_name` zaten `OmniDrive` olarak tanımlıydı; Hakkında ekranı hâlâ NewREX gösteriyordu. Hakkında ekranı OmniDrive olarak düzeltildi.
- Özgün geliştirici atfı NewREX olarak korundu; atıf bağlantısı özgün projeye, uygulama içi GitHub bağlantısı ise msmmsm deposuna ayrıldı.
- README ve gizlilik belgesinin ürün adı OmniDrive olarak güncellendi.
- Android `applicationId` artık `com.omnidrive.auto`; kaynak `namespace` değeri `com.newrex.auto` olarak korundu. Eski uygulamadan ayrı kurulum oluşur; veri aktarımı yoktur.

### OBD-II kaynak kodu bulguları
- Mevcut `ObdManager` yalnızca Bluetooth Classic RFCOMM/SPP kullanıyor; BLE tarama/bağlantı kodu yok. README'deki BLE iddiası düzeltilerek desteklenmediği belirtildi.
- Polling döngüsü ile DTC okuma/silme işlemleri aynı Bluetooth akışını kullanıyordu. Komut/yanıt çiftlerinin karışmasını önlemek için Mutex ile seri hâle getirildi.
- DTC silme işlemi, ECU'nun Mode 04 olumlu yanıtı (`44`) alınmadan yerel hata listesini temizleyebiliyordu. Yerel liste artık yalnızca olumlu yanıt sonrası temizleniyor.
- DTC kategori kodlamasında C/B/U ailelerinin bazı alt kategorileri yanlış oluşturulabiliyordu. SAE iki kategori biti çözümlemesi düzeltildi.
- Adaptör bağlantısı kesilince önceki bağlantıdan kalan RPM, hız, sıcaklık, voltaj, yakıt ve DTC değerleri temizleniyor.
- Bu davranışlar için JUnit testleri eklendi. Testler GitHub Actions üzerinden çalıştırılmadı; bu çalışma için Actions kullanılmayacak.

### Uygulama yapılandırması
- Manifest uygulama kategorisi `game` yerine `productivity` yapıldı.
- Cleartext HTTP izni henüz daraltılmadı. Kullanıcı tanımlı IPTV/radyo akışları ve yerel ağ kaynakları HTTP kullanabildiğinden, gerçek URL envanteri ve uyumluluk testi olmadan genel izni kapatmak medya işlevlerini bozabilir. Bu konu açık güvenlik işi olarak kalıyor.
- Yeni OBD parser testleri eklendi; test kaynak dosyasında şu anda 7 test metodu bulunuyor. Yerel Android SDK/Gradle ortamı olmadığı için testler henüz çalıştırılmadı.

### Doğrulama durumu
- Kaynak değişiklikleri GitHub `master` dalına işlendi.
- Kullanıcının talimatı doğrultusunda GitHub Actions kullanılmadı. Bu çalışma ortamında Android SDK ve Gradle bulunmadığından yerel APK derlemesi/testi de yapılamadı. Derleme ve testlerin başarılı olduğu iddia edilmiyor.
- Gerçek ELM327 adaptörü, araç ECU'su ve Toyota C-HR üzerinde fiziksel test yapılmadı.


### Marka tutarlılığı ve güncelleme kanalı
- Telefon ana ekranı, araç ekranı, Hakkında, Ayarlar, yardım metinleri ve sesli karşılama içindeki görünen ürün adı OmniDrive olarak düzenlendi.
- Uygulama içi güncelleme denetleyicisi artık özgün NewREX deposunu değil OmniDrive fork deposunu kontrol ediyor.
- Güncelleme denetleyicisi, OmniDrive deposunda sürüm bulunmadığında önceki upstream sürüm önbelleğini temizliyor; eski NewREX APK'sını OmniDrive güncellemesi gibi önermiyor.
- Self-install geçici APK dosya adı OmniDrive.apk olarak değiştirildi.
- OmniDrive deposunda şu anda yayımlanmış Release bulunmuyor. Bu nedenle README, APK'nın henüz yayımlanmadığını belirtiyor ve sürüm sayfasını işaret ediyor.
- Hakkında ekranında lisansın istediği tam `Original developer: NewREX` atfı ve özgün proje bağlantısı gösteriliyor.

### Açık dağıtım blokajı
- `applicationId` `com.omnidrive.auto` olarak değiştirildi; mevcut `com.newrex.auto` uygulamasının üzerine güncelleme olmaz. Yeni paket ayrı uygulama olarak kurulur ve veriler otomatik taşınmaz. `SelfInstall` ve FileProvider authority yeni applicationId'yi kullanır; fiziksel cihazda kurulum testi henüz yapılmadı.
- OmniDrive için ilk Release oluşturulmadığından otomatik güncelleme ve APK indirme akışı henüz uçtan uca doğrulanamaz.


### OBD veri keşfi — ek geliştirme
- Bağlantı başlatılırken Mode 01 PID destek bitmap'leri (PID 00/20/40 blokları) okunmaya başlandı.
- ECU'nun desteklemediği hız, RPM, motor yükü, soğutma suyu ve yakıt PID'leri bilindiğinde gereksiz yere sürekli sorgulanmıyor.
- Motor yükü PID 04 ayrıştırması eklendi.
- TelemetryHub artık motor yükü ve boost akışlarını da `combine` girdisi olarak dinliyor; yalnızca bu iki alan değiştiğinde ekranın güncellenmemesi sorunu giderildi.
- PID bitmap ve motor yükü ayrıştırması için ek JUnit testleri eklendi.
- Destek bitmap'i alınamazsa geriye dönük uyumluluk için temel PID sorguları denenmeye devam ediyor.

**Sınırlama:** BLE desteği yoktur; ELM327 Bluetooth Classic SPP gerektirir. Toyota hibrit özel PID'leri henüz uygulanmış veya gerçek C-HR üzerinde doğrulanmış değildir.


### 2026-11-15 — OmniDrive dağıtım paket kimliği değişikliği
- Android dağıtım kimliği `applicationId`, `com.newrex.auto` yerine `com.omnidrive.auto` olarak değiştirildi.
- Kotlin kaynaklarının `namespace` değeri `com.newrex.auto` olarak korundu. Bu, kaynak paketlerinin gereksiz taşınmasını önler; Android'e kurulan uygulamanın benzersiz kimliği ise yeni applicationId'dir.
- FileProvider authority manifestte `${applicationId}.files` üzerinden üretildiğinden yeni kimliğe otomatik uyum sağlar. Self-install akışı `context.packageName` kullandığından yeni kimlikle çalışacak şekilde kalır.
- Windows kurulum betiği yeni paket kimliğini ve bu fork'un GitHub deposunu kullanacak şekilde güncellendi.
- Eski ZIP kurulum aracı yeni paket kimliğini içermediğinden depodan kaldırıldı; yeni paket kimliğiyle yeniden paketlenip test edilmeden indirilebilir gösterilmeyecek.
- `com.newrex.auto` ile kurulu eski sürüm ayrı uygulama olarak kalır. Uygulama verileri otomatik taşınmaz; eski uygulamayı kaldırmak kullanıcı tercihidir.
- Henüz APK derlemesi, yerel test veya cihaz üzerinde yükleme doğrulaması yapılmadı. İlk OmniDrive APK'sı ve güncel kurulum paketi yayımlanmadan dağıtım tamamlanmış sayılmaz.


### 2026-11-15 — OBD telemetri tazelik koruması
- Desteklenen PID sorgusu boş, hatalı veya ECU tarafından reddedilen yanıt döndürdüğünde önceki başarılı ölçüm artık ekranda tutulmuyor; ilgili StateFlow alanı `null` yapılıyor.
- Bluetooth taşıma katmanı istisna verirse OBD bağlantı durumu kapatılıyor, tüm OBD ölçümleri temizleniyor, bağlantı durumu `ERROR` oluyor ve polling döngüsü duruyor. Böylece kopmuş adaptörün son ölçümleri canlıymış gibi sunulmuyor.
- Bu değişiklikler kaynak koduna işlendi; yerel test ve fiziksel ELM327/araç doğrulaması bekliyor.


### 2026-11-15 — Çoklu ECU yanıt ayrıştırma düzeltmesi
- Bazı ELM327 adaptörleri bir sorguda birden fazla ECU'nun CAN yanıtını art arda döndürebilir. Hız, soğutma suyu, yakıt ve motor yükü ayrıştırıcıları artık ilgili PID yanıtından sonra yalnızca ilk veri baytını okur; takip eden ECU çerçeveleri ilk değerin sayısal dönüşümünü bozmaz.
- RPM ayrıştırıcısı zaten ilk iki veri baytını kullanıyordu; çoklu ECU örneği için regresyon testi eklendi.
- Çoklu ECU örnekleri için test kapsamı genişletildi. Test dosyasında toplam 7 test metodu vardır.
- Bu testler kaynak koduna eklendi ancak çalıştırılmadı. Gerçek ELM327/araç yanıtlarıyla doğrulama gereklidir.


### GitHub Actions kullanım kararı
- Kullanıcı talimatıyla GitHub Actions iş akışı depodan kaldırıldı; otomatik APK derleme/yayın akışı kullanılmayacak.
- Bu ortamda Java 21 var; ancak Gradle ve Android SDK/ADB bulunmadığından burada Android APK derlemesi yapılamadı.
- APK üretimi için geliştirici bilgisayarında Android SDK ve Gradle Wrapper ile yerel derleme yapılması gerekir. Bu raporda derleme başarılı olarak işaretlenmemiştir.


### 2026-11-15 — Medya bağlantısında TLS düşürme güvenlik düzeltmesi

**Bulgu:** Önceki TlsFallbackFactory, herhangi bir HTTPS medya kaynağında TLS/sertifika hatası oluştuğunda aynı adresi otomatik HTTP ile yeniden açabiliyordu. Ayrıca Media3 HTTP kaynağında çapraz protokol yönlendirmeleri açıktı. Bu, yayın içeriğinin şifrelenmemiş bağlantıya düşmesine neden olabilirdi.

**Düzeltme:**
- Oynatıcıdan otomatik HTTPS→HTTP TLS fallback kaldırıldı.
- Çapraz protokol yönlendirmeleri kapatıldı.
- Uygulama ağ yapılandırmasında kullanıcı tarafından yüklenmiş CA sertifikalarına genel güven kaldırıldı; sistem CA deposu kullanılıyor.
- Kullanıcının açıkça HTTP olarak tanımladığı IPTV/radyo adresleri, mevcut cleartext ağ politikası nedeniyle çalışabilir. HTTPS sertifika hatası veren kaynaklar artık HTTP'ye otomatik düşürülmez.

**Uyumluluk etkisi:** Hatalı/sona ermiş sertifika kullanan bazı yayınlar artık açılmayabilir. Bu, bağlantıyı sessizce şifresiz hâle getirmekten daha güvenli davranıştır. Yayın kaynağının geçerli TLS sertifikasıyla düzeltilmesi gerekir.

**Doğrulama:** Kaynak kodu değişikliği yapıldı; Android SDK/Gradle bulunmadığından derleme, lint ve cihaz üzerinde medya akışı testi çalıştırılmadı.


### F0-07 — Gereksiz Bluetooth tarama izni ve uygulama içi gizlilik ifadesi
**Dosyalar:** app/src/main/AndroidManifest.xml, app/src/main/java/com/newrex/auto/MainActivity.kt, PRIVACY.md  
**Durum:** Kaynak kodunda düzeltildi
- OBD yöneticisi yalnızca Android ile önceden eşleştirilmiş Bluetooth cihazlarını `bondedDevices` üzerinden listeliyor; yakındaki cihazları taramıyor. Bu nedenle Android 12+ `BLUETOOTH_SCAN` izni kaldırıldı. `BLUETOOTH_CONNECT` izni korundu.
- Telefon ekranının altındaki "hiçbir veri toplanmaz" ifadesi, üçüncü taraf servislerin IP adresini görebildiğini ve hava durumu sorgusunda yaklaşık konum gönderildiğini belirten gizlilik belgesiyle çelişiyordu. Metin, reklam/analiz takibinin bulunmadığını ve bazı özelliklerin üçüncü taraf servislere bağlandığını açıklayacak şekilde düzeltildi.
- PRIVACY.md Bluetooth izninin yalnızca eşleştirilmiş OBD adaptörüne bağlanmak için kullanıldığını açıklıyor.

**Doğrulama:** Kaynak kodu ve manifest statik olarak incelendi. Android izin diyaloğu ve gerçek Bluetooth adaptörüyle bağlantı testi yapılmadı.


### F0-08 — BLE desteği beyanı ve güncelleme bağlantısı doğrulaması
- Manifestte BLE özelliği gereksiz yere beyan ediliyordu; mevcut OBD yöneticisi Bluetooth Classic SPP kullanıyor ve BLE bağlantısı uygulamıyor. BLE feature bildirimi kaldırıldı ve izin açıklaması Classic SPP ile sınırlandırıldı. README zaten BLE desteğinin bulunmadığını açıklıyor.
- Güncelleme denetimi GitHub Releases API yanıtındaki `html_url` değerini açıyordu. Uygulama artık güncelleme ekranında sabit, resmi OmniDrive depo Releases adresini açıyor; API yanıtındaki yönlendirme URL'si kullanıcıya açılan bağlantı olarak kullanılmıyor.
- Bu değişiklikler kaynak kodu statik olarak işlendi. Derleme ve cihaz testi yapılmadı.
