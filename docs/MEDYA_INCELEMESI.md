# OmniDrive — Medya özellikleri statik incelemesi

Tarih: 2026-11-15  
Depo: `msmmsm/NewREX-Android-Auto`  
Dal: `master`  
İncelenen kaynak: `master` (IPTV ayrıştırıcı düzeltmeleri 2026-11-15 tarihinde eklendi)

> Bu belge kaynak kodu incelemesidir. APK derlemesi, otomatik test, gerçek IPTV/radyo/RSS adresleri, Android telefon veya Android Auto araç ekranı üzerinde oynatma testi yapılmamıştır. Kodda bir ekranın bulunması özelliğin çalıştığını kanıtlamaz.

## 1. Medya oynatıcı — `PlayerHub.kt`

### Mevcut uygulama
- TV, radyo ve podcast aynı Media3/ExoPlayer örneğini kullanıyor.
- `OmniDriveMediaService`, Media3 `MediaLibraryService` sınıfından türetildi; ExoPlayer ve tek `MediaLibrarySession` servis oluşturulurken başlatılıyor.
- Medya kütüphanesi kökünde TV favorileri, radyo favorileri, podcast favorileri, son dinlenenler, geçerli çalma kuyruğu ve o anki öğe Türkçe kategoriler olarak sunuluyor. Kategori çocukları sayfalanıyor; kayıtlı medya başlığı/grubu üzerinden arama yapılabiliyor.
- Dış medya tarayıcılarına yalnızca HTTP(S) sunucusu olan ve URL içinde kullanıcı/parola veya bilinen erişim anahtarı parametresi taşımayan medya adresleri verilir. Kimlik bilgisi içeren öğeler telefon uygulamasında oynatılabilir; harici katalogdan gizlenir.
- Android Auto/uyumlu medya tarayıcıları, favori ve son dinlenen öğeleri, geçerli kuyruğu ve o anki öğeyi başlık/tür/kapak metadata'sıyla tarayabilir. Tüm IPTV listeleri otomatik olarak dışarı açılmaz; yalnızca kullanıcının kaydettiği favoriler, son dinlenenler ve etkin kuyruk sunulur.
- `CarActivity`, `SessionToken` ve asenkron `MediaController` bağlantısıyla servise bağlanıyor. Activity yok edilirken yalnızca controller bağlantısı bırakılıyor; oynatma sürerken servis/oynatıcı serbest bırakılmıyor.
- Mini oynatıcı, sayfa kontrolleri ve direksiyon ileri/geri komutları `MediaController` üzerinden `MediaLibrarySession`'a gönderilir. Tam ekran TV sayfası video yüzeyini servis sahibi ExoPlayer'a bağlar; bu bağlantı görüntü içindir, kullanıcı komutları yine MediaController'a gider.
- Servis `ConnectivityManager` varsayılan ağ callback'ini izler. Gerçek ağ bağlantı hatasında alternatif yayın aramayı durdurur; bağlantı geri geldiğinde kullanıcı oynatmayı duraklatmadıysa canlı yayını veya podcast'i yeniden açar. Yalnızca genel internet erişimi yok diye yerel ağ IPTV akışları çevrimdışı sayılmaz.
- Favori, son dinlenen, kuyruk, mevcut öğe ve podcast indirme durumu değiştiğinde bağlı medya tarayıcılarına ilgili kütüphane kategorilerinin değiştiği bildirilir.
- Manifestte `FOREGROUND_SERVICE` ve `FOREGROUND_SERVICE_MEDIA_PLAYBACK` izinleri ile `mediaPlayback` türünde, Media3 oturum eylemi yayınlayan servis tanımlandı. Media3, oynatma sürerken foreground durumunu ve medya bildirimini yönetiyor.
- `PlayerHub.stop()` artık MediaLibrarySession'ı bırakmıyor; oynatma listesini temizliyor. Oturum ve ExoPlayer yalnızca servis `onDestroy()` aşamasında serbest bırakılıyor.
- Kuyruk, seçili indeks ve podcast konumu sürüm 1 şemasıyla SharedPreferences içinde kalıcılaştırılıyor. Büyük listelerde seçili öğeyi koruyan en fazla 50 öğelik pencere saklanıyor.
- MediaSession `onPlaybackResumption` callback'i kayıtlı kuyruğu ve podcast konumunu geri veriyor; MediaButtonReceiver sistem medya düğmesi/yeniden başlatma talebini servise iletiyor.
- Geri yükleme yalnızca HTTP(S), sunucu adı bulunan ve açık kullanıcı/parola veya bilinen token parametresi taşımayan URL'lerde yapılır. Kimlik doğrulama başlıkları/çerezleri saklanmaz. Özel kimlik doğrulamalı yayınlar otomatik geri yüklenmeyebilir.
- Kullanıcı `Durdur` komutunu verdiğinde kuyruk kaydı silinir; beklenmedik servis/süreç kapanışında son kuyruk korunur.
- DownloadManager tamamlanan dosyaları çevrimdışı listeye almadan önce 1 bayt–512 MiB boyut aralığı ve izin verilen ses MIME türleri, Ogg, MP4 veya `application/octet-stream` açısından doğrular; doğrulamayı geçmeyen indirme kaydı ve dosyası kaldırılır.
- Podcast konumu 10 saniyelik aralıklarla kaydediliyor; durdurma ve başka bölüme geçişte de mevcut konum kaydediliyor.
- Canlı yayınlarda artan bekleme aralığıyla yeniden bağlanma ve radyo için alternatif akış arama mekanizması var. Yeniden bağlanma denemeleri ilk açılışta 2, daha önce çalmış akışta 10 denemeyle sınırlıdır; bekleme 1/2/4/8/15 saniye düzenindedir. Kullanıcı veya Android Auto duraklattığında bekleyen yeniden bağlanma iptal edilir ve yeniden yükleme duraklatma durumunu korur.
- HTTPS→HTTP otomatik düşürme ve çapraz protokol yönlendirmesi kapalı.

