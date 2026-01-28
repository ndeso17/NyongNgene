# NyongNgene - Offline Hiking Safety & Communication App

**NyongNgene** adalah aplikasi keselamatan pendakian _offline-first_ yang mengintegrasikan komunikasi **Bluetooth Low Energy (BLE)** untuk jarak dekat dan **LoRa** untuk jarak jauh, memungkinkan komunikasi dan pelacakan tanpa memerlukan sinyal seluler atau internet.

---

## 🚀 Fitur Utama

### 1. Chat Bluetooth Mesh (Offline)

Komunikasi teks antar pendaki dalam jangkauan Bluetooth tanpa pairing manual.

- **Mesh Networking**: Menggunakan prinsip _store & forward_ untuk memperluas jangkauan.
- **Smart Routing**: Penyatuan chat berdasarkan identitas nama (mengatasi _BLE Address Randomization_).
- **Mode Chat**:
  - **Broadcast**: Pesan publik ke semua pendaki di sekitar.
  - **Private**: Pesan personal ke pendaki tertentu dengan dedupilasi cerdas.
- **Fitur Pesan**: Teks, Share Lokasi, dan SOS.

### 2. LoRa Tracking & SOS (Long Range)

Pelacakan posisi dan sinyal darurat jarak jauh menggunakan modul USB LoRa (SX1262).

- **Tracking Periodik**: Broadcast koordinat GPS secara berkala.
- **Mode SOS**: Sinyal darurat prioritas tinggi dengan interval cepat.
- **Buffer Offline**: Penyimpanan data lokal saat pengiriman gagal (mekanisme _store & forward_).
- **Monitoring Sinyal**: Visualisasi kekuatan sinyal (RSSI) dan kualitas sinyal (SNR) untuk debugging.

### 3. Visi Konektivitas Masa Depan

Saat ini, aplikasi mengandalkan koneksi **LoRa** dan **BLE**. Kedepannya, sistem akan dikembangkan untuk menggunakan **LTE** juga sebagai jalur utama. Dalam skenario ini, LoRa dan BLE akan berperan sebagai _backup priority_. Hal ini disiapkan untuk menyongsong masa depan di mana _smartphone_ akan memiliki kemampuan untuk terkoneksi langsung dengan **sinyal satelit** (_direct-to-satellite_), sehingga redundansi koneksi menjadi kunci keselamatan pendaki.

---

## 🛠️ Teknologi yang Digunakan

- **Bahasa**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material3)
- **Database**: Room Persistence Library
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Async**: Kotlin Coroutines & Flow
- **Hardware Integration**:
  - `usb-serial-for-android` (Driver LoRa)
  - Android Bluetooth LE API

---

## 🔧 Cara Instalasi & Pengujian

### Prasyarat

- Android Studio (Disarankan menggunakan JDK bundled untuk kompatibilitas build).
- Minimal SDK 26 (Android 8.0).
- Perangkat Android dengan dukungan BLE Advertising.
- Modul LoRa SX1262 (USB) untuk pengujian fitur LoRa.

### Mengatasi Masalah Build

Jika mengalami error versi Java ("Major version 69"), pastikan `gradle.properties` menggunakan JDK internal Android Studio:

```properties
org.gradle.java.home=<path-to-android-studio>/jbr
```

(Lihat `implementation_plan.md` untuk detailnya).

### Pengujian BLE Chat

1. Instal aplikasi di 2 perangkat Android.
2. Berikan izin lokasi dan Bluetooth.
3. Buka menu Chat → Tunggu hingga perangkat lain muncul di _Contact List_.
4. Coba kirim pesan Direct Message atau Broadcast.
5. Verifikasi bahwa pesan diterima meskipun tanpa koneksi internet.

### Pengujian LoRa

1. Hubungkan modul SX1262 via USB OTG.
2. Buka **LoRa Settings** untuk verifikasi koneksi USB.
3. Monitor log untuk melihat paket data masuk/keluar.

---

## 📄 Lisensi

Proyek ini dikembangkan untuk tujuan keselamatan dan edukasi.

---

## ☕ Dukung Pengembangan Project Ini

Kalau kamu merasa project ini bermanfaat dan ingin mendukung pengembangannya,
silakan berdonasi melalui QRIS di link berikut:

👉 **https://naxgrinting.my.id/**

![QRIS Donasi](https://naxgrinting.my.id/QrisKu.jpg)

Terima kasih sudah mendukung pengembangan aplikasi 🙏

Setiap dukungan = tambahan semangat ngoding bagi tim pengembang (kontributor)🚀.
