# SonicShield 3.0.14 — Powered by Ridi

Android Studio project and GitHub Actions workflow.

## Perubahan 3.0.14

- Tidak lagi meminta profil Bluetooth komunikasi, tidak mengaktifkan SCO/HFP, dan tidak mengubah audio ke mode panggilan.
- Input pemrosesan menggunakan mikrofon HP.
- Output diarahkan ke perangkat Bluetooth A2DP (jalur musik/media) seperti A9 Pro.
- Software Reduce menggunakan Android NoiseSuppressor bila tersedia, ditambah filter low-rumble ringan. Jika efek NoiseSuppressor tidak tersedia, aplikasi menampilkan status bahwa hanya filter dasar yang aktif.
- Transparency adalah pemantauan mic HP melalui A2DP. Karena latensi Bluetooth, suara pantauan dapat terlambat dan tidak setara Transparency bawaan TWS.
- Pemeriksaan koneksi memeriksa output A2DP saja; aplikasi tidak menganggap mic TWS sebagai syarat.
- Versi aplikasi 3.0.14 dan ikon earbuds bergaya futuristik.

## Build APK via GitHub Actions

1. Ekstrak ZIP.
2. Upload seluruh isi langsung ke root repository GitHub, termasuk folder `.github`.
3. Jalankan workflow **SonicShield Android Build** dari tab Actions.
4. Unduh APK dari bagian Artifacts setelah workflow selesai.

## Batas kemampuan audio

Versi ini tidak membuat ANC akustik sejati. Aplikasi memproses suara yang ditangkap mikrofon HP lalu memutarnya melalui A2DP. Jalur Bluetooth A2DP mempunyai latensi sehingga pemantauan sekitar bisa terlambat. NoiseSuppressor Android bergantung pada dukungan perangkat dan tidak menjamin semua suara bising hilang. Jangan gunakan aplikasi ini sebagai pelindung pendengaran atau ketika perlu mendengar lalu lintas/peringatan sekitar. Hentikan mode bila muncul feedback, dengung, atau suara tidak nyaman.

## Build configuration

- Android Gradle Plugin 8.9.2
- Kotlin 2.0.21
- Gradle 8.11.1
- JDK 17
- compileSdk 35, minSdk 26, targetSdk 35
