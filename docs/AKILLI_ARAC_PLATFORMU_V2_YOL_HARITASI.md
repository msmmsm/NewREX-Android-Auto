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
- [x] Yerel DTC geçmişi: ilk/son görülme, doğrulanmış tarama sayısı ve ECU temizleme onayı
- [ ] Sensör grafikleri, bakım planı ve servis kayıtları
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


### Bağlantı hatalarının ortak ele alınması

- ELM327 komut zaman aşımı veya giriş/çıkış akışı hatası DTC okuma/silme işlemlerinde de bağlantı hatası olarak ele alınır; bağlantı durumu ERROR yapılır, eski OBD ölçümleri temizlenir ve taşıma katmanı kapatılır.
- Kullanıcı bağlantıyı iptal ettiğinde coroutine iptali bağlantı hatası olarak raporlanmaz.
- Giriş tamponunda 4 KiB sınırını aşan bekleyen veri varsa yeni komut gönderilmez.


### OBD-II bağlantı kurtarma ve veri güncelliği

- Beklenmeyen ELM327 bağlantı/yanıt hatasından sonra aynı seçili adaptöre en fazla üç otomatik yeniden bağlanma denemesi yapılır. Beklemeler 1, 3 ve 7 saniyedir; kullanıcı bağlantıyı keserse coroutine iptal edilir ve yeniden bağlanma yapılmaz.
- Deneme sayacı aynı hata/yeniden bağlanma dizisi boyunca korunur; kısa süreli bağlantı kurulması sınırsız yeniden deneme döngüsü oluşturmaz.
- Telemetri anlık görüntüsüne `obdLastUpdatedMs` eklendi. Bu zaman damgası yalnızca başarıyla ayrıştırılmış OBD yanıtlarında ilerler; bağlantı kesildiğinde temizlenir. Arayüz bu alanı kullanarak OBD verisinin yaşını gösterebilir.
- Voltaj yanıtı sayısal biçim ve 0–30 V aralığı açısından doğrulanır; bozuk metin veya gerçek dışı değerler kabul edilmez.


### OBD bağlantı yarış koşulu ve hatalı ölçüm temizliği

- Her bağlantı oturumuna artan bir nesil kimliği atanır. Kullanıcı adaptör değiştirir veya bağlantıyı keserse eski coroutine geçersiz olur; geç gelen iptal/hata işleyicisi yeni oturumun soketini kapatamaz.
- PID yanıtı bozuk veya eksikse ilgili sensör alanı eski değerde bırakılmaz, `null` yapılır. Son başarılı OBD yanıt zaman damgası yalnızca geçerli ayrıştırmada ilerler.
- ELM voltajı yalnızca sayısal biçimde ve 0–30 V aralığında kabul edilir; negatif, aşırı veya ek metin içeren yanıtlar reddedilir.
- Yeniden bağlanma politikası için 3 deneme sınırı ve 1/3/7 saniyelik bekleme dizisi birim testleriyle tanımlanmıştır. Gerçek bağlantı kopma/yeniden bağlanma doğrulaması Bluetooth adaptörü ve araç üzerinde beklemektedir.


### Mode 02 Freeze Frame ve Türkçe DTC kataloğu

- `ObdManager.readFreezeFrame()` Mode 02 PID 00 destek bitmap'ini önce sorgular; yalnızca ECU'nun desteklediği seçili standart PID'leri ister.
- Okunabilen alanlar: motor devri, araç hızı, soğutma suyu sıcaklığı, emme havası sıcaklığı, motor yükü ve gaz kelebeği konumu.
- Freeze Frame verisi `ObdFreezeFrame` modeliyle döndürülür; canlı telemetri StateFlow alanlarına yazılmaz. Böylece geçmişteki arıza anı ölçümleri güncel değerlerle karıştırılmaz.
- `ObdDiagnostics.description(code)` sınırlı Türkçe DTC kataloğunda bulunan kodlara açıklama verir. Katalogda bulunmayan kodlar için açıklama uydurulmaz; ham kod korunur.
- Freeze Frame bitmap'i, CAN başlığı, kare numarası, eksik yanıt ve bilinen/bilinmeyen DTC açıklaması için birim testleri eklendi.
- Veri erişim katmanı ve telefon kokpitindeki temel DTC/Freeze Frame ekranı eklendi (arayüz ayrıntıları aşağıdaki bölümde). Gerçek ECU uyumluluk doğrulaması ve araç üzerindeki kabul testi henüz tamamlanmamıştır.


