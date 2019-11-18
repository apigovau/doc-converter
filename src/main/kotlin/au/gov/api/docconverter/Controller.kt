package au.gov.api.docconverter

import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


@RestController
class Controller {
    @CrossOrigin
    @RequestMapping("/pandoc")
    fun pandoc(@RequestParam format: String, @RequestParam(defaultValue = "gfm") toFormat: String,
               @RequestParam(defaultValue = "true") tryExtractImages: Boolean, @RequestBody x: ByteArray): String {
        val md5 = hashString("MD5", x.toString())
        File("$md5.$format").writeBytes(x)

        val f = File("${md5}.$toFormat")
        if (!f.isFile()) {

            val progName = if (System.getProperty("os.name").toLowerCase().contains("windows")) "pandoc" else "./pandoc"
            val command = "$progName -f $format -t $toFormat -o ${md5}.$toFormat $md5.$format"
            val p = Runtime.getRuntime().exec(command)
            p.waitFor()
        }
        var svg = File("${md5}.$toFormat").readText()

        if (tryExtractImages && (toFormat == "gfm" || toFormat == "markdown")) {
            if (format == "docx") {
                val images = UnzipFile.getImagesFromDocx("$md5.$format")
                images.forEach {
                    svg = svg.replace("media/${it.key}", it.value)
                }
            }
        }
        try {
            if (true) {
                //Delete the temp files
                File("${md5}.$toFormat").delete()
                File("${md5}.$format").delete()
            }
        } catch (e: Exception) {
            println("Filed to delete the following files:")
            println("${md5}.$toFormat")
            println("${md5}.$format")
        }
        return svg
    }

    private fun hashString(type: String, input: String) =
            MessageDigest
                    .getInstance(type)
                    .digest(input.toByteArray())
                    .map { String.format("%02X", it) }
                    .joinToString(separator = "")

}

object UnzipFile {
    @JvmStatic
    fun getImagesFromDocx(file: String): HashMap<String, String> {
        var output = hashMapOf<String, String>()
        val fileZip = file
        val zis = ZipInputStream(FileInputStream(fileZip))
        var zipEntry: ZipEntry? = zis.nextEntry
        val size = 1024
        while (zipEntry != null) {
            if (zipEntry.name.startsWith("word/media/")) {
                var f = mutableListOf<ByteArray>()
                var data = ByteArray(size)
                var readFile = false
                var x: Int = 0
                var lastLen = x
                while (!readFile) {
                    lastLen = x
                    x = zis.read(data, 0, size)
                    if (x != -1) {
                        f.add(data.clone())
                    } else {
                        readFile = true
                    }
                }
                zis.closeEntry()
                val fileLen = ((f.count() - 1) * size) + lastLen
                var file = ByteArray(0)
                f.forEach { file = file.plus(it) }
                file = file.take(fileLen).toByteArray()
                var base64im = java.util.Base64.getEncoder().encodeToString(file)
                val key = zipEntry.name.split('/').last()
                val ext = key.split('.').last()
                output.put(key, "data:image/$ext;base64,$base64im")
                println(base64im)
            }
            zipEntry = zis.nextEntry
        }
        zis.close()
        return output
    }
}