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
- Manifestte `FOREGROUND_SERVICE` ve `FOREGROUND_SERVICE_MEDIA_PLAYBACK` izinleri ile `mediaPlayback` türünde, Media3 oturum eylemi yayınlayan servis tanımlandı. Media3, oynatma sürerken foreground durumunu ve medya bildirimini yönetiyor.
- `PlayerHub.stop()` artık MediaSession'ı bırakmıyor; oynatma listesini temizliyor. Oturum ve ExoPlayer yalnızca servis `onDestroy()` aşamasında serbest bırakılıyor.
- Kuyruk, seçili indeks ve podcast konumu sürüm 1 şemasıyla SharedPreferences içinde kalıcılaştırılıyor. Büyük listelerde seçili öğeyi koruyan en fazla 50 öğelik pencere saklanıyor.
- MediaSession `onPlaybackResumption` callback'i kayıtlı kuyruğu ve podcast konumunu geri veriyor; MediaButtonReceiver sistem medya düğmesi/yeniden başlatma talebini servise iletiyor.
- Geri yükleme yalnızca HTTP(S), sunucu adı bulunan ve açık kullanıcı/parola veya bilinen token parametresi taşımayan URL'lerde yapılır. Kimlik doğrulama başlıkları/çerezleri saklanmaz. Özel kimlik doğrulamalı yayınlar otomatik geri yüklenmeyebilir.
- Kullanıcı `Durdur` komutunu verdiğinde kuyruk kaydı silinir; beklenmedik servis/süreç kapanışında son kuyruk korunur.
- Podcast konumu 10 saniyelik aralıklarla kaydediliyor; durdurma ve başka bölüme geçişte de mevcut konum kaydediliyor.
- Canlı yayınlarda artan bekleme aralığıyla yeniden bağlanma ve radyo için alternatif akış arama mekanizması var.
- HTTPS→HTTP otomatik düşürme ve çapraz protokol yönlendirmesi kapalı.

### Açık riskler / doğrulama işleri
- [x] Media3 `MediaSessionService` sınıfı, medya foreground service izinleri ve `mediaPlayback` manifest tanımı eklendi.
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
- HTTP listeleri 6 saatlik dosya önbelleğine yazılıyor; indirme hatasında önbellek varsa kullanılabiliyor.
- `#EXTINF` içinden kanal adı, `tvg-logo` ve `group-title` okunuyor.
- Aynı URL'ye sahip kanallar tekilleştiriliyor.

### Statik bulgular
1. **HLS manifesti ile kanal listesi ayrımı yok.** `#EXT-X-...` satırları yorum satırı olarak atlanıyor; ardından gelen segment URI'leri normal kanal girdisi gibi değerlendirilebilir. HLS medya manifesti bir kanal listesi değildir.
2. **Göreli URI çözümlemesi eklendi.** HTTP(S) kaynak listesindeki göreli kanal adresleri liste URL'sine göre çözülüyor.
3. **EXTINF başlık ayrıştırması düzeltildi.** Tırnak dışındaki ilk virgül ayırıcı olarak kullanılıyor; kanal adının içindeki virgüller korunuyor.
4. **Önbellek anahtarı güçlendirildi.** URL tabanlı `String.hashCode()` yerine SHA-256 özeti kullanılıyor. Eski hash adlı dosyalar kullanılmayacak; Android önbelleği temizlendiğinde silinebilir.
5. **Liste hata görünürlüğü** hâlâ ayrıca UI/cihaz testi gerektiriyor.

### Düzeltme ve test planı
- [x] `#EXT-X-` içeren HLS ana/medya manifestlerini kanal listesi ayrıştırıcısından ayır.
- [x] HTTP(S) liste adresi biliniyorsa göreli URI'leri ana liste URL'sine göre çöz.
- [x] EXTINF satırında tırnak dışındaki ilk ayırıcı virgülü bul; başlıktaki virgülleri koru.
- [x] UTF-8 BOM, CRLF, HLS medya/ana manifesti, başlık virgülü ve göreli URI için JUnit regresyon testleri ekle.
- [x] Önbellek dosya adını URL'nin SHA-256 özetiyle üret.
- [ ] Hatalı veya desteklenmeyen URI şemalarını kontrollü hata olarak raporla; kullanıcıya özel ağ/HTTP kaynaklarını gereksiz yere devre dışı bırakma.

> IPTV ve podcast için JUnit regresyon testleri kaynak koduna eklendi ancak bu ortamda Android Gradle derlemesi/testi çalıştırılamadığı için sonuçları henüz doğrulanmadı.

## 3. Podcast RSS — `data/Services.kt`, `pages/PodcastPage.kt`

### Mevcut uygulama
- Popüler program ve arama verisi Apple Podcasts katalog uç noktalarından alınıyor.
- Bölümler programın RSS akışından `enclosure` URL'si ile okunuyor.
- Abonelikler ve son dinlenenler yerel SharedPreferences içinde saklanıyor.
- Podcast ilerlemesi URL anahtarıyla yerel olarak kaydediliyor ve bölüm yeniden açıldığında kaldığı yerden başlatılıyor.

