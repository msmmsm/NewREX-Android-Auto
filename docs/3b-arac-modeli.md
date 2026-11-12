# NewREX 3B Araç Modeli ve Ana Ekran Planı

## Mevcut değişiklik

- Ana ekrana NewREX başlığı ve kısa açıklama eklendi.
- PARK / SÜRÜŞTE göstergesi `DriveState.driving` akışından beslenir.
- Hız, yakıt, menzil veya motor durumu gibi araçtan doğrulanamayan değerler gösterilmez.
- TV, YouTube, Radyo ve Web kartlarının mevcut gezinme davranışı korunmuştur.

## 3B modelin teknik tasarımı

3B araç görünümü, ana ekranın geri kalanından bağımsız bir `CarModelView` bileşeni olarak geliştirilmelidir. Model kaynağı GLB/glTF olmalı; çizim için Android uyumlu bir 3B çalışma zamanı (tercihen Filament) kullanılmalıdır.

### İlk sürüm kapsamı

1. Araç markası/modeli/yılı seçilene kadar açıkça "Genel araç modeli" olarak etiketlenmiş nötr bir model kullan.
2. Modeli düşük poligonlu ve mobil GPU için optimize edilmiş tut.
3. İlk açılışta sabit üç çeyrek açı kullan; kullanıcı döndürme hareketini yalnızca araç park halindeyken etkinleştir.
4. Sürüş sırasında döndürme, yakınlaştırma, sürekli animasyon ve dikkat dağıtan parıltıları kapat.
5. Tasarruf modu veya görünüm ekrandan ayrıldığında çizim döngüsünü durdur.
6. Model dosyası yüklenemezse uygulama çökmesin; sade araç simgesine geri dön.
7. Gerçek araç telemetrisi mevcut değilse modelin yanında hız, yakıt, menzil, lastik basıncı veya motor arıza durumu varmış gibi değer üretme.

### Kabul ölçütleri

- Android API 26+ üzerinde açılma ve ekran döndürme/yeniden boyutlandırma testleri.
- Android Auto ekranında ve park uygulaması kapısında doğru ölçeklenme.
- Sürüşte model etkileşimlerinin kilitlenmesi.
- Arka plana geçişte GPU kaynaklarının bırakılması; geri dönüşte modelin yeniden kurulması.
- Düşük bellekli cihazda model yüklenemezse güvenli geri dönüş.
- Uzun süreli kullanımda bellek/GPU kaynaklarında sürekli artış olmaması.

## Açık karar

Araca özel gövde, jant ve iç tasarım kullanmak için marka, model ve model yılı gerekir. Bu bilgi verilene kadar uygulamada belirli bir üreticiye aitmiş gibi görünen model kullanılmamalıdır.