### Açık riskler / doğrulama işleri
- [x] Media3 `MediaLibraryService` sınıfı, medya foreground service izinleri ve `mediaPlayback` manifest tanımı eklendi.
- [x] `CarActivity` servis bağlantısını `MediaController` ile kuruyor; Activity artık `PlayerHub.release()` çağırmıyor.
- [x] Medya oturumu `MediaLibraryService` / `MediaLibrarySession` olarak genişletildi; Türkçe favori/son dinlenen/çalma kuyruğu/şimdi çalıyor kütüphane ağacı, arama başlatma ve sayfalı arama sonuçları callback'leri eklendi.
- [x] Harici katalog için URL güvenlik filtresi ve token/kullanıcı bilgisi sızıntısı regresyon testi eklendi.
- [x] Araç arayüzü denetimleri MediaController üzerinden MediaLibrarySession'a yönlendirildi; ileri/geri komutları döngüsel medya kuyruğuna bağlandı.
- [x] Ağ durumu izleme, çevrimdışı bekleme ve bağlantı geri geldiğinde kontrollü yeniden bağlanma eklendi.
- [x] Podcast çevrimdışı indirme ve indirilen dosyayı aynı bölüm URL'siyle otomatik oynatma eklendi.
- [ ] Arka plana alma, ekran kilidi ve araç bağlantısının kesilmesi/yeniden kurulması gerçek cihazlarda doğrulanmalı.
- [x] Kuyruğu, seçili öğeyi ve podcast konumunu kalıcı sakla; Media3 `onPlaybackResumption` ve `MediaButtonReceiver` ile servis yeniden oluşturulduğunda kurtarma akışını uygula.
- [ ] Force-stop, cihaz yeniden başlatma, Android Auto bağlantı kesilmesi ve servis zaman aşımı senaryolarını fiziksel cihazlarda doğrula; sistem üreticisi kısıtlamaları nedeniyle otomatik başlatma garantisi verilmez.
- [ ] Kimlik doğrulamalı IPTV/radyo adresleri geri yükleme dışında kalır; güvenli kimlik bilgisi deposu tasarlanmadan bu kısıt kaldırılmamalı.
- [ ] Android 14/15 ve farklı üretici pil yönetimi davranışları fiziksel cihazlarda test edilmeli; servis uygulama davranışı bu testler tamamlanmadan doğrulanmış sayılmaz.
- [ ] Ağ kopması, DNS hatası, HTTP 404/5xx, hatalı MIME türü, bozuk HLS manifesti ve canlı pencerenin gerisinde kalma senaryoları test edilmeli.
- [ ] Podcast konumu için 10 saniyelik kayıt aralığında beklenmedik süreç sonlandırmasındaki olası ilerleme kaybı ölçülmeli.
- [ ] `stop()`, `release()`, parça değiştirme ve sayfa yok edilmesi sırasında durum akışlarının (playing/buffering/error) tutarlılığı test edilmeli.
- [x] Medya oturumu kimliği `omnidrive` olarak ayarlandı; metadata başlıkları OmniDrive TV/Radyo/Podcast olarak kalıyor.

