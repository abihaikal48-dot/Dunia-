# 💑 DUNIA - Aplikasi Sinergi Finansial & Rutinitas Korporasi Mitra (Haikal & Ummu)

Aplikasi manajemen finansial, jadwal harian, monitoring ibadah, target pernikahan, dan sinergi hemat pengeluaran yang dirancang khusus untuk membangun masa depan tangguh bersama seluruh aktivitas Haikal & Ummu.

---

## 🚀 Cara Otomatis Mengunduh APK dari GitHub

Kami telah mengonfigurasi **GitHub Actions** otomatis agar setiap kali Anda melakukan push/sinkronisasi kode ke GitHub, sistem akan otomatis mengompilasi kode dan menyediakan installer APK yang langsung bisa diinstal di HP Android Anda.

### Metode 1: Mengunduh dari Artifacts Hasil Build (Setiap Push)
1. Lakukan push atau sinkronisasi kode proyek Anda ke GitHub.
2. Buka halaman repositori GitHub Anda di browser web.
3. Klik tab **"Actions"** di bagian atas menu repositori.
4. Klik pada proses build/workflow terbaru yang bernama **"Build & Install Android APK"**.
5. Gulir ke bawah hingga Anda melihat bagian **"Artifacts"**.
6. Klik dan unduh file **`DUNIA-Mitra-App-Debug-APK`**. Ekstrak file zip tersebut untuk mendapatkan file `.apk` siap pasang!

### Metode 2: Mengunduh lewat Menu Releases (Versi Publik/Resmi)
1. Kapan pun Anda siap menandai versi rilis baru, buatlah sebuah rilis tag (misal: `v1.0.0`, `v1.0.1`) lewat git:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. GitHub secara otomatis akan memproses build khusus rilis dan melampirkan file APK langsung di menu **"Releases"** di bagian kanan halaman utama repositori GitHub Anda.
3. Klik rilis terbaru tersebut dan download file **`DUNIA-Mitra-App-Debug.apk`**.

---

## ⚡ Cara Cepat Menghasilkan & Mengunduh APK Langsung di Google AI Studio
Jika Anda ingin mencoba aplikasinya di HP tanpa menunggu sinkronisasi GitHub:
1. Di layar Google AI Studio tempat Anda melihat preview emulator, lihat panel atas atau menu pengaturan (**Settings / Gear Icon** atau menu dropdown di proyek).
2. Temukan opsi **"Build APK"** atau **"Generate APK / Export APK"**.
3. Sistem cloud kami akan mengompilasi aplikasimu secara langsung dan menyuguhkan tombol download installer `.apk` secara instan ke browsermu.

---

## 🛠️ Cara Melakukan Build Manual di Komputer Lokal

Jika Anda menyalin proyek ini ke komputer lokal (menggunakan Laptop/PC):

1. **Pastikan Java JDK 17 telah terpasang** di PC Anda.
2. Buka terminal/command prompt di direktori root proyek ini.
3. Jalankan perintah kompilasi:
   - **Di Linux & macOS:**
     ```bash
     chmod +x gradlew
     ./gradlew assembleDebug
     ```
   - **Di Windows:**
     ```cmd
     gradlew.bat assembleDebug
     ```
4. Setelah build berhasil, file APK siap instal dapat Anda ambil di folder berikut:
   `app/build/outputs/apk/debug/app-debug.apk`
