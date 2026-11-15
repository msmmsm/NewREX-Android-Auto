# OmniDrive / NewREX — Akıllı Araç Platformu V2.0 Yol Haritası

> Durum: Planlama ve teknik doğrulama başlangıcı  
> Hedef: Android telefon + Android Auto + uyumlu OBD-II/BLE donanımı ile güvenilir, Türkçe akıllı araç platformu.  
> Temel ilke: Ölçülmeyen veya araç tarafından sağlanmayan hiçbir veri üretilmiş gibi gösterilmez. Güvenlik-kritik araç kontrolü yapılmaz.

## 1. Ürün kapsamı

### 1.1 Dijital kokpit
- [ ] Hız, devir, motor soğutma suyu sıcaklığı, emme havası sıcaklığı, akü/şarj voltajı
- [ ] Yakıt seviyesi, anlık/ortalama tüketim ve tahmini menzil (yalnızca desteklenen PID'lerde)
- [ ] Pusula, GPS irtifası, eğim/ivme ve yolculuk ölçümleri
- [ ] Minimal, Konfor ve Sportif gösterge profilleri
- [ ] HUD benzeri telefon ekranı görünümü; sürüşte dikkat dağıtmayacak tasarım
- [ ] Veri kaynağı, son güncellenme zamanı, doğruluk ve desteklenmiyor durumları

### 1.2 OBD-II ve araç sağlık merkezi
- [ ] ELM327 Bluetooth Classic/BLE bağlantı sihirbazı
- [ ] Adaptör uyumluluk/kalite testi, bağlantı durumu ve otomatik yeniden bağlanma
- [ ] Standart SAE PID kataloğu ve araç bazlı destek matrisi
- [ ] DTC okuma, açıklama, dondurulmuş kare ve geçmiş kayıt
- [ ] DTC silme yalnızca kullanıcı onayı, araç durumu ve güvenli koşul kontrolleriyle
- [ ] Canlı sensör panosu ve zaman serisi grafikleri
- [ ] Akü/şarj sistemi, sıcaklık, rölanti ve sürüş verisi izleme
- [ ] Bakım takvimi, servis/onarım geçmişi ve masraf kaydı
- [ ] Sağlık puanı: yalnızca ölçülebilir ve doğrulanmış parametrelerden hesaplanır

### 1.3 Toyota C-HR hibrit profili
- [ ] Model yılı, motor/hibrit varyantı ve pazar bölgesi seçimi
- [ ] Standart OBD-II ile erişilebilen PID'lerin otomatik tespiti
- [ ] Hibrit batarya/EV akışı, motor ve elektrik motoru durumu için destek araştırması
- [ ] Rejeneratif frenleme ve enerji tüketimi analitiği
- [ ] Hibrit batarya sıcaklığı/şarj bilgisi yalnızca doğrulanmış ECU veya uyumlu arayüz verisiyle
- [ ] Toyota'ya özgü PID'ler için kaynak, araç varyantı ve doğrulama kaydı
- [ ] Desteklenmeyen Toyota özel verilerinde açıkça "Araç tarafından sunulmuyor" gösterimi

### 1.4 Sürüş analizi ve sürüş koçu
- [ ] Ani hızlanma/frenleme ve viraj ivmesi tespiti
- [ ] Yolculuk skoru ve ekonomik sürüş önerileri
- [ ] Hız sınırı uyarıları (harita/veri kaynağı ve konum izni gerektirir)
- [ ] Sürüş geçmişi, rota, mesafe, süre ve tüketim özeti
- [ ] Sürücü tarafından ayarlanabilir uyarı eşikleri
- [ ] Yanlış pozitifleri azaltmak için sensör güven seviyesi ve kalibrasyon

### 1.5 Sürüş güvenliği yardımcıları
- [ ] Kamera tabanlı trafik işareti/şerit/öndeki araç araştırma prototipi
- [ ] Telefon sensörleriyle sınırlı tehlike/ani manevra uyarıları
- [ ] Sürücü yorgunluğu özellikleri için izin, mahremiyet ve cihaz uyumluluğu analizi
- [ ] Her ADAS benzeri özellik için sınırlamalar, test senaryoları ve açık uyarı
- [ ] OEM ADAS yerine geçmediğinin uygulama içinde belirtilmesi
- [ ] Direksiyon, fren, gaz veya vites kontrolü yok; CAN üzerinden keyfi komut gönderimi yok

### 1.6 Araç takibi ve park asistanı
- [ ] Park konumunu otomatik kaydetme ve haritada gösterme
- [ ] Yolculuk başlangıç/bitiş algılama ve geçmişi
- [ ] Coğrafi alan ve park süresi hatırlatıcıları
- [ ] Telefon araçtan ayrıldığında bağlantı kesilmesi üzerinden park kaydı
- [ ] Hareket/çekilme uyarısı yalnızca araçta sürekli güç ve bağlantı sağlayan donanım varsa
- [ ] Aile/cihaz paylaşımı için açık rıza ve erişim kontrolleri

### 1.7 Sesli asistan ve yapay zekâ
- [ ] Türkçe konuşma tanıma ve metin-ses
- [ ] Eller serbest, sürüşte kısa ve güvenli komut kümesi
- [ ] "Araç durumum nasıl?", "Son yolculuğum", "Arıza kodunu açıkla" sorguları
- [ ] Sensör verisini açıklayan asistan; bilinmeyen veriye tahmin üretmeme
- [ ] Bakım önerileri ve sürüş koçluğu
- [ ] Çevrimdışı temel komutlar ve çevrimiçi gelişmiş yanıt ayrımı
- [ ] Ses kayıtları ve bulut aktarımı için açık izin/mahremiyet kontrolleri

### 1.8 Otomasyonlar ve sürüş profilleri
- [ ] Bluetooth/Android Auto bağlantısında otomatik sürüş oturumu
- [ ] Bağlantı kesilince yolculuğu sonlandırma ve park konumu kaydı
- [ ] Gece/gündüz tema ve ses profili
- [ ] Şehir, uzun yol, ekonomi, sportif ve park profilleri
- [ ] Bakım, düşük voltaj ve kullanıcı tanımlı eşik uyarıları
- [ ] Otomasyonlar için izin, pil tüketimi ve arka plan çalışma testleri

### 1.9 Multimedya merkezi
- [ ] TV/IPTV, YouTube, radyo, podcast ve yerel medya için ortak oynatıcı deneyimi
- [ ] Kaynak/akış uyumluluk testi, yeniden deneme ve hata tanılama
- [ ] Türkçe sesli içerik arama
- [ ] Navigasyon ve uyarı sesleri için ses odağı/ducking
- [ ] Bağlantı kalitesine göre akış kalitesi ve veri tasarrufu
- [ ] Çevrimdışı medya ve devam etme durumu
- [ ] Android Auto sürüş kısıtları ve park hâli davranışlarının doğrulanması

### 1.10 Donanım genişletmeleri
- [ ] TPMS: uyumlu sensör/alıcı gereksinimi ve cihaz eşleştirme
- [ ] Park sensörü/geri kamera: cihaz ve araç bağlantı uyumluluğu
- [ ] Dashcam: depolama, döngüsel kayıt, ısı ve güç yönetimi
- [ ] Kabin sıcaklığı/hava kalitesi: harici sensör gereksinimi
- [ ] Kapı/kilit/cam/uzaktan çalıştırma: yalnızca resmi OEM entegrasyonu veya güvenli, belgeli donanım
- [ ] Kör nokta/360 kamera: harici donanım gereksinimi ve doğrulama

## 2. Teknik mimari ilkeleri

- Modüler katmanlar: vehicle, obd, telemetry, cockpit, trips, maintenance, assistant, safety, media, automation.
- Araç verisi için ortak model: değer, birim, kaynak, zaman damgası, güven seviyesi, destek durumu.
- OBD protokol işlemleri tek bir servis katmanından yürütülür; UI doğrudan ELM327 komutu göndermez.
- Bluetooth bağlantısı durum makinesiyle yönetilir: kapalı, taranıyor, bağlanıyor, bağlı, hata, yeniden bağlanıyor.
- Uzun süreli ölçümler için pil/ısı/depoma bütçesi ve örnekleme politikası.
- Hassas konum ve sürüş geçmişi varsayılan olarak cihazda tutulur; dışa aktarım/paylaşım açık kullanıcı eylemi gerektirir.
- Ağ bağlantısı olmayan durumda temel kokpit, OBD ve yolculuk işlevleri çalışabilir olmalıdır.
- Uygulama Android Auto, telefon ekranı ve park hâli arayüzlerini ayrı kullanım politikalarıyla yönetir.
- Uygulama adı/marka, paket adı, lisans ve gizlilik metinleri tek ve tutarlı ürün kimliğine göre güncellenir.

## 3. Geliştirme fazları ve kabul ölçütleri

### Faz 0 — Mevcut durum ve güvenilirlik kapısı
- [ ] README iddialarını gerçek kodla eşleştir; çalışmayan/eksik özellikleri ayır
- [ ] Manifest, izinler, cleartext ağ politikası, gizlilik ve lisans incelemesi
- [ ] Android Auto sürüş/park kısıtları ve uygulama dağıtım uygunluğu kontrolü
- [ ] Build, lint, unit test ve mevcut cihaz testlerinin başlangıç raporu
- [ ] Her modül için "mevcut / kısmi / yok / test edilmedi" matrisi

**Kabul:** Temiz checkout ile tekrarlanabilir derleme; kritik izin/ağ/lisans risklerinin kayıt altına alınması.

### Faz 1 — Araç bağlantısı ve veri temeli
- [ ] OBD-II bağlantı katmanı ve ELM327 Classic/BLE
- [ ] PID keşfi, sensör veri modeli, bağlantı telemetrisi
- [ ] DTC okuma ve canlı veri ekranı
- [ ] Simülatör/fake adapter ile otomatik testler; gerçek adaptörle cihaz testi

**Kabul:** Kopma/yeniden bağlanma, hatalı cevap, desteklenmeyen PID ve zaman aşımı testleri geçer; sahte veri gerçek veri gibi sunulmaz.

### Faz 2 — Dijital kokpit ve sürüş kayıtları
- [ ] Kokpit ekranları ve sürüş profilleri
- [ ] Yolculuk algılama, kayıt, geçmiş ve park konumu
- [ ] Hız/ivme uyarıları ve kalibrasyon

**Kabul:** GPS/araç hızı kaynağı ayrıştırılır; konum ve yolculuk verileri izinler olmadan toplanmaz.

### Faz 3 — Araç sağlık ve bakım
- [ ] DTC geçmişi, sensör grafikleri, bakım planı ve servis kayıtları
- [ ] Sağlık değerlendirmesi ve açıklanabilir uyarılar

**Kabul:** Her uyarı ölçüm kaynağı, eşik, zaman ve güven seviyesiyle izlenebilir.

### Faz 4 — Toyota C-HR hibrit analitiği
- [ ] C-HR varyant profilleri
- [ ] Toyota özel veri erişimi için doğrulanmış PID/arayüz araştırması
- [ ] Enerji akışı ve hibrit analitik ekranları

**Kabul:** Her gösterge gerçek araç/ECU verisiyle doğrulanır; erişilemeyen alanlar kapalı veya desteklenmiyor gösterilir.

### Faz 5 — Sürüş koçu ve güvenlik yardımcıları
- [ ] Sürüş puanı, ekonomik sürüş ve güvenli uyarılar
- [ ] Kamera tabanlı özellikler için ayrı prototip ve performans testleri

**Kabul:** Sürüş sırasında dikkat dağıtıcı etkileşim yok; yanlış pozitif/negatifler belgelenir; araç kontrolü yapılmaz.

### Faz 6 — Türkçe yapay zekâ asistanı
- [ ] Sesli komut altyapısı, komut güvenlik politikası
- [ ] Araç verisi sorguları, bakım ve sürüş özeti

**Kabul:** Asistan yalnızca mevcut telemetriyi kullanır; kritik veya belirsiz verilerde tahmin yapmaz; sürüşte ekran etkileşimi gerektirmez.

### Faz 7 — Otomasyon ve harici donanım
- [ ] Otomasyon kuralları ve kullanıcı izinleri
- [ ] TPMS, kamera, sensör ve OEM entegrasyon araştırmaları

**Kabul:** Her donanım özelliği desteklenen cihaz listesi ve fiziksel test raporuyla yayımlanır.

### Faz 8 — Multimedya ve deneyim bütünlüğü
- [ ] TV/YouTube/radyo/podcast ortak deneyimi
- [ ] Ses odağı, ağ/pil yönetimi ve hata kurtarma
- [ ] Türkçe arayüz ve erişilebilirlik denetimi

**Kabul:** Sürüş/park senaryoları, bağlantı kesintileri ve uzun süreli oynatma testleri geçer.

### Faz 9 — Yayın ve saha doğrulaması
- [ ] Desteklenen telefon/Android sürümü/araç/OBD adaptörü matrisi
- [ ] Gerçek araç testleri, pil/ısı/kararlılık ölçümleri
- [ ] Gizlilik, lisans, güvenlik ve kullanıcı belgeleri
- [ ] Sürümleme, geri alma ve hata raporlama süreci

**Kabul:** Yayın notlarında yalnızca test edilmiş özellikler "hazır" olarak işaretlenir.

## 4. Test matrisi

Her özellik aşağıdaki koşullarda test edilir:
- [ ] Desteklenen veri var
- [ ] Veri/PID desteklenmiyor
- [ ] Adaptör bağlı değil
- [ ] Bluetooth kapandı veya bağlantı koptu
- [ ] ECU yanıt vermiyor / bozuk yanıt
- [ ] Telefon çevrimdışı
- [ ] Uygulama arka plana alındı / süreç öldürüldü
- [ ] Android Auto bağlı / bağlantı kesildi
- [ ] Araç hareket hâlinde / park hâlinde
- [ ] İzin reddedildi / sonradan iptal edildi
- [ ] Düşük pil ve yüksek sıcaklık koşulları

## 5. Güvenlik ve kapsam dışı bırakılanlar

- Uygulama fren, gaz, direksiyon, vites, immobilizer veya kilit sistemlerini kontrol etmeyecek.
- Belgelenmemiş CAN mesajları gönderilmeyecek; güvenlik mekanizmaları atlatılmayacak.
- Telefon kamerası tabanlı uyarılar OEM ADAS olarak pazarlanmayacak.
- Standart OBD-II'nin sağlamadığı hibrit batarya/kapı/kilit/uzaktan çalıştırma verileri varmış gibi gösterilmeyecek.
- Sürüş sırasında video oynatma veya Android Auto kısıtlarını aşmaya yönelik davranış eklenmeyecek.
- Bulut/AI aktarımı, konum paylaşımı ve ses kaydı açık rıza ve açıklanmış veri akışı olmadan yapılmayacak.

## 6. İş takibi kuralı

Her iş maddesi için GitHub üzerinde şu bilgiler tutulur:
1. Etkilenen modül ve dosyalar
2. Veri kaynağı ve gerekli donanım
3. Uygulama/izin/gizlilik etkisi
4. Otomatik test ve gerçek cihaz kabul kriteri
5. Durum: Bekliyor / Geliştiriliyor / Testte / Tamamlandı / Donanım bekliyor

Bir madde yalnızca kod yazıldığı için tamamlandı sayılmaz; otomatik test ve gerekiyorsa gerçek araç/adaptör testi kanıtı bulunmalıdır.


## OBD-II ayrıştırma güvenilirliği — kod inceleme notu

- Mode 01 PID destek bitmap'i ayrıştırılırken ELM satır sonları/boşlukları ve CAN ECU başlıkları normalize edilir; yalnızca standart 32-PID bitmap başlangıç blokları kabul edilir.
- Mode 03 DTC yanıtları birden fazla 11-bit CAN ECU başlığı (7E8–7EF) içerdiğinde her ECU'nun yanıt yükü ayrı ayrıştırılır. ECU başlığı ve sonraki yanıt, önceki ECU'nun DTC baytlarına eklenmez; yinelenen kodlar tekilleştirilir.
- Eksik DTC bayt çiftleri ve boş 0000 yuvaları sonuç listesine eklenmez.
- Bu değişiklikler için çoklu ECU, CAN başlığı, satır sonu, yinelenen kod ve eksik veri regresyon testleri kaynak koda eklenmiştir. JUnit testleri ve Android derlemesi çalıştırılmadığından otomatik doğrulama sonucu beklemektedir; gerçek ELM327/araç testi ayrıca gereklidir.


## ELM327 komut taşıma katmanı — ek güvenlik

- Komut/yanıt işlemi artık ELM327'nin `>` yanıt sonlandırıcısını bekler. Süre dolarsa kısmi yanıt başarı gibi döndürülmez; zaman aşımı hatası oluşur ve aktif polling bağlantı hatası yoluna geçer.
- Komut yanıtı 4 KiB ile sınırlandırılır; önceki komuttan kalmış giriş baytları yeni komut gönderilmeden önce sınırlı biçimde temizlenir.
- Süre ölçümü duvar saati yerine monotonik saatle yapılır.
- Bağlantı kurulumu iptal edildiğinde iptal istisnası normal bağlantı hatası gibi yutulmaz; coroutine iptali korunur.
- Mode 04 DTC temizleme onayı artık rastgele bir `44` alt dizesine göre verilmez; pozitif servis yanıtının beklenen yalın veya 7E8–7EF ECU yanıt biçiminde bulunması gerekir.
- Bu davranışlar için Mode 04 olumlu/olumsuz yanıt testleri eklendi. Donanım zaman aşımı, Bluetooth kopması ve gerçek ECU yanıtları fiziksel adaptörle ayrıca doğrulanmalıdır.