### Telefon kokpiti — OBD-II arıza tanılama arayüzü

- Telefon ekranındaki **Kokpit & Araç Telemetrisi** bölümüne arıza tanılama alanı eklendi.
- Kullanıcı, bağlı ELM327 üzerinden Mode 03 arıza kodlarını elle okuyabilir. Kodlar Türkçe katalogda varsa açıklamasıyla, yoksa açıklama uydurulmadan ham koduyla gösterilir.
- Mode 02 dondurulmuş kare okuma ayrı bir eylemdir. ECU'nun desteklediği alanlar ve kare numarası gösterilir; geçmiş ölçümler canlı sensör değerlerine aktarılmaz.
- Mode 04 temizleme eylemi ayrı düğmede ve açık onay penceresi arkasındadır. Ekran yalnızca ECU'dan olumlu Mode 04 yanıtı geldiyse temizleme isteğinin onaylandığını bildirir. Arızanın onarıldığı iddia edilmez; hazırlık monitörlerinin sıfırlanabileceği uyarılır.
- Boş/henüz taranmamış liste ile tarama sonrası görüntülenebilir kod alınamaması ayrı metinlerle gösterilir. ECU yanıtının tüm modülleri kapsadığı varsayılmaz.
- DTC geçmişi, otomatik canlı yenileme ve gerçek araç/ELM327 kabul testi bu kapsamda tamamlanmış değildir. Android derlemesi ve JUnit testleri çalıştırılmadı; fiziksel araç doğrulaması bekliyor.

### OBD-II DTC geçmişi ve tarama doğrulaması

- Mode 03 yanıtı doğrulanmadan geçmiş güncellenmez. NO DATA geçerli boş tarama kabul edilir; eksik/bozuk yanıt geçmiş kayıtlarını değiştirmez.
- Son 100 farklı DTC kaydı cihazın yerel SharedPreferences alanında saklanır. Her kayıtta ilk görülme, son görülme, kodun yer aldığı doğrulanmış tarama sayısı ve son taramada raporlanıp raporlanmadığı tutulur.
- Kokpitte son 10 kayıt gösterilir; toplam kayıt sayısı ayrıca belirtilir. Son taramada raporlanmamak arızanın giderildiği anlamına gelmez.
- ECU Mode 04 temizleme isteğini olumlu yanıtladığında geçmiş silinmez; temizleme onayı zamanı saklanır. Kod yeniden raporlanırsa son tarama durumu güncellenir.
- Geçmiş birimi için tekrar tarama, boş tarama, zaman damgası, temizleme onayı ve 100 kayıt sınırı testleri eklendi. Android derlemesi ve JUnit testleri bu ortamda çalıştırılmadı.

### Standart hibrit batarya PID 5B — sınırlı destek