## 2. IPTV M3U / HLS M3U8 — `data/Net.kt`

### Mevcut uygulama
- Kullanıcı URL'si veya `content://` belge URI'si üzerinden M3U metni okunabiliyor.
- Sınırlı akış okuyucusu, sıfır uzunlukta okuma döndüren `InputStream` uygulamalarında tek baytlık geri dönüş kullanır; böylece döngüde takılmaz.
- HTTP listeleri 6 saatlik dosya önbelleğine yazılıyor; indirme hatasında önbellek varsa kullanılabiliyor. Ağ ve `content://` liste okumaları 8 MiB sınırıyla yapılır; MIME türü M3U ve metin biçimleriyle sınırlandırılır; aşırı büyük eski önbellek silinir.
- `#EXTINF` içinden kanal adı, `tvg-logo` ve `group-title` okunuyor.
- Aynı URL'ye sahip kanallar tekilleştiriliyor.

### Statik bulgular
1. **HLS manifesti ile kanal listesi ayrımı düzeltildi.** `#EXT-X-...` satırı içeren ana/medya manifestleri IPTV kanal listesi olarak ayrıştırılmaz; segment URI'leri kanal olarak eklenmez.
2. **Göreli URI çözümlemesi eklendi.** HTTP(S) kaynak listesindeki göreli kanal adresleri liste URL'sine göre çözülüyor.
3. **EXTINF başlık ayrıştırması düzeltildi.** Tırnak dışındaki ilk virgül ayırıcı olarak kullanılıyor; kanal adının içindeki virgüller korunuyor.
4. **Önbellek anahtarı güçlendirildi.** URL tabanlı `String.hashCode()` yerine SHA-256 özeti kullanılıyor. Eski hash adlı dosyalar kullanılmayacak; Android önbelleği temizlendiğinde silinebilir.
5. **M3U kaynak sınırları ve URI doğrulaması eklendi.** Liste 8 MiB, 100.000 satır ve 10.000 kanal ile sınırlandırılır; başlık/grup uzunlukları kesilir. Kanal akışlarında HTTP(S) ve RTSP kabul edilir; `file:`, `content:`, `javascript:` gibi şemalar, hatalı host ve URL içine gömülü kullanıcı/parola reddedilir. Logo adresleri yalnızca güvenli HTTP(S) kabul eder. Yerel ağdaki HTTP IPTV adresleri bilerek engellenmez; RTSP korunur. HTTP(S) liste URL'sinden göreli adres çözümlemesi yapılır; güvenilir HTTP(S) taban URL yoksa göreli kayıt atlanır.
6. **Liste hata görünürlüğü** hâlâ ayrıca UI/cihaz testi gerektiriyor.

### Düzeltme ve test planı
- [x] `#EXT-X-` içeren HLS ana/medya manifestlerini kanal listesi ayrıştırıcısından ayır.
- [x] HTTP(S) liste adresi biliniyorsa göreli URI'leri ana liste URL'sine göre çöz.
- [x] EXTINF satırında tırnak dışındaki ilk ayırıcı virgülü bul; başlıktaki virgülleri koru.
- [x] UTF-8 BOM, CRLF, HLS medya/ana manifesti, başlık virgülü ve göreli URI için JUnit regresyon testleri ekle.
- [x] Önbellek dosya adını URL'nin SHA-256 özetiyle üret.
- [x] Hatalı/desteklenmeyen URI şemalarını ve kimlik bilgili adresleri reddet; özel ağ HTTP ve RTSP akışlarını koru; liste kaynak boyutlarını sınırla.
- [x] IPTV HTTP ve `content://` M3U kaynaklarını 8 MiB akış sınırıyla oku; uygun olmayan MIME türlerini ve aşırı büyük eski önbelleği reddet.

