import sbt._
import sbt.Keys._
import sbt.io.IO
import sbt.plugins.DependencyTreeKeys.dependencyList
import sbt.plugins.DependencyTreeSettings.targetFileAndForceParser
import sbt.plugins.MiniDependencyTreeKeys.{asString, toFile}
import java.io.File

/**
  * dependencyList command generates a file with a list of published artifacts dependencies only.
  *
  * Usage example: dependencyListGT/toFile target/dependencies-list.txt
  */
object DependencyListPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object Keys {
    val dependencyListGT       = inputKey[Unit]("Execute dependencyList GeoTrellis command; usage example: dependencyListGT/toFile target/dependencies-list.txt")
    val dependencyListGTAppend = settingKey[Boolean]("dependencyList GeoTrellis command append mode: true by default").withRank(KeyRanks.Invisible)
    val dependencyListGTIgnore = settingKey[Seq[String]]("dependencyList GeoTrellis command ignored dependencies: skips dependencies containing ignored strings in the output file").withRank(KeyRanks.Invisible)
  }

  val autoImport = Keys
  import autoImport._

  override def projectSettings = renderingTaskSettings(dependencyListGT)

  private def renderingTaskSettings(key: InputKey[Unit]) =
    Seq(
      dependencyListGTAppend := true,
      dependencyListGTIgnore := Seq("locationtech"),
      key := {
        val s = streams.value
        val str = (Compile / dependencyList / asString).value
        s.log.info(str)
      },
      key / toFile := {
        val (targetFile, force) = targetFileAndForceParser.parsed
        val list = if(!(publish / skip).value) { // generate a list of published artifacts dependencies only
          val string =
            (Compile / dependencyList / asString)
              .value
              .split('\n')
              .filterNot(str => dependencyListGTIgnore.value.exists(str.contains))
              .mkString("\n")

          if (string.nonEmpty) string ++ "\n" else string
        } else ""

        writeToFile(key.key.label, list, targetFile, force, streams.value, dependencyListGTAppend.value)
      },
    )

  private def writeToFile(
    what: String,
    data: String,
    targetFile: File,
    force: Boolean,
    streams: TaskStreams,
    append: Boolean
  ): File =
    if (targetFile.exists && !force && !append) {
      throw new RuntimeException(
        s"Target file for $what already exists at ${targetFile.getAbsolutePath}. Use '-f' to override"
      )
    } else {
      IO.write(targetFile, data, IO.utf8, append)

      streams.log.info(s"Wrote $what to '$targetFile'")
      targetFile
    }
}
