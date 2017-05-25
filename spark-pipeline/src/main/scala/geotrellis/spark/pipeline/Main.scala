package geotrellis.spark.pipeline

import java.io.File

import org.clapper.classutil._
import grizzled.file.Implicits._
import grizzled.file.{util => fileutil}

object Main {
  private val (runtimeClassFiles, runtimeClassFinder) = {
    import scala.util.Properties
    val version = Properties.releaseVersion.get
    val shortVersion = version.split("""\.""").take(2).mkString(".")

    val targetDirectory: Option[File] = Array(
      fileutil.joinPath("spark-pipeline/target", s"scala-$version"),
      fileutil.joinPath("spark-pipeline/target", s"scala-$shortVersion")
    )
      .map(new File(_))
      .find(_.exists)

    assert(targetDirectory.isDefined)
    val dir = targetDirectory.get

    // SBT-dependent paths
    val classDir = new File(fileutil.joinPath(dir.getPath, "classes"))
    val testClassDir = new File(fileutil.joinPath(dir.getPath, "test-classes"))

    // Get class files under the directory.
    val classFiles = classDir.listRecursively()
      .filter(_.getName.endsWith(".class"))
      .toVector
    val testClassFiles = testClassDir.listRecursively()
      .filter(_.getName.endsWith(".class"))
      .toVector

    // The number of returned classInfo objects should be the same number
    // as the number of class files.
    val allClassFiles = classFiles ++ testClassFiles
    val finder = ClassFinder(Seq(classDir, testClassDir))
    assert(finder.getClasses().size == allClassFiles.length)
    (allClassFiles, finder)
  }

  def main(args: Array[String]): Unit = {
    //val classes = runtimeClassFinder.getClasses()

    //classes.take(1).foreach(println)
    //val zz = ClassFinder.concreteSubclasses("geotrellis.spark.pipeline.function.TileCellTypeChange", classes)



    //println("========")
    //zz.foreach(println)

    //val res: ClassInfo = zz.toList.head
    //res.methods.filter()
    //val classes = finder.getClasses // classes is an Iterator[ClassInfo]
    //classes.foreach(println)
    //println("========")

    //Class.forName("geotrellis.spark.pipeline.function.TileCellTypeChange")

    //ClassUtil


  }

}
