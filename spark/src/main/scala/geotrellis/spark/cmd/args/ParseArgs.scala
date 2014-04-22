package geotrellis.spark.cmd.args

trait ArgsParser {
  def parseArgs(args: String): Seq[(String, String)] = {
    val l = for (argval <- args.split(";")) yield { val t = argval.split("="); (t(0), t(1)) }
    l
  }  
}