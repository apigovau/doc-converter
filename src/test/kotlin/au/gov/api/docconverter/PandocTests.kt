package au.gov.api.docconverter

import org.junit.Assert
import org.junit.Test
import java.io.File

class PandocTests {
    @Test
    fun can_convert_docx_to_gfm() {
        var controller:Controller = Controller()

        //Read the test file
        val byArray = File("./Test.docx").readBytes()
        var x = controller.pandoc("docx","gfm",true,byArray)
        Assert.assertTrue(x.startsWith("# Header 1"))
        Assert.assertTrue(x.contains("## Header 2"))
        Assert.assertTrue(x.contains("### Header 3"))
        Assert.assertTrue(x.contains("#### Header 4"))
        Assert.assertTrue( x.contains("data:image/jpeg;base64"))


    }
}