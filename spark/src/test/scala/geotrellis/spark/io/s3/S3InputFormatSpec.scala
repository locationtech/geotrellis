package geotrellis.spark.io.s3

import org.scalatest._

class S3InputFormatSpec extends FunSpec
  with Matchers
{
  describe("S3 InputFormat") {

    it("should parse the s3 url containing keys") {
      // don't get too excited, not real keys
      val url = "s3n://AAIKJLIB4YGGVMAATT4A:ZcjWmdXN+75555bptjE4444TqxDY3ESZgeJxGsj8@nex-bcsd-tiled-geotiff/prefix/subfolder"
      val S3InputFormat.S3UrlRx(id,key,bucket,prefix) = url
      
      id should be ("AAIKJLIB4YGGVMAATT4A")
      key should be ("ZcjWmdXN+75555bptjE4444TqxDY3ESZgeJxGsj8")
      bucket should be ("nex-bcsd-tiled-geotiff")
      prefix should be ("prefix/subfolder")      
    }

    it("should parse s3 url without keys"){
      val url = "s3n://nex-bcsd-tiled-geotiff/prefix/subfolder"      
      val  S3InputFormat.S3UrlRx(id,key,bucket,prefix) = url
      
      id should be (null)
      key should be (null)
      bucket should be ("nex-bcsd-tiled-geotiff")
      prefix should be ("prefix/subfolder")  
    }
  }
}