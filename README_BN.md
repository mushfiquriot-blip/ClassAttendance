# Class Attendance — Android Studio Project

এটি সরাসরি APK নয়; এটি Android Studio-তে খুলে APK build করার সম্পূর্ণ প্রজেক্ট।

## APK বানানোর নিয়ম
1. কম্পিউটারে Android Studio ইনস্টল করুন।
2. ZIP ফাইল Extract করুন।
3. Android Studio → Open → `ClassAttendance` ফোল্ডার নির্বাচন করুন।
4. Gradle Sync শেষ হওয়া পর্যন্ত অপেক্ষা করুন (প্রথমবার ইন্টারনেট লাগবে)।
5. Build → Build Bundle(s) / APK(s) → Build APK(s) চাপুন।
6. APK পাওয়া যাবে: `app/build/outputs/apk/debug/app-debug.apk`

## ফিচার
- QR code scanner (ZXing)
- নাম/রোল হাতে যোগ
- একই দিনে একই নাম/রোল দ্বিতীয়বার গণনা হয় না
- SQLite লোকাল ডেটাবেসে তারিখ ও সময়সহ সংরক্ষণ
- তারিখ ধরে রেকর্ড দেখা
- CSV ফাইল সেভ

## QR ফরম্যাট
QR-এর লেখা সরাসরি নাম বা রোল হতে পারে। JSON হলে `name`, `studentName`, `student_name`, `roll` অথবা `id` ফিল্ড থেকে নাম/পরিচয় নেওয়ার চেষ্টা করে।

## গুরুত্বপূর্ণ
- প্রথম build-এ ইন্টারনেট লাগবে, কারণ Gradle dependency ডাউনলোড হবে।
- ক্যামেরা ব্যবহারের অনুমতি দিতে হবে।
- ডেটা ফোনের অ্যাপ-স্টোরেজে থাকে। অ্যাপ আনইনস্টল করলে লোকাল ডেটা মুছে যাবে; CSV রিপোর্ট নিয়মিত Export করুন।


## নমুনা QR কোড
আপলোড করা নমুনা QR কোডের ডেটা `8526284`; এই আইডির জন্য অ্যাপে `Mushfiqur Rahman Siam (8526284)` নাম ম্যাপ করা হয়েছে। অন্য শিক্ষার্থীদের QR-এর জন্য তাদের আইডি ও নামের তালিকা যোগ করতে হবে।
