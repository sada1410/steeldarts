package de.steeldeers.app.data.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.util.FileUtil
import org.jetbrains.anko.getStackTraceString
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.nio.file.FileSystem

class XmlDownloader() {
    lateinit var url : String
    lateinit var fileName: String
    lateinit var context: Context

    constructor( url: String, fileName: String, context: Context ) : this() {
        this.url = url
        this.fileName = fileName
        this.context = context
    }

    @Throws(IOException::class)
    fun filesCompareByByte(file1: File, file2: File): Long {
        BufferedInputStream(FileInputStream(file1)).use { fis1 ->
            BufferedInputStream(FileInputStream(file2)).use { fis2 ->
                var ch = 0
                var pos: Long = 1
                while (fis1.read().also { ch = it } != -1) {
                    if (ch != fis2.read()) {
                        return pos
                    }
                    pos++
                }
                return if (fis2.read() == -1) {
                    -1
                } else {
                    pos
                }
            }
        }
    }

    public fun download() : Boolean {
        var success : Boolean

        var urlToOpen = URL( url )
        var connection = urlToOpen.openConnection()

        try {
            connection.connect()
            var inputStream = BufferedInputStream( urlToOpen.openStream() )
            var outputStream = FileOutputStream(    context.getDir("data",Context.MODE_PRIVATE).toString() + fileName + ".tmp")
            var total = 0
            var count = 0
            var data = ByteArray(1024)
            while(count != -1)  {
                count = inputStream.read(data)
                total += count
                if( count != -1 ) {
                    outputStream.write(data, 0, count)
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            var file = File( context.getDir("data",Context.MODE_PRIVATE).toString() + fileName )
            if( file.exists() )  {
                Log.d(fileName, "exists!")
                var tmpFile = File( context.getDir("data",Context.MODE_PRIVATE).toString() + fileName + ".tmp")

                if( filesCompareByByte(file,tmpFile) == -1L) {
                    Log.d(fileName, "file is up-to-date!")
                }
                else {
                    Log.d(fileName, "update is required")
                    file.delete()
                    tmpFile.renameTo(file)
                }
            }
            else  {
                Log.w(fileName, "not exists!")
                var tmpFile = File( context.getDir("data",Context.MODE_PRIVATE).toString() + fileName + ".tmp")
                tmpFile.renameTo( file )
                tmpFile.delete()
            }

            success = true
        } catch (e: Exception) {
            Log.e("Error: ", e.getStackTraceString())
            success = false
        }


        return success
    }

}