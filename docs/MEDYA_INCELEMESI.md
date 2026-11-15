# OmniDrive — Medya özellikleri statik incelemesi

Tarih: 2026-11-15  
Depo: `msmmsm/NewREX-Android-Auto`  
Dal: `master`  
İncelenen kaynak: `master` (IPTV ayrıştırıcı düzeltmeleri 2026-11-15 tarihinde eklendi)

> Bu belge kaynak kodu incelemesidir. APK derlemesi, otomatik test, gerçek IPTV/radyo/RSS adresleri, Android telefon veya Android Auto araç ekranı üzerinde oynatma testi yapılmamıştır. Kodda bir ekranın bulunması özelliğin çalıştığını kanıtlamaz.

## 1. Medya oynatıcı — `PlayerHub.kt`

### Mevcut uygulama
- TV, radyo ve podcast aynı Media3/ExoPlayer örneğini kullanıyor.
- Medya oturumu oynatma başlayınca `PlayerHub` içinde oluşturuluyor; durdurmada oturum bırakılıyor.
- `PlayerHub`, `CarActivity` tarafından başlatılıyor ve etkinlik kalıcı olarak kapatılırken serbest bırakılıyor. Manifestte `MediaSessionService` veya medya oynatma türünde bir foreground service tanımı bulunmuyor. Bu nedenle uygulama arka plana alındığında ve Android süreci sonlandırdığında kesintisiz oynatma garantisi kaynak kodundan doğrulanamıyor.
- Podcast konumu 10 saniyelik aralıklarla kaydediliyor; durdurma ve başka bölüme geçişte de mevcut konum kaydediliyor.
- Canlı yayınlarda artan bekleme aralığıyla yeniden bağlanma ve radyo için alternatif akış arama mekanizması var.
- HTTPS→HTTP otomatik düşürme ve çapraz protokol yönlendirmesi kapalı.

### Açık riskler / doğrulama işleri
- [ ] Uygulama arka plana alındığında, ekran kilitlendiğinde ve süreç Android tarafından sonlandırıldığında oynatma/MediaSession yaşam döngüsü gerçek cihazda doğrulanmalı.
- [ ] Uzun süreli arka plan oynatımı ürün gereksinimiyse `MediaSessionService` tabanlı servis mimarisi değerlendirilmeli; foreground service türü/izinleri, bildirim ve Android 14/15 davranışı birlikte tasarlanmalı.
- [ ] Ağ kopması, DNS hatası, HTTP 404/5xx, hatalı MIME türü, bozuk HLS manifesti ve canlı pencerenin gerisinde kalma senaryoları test edilmeli.
- [ ] Podcast konumu için 10 saniyelik kayıt aralığında beklenmedik süreç sonlandırmasındaki olası ilerleme kaybı ölçülmeli.
- [ ] `stop()`, `release()`, parça değiştirme ve sayfa yok edilmesi sırasında durum akışlarının (playing/buffering/error) tutarlılığı test edilmeli.
- [ ] Medya oturumu kimliği ve görünen metadata ürün adı OmniDrive ile tutarlı tutulmalı.

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
- **Bölüm indirme ve çevrimdışı dinleme akışı görünmüyor.** Mevcut davranış akış üzerinden oynatma ve konum saklama. Uygulama çevrimdışı indirme destekliyor şeklinde tanıtılmamalı.
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
- [ ] Çevrimdışı indirme isteniyorsa indirme kuyruğu, depolama kotası, iptal/devam, dosya temizliği ve çevrimdışı oynatma ayrı bir özellik olarak tasarlanmalı; şu an varmış gibi gösterilmemeli.

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
| P1 | Player/MediaSession arka plan dayanıklılığı | Kaynakta MediaSessionService/medya foreground service yok; gerçek cihaz testi ve servis mimarisi kararı açık |
| P2 | Podcast çevrimdışı indirme özelliğini kapsamlandır (ürün kararı) | Uygulanmamış |

## Doğrulama sınırı

Bu belge statik kod incelemesine dayanır. Bu çalışma kapsamında APK derlenmedi, JUnit testleri çalıştırılmadı, GitHub Actions kullanılmadı, sunucuya erişilmedi ve fiziksel cihaz/araç testi yapılmadı. Hiçbir medya özelliği saha testinden geçmiş olarak işaretlenmemiştir.
