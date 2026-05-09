package com.college.bustracker

import android.content.ContentValues
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.college.bustracker.model.BusLog
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var etDate: EditText
    private lateinit var btnFetchLogs: Button
    private lateinit var btnDownloadPdf: Button
    private lateinit var btnBrakeReport: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BusLogAdapter

    private val logList = ArrayList<BusLog>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        etDate = findViewById(R.id.etDate)
        btnFetchLogs = findViewById(R.id.btnFetchLogs)
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)
        btnBrakeReport = findViewById(R.id.btnBrakeReport)
        recyclerView = findViewById(R.id.recyclerViewLogs)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BusLogAdapter(logList)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("busLogs")

        btnFetchLogs.setOnClickListener {
            val date = etDate.text.toString().trim()
            if (date.isEmpty()) {
                Toast.makeText(this, "Enter date in yyyy-MM-dd", Toast.LENGTH_SHORT).show()
            } else {
                fetchLogs(date)
            }
        }

        btnDownloadPdf.setOnClickListener {
            if (logList.isEmpty()) {
                Toast.makeText(this, "No logs to export", Toast.LENGTH_SHORT).show()
            } else {
                createPdfReport()
            }
        }

        btnBrakeReport.setOnClickListener {
            startActivity(Intent(this, BrakeReportActivity::class.java))
        }
    }

    private fun fetchLogs(date: String) {
        database.child(date).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                logList.clear()

                for (child in snapshot.children) {
                    val log = child.getValue(BusLog::class.java)
                    if (log != null) {
                        logList.add(log)
                    }
                }

                adapter.notifyDataSetChanged()

                if (logList.isEmpty()) {
                    Toast.makeText(this@AdminDashboardActivity, "No logs found", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AdminDashboardActivity, "Logs loaded", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminDashboardActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createPdfReport() {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1200, 2000, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 32f
        paint.isFakeBoldText = true
        canvas.drawText("College Bus Entry Report", 40f, 60f, paint)

        paint.textSize = 24f
        paint.isFakeBoldText = false
        var y = 120f

        canvas.drawText("Bus ID", 40f, y, paint)
        canvas.drawText("Driver ID", 220f, y, paint)
        canvas.drawText("Gate", 430f, y, paint)
        canvas.drawText("Time", 650f, y, paint)
        canvas.drawText("Date", 850f, y, paint)

        y += 40f

        for (log in logList) {
            canvas.drawText(log.busId ?: "", 40f, y, paint)
            canvas.drawText(log.driverId ?: "", 220f, y, paint)
            canvas.drawText(log.gateName ?: "", 430f, y, paint)
            canvas.drawText(log.time ?: "", 650f, y, paint)
            canvas.drawText(log.date ?: "", 850f, y, paint)
            y += 35f

            if (y > 1900f) {
                break
            }
        }

        document.finishPage(page)

        try {
            val fileName = "BusLogReport_${System.currentTimeMillis()}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }

                    Toast.makeText(this, "PDF saved in Downloads", Toast.LENGTH_LONG).show()
                    openPdf(uri)
                } else {
                    Toast.makeText(this, "Failed to create PDF file", Toast.LENGTH_SHORT).show()
                }

            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val file = File(downloadDir, fileName)
                document.writeTo(FileOutputStream(file))

                Toast.makeText(this, "PDF saved in Downloads: ${file.name}", Toast.LENGTH_LONG).show()

                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )
                openPdf(uri)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "PDF error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
        }
    }

    private fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No PDF viewer found. File saved in Downloads.", Toast.LENGTH_LONG).show()
        }
    }
}