> IPTV ve podcast için JUnit regresyon testleri kaynak koduna eklendi ancak bu ortamda Android Gradle derlemesi/testi çalıştırılamadığı için sonuçları henüz doğrulanmadı.

## 3. Podcast RSS — `data/Services.kt`, `pages/PodcastPage.kt`

### Mevcut uygulama
- Popüler program ve arama verisi Apple Podcasts katalog uç noktalarından alınıyor.
- Bölümler programın RSS akışından `enclosure` URL'si ile okunuyor.
- Abonelikler ve son dinlenenler yerel SharedPreferences içinde saklanıyor.
- Podcast ilerlemesi URL anahtarıyla yerel olarak kaydediliyor ve bölüm yeniden açıldığında kaldığı yerden başlatılıyor.

### Statik bulgular / ürün kapsamı
- **Podcast çevrimdışı indirme eklendi.** Android DownloadManager, bölümleri Android 10+ sürümlerde ortak İndirilenler klasörüne; Android 8/9'da uygulamaya özel Podcasts klasörüne indirir. Android sistem bildirimi indirme durumunu gösterir. İndirme kaydı URL'nin kendisini değil SHA-256 anahtarını ve DownloadManager kimliğini saklar. İndirilen dosya oynatma sırasında otomatik seçilir; indirme düğmesi araç hareket halindeyken engellenir.
- İndirilen bölümler Podcast ekranındaki ayrı **İndirilenler** sekmesinden, RSS akışına erişim olmasa da listelenir; uyumlu medya kütüphanesinde de ayrı kategori olarak sunulur. İndirme tamamlanma/iptal/progress değişimleri medya kütüphanesine bildirilir.
- Android 10 ve üzeri sürümlerde dosyalar ortak **İndirilenler** klasörüne `OmniDrive_Podcasts_` önekiyle yazılır; böylece DownloadManager'ın yalnızca uygulamaya özel alandaki görünmeyen dosyaları temizlik sırasında kaldırması riski azaltılır. Android 8/9'da uygulamaya özel Podcast klasörü kullanılır. Android 10+ ortak klasördeki dosyalar uygulama kaldırıldığında kullanıcı tarafından ayrıca silinene kadar kalabilir.
- İndirme mobil ağda başlatılabilir, dolaşımda indirme kapalıdır.
- RSS ayrıştırıcısı `item` içindeki `enclosure` URL'sini okur. Feed indirmesinde 4 MiB yanıt sınırı, XML MIME türü kontrolü, HTTP(S)/host/kullanıcı bilgisi doğrulaması ve en fazla 5 güvenli yönlendirme uygulanır; HTTPS→HTTP düşüşü ve güvenli olmayan yönlendirme hedefleri reddedilir.
- Tarih ayrıştırıcı saniyeli/saniyesiz RFC 822 ve yaygın ISO-8601 biçimlerini dener; tanınmayan tarih boş bırakılır.
- **RSS enclosure adresleri düzeltildi.** Göreli bölüm dosyası adresleri feed URL'sine göre çözümleniyor; mutlak adresler korunuyor. Boş veya çözümlenemeyen adresler bölüm oynatılabilir adresi olarak kullanılmıyor.
- **RSS enclosure URL güvenliği güçlendirildi.** Bölüm adresleri yalnızca HTTP(S), geçerli host ve kimlik bilgisi içermeyen URL'lerden kabul edilir. `file:`, `ftp:`, `content:` gibi şemalar, kullanıcı/parola gömülü adresler, boşluk/kontrol karakterleri ve 4 KiB üzerindeki URL'ler reddedilir. Göreli adresler yalnızca güvenli HTTP(S) feed adresine göre çözümlenir; özel ağ adresleri bu aşamada ayrıca engellenmez.
- **RSS tarih ayrıştırması güçlendirildi.** Saniyeli/saniyesiz RFC 822 tarihleri ve yaygın saat dilimi yazımları deneniyor; tanınmayan tarih boş bırakılıyor.
- Feed yükleme ve bölüm listesi Android cihazda farklı RSS sağlayıcılarıyla doğrulanmadı.

