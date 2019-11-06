package au.gov.api.docconverter

import org.springframework.web.bind.annotation.*
import java.io.File
import java.security.MessageDigest

@RestController
class Controller {

    @CrossOrigin
    @RequestMapping("/pandoc")
    fun pandoc(@RequestParam format:String, @RequestParam(defaultValue = "gfm") toFormat: String, @RequestBody x:ByteArray): String{
        val md5= hashString("MD5",x.toString())
        File("$md5.$format").writeBytes(x)

        val f =  File("${md5}.$toFormat")
        if (!f.isFile()) {
            val p = Runtime.getRuntime().exec("./pandoc -f $format -t $toFormat -o ${md5}.$toFormat $md5.$format")
            p.waitFor()
        }

        var svg = File("${md5}.$toFormat").readText()
        return svg

    }

    private fun hashString(type: String, input: String) =
            MessageDigest
                    .getInstance(type)
                    .digest(input.toByteArray())
                    .map { String.format("%02X", it) }
                    .joinToString(separator = "")

}
