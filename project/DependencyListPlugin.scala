import sbt.*
import sbt.Keys.*
import sbt.complete.DefaultParsers.*
import sbt.complete.Parser
import sbt.io.IO
import sbt.plugins.DependencyTreePlugin.autoImport.dependencyTree
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
    val dependencyListGTIgnore = settingKey[Set[String]]("dependencyList GeoTrellis command ignored dependencies: skips dependencies containing ignored strings in the output file").withRank(KeyRanks.Invisible)
    // sbt 2 dropped the dependencyList / asString / toFile keys of the sbt 1 dependency tree plugin
    val toFile                 = inputKey[File]("Writes the task output to a file; usage: <task>/toFile [-f] <target-file>")
  }

  val autoImport = Keys
  import autoImport.*

  override def projectSettings = renderingTaskSettings(dependencyListGT)

  // sbt 1 DependencyTreeSettings.targetFileAndForceParser, which is private[sbt] in sbt 2
  private val targetFileAndForceParser: Parser[(File, Boolean)] =
    (Space ~> fileParser(new File("."))) ~ (Space ~> (literal("-f") | literal("--force"))).?.map(_.isDefined)

  // sbt 1 dependencyList / asString: the flat list of the non evicted Compile modules
  private val dependencyListAsString = (Compile / dependencyTree).toTask(" list --quiet")

  private def renderingTaskSettings(key: InputKey[Unit]) =
    Seq(
      dependencyListGTAppend := true,
      dependencyListGTIgnore := Set("locationtech"),
      key := {
        val s = streams.value
        val str = dependencyListAsString.value
        s.log.info(str)
      },
      key / toFile := {
        val (targetFile, force) = targetFileAndForceParser.parsed
        val list = if(!(publish / skip).value) { // generate a list of published artifacts dependencies only
          // provided-scope modules (and their transitive closure) to drop from the published list
          val providedModules =
            update.value.configurations
              .find(_.configuration.name == "provided")
              .toSeq
              .flatMap(_.modules)
              .map(r => s"${r.module.organization}:${r.module.name}:${r.module.revision}")
              .toSet

          val string =
            dependencyListAsString
              .value
              .split('\n')
              .filterNot(str => dependencyListGTIgnore.value.exists(str.contains))
              .filterNot(str => providedModules.contains(str.trim))
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
    // the modules write the same file from parallel tasks; sync the writers to avoid overwrites
    DependencyListPlugin.synchronized {
      if (targetFile.exists && !force && !append) {
        throw new RuntimeException(
          s"Target file for $what already exists at ${targetFile.getAbsolutePath}. Use '-f' to override"
        )
      } else {
        // in append mode each module contributes its own list; merge with the existing content and keep
        // the file distinct (and sorted), which also makes re-runs idempotent
        val existing = if (append && targetFile.exists) IO.readLines(targetFile, IO.utf8) else Nil
        val merged = (existing ++ data.split('\n')).map(_.trim).filter(_.nonEmpty).distinct.sorted
        // write to a sibling file and move it over, so the target never holds a partial list
        val tmp = new File(targetFile.getAbsolutePath + ".tmp")
        IO.writeLines(tmp, merged, IO.utf8, append = false)
        IO.move(tmp, targetFile)

        streams.log.info(s"Wrote $what to '$targetFile'")
        targetFile
      }
    }
}
