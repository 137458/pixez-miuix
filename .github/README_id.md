<img src="../compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="logo" width="144" height="144" align="right" />

# PixEz MIUIX ![](https://camo.githubusercontent.com/f2b4fa6779c8a4825e0e7347076746a2047ed100/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6c6963656e73652d47504c2d2d332e302d6f72616e67652e737667)

> **Compose adalah jalur utama**: PixEz MIUIX dikembangkan dan dirilis dari [`compose-miuix/`](../compose-miuix). Flutter v1 telah diarsipkan di [`archive/flutter-v1/`](../archive/flutter-v1/) untuk penelusuran historis dan perbaikan keamanan. Di Android, Flutter v1 dan versi Compose memakai application ID yang berbeda sehingga tidak dapat ditimpa saat pembaruan.

Klien pihak ketiga Pixiv yang dibangun di atas Kotlin Multiplatform (KMP) dan Compose Multiplatform (CMP), dengan antarmuka berbasis pustaka komponen MIUIX (`top.yukonga.miuix.kmp`). Rilis aktif tersedia untuk Android dan Windows Desktop; iOS hanya diverifikasi kompilasi, dan target macOS belum diaktifkan.

Koneksi langsung ke Pixiv dari Tiongkok Daratan tanpa proxy: mode SNI langsung, DNS aman DoH, dan mirror sumber gambar opsional.

# Download

|OS|Market|GitHub Releases|
|:---:|:---:|:---:|
|Android|/|[`PixEz-MIUIX-v*.apk`](https://github.com/137458/pixez-miuix/releases)|
|Windows Desktop|/|[`PixEz-Standalone.exe` / `PixEz-windows-x64-portable.zip` / `PixEz-*.msi`](https://github.com/137458/pixez-miuix/releases)|
|Linux|/|Build dari sumber (termasuk paket Deb), belum ada rilis resmi|
|macOS|/|Target belum diaktifkan, belum ada rilis resmi|

Android 7.0 (API 24) atau lebih baru, untuk ponsel dan tablet. EXE Windows dapat dijalankan tanpa pemasangan Java. Versi portabel diekstrak terlebih dahulu, lalu jalankan `PixEz.exe`.

# Preview

|![Preview](../.github/preview/2.jpg) | ![Preview](../.github/preview/1.jpg) | 
|:-------------------:|:------------------------:|

# Tentang repositori ini

PixEz MIUIX adalah versi tulis-ulang dari PixEz, klien pihak ketiga Pixiv. `shared/src/commonMain` memuat 20 berkas `*Repository.kt` dan 42 berkas `*Screen.kt`.

Komponen utama:

- Navigasi Decompose.
- Ktor Client: login OAuth2 PKCE, pembaruan token otomatis beserta pencobaan ulang permintaan asli, dan dukungan multi-akun.
- SQLDelight untuk basis data lokal; Coil 3 untuk pemuatan gambar.
- Bilah bawah mengapung dengan lima tab: Rekomendasi, Pencarian, Peringkat, Feed, dan Spotlight; mendukung rendering Liquid Glass dan otomatis kembali ke blur bila `RuntimeShader` tidak didukung.
- Novel: pencarian dalam aplikasi, penjelajahan detail, dan pembaca dengan rendering teks kaya (ilustrasi tersemat, penanda bab, dan tautan halaman); bookmark, riwayat bacaan, dan komentar.
- Feed mengikuti menyertakan daftar pantauan serial manga; serial dapat dipantau atau batal dipantau langsung dari halaman serial.
- Daftar mendukung tarik-turun untuk menyegarkan dan memuat lebih banyak konten secara otomatis saat mencapai dasar daftar.

## Kontribusi

Terima kasih kepada semua kontributor proyek ini:

<table>
  <tr>
    <td align="center"><a href="https://github.com/137458"><img src="https://avatars.githubusercontent.com/u/138956834?v=4" width="100px;" alt=""/><br /><sub><b>137458 (Rosemary)</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=137458" title="Code & UI">💻 🎨</a></td>
    <td align="center"><a href="https://github.com/Notsfsssf"><img src="https://avatars3.githubusercontent.com/u/16934707?v=4" width="100px;" alt=""/><br /><sub><b>Perol_Notsfsssf</b></sub></a><br /><a href="https://github.com/Notsfsssf/pixez-flutter/commits?author=Notsfsssf" title="Code">💻</a></td>
    <td align="center"><a href="https://xyx.moe"><img src="https://avatars3.githubusercontent.com/u/9017470?v=4" width="100px;" alt=""/><br /><sub><b>Skimige</b></sub></a><br /><a href="https://github.com/Skimige" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/TragicLifeHu"><img src="https://avatars3.githubusercontent.com/u/16817202?v=4" width="100px;" alt=""/><br /><sub><b>Tragic Life</b></sub></a><br /><a href="#translation-TragicLifeHu" title="Translation">🌍 (zh_TW)</a></td>
    <td align="center"><a href="http://ivtune.net"><img src="https://avatars0.githubusercontent.com/u/54385201?v=4" width="100px;" alt=""/><br /><sub><b>karin722</b></sub></a><br /><a href="#translation-karin722" title="Translation">🌍 (ja)</a></td>
    <td align="center"><a href="http://archman.fun"><img src="https://avatars0.githubusercontent.com/u/68731023?v=4" width="100px;" alt=""/><br /><sub><b>Romani-Archman</b></sub></a><br /><a href="#documentation-Romani-Archman" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/itzXian"><img src="https://avatars1.githubusercontent.com/u/34748039?v=4" width="100px;" alt=""/><br /><sub><b>Xian</b></sub></a><br /><a href="#translation-itzXian" title="Translation">🌍 (en_US)</a></td>
    <td align="center"><a href="https://github.com/ReikiAigawara"><img src="https://avatars1.githubusercontent.com/u/66962815?v=4" width="100px;" alt=""/><br /><sub><b>Reiki Aigawara</b></sub></a><br /><a href="#translation-ReikiAigawara" title="Translation">🌍 (id_ID)</a></td>
  </tr>
</table>

Kontribusi kode, antarmuka, dan terjemahan diterima melalui Pull Request.

# Komunikasi

Panduan penggunaan dan FAQ tersedia [di sini (bahasa Mandarin)](./FAQ.md).

Umpan balik melalui surel: [gaaf14738@gmail.com](mailto:gaaf14738@gmail.com)

# Penafian (Disclaimer)

1. **Klien Tidak Resmi**: PixEz MIUIX adalah klien pihak ketiga open-source untuk Pixiv yang dikembangkan untuk tujuan pembelajaran, riset, dan penggunaan pribadi. Proyek ini tidak berafiliasi dengan, didukung oleh, atau terkait dengan pixiv Inc.
2. **Hak Cipta & Kekayaan Intelektual**: Semua karya seni, ilustrasi, komik, novel, ugoira, dan konten yang ditampilkan dalam aplikasi ini adalah hak milik dari pembuat aslinya dan pixiv Inc. Konten yang diakses atau diunduh hanya ditujukan untuk apresiasi dan penggunaan pribadi non-komersial.
3. **Kepatuhan Hukum**: Fitur koneksi langsung disediakan semata-mata untuk eksplorasi teknis. Pengguna bertanggung jawab penuh untuk mematuhi hukum setempat dan Ketentuan Layanan Pixiv. Pengembang dan kontributor tidak bertanggung jawab atas segala penyalahgunaan aplikasi ini.
