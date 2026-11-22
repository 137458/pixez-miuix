<img src="../compose-miuix/shared/src/main/res/drawable-nodpi/ic_pixez_logo.png" alt="logo PixEz MIUIX" width="144" height="144" align="right" />

# PixEz MIUIX [![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](../LICENSE)

Klien pihak ketiga Pixiv untuk ponsel dan tablet Android, mendukung tema antarmuka Xiaomi HyperOS (MIUIX) dan ColorOS (COUI).

Aplikasi menjangkau Pixiv dari Tiongkok Daratan tanpa proxy. Resolusi nama domain memakai DNS-over-HTTPS dengan cadangan DNS sistem, dan mirror sumber gambar bersifat opsional. Tingkat ECH dan kompatibilitas ditentukan oleh negosiasi TLS lapisan bawah; aplikasi tidak mengubah SNI.

## Unduh

| OS | Unduhan |
| :--- | :--- |
| Android | [`PixEz-MIUIX-v*.apk`](https://github.com/137458/pixez-miuix/releases) |

Rilis saat ini: `0.9.110.0-miuix`. Membutuhkan Android 12 (API 31) atau lebih baru untuk ponsel dan tablet.

## Pratinjau

| ![Pratinjau halaman rekomendasi dan peringkat](./preview/2.jpg) | ![Pratinjau halaman detail karya](./preview/1.jpg) |
| :---: | :---: |

## Tentang Repositori Ini

PixEz MIUIX adalah versi tulis-ulang PixEz, klien pihak ketiga Pixiv. `shared/src/main/kotlin` memuat 385 berkas Kotlin, termasuk 87 berkas `*Screen.kt` (mencakup tema HyperOS dan ColorOS). Tumpukan utamanya: Kotlin 2.4.10, Jetpack Compose BOM 2026.08.01, MIUIX 0.9.4, COUI 0.0.1-alpha12, Ktor 3.1.2, SQLDelight 2.0.2, Coil 3.6.3, dan Decompose 3.3.0. Proyek memakai `compileSdk` 36 dan `minSdk` 31.

Komponen utama:

- Navigasi Decompose.
- Ktor Client: login OAuth2 PKCE, pembaruan token otomatis dengan pencobaan ulang permintaan asli, dan dukungan multi-akun.
- SQLDelight untuk basis data lokal; Coil 3 untuk pemuatan gambar.
- Jaringan: resolusi DoH dengan cadangan DNS sistem dan pemutus sirkuit lima menit. Sumber gambar dapat memakai host bawaan, host mirror, atau host kustom.
- Bilah bawah mengapung dengan lima tab: Rekomendasi, Pencarian, Peringkat, Feed, dan Spotlight. Bilah ini mendukung rendering Liquid Glass dan otomatis kembali ke blur bila `RuntimeShader` tidak tersedia.
- Novel: pencarian dalam aplikasi, penjelajahan detail, dan pembaca dengan rendering teks kaya (ilustrasi tersemat, penanda bab, dan tautan halaman); bookmark, riwayat bacaan, dan komentar.
- Feed mengikuti menyertakan daftar pantauan serial manga; serial dapat dipantau atau batal dipantau langsung dari halaman serial.
- Daftar mendukung tarik-turun untuk menyegarkan dan memuat lebih banyak konten secara otomatis saat mencapai dasar daftar.
- URL API, gambar, Pixivision, dan aset rilis divalidasi terhadap daftar host tepercaya sebelum permintaan dikirim.

## Kontribusi

Terima kasih kepada semua kontributor proyek ini:

<table>
  <tr>
    <td align="center"><a href="https://github.com/137458"><img src="https://avatars.githubusercontent.com/u/138956834?v=4" width="100px;" alt=""/><br /><sub><b>137458 (Rosemary)</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=137458" title="Code & UI">Kode &amp; UI</a></td>
    <td align="center"><a href="https://github.com/Notsfsssf"><img src="https://avatars3.githubusercontent.com/u/16934707?v=4" width="100px;" alt=""/><br /><sub><b>Perol_Notsfsssf</b></sub></a><br /><a href="https://github.com/Notsfsssf/pixez-flutter/commits?author=Notsfsssf" title="Code">Kode</a></td>
    <td align="center"><a href="https://xyx.moe"><img src="https://avatars3.githubusercontent.com/u/9017470?v=4" width="100px;" alt=""/><br /><sub><b>Skimige</b></sub></a><br /><a href="https://github.com/Skimige" title="Documentation">Dokumentasi</a></td>
    <td align="center"><a href="https://github.com/TragicLifeHu"><img src="https://avatars3.githubusercontent.com/u/16817202?v=4" width="100px;" alt=""/><br /><sub><b>Tragic Life</b></sub></a><br /><a href="#translation-TragicLifeHu" title="Translation">Terjemahan (zh_TW)</a></td>
    <td align="center"><a href="http://ivtune.net"><img src="https://avatars0.githubusercontent.com/u/54385201?v=4" width="100px;" alt=""/><br /><sub><b>karin722</b></sub></a><br /><a href="#translation-karin722" title="Translation">Terjemahan (ja)</a></td>
    <td align="center"><a href="http://archman.fun"><img src="https://avatars0.githubusercontent.com/u/68731023?v=4" width="100px;" alt=""/><br /><sub><b>Romani-Archman</b></sub></a><br /><a href="#documentation-Romani-Archman" title="Documentation">Dokumentasi</a></td>
    <td align="center"><a href="https://github.com/itzXian"><img src="https://avatars1.githubusercontent.com/u/34748039?v=4" width="100px;" alt=""/><br /><sub><b>Xian</b></sub></a><br /><a href="#translation-itzXian" title="Translation">Terjemahan (en_US)</a></td>
    <td align="center"><a href="https://github.com/ReikiAigawara"><img src="https://avatars1.githubusercontent.com/u/66962815?v=4" width="100px;" alt=""/><br /><sub><b>Reiki Aigawara</b></sub></a><br /><a href="#translation-ReikiAigawara" title="Translation">Terjemahan (id_ID)</a></td>
  </tr>
</table>

Kontribusi kode, antarmuka, dan terjemahan diterima melalui Pull Request.

## Komunikasi

Panduan penggunaan dan FAQ tersedia [di sini (bahasa Mandarin)](./FAQ.md).

Umpan balik melalui surel: [PxezFeedBack@outlook.com](mailto:PxezFeedBack@outlook.com)

## Penafian

1. **Klien tidak resmi**: PixEz MIUIX adalah klien pihak ketiga open-source untuk Pixiv yang dikembangkan oleh pengembang independen dan komunitas open-source untuk tujuan pembelajaran, riset, dan penggunaan pribadi. Proyek ini tidak berafiliasi dengan, didukung oleh, atau terkait dengan pixiv Inc.
2. **Hak cipta dan kekayaan intelektual**: Semua ilustrasi, manga, novel, ugoira, artikel Pixivision, dan konten terkait dalam aplikasi ini adalah hak milik pembuat aslinya dan pixiv Inc. Konten yang diakses atau diunduh hanya ditujukan untuk apresiasi pribadi non-komersial. Penggunaan komersial dan distribusi ulang tanpa izin dilarang.
3. **Kepatuhan hukum**: Fitur jaringan disediakan semata-mata untuk riset konektivitas. Pengguna bertanggung jawab penuh untuk mematuhi hukum setempat dan Ketentuan Layanan Pixiv. Pengembang dan kontributor tidak bertanggung jawab atas segala penyalahgunaan aplikasi ini.

## Lisensi

Proyek ini menggunakan lisensi [GPL-3.0](../LICENSE).