### Düzeltme ve test planı
- [x] RSS tarih biçimlerine toleranslı ayrıştırma ve bozuk tarih testleri eklendi.
- [x] RSS MIME türü kontrolü, güvenli yönlendirme sınırı ve HTTPS→HTTP düşüşünü engelleme eklendi.
- [x] Tamamlanan indirmede izin verilen medya MIME türü ve 512 MiB bölüm boyutu üst sınırı doğrulanır; geçersiz dosya kaldırılır.
- [ ] İndirilen enclosure dosyasının MIME/uzunluk doğrulaması ve kullanıcıya açık hata mesajları.
- [x] Göreli enclosure URL'leri feed URL'sine göre çözümleniyor; mutlak URL korunuyor.
- [x] RSS enclosure şeması, kimlik bilgisi ve uzunluk doğrulaması için regresyon testleri eklendi.
- [x] RSS feed URL doğrulaması ve 4 MiB indirme sınırı eklendi; sınır davranışı için birim testleri eklendi.
- [x] RSS XML namespace yerel adları desteklenecek şekilde ayrıştırma ve harici belge bildirimi kapatma eklendi.
- [ ] Uzun RSS, eksik alan ve bozuk XML testleri; XML namespace ayrıştırma kodu eklendi ancak farklı sağlayıcılarla saha doğrulaması yapılmadı.
- [x] Podcast bölümleri için Android DownloadManager tabanlı indirme, durum/progress, iptal/silme, indirilmiş dosyayı otomatik oynatma ve çevrimdışı indirme listesi eklendi.
- [ ] Büyük indirmelerde depolama kotası/otomatik temizlik, dosya bütünlüğü ve farklı RSS sunucularıyla çevrimdışı cihaz testi yapılmalı.

## 4. İnternet radyosu — `RadioApi`, `RadioPage.kt`