- SAE J1979 Mode 01 PID 5B, destek bitmap'inde ilan edilmişse sorgulanır; yanıt `0–100%` aralığında ayrıştırılır.
- Parametrenin standart adı hibrit/EV batarya paketi **kalan ömrüdür**. Arayüzde bu ad kullanılır; SOC (şarj seviyesi) veya SOH (sağlık durumu) olarak yeniden adlandırılmaz.
- ECU PID 5B'yi desteklemiyorsa komut gönderilmez ve değer yok olarak gösterilir. Yanıt hatalıysa eski değer korunmaz.
- Toyota HV batarya ECU'sundaki SOC, blok gerilimleri, akım ve sıcaklık gibi daha ayrıntılı parametreler için üreticiye özgü teşhis erişimi gerekir. Toyota teknik dokümanlarında bu veriler HV Battery / HV ECU Data List üzerinden listelenir; ECU seçimi ve araç varyantı önemlidir. Bu nedenle doğrulanmamış Mode 21/22 PID'leri veya CAN başlıkları OmniDrive'e eklenmedi.
- Kaynaklar: SAE J1979 PID 5B tanımı ([SAE J1979 kopyası](https://www.autonerdz.com/yabbfiles/Attachments/1979v3.pdf)); Toyota hibrit batarya Data List örneği ([Toyota Hybrid Battery Control](https://toyotaspace.com/wp-content/uploads/2024/07/Hybrid-Battery-Control.pdf)); hibrit batarya freeze-frame/Data List alanları ([Toyota Prius Repair Manual](https://toyotamanuals.gitlab.io/PZ471-Z01S0-CA/htmlweb/rm/rm1076e/m_05_0662.pdf)).
- Gerçek Toyota C-HR model yılı, pazar ve hibrit varyantında PID desteği/ölçüm doğruluğu fiziksel araç ve uyumlu tarayıcıyla doğrulanmayı bekler. Derleme, JUnit ve araç testi yapılmadan özellik yayın onaylı sayılmaz.
### Referans araç: 2025 Toyota C-HR 1.8 Hybrid Passion (Türkiye)

- OmniDrive geliştirmelerinde kullanıcı tarafından belirtilen referans araç 2025 model 1.8 Hybrid Passion donanımıdır.
- 2025 Türkiye teknik donanım tablosuna göre Passion'da geri görüş kamerası, Toyota Safety Sense 3, ön radar/kamera destekli sistemler, kör nokta uyarısı (BSM) ve sürücü takip kamerası listelenir.
- Aynı tabloda 360° çevre görüş (PVM) Passion için değil, Passion X-Sport ve üst donanımlar için listelenmiştir. Passion'da PVM varmış gibi özellik gösterilmez.
- `ToyotaChr2025PassionProfile` donanım varlığını veri erişiminden ayrı tutar. Geri kamera OEM multimedya/park sisteminde; ön kamera ve radarlar güvenlik ECU'larında yer alır. ELM327 ile görüntü akışı veya radar hedef listesi alındığı iddia edilmez.
- Öncelik sırası: araç donanım/ECU kimliklerini salt okunur doğrulama; belgelenmiş Data List ve DTC alanları; yalnızca güvenli ve onaylı arayüzle sensör durum telemetrisi. Kalibrasyon, aktif test, ECU kodlama ve güvenlik sistemlerine komut gönderme kapsam dışıdır.
- Toyota kılavuzu sensörleri ön radar, ön kamera, donanıma bağlı ön/arka yan radar ve sürücü izleme kamerası olarak açıklar. Araç bilgisayarlarının belirli koşullarda kamera görüntüsü kaydedebilmesi, üçüncü taraf uygulamanın canlı video erişimi olduğu anlamına gelmez.
- Kaynaklar: [Toyota Türkiye 2025 C-HR teknik özellikler PDF](https://www.toyota.com.tr/content/dam/toyota/nmsc/turkey/cars/e-brosur/toyota-c-hr/25/TOYOTA_CHR_Teknik_Ozellikler_2025.pdf), [Toyota C-HR kullanım kılavuzu — sürüş destek sensörleri](https://ax20.tochr.net/sensors-used-by-toyota-safety-sense-114.html), [Toyota C-HR kullanım kılavuzu — araç veri kaydı](https://ax20.tochr.net/main-owner-s-manual-3.html), [C-HR CAN iletişim teşhis sistemi](https://www.tochr.net/908/diagnosis_system.html).
- Gerçek araçta üretim tarihi, ülke varyantı, opsiyonlar ve ECU yazılım sürümü doğrulanmadan tekil sensör/ECU veri erişimi kesin kabul edilmez.

### Standart OBD-II ECU kimlik keşfi (Mode 09)

OmniDrive, OBD-II bağlantısı kurulduğunda kullanıcı isteğiyle standart SAE J1979 Service 09 üzerinden emisyonla ilişkili ECU kimlik alanlarını okuyabilir:

- Önce PID 00 destek bitmap'i okunur. ECU'nun desteklediğini bildirmediği PID için sorgu gönderilmez.
- Destekleniyorsa PID 02 VIN, PID 04 kalibrasyon kimliği (CALID), PID 06 kalibrasyon doğrulama numarası (CVN) ve PID 0A ECU adı okunur.
- VIN tam biçimi uygulama ekranında gösterilmez; yalnızca maskelenmiş biçimde gösterilir. Kimlik verileri kalıcı depolamaya veya loglara yazılmaz.
- Yanıt yoksa veya ayrıştırılamıyorsa tahmini ECU kimliği üretilmez.
- Bu tarama yalnızca standart OBD-II emisyon ECU'sunu tanımlar. Toyota hibrit batarya, radar, kör nokta veya kamera ECU'larını keşfettiği anlamına gelmez.
- Çoklu ECU yanıtlarının her adaptör/protokolde eksiksiz toplanması henüz doğrulanmamıştır. İlk yanıtın kimliği, tüm araç ECU'larının envanteri olarak sunulmaz.

**Güvenlik sınırı:** Bu özellik salt okunur sorgularla sınırlıdır. Kalibrasyon yazma, kodlama, aktif test, güvenlik ECU'su komutu veya CAN enjeksiyonu yapılmaz.

Teknik dayanak:
- SAE J1979 Mode 09 araç bilgisi ve desteklenen kimlik alanları: https://py-obdii.readthedocs.io/en/latest/api/modes/mode_09.html
- VIN, CALID, CVN ve ECU adı alanlarının Mode 09 kapsamı: https://ww2.arb.ca.gov/sites/default/files/classic/msprog/hdim/meetings/20191108_workgroup/hdim_obd_specs_tables.pdf


### Toyota gelişmiş ECU veri kataloğu — salt okunur araştırma aşaması

Kokpitteki **Toyota gelişmiş ECU veri kataloğu**, aşağıdaki sistemleri araştırma hedefi olarak listeler; canlı veri veya çalışan teşhis bağlantısı olduğunu iddia etmez:

- **HV Battery ECU:** SOC, batarya sıcaklık sensörleri, blok gerilimleri ve akım parametre sınıfları.
- **Hybrid Control ECU:** hibrit sistem çalışma durumu, motor/jeneratör ve READY/güç aktarım durumu.
- **Pre-Collision / ön radar ECU:** iletişim, arıza ve sistem kullanılabilirlik durumu. Ham radar hedefleri kapsam dışıdır.
- **BSM ECU:** iletişim, arıza ve sistem kullanılabilirlik durumu. Ham hedef/mesafe/hız akışı doğrulanmış değildir.

Toyota servis dokümanları Data List'in ilgili ECU seçilerek üretici teşhis cihazından okunabildiğini gösterir. Eski Toyota hibrit eğitim/onarım dokümanlarındaki parametre adları bu 2025 Türkiye C-HR varyantına doğrudan taşınamaz; yalnızca parametre sınıflarını araştırmak için referanstır. 2025 modelin kesin ECU adı, parametre etiketi, ölçüm birimi, ölçeği ve destek durumu araç üzerinde doğrulanmalıdır.

**Arayüz gereksinimi:** Toyota'nın kendi teknik bilgi sayfaları tam teşhis işlevleri için Techstream/Techstream Lite'ı işaret eder. Toyota'nın onaylı J2534 yeniden programlama cihazları sayfası, bu cihazların tek başına teşhis tarayıcısı işlevi sunmadığını açıkça belirtir. Bu nedenle “J2534 uyumlu” ibaresi tek başına yeterli değildir. OmniDrive'e henüz J2534, DoIP veya Toyota'ya özgü CAN sorgusu eklenmedi.

**Güvenlik kapısı:** Şimdilik yalnızca katalog ve salt okunur teşhis araştırması vardır. Toyota protokolü, araç varyantı ve her parametrenin ölçeği doğrulanmadan Mode 21/22 PID, CAN ID, gateway yönlendirmesi veya komut gönderilmez. Aktif test, kalibrasyon, kodlama ve ECU yazma kapsam dışıdır.

Kaynaklar:
- [Toyota TIS — J2534 yeniden programlama cihazları ve teşhis sınırlaması](https://techinfo.toyota.com/techInfoPortal/appmanager/t3/ti?_nfpb=true&_pageLabel=ti_j2534_device)
- [Toyota TIS — Techstream ve araç teşhis arayüzleri](https://techinfo.toyota.com/techInfoPortal/appmanager/t3/ti?_nfpb=true&_pageLabel=ti_vehicle_reprog)
- [Toyota C-HR — Pre-Collision ECU Data List](https://www.tochr.net/2046/data_list_active_test.html)
- [Toyota C-HR — CAN iletişim teşhis sistemi](https://www.tochr.net/908/diagnosis_system.html)
- [Toyota hibrit teşhis Data List örneği](https://toyotaspace.com/wp-content/uploads/2024/07/Hybrid-Battery-Control.pdf)

