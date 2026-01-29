# NyongNgene - Offline Hiking Safety & Communication App

**NyongNgene** adalah aplikasi keselamatan pendakian _offline-first_ yang mengintegrasikan komunikasi **Bluetooth Low Energy (BLE)** untuk jarak dekat dan **LoRa** untuk jarak jauh, memungkinkan komunikasi dan pelacakan tanpa memerlukan sinyal seluler atau internet.

---

## 🚀 Fitur Utama

### 1. AR Turn-by-Turn Navigation 🧭

Navigasi augmented reality dengan panduan arah real-time.

- **Mode Kamera AR**: Live camera feed dengan overlay peta transparan
- **Anak Panah Arah**: Kompas visual yang menunjuk ke arah tujuan
- **Text-to-Speech**: Instruksi suara dalam Bahasa Indonesia
- **Mode Tempuh**:
  - 🚶 **Jalan Kaki**: Untuk pendakian
  - 🏍️ **Motor**: Untuk perjalanan motor trail
  - 🚗 **Mobil**: Untuk akses jalan raya
- **Statistik Live**:
  - Jarak Total & Tersisa
  - Estimasi Waktu Tiba (ETA)
  - Kecepatan Real-time
  - Rata-rata Kecepatan
- **Mode Kamera**: Dukungan Normal dan Ultrawide

### 2. Chat Bluetooth Mesh (Offline)

Komunikasi teks antar pendaki dalam jangkauan Bluetooth tanpa pairing manual.

- **Mesh Networking**: Menggunakan prinsip _store & forward_ untuk memperluas jangkauan.
- **Smart Routing**: Penyatuan chat berdasarkan identitas nama (mengatasi _BLE Address Randomization_).
- **Mode Chat**:
  - **Broadcast**: Pesan publik ke semua pendaki di sekitar.
  - **Private**: Pesan personal ke pendaki tertentu dengan dedupilasi cerdas.
- **Fitur Pesan**: Teks, Share Lokasi, dan SOS.

### 3. LoRa Tracking & SOS (Long Range)

Pelacakan posisi dan sinyal darurat jarak jauh menggunakan modul USB LoRa (SX1262).

- **Tracking Periodik**: Broadcast koordinat GPS secara berkala.
- **Mode SOS**: Sinyal darurat prioritas tinggi dengan interval cepat.
- **Buffer Offline**: Penyimpanan data lokal saat pengiriman gagal (mekanisme _store & forward_).
- **Monitoring Sinyal**: Visualisasi kekuatan sinyal (RSSI) dan kualitas sinyal (SNR).

### 4. Peta Offline

Unduh dan gunakan peta tanpa internet.

- **Download Tiles**: Simpan tiles OpenStreetMap untuk akses offline
- **Multiple Trail Support**: Dukungan berbagai jalur pendakian populer
- **Share via WiFi**: Bagikan peta ke pendaki lain tanpa internet

### 5. Visi Konektivitas Masa Depan

Kedepannya, sistem akan dikembangkan untuk:

- Integrasi **LTE** sebagai jalur utama (LoRa/BLE sebagai backup)
- Dukungan **direct-to-satellite** connectivity
- **Machine Learning** untuk deteksi jalur yang salah

---

## 📱 Screenshots

| 2D Map View      | AR Navigation             |
| ---------------- | ------------------------- |
| Peta dengan rute | Kamera + overlay navigasi |

---

## 🛠️ Teknologi yang Digunakan

- **Bahasa**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material3)
- **Database**: Room Persistence Library
- **Camera**: CameraX API
- **Maps**: OSMDroid + Leaflet.js (WebView)
- **Routing**: OSRM API
- **TTS**: Android TextToSpeech
- **Hardware Integration**:
  - `usb-serial-for-android` (Driver LoRa)
  - Android Bluetooth LE API
  - CameraX for AR features

---

## 🔧 Cara Instalasi & Pengujian

### Prasyarat

- Android Studio (Disarankan menggunakan JDK bundled untuk kompatibilitas build).
- Minimal SDK 24 (Android 7.0).
- Perangkat Android dengan dukungan BLE Advertising.
- Modul LoRa SX1262 (USB) untuk pengujian fitur LoRa.

### Izin yang Diperlukan

- **Kamera**: Untuk fitur AR Navigation
- **Lokasi**: Untuk GPS tracking dan navigasi
- **Bluetooth**: Untuk mesh chat
- **Internet**: Untuk download peta dan routing (bisa offline setelah download)

### Mengatasi Masalah Build

Jika mengalami error versi Java ("Major version 69"), pastikan `gradle.properties` menggunakan JDK internal Android Studio:

```properties
org.gradle.java.home=<path-to-android-studio>/jbr
```

### Pengujian AR Navigation

1. Buka aplikasi → Pilih trail dari daftar
2. Izinkan akses Kamera dan Lokasi
3. Pilih mode tempuh (Jalan Kaki/Motor/Mobil)
4. Arahkan HP ke depan untuk melihat AR overlay
5. Ikuti anak panah dan instruksi suara

### Pengujian BLE Chat

1. Instal aplikasi di 2 perangkat Android.
2. Berikan izin lokasi dan Bluetooth.
3. Buka menu Chat → Tunggu hingga perangkat lain muncul di _Contact List_.
4. Coba kirim pesan Direct Message atau Broadcast.

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