- Radio Browser katalogu iki API sunucusundan sırayla alınır; boş/bozuk yanıt mevcut başarılı katalogun üzerine yazılmaz.
- Popüler istasyon kataloğu ve arama sonuçları yerel SharedPreferences içinde saklanır. Önbellek 6 saat taze kabul edilir; süre dolduğunda ağdan yenilenir, iki sunucu da başarısız olursa son başarılı katalog kullanılır.
- Arama önbelleği anahtarında arama metni açıkça tutulmaz; SHA-256 anahtarı kullanılır ve en fazla 20 arama kaydı saklanır.
- Akış, logo ve ana sayfa adresleri yalnızca HTTP(S), geçerli host ve kullanıcı bilgisi içermeyen URL'lerden kabul edilir. Çözümlenmiş yayın adresi güvenli değilse güvenli özgün URL denenir; ikisi de geçersizse istasyon katalogdan çıkarılır.
- İstasyon adı boşsa okunabilir varsayılan ad atanır; yinelenen akış adresleri tekilleştirilir.
- Radyo alternatif istasyon araması da aynı kalıcı önbelleği kullanır.
- Radio Browser yanıtından codec, bitrate, ülke kodu, oy sayısı, tıklanma sayısı ve dizinin son kontrol sonucu alınır. Codec/bitrate/ülke ve son dizin kontrol sonucu istasyon kutucuğunun alt bilgisinde gösterilir. Bu değerler sağlayıcının bildirdiği katalog metaverisidir; canlı akışın şu anda erişilebilir olduğuna dair gerçek zamanlı garanti değildir.
- Teknik metaveri `MediaEntry` JSON kaydında da saklanır; çevrimdışı katalogda gösterim için korunur.
- API ülke kodları iki harfli biçime normalize edilir, arama metni 100 karakterle, sonuç sayısı 1–300 aralığıyla sınırlandırılır. Boş arama ağa gönderilmez.
- Önbellek 6 saat taze kabul edilir; ağ hatasında en fazla 30 günlük son başarılı kayıt kullanılabilir. Daha eski kayıtlar atılır; ileri tarihli bozuk zaman damgaları da reddedilir. Arama anahtarları SHA-256 ile saklanır; arama önbelleği en fazla 20 kayıtla sınırlandırılır.
- Oynatma hatalarında HTTP 401/403, 404/410, 429 ve 5xx yanıtları ile bozuk/desteklenmeyen medya biçimleri ayrı Türkçe hata mesajlarıyla raporlanır. Aynı istasyon için alternatif adres aranması sürdürülür.
- Radyo ekranındaki hız/teşhis eylemi, yalnızca kullanıcının o anda çaldığı istasyonu elle sınar; katalogdaki tüm istasyonlara arka planda toplu istek göndermez.
- **Radyo hata geçmişi ve adres bekletme eklendi.** Oynatma hatası ve elle yapılan sağlık kontrolü, yayın URL'sinin kendisini değil SHA-256 özetini saklar. Hatalı adres ilk hatada 5 dakika, ikinci ardışık hatada 15 dakika, sonraki hatalarda en fazla 1 saat alternatif seçiminden çıkarılır. Başarılı oynatma/sağlık kontrolü ilgili adresin hata kaydını sıfırlar. Geçmiş 7 gün ve en fazla 40 adres kaydıyla sınırlandırılır.
- Alternatif radyo seçimi artık yalnızca ad benzerliğine göre ilk sonucu almaz: aynı istasyon adı, ülke kodu, dizinin son sağlık bildirimi, codec/bitrate ve oy bilgisiyle sıralama yapar. Kullanıcının seçtiği istasyonla adı yeterince uyuşmayan bir istasyona otomatik geçiş yapılmaz.
- IPv4 sağlık denetimi engel listesi daraltıldı: 192.0.0.0/24, 192.0.2.0/24 ve 192.88.99.0/24 özel amaçlı blokları reddedilir; artık 192.0.0.0/16'nın tamamı gereksiz yere engellenmez. Bu blok ayrımı için regresyon testleri eklendi.
- Sağlık testi HTTP Range isteğiyle en fazla ilk veri baytını okumayı dener; bağlantı/okuma zaman aşımı sınırlıdır ve tam ses akışı indirilmez. İlk yanıt süresi 2 saniyeyi aşarsa yavaş olarak sınıflandırılır.
- Test otomatik HTTP yönlendirmelerini izlemez; 3xx ayrı raporlanır. 401/403, 404/410, 429, 5xx, HTML sayfası, boş gövde, TLS ve DNS sorunları ayrı sonuçlara ayrılır.
- Testten önce host DNS üzerinden çözülür; localhost, .local, loopback, link-local, özel ağ, paylaşımlı adres blokları, IPv6 ULA ve multicast adresleri sınanmaz. Kontrol edilmiş DNS yanıtı istek sırasında sabitlenir; ikinci DNS çözümlemesiyle adres değiştirme (DNS rebinding) engellenir. Böylece katalogdan gelen bir adresin yerel ağa istek göndermesi engellenir. Bu nedenle özel ağdaki yerel radyo akışları bu teşhis aracıyla sınanamaz.
- Bu test yalnızca ilk HTTP yanıtı ve ilk baytın alınmasını doğrular; codec çözümleme, uzun süreli kararlılık, ses duyulabilirliği veya gerçek oynatıcı uyumluluğu garantisi vermez.
- [x] HTTP durum sınıflandırması, HTML yanıtı ve gecikme eşiği için birim testleri eklendi.
- [x] Radyo hata geçmişi, bekletme süreleri ve alternatif adres sıralaması için birim testleri eklendi.
- Önbellekten okunan yayın, logo ve ana sayfa adresleri tekrar doğrulanır; bozuk veya değiştirilmiş kayıtlar oynatma listesine alınmaz.
- [x] Radyo katalog önbelleği, güvenli adres ayrıştırma, katalog tekilleştirme, teknik metaveri, JSON kalıcılığı ve ülke kodu doğrulama testleri eklendi.
- [ ] Radio Browser sunucu kesintisi, çevrimdışı açılış ve gerçek radyo akışlarının Android cihazda kabul testi yapılmalı.

## 5. YouTube ve genel WebView — `pages/WebPage.kt`, `pages/YouTubeAdBlocker.kt`

