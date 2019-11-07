package au.gov.api.docconverter

import com.sun.org.apache.xpath.internal.operations.Bool
import org.springframework.security.crypto.codec.Base64
import org.springframework.web.bind.annotation.*
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream



@RestController
class Controller {

    @CrossOrigin
    @RequestMapping("/pandoc")
    fun pandoc(@RequestParam format:String, @RequestParam(defaultValue = "gfm") toFormat: String,
               @RequestParam(defaultValue = "true") tryExtractImages: Boolean, @RequestBody x:ByteArray): String{
        val md5= hashString("MD5",x.toString())
        File("$md5.$format").writeBytes(x)

        val f =  File("${md5}.$toFormat")
        if (!f.isFile()) {

            //val p = Runtime.getRuntime().exec("pandoc -f $format -t $toFormat -o ${md5}.$toFormat $md5.$format") //For windows dev
            val p = Runtime.getRuntime().exec("./pandoc -f $format -t $toFormat -o ${md5}.$toFormat $md5.$format")
            p.waitFor()
        }
        var svg = File("${md5}.$toFormat").readText()

        if(tryExtractImages && (toFormat == "gfm" || toFormat =="markdown"))
        {
            if(format=="docx")
            {
                val images = UnzipFile.getImagesFromDocx("$md5.$format")
                images.forEach {
                    svg = svg.replace("media/${it.key}",it.value)
                }
            }
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
    fun getImagesFromDocx(file:String) : HashMap<String,String> {
        var output = hashMapOf<String,String>()
        val fileZip = file
        val zis = ZipInputStream(FileInputStream(fileZip))
        var zipEntry: ZipEntry? = zis.nextEntry
        val size = 1024
        while (zipEntry != null)
        {
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
                output.put(key,"data:image/$ext;base64,$base64im")
                println(base64im)
            }
            zipEntry = zis.nextEntry
        }
        zis.close()
        return output
    }
}