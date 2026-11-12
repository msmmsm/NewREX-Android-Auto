# Sketchfab'den 3D Araç Modeli Ekleme Rehberi

NewREX Android Auto, **glTF 2.0 / GLB** formatındaki tüm 3B araç modellerini destekleyecek şekilde tasarlanmıştır.

---

## 1. Sketchfab'de Doğru Modeli Bulma

[Sketchfab](https://sketchfab.com/3d-models?features=downloadable&sort_by=-likeCount) üzerinde binlerce ücretsiz ve yüksek kaliteli otomobil modeli bulunmaktadır.

### 🔍 Önerilen Arama Terimleri
* `car low poly glb`
* `sedan game ready`
* `corolla low poly` / `egea low poly` / `megane low poly`
* `vehicle realtime pbr`

### ⚠️ Otomobil Ekranları İçin Optimizasyon Kriterleri
Android Auto ünitesinde ve telefonunuzda aşırı ısınma (`diagnose-android-overheating`), kare düşüşü (FPS drop) veya aşırı RAM tüketimi yaşamamak için şu kriterlere dikkat edin:

| Kriter | Önerilen Sınır | Açıklama |
| :--- | :--- | :--- |
| **Format** | **.GLB** (Binary glTF) | Kaplama, ışık ve geometrinin tek dosyada toplandığı standart format. |
| **Dosya Boyutu** | **< 3.5 MB** | 10 MB+ modeller açılışta gecikmeye yol açar. |
| **Poligon (Vertices)** | **< 25.000 poligon** | Mobil GPU'nun 60 FPS akıcı dönebilmesi için şarttır. |
| **Doku (Texture)** | **1024x1024 (1K)** | 4K kaplamalar araç ekranında gereksiz VRAM harcar. |
| **Lisans** | **CC-BY / Free** | Ticari ve kişisel kullanıma açık Creative Commons modelleri. |

---

## 2. Modeli İndirme Adımları

1. Beğendiğiniz modelin sayfasında **"Download 3D Model"** butonuna tıklayın.
2. Açılan pencerede **"glTF"** veya **"GLB"** seçeneğini seçin.
3. İndirdiğiniz dosyanın uzantısı `.glb` değilse (klasör olarak `.gltf` + `.bin` + `textures/` geldiyse), ücretsiz [glTF-Viewer](https://gltf-viewer.donmccurdy.com/) veya [Blender](https://www.blender.org/) ile **Export -> glTF 2.0 (.glb)** diyerek tek bir dosya haline getirebilirsiniz.

---

## 3. NewREX'e Ekleme Yolları

### Yöntem A — Doğrudan Projeye Gömme (Offline & En Hızlı)
İndirdiğiniz modeli şu yola kopyalayın:
```
app/src/main/assets/models/car.glb
```
Uygulama derlendiğinde araç internete ihtiyaç duymadan anında bu modeli kullanır.

### Yöntem B — Cihaz Depolamasından Yükleme
Uygulama açıldığında telefonun yerel klasörünü kontrol eder:
```
/Android/data/com.newrex.auto/files/models/car.glb
```
Buraya attığınız model otomatik olarak önceliklendirilir.

---

## 4. Renk ve Materyal Uyumu
NewREX'in tema motoru (`Palette.accent` / `ColorPicker`), GLB modelin boya materyaline (`CarPaint` / `Body`) dinamik renk filtrelemesi uygular. Beyaz veya açık gri bazlı modeller renk değişimlerinde en iyi sonucu verir.