### Mevcut uygulama
- YouTube, mobil web sitesi üzerinden Android WebView içinde açılıyor; resmî YouTube Data API veya yerel YouTube oynatıcı entegrasyonu görülmüyor.
- YouTube sayfası standart WebView gezinmesiyle açılır; uygulama reklam isteklerine müdahale etmez ve reklam atlama otomasyonu çalıştırmaz.
- WebView JavaScript ve DOM storage kullanıyor; çerezler TV+, Netflix ve YouTube oturumlarının korunması için kabul ediliyor.
- Uygulama açma bağlantıları engelleniyor; yalnızca HTTP/HTTPS gezinmeye izin veriliyor. TLS sertifika hataları açıkça iptal ediliyor; karma içerik (HTTPS sayfada HTTP alt kaynağı) engelleniyor.
- Sürüşte görüntünün gizlenmesi için park koruması var.

### Riskler
- Önceki sürümdeki özel reklam isteği filtresi, reklam katmanı gizleme ve otomatik atlama kodu kaldırıldı. Bu müdahaleler oynatıcı akışını bozabilir ve desteklenen YouTube API sözleşmesinin parçası değildir. YouTube artık standart web oynatımıyla çalışır; reklamlar YouTube tarafından sunulur.
- WebView oturum çerezleri cihazda kalıcıdır. Kullanıcıya oturum/çerez temizleme kontrolü ve gizlilik açıklaması sunulduğu doğrulanmalı.
- JavaScript açık olduğundan gezinme alanı, dosya erişimi, özel şema ve izin istekleri güvenlik açısından regresyon testine tabi tutulmalı.
- WebView içindeki video oynatımı, Android Auto sürüş güvenliği ve park kilidi fiziksel araç ekranında test edilmelidir.

### Düzeltme ve test planı
- [x] YouTube işlevinin WebView ile YouTube web sitesi olduğu netleştirildi; resmî API/yerel oynatma iddiası yok.
- [x] Reklam isteği filtresini, reklam katmanı gizleme ve otomatik atlama betiğini kaldır; standart YouTube WebView oynatımına dön.
- [ ] Çerezleri ve web verisini kullanıcı kontrollü temizleme seçeneğini değerlendir.
- [x] HTTP dışı gezinme şemaları engelleniyor; dosya ve içerik URI erişimi kapalı; karma içerik engelli; TLS hataları iptal ediliyor; DRM izni yalnızca TV+ / Netflix alan adlarına sınırlı.
- [ ] Ana çerçeve yönlendirme ve üçüncü taraf çerezleri için ek testler; oturum/çerez temizleme kontrolü değerlendirmesi.
- [ ] Sürüş/park modu geçişleri ve tam ekran video için gerçek Android Auto testi.

## 6. Sonuç ve öncelik

| Öncelik | İş | Durum |
|---|---|---|
| P0 | HLS M3U8 manifestini kanal listesi gibi ayrıştırma riskini gider | Kod düzeltildi; test sonucu bekliyor |
| P1 | M3U göreli URL ve EXTINF başlık ayrıştırmasını düzelt/test et | Kod ve regresyon testleri eklendi; test sonucu bekliyor |
| P1 | Podcast RSS URL/tarih/enclosure dayanıklılığını artır | URL/tarih/feed boyut sınırı kodu ve regresyon testleri eklendi; MIME ve XML saha testleri açık |
| P1 | WebView/YouTube oturum, reklam müdahalesi ve temel TLS/karma içerik güvenliği | Reklam müdahalesi kaldırıldı; TLS ve karma içerik korumaları eklendi; cihaz testi açık |
| P1 | Player/MediaSession arka plan dayanıklılığı | MediaLibraryService + Türkçe medya kütüphanesi + kalıcı kuyruk + playback resumption eklendi; gerçek cihaz/Android Auto testi açık |
| P2 | Podcast çevrimdışı indirme ve dosya yönetimi | DownloadManager, indirme durumu, iptal/silme ve çevrimdışı liste eklendi; depolama kotası ve cihaz testi açık |

## Doğrulama sınırı

Bu belge statik kod incelemesine dayanır. Bu çalışma kapsamında APK derlenmedi, JUnit testleri çalıştırılmadı, GitHub Actions kullanılmadı, sunucuya erişilmedi ve fiziksel cihaz/araç testi yapılmadı. Playback resumption kodunun Android sürümleri ve üretici güç yönetimi davranışlarıyla uyumu cihaz testleri tamamlanana kadar doğrulanmış kabul edilmez. Hiçbir medya özelliği saha testinden geçmiş olarak işaretlenmemiştir.