### Statik bulgular / ürün kapsamı
- **Podcast çevrimdışı indirme eklendi.** Android DownloadManager, bölümleri uygulamaya özel Podcasts klasörüne indirir; Android sistem bildirimi indirme durumunu gösterir. İndirme kaydı URL'nin kendisini değil SHA-256 anahtarını ve DownloadManager kimliğini saklar. İndirilen dosya oynatma sırasında otomatik seçilir; indirme düğmesi araç hareket halindeyken engellenir.
- İndirilen bölümler Podcast ekranındaki ayrı **İndirilenler** sekmesinden, RSS akışına erişim olmasa da listelenir; uyumlu medya kütüphanesinde de ayrı kategori olarak sunulur. İndirme tamamlanma/iptal/progress değişimleri medya kütüphanesine bildirilir.
- İndirme hedefi uygulamaya özel harici depolamadır; uygulama kaldırılırsa dosyalar da kaldırılabilir. İndirme mobil ağda başlatılabilir, dolaşımda indirme kapalıdır.
- RSS ayrıştırıcısı `item` içindeki `enclosure` URL'sini okuyor; MIME türü, dosya boyutu, HTTP yanıtı ve URL geçerliliği için açık bir doğrulama görünmüyor.
- Tarih ayrıştırma tek RFC 822 biçimine dayanıyor; farklı RSS tarih biçimleri boş tarih üretebilir.
- **RSS enclosure adresleri düzeltildi.** Göreli bölüm dosyası adresleri feed URL'sine göre çözümleniyor; mutlak adresler korunuyor. Boş veya çözümlenemeyen adresler bölüm oynatılabilir adresi olarak kullanılmıyor.
- **RSS tarih ayrıştırması güçlendirildi.** Saniyeli/saniyesiz RFC 822 tarihleri ve yaygın saat dilimi yazımları deneniyor; tanınmayan tarih boş bırakılıyor.
- Feed yükleme ve bölüm listesi Android cihazda farklı RSS sağlayıcılarıyla doğrulanmadı.

### Düzeltme ve test planı
- [x] RSS tarih biçimlerine toleranslı ayrıştırma ve bozuk tarih testleri eklendi.
- [ ] Enclosure MIME/uzunluk doğrulaması ve kullanıcıya açık hata mesajları.
- [x] Göreli enclosure URL'leri feed URL'sine göre çözümleniyor; mutlak URL korunuyor.
- [ ] Uzun RSS, eksik alan, XML namespace ve bozuk XML testleri.
- [x] Podcast bölümleri için Android DownloadManager tabanlı indirme, durum/progress, iptal/silme, indirilmiş dosyayı otomatik oynatma ve çevrimdışı indirme listesi eklendi.
- [ ] Büyük indirmelerde depolama kotası/otomatik temizlik, dosya bütünlüğü ve farklı RSS sunucularıyla çevrimdışı cihaz testi yapılmalı.

## 4. YouTube ve genel WebView — `pages/WebPage.kt`, `pages/YouTubeAdBlocker.kt`

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

## 5. Sonuç ve öncelik

| Öncelik | İş | Durum |
|---|---|---|
| P0 | HLS M3U8 manifestini kanal listesi gibi ayrıştırma riskini gider | Kod düzeltildi; test sonucu bekliyor |
| P1 | M3U göreli URL ve EXTINF başlık ayrıştırmasını düzelt/test et | Kod ve regresyon testleri eklendi; test sonucu bekliyor |
| P1 | Podcast RSS URL/tarih/enclosure dayanıklılığını artır | URL ve tarih kodu/testleri eklendi; MIME ve XML saha testleri açık |
| P1 | WebView/YouTube oturum, reklam müdahalesi ve temel TLS/karma içerik güvenliği | Reklam müdahalesi kaldırıldı; TLS ve karma içerik korumaları eklendi; cihaz testi açık |
| P1 | Player/MediaSession arka plan dayanıklılığı | MediaLibraryService + Türkçe medya kütüphanesi + kalıcı kuyruk + playback resumption eklendi; gerçek cihaz/Android Auto testi açık |
| P2 | Podcast çevrimdışı indirme özelliğini kapsamlandır (ürün kararı) | Uygulanmamış |

## Doğrulama sınırı

Bu belge statik kod incelemesine dayanır. Bu çalışma kapsamında APK derlenmedi, JUnit testleri çalıştırılmadı, GitHub Actions kullanılmadı, sunucuya erişilmedi ve fiziksel cihaz/araç testi yapılmadı. Playback resumption kodunun Android sürümleri ve üretici güç yönetimi davranışlarıyla uyumu cihaz testleri tamamlanana kadar doğrulanmış kabul edilmez. Hiçbir medya özelliği saha testinden geçmiş olarak işaretlenmemiştir.
