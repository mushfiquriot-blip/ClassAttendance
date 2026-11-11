package com.musfik.classattendance

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Attendance(val id: Long, val student: String, val day: String, val time: String)

class AttendanceDb(context: android.content.Context) :
    SQLiteOpenHelper(context, "attendance.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student TEXT NOT NULL,
                day TEXT NOT NULL,
                time TEXT NOT NULL,
                UNIQUE(student, day)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun add(student: String): Boolean {
        val now = Date()
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        val values = ContentValues().apply {
            put("student", student.trim())
            put("day", day)
            put("time", time)
        }
        return writableDatabase.insertWithOnConflict(
            "attendance", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun today(): List<Attendance> {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return byDay(day)
    }

    fun byDay(day: String): List<Attendance> {
        val result = mutableListOf<Attendance>()
        readableDatabase.rawQuery(
            "SELECT id, student, day, time FROM attendance WHERE day=? ORDER BY id DESC",
            arrayOf(day)
        ).use { c ->
            while (c.moveToNext()) result.add(
                Attendance(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))
            )
        }
        return result
    }

    fun all(): List<Attendance> {
        val result = mutableListOf<Attendance>()
        readableDatabase.rawQuery(
            "SELECT id, student, day, time FROM attendance ORDER BY day DESC, id DESC",
            null
        ).use { c ->
            while (c.moveToNext()) result.add(
                Attendance(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))
            )
        }
        return result
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var db: AttendanceDb
    private lateinit var countText: TextView
    private lateinit var listBox: LinearLayout
    private lateinit var statusText: TextView
    private val blue = 0xFF123B70.toInt()
    private val todayKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AttendanceDb(this)
        buildScreen()
        refresh()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
            setBackgroundColor(0xFFF3F6FB.toInt())
        }
        scroll.addView(page)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            setBackgroundColor(blue)
        }
        header.addView(label("ক্লাস অ্যাটেনডেন্স", 24, 0xFFFFFFFF.toInt(), true))
        header.addView(label("QR স্ক্যান করে প্রতিদিনের উপস্থিতি সংরক্ষণ", 14, 0xFFE1EAF7.toInt()))
        page.addView(header, matchWrap())

        val summary = card(page)
        summary.addView(label("আজকের উপস্থিতি", 18, 0xFF172033.toInt(), true))
        summary.addView(label("তারিখ: ${displayDate(todayKey)}", 14, blue, true))
        countText = label("আজ উপস্থিত: ০ জন", 22, blue, true)
        summary.addView(countText)

        val scannerCard = card(page)
        scannerCard.addView(label("QR কোড স্ক্যানার", 18, 0xFF172033.toInt(), true))
        val scanButton = button("📷  QR স্ক্যান করুন")
        scanButton.setOnClickListener {
            try {
                val integrator = IntentIntegrator(this)
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                integrator.setPrompt("শিক্ষার্থীর QR কোড স্ক্যান করুন")
                integrator.setBeepEnabled(true)
                integrator.setOrientationLocked(false)
                integrator.initiateScan()
            } catch (e: Exception) {
                showStatus("স্ক্যানার চালু করা যায়নি: ${e.localizedMessage}")
            }
        }
        scannerCard.addView(scanButton, matchWrap())
        statusText = label("স্ক্যান করলে নাম/রোল উপস্থিতির তালিকায় যোগ হবে।", 13, 0xFF36516F.toInt())
        scannerCard.addView(statusText)

        val manual = card(page)
        manual.addView(label("নাম হাতে যোগ করুন", 18, 0xFF172033.toInt(), true))
        val input = EditText(this).apply {
            hint = "শিক্ষার্থীর নাম বা রোল"
            singleLine = true
            textSize = 16f
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        manual.addView(input, matchWrap())
        val add = button("প্রেজেন্ট করুন")
        add.setOnClickListener {
            val value = input.text.toString().trim()
            if (value.isBlank()) showStatus("আগে নাম বা রোল লিখুন।")
            else {
                if (db.add(value)) showStatus("$value — উপস্থিতি সংরক্ষিত হয়েছে।")
                else showStatus("$value আজ আগেই প্রেজেন্ট করা হয়েছে।")
                input.text.clear()
                refresh()
            }
        }
        manual.addView(add, matchWrap())

        val today = card(page)
        today.addView(label("আজকের উপস্থিতির তালিকা", 18, 0xFF172033.toInt(), true))
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        today.addView(listBox, matchWrap())

        val tools = card(page)
        tools.addView(label("রিপোর্ট ও পুরোনো উপস্থিতি", 18, 0xFF172033.toInt(), true))
        val history = button("📅  তারিখ অনুযায়ী উপস্থিতি দেখুন")
        history.setOnClickListener { chooseDate() }
        tools.addView(history, matchWrap())
        val all = button("সব দিনের রেকর্ড দেখুন").apply { setBackgroundColor(0xFF36516F.toInt()) }
        all.setOnClickListener { showAllRecords() }
        tools.addView(all, matchWrap())
        val export = button("⬇  CSV রিপোর্ট শেয়ার / সেভ")
        export.setOnClickListener { exportCsv() }
        tools.addView(export, matchWrap())
        tools.addView(label("ডেটা এই অ্যাপের SQLite ডেটাবেসে থাকে। অ্যাপ বন্ধ বা ফোন রিস্টার্ট হলেও থাকবে। অ্যাপ আনইনস্টল করলে ডেটা মুছে যাবে—CSV রিপোর্ট নিয়মিত সেভ রাখুন।", 12, 0xFF64748B.toInt()))
    }

    private fun card(parent: LinearLayout): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFFFFFFFF.toInt())
            elevation = dp(2).toFloat()
        }
        val params = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        parent.addView(box, params)
        return box
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun button(text: String) = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 15f
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun refresh() {
        val rows = db.today()
        countText.text = "আজ উপস্থিত: ${rows.size} জন"
        listBox.removeAllViews()
        if (rows.isEmpty()) {
            listBox.addView(label("এখনো কেউ উপস্থিত হয়নি।", 14, 0xFF64748B.toInt()))
        } else {
            rows.asReversed().forEachIndexed { index, row ->
                val line = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(7), 0, dp(7))
                }
                val name = label("${index + 1}.  ${row.student}", 15, 0xFF172033.toInt(), true)
                line.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
                line.addView(label(row.time, 12, 0xFF64748B.toInt()))
                listBox.addView(line)
                val divider = android.view.View(this).apply { setBackgroundColor(0xFFE9EEF5.toInt()) }
                listBox.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
            }
        }
    }

    private fun showStatus(message: String) {
        statusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun parseQr(raw: String): String {
        val value = raw.trim()
        // Known student QR mapping supplied for this project.
        val knownStudents = mapOf(
            "8526284" to "Mushfiqur Rahman Siam (8526284)"
        )
        knownStudents[value]?.let { return it }
        return try {
            val obj = JSONObject(value)
            obj.optString("name").takeIf { it.isNotBlank() }
                ?: obj.optString("studentName").takeIf { it.isNotBlank() }
                ?: obj.optString("student_name").takeIf { it.isNotBlank() }
                ?: obj.optString("roll").takeIf { it.isNotBlank() }
                ?: obj.optString("id").takeIf { it.isNotBlank() }
                ?: value
        } catch (_: Exception) { value }
    }

    @Deprecated("Deprecated in Android, retained for ZXing callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            val contents = result.contents
            if (contents == null) showStatus("স্ক্যান বাতিল করা হয়েছে।")
            else {
                val student = parseQr(contents).take(160)
                if (db.add(student)) showStatus("$student — উপস্থিতি সংরক্ষিত হয়েছে।")
                else showStatus("$student আজ আগেই প্রেজেন্ট করা হয়েছে।")
                refresh()
            }
            return
        }
        if (requestCode == 7202 && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(csvText().toByteArray(Charsets.UTF_8)) }
                    Toast.makeText(this, "CSV রিপোর্ট সেভ হয়েছে।", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "ফাইল সেভ করা যায়নি।", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun displayDate(day: String): String {
        val parts = day.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else day
    }

    private fun chooseDate() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val day = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            showDay(day)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showDay(day: String) {
        val rows = db.byDay(day)
        val text = buildString {
            append("${displayDate(day)} — মোট ${rows.size} জন\n\n")
            rows.forEachIndexed { i, r -> append("${i + 1}. ${r.student}   ${r.time}\n") }
            if (rows.isEmpty()) append("এই তারিখে কোনো উপস্থিতি নেই।")
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("উপস্থিতির রেকর্ড")
            .setMessage(text)
            .setPositiveButton("বন্ধ", null)
            .show()
    }

    private fun showAllRecords() {
        val rows = db.all()
        val text = if (rows.isEmpty()) "এখনো কোনো রেকর্ড নেই।" else buildString {
            rows.forEach { append("${displayDate(it.day)} | ${it.student} | ${it.time}\n") }
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("সব উপস্থিতির রেকর্ড")
            .setMessage(text)
            .setPositiveButton("বন্ধ", null)
            .show()
    }

    private fun csvText(): String {
        val rows = db.all()
        val out = StringBuilder("\uFEFFতারিখ,নাম/রোল,সময়\n")
        rows.forEach {
            fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
            out.append("${q(it.day)},${q(it.student)},${q(it.time)}\n")
        }
        return out.toString()
    }

    private fun exportCsv() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "class-attendance.csv")
        }
        startActivityForResult(intent, 7202)
    }
}
