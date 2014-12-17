package geotrellis.spark.op.local.temporal

class LocalTemporalSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {

  ifCanRunSpark {

    describe("Local Temporal Operations") {

      it("should work for years") {

      }
    }

  }

}
