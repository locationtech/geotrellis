import sbt.*
import sbt.Keys.*
import com.github.sbt.sbom.BomSbtPlugin
import com.github.sbt.sbom.BomSbtPlugin.autoImport.*

/**
  * CycloneDX SBOM generation for published artifacts only.
  *
  * makeBomGT writes a JSON SBOM per published module into target/sbom of the root project;
  * modules with publish / skip are ignored.
  *
  * Usage example: makeBomGT
  */
object SbomPlugin extends AutoPlugin {
  override def requires = BomSbtPlugin
  override def trigger = allRequirements

  object Keys {
    // the SBOM files are written as a side effect, sbt 2 cannot cache File outputs
    @transient val makeBomGT = taskKey[Option[File]]("Generate CycloneDX SBOMs of published artifacts into target/sbom")
  }

  val autoImport = Keys
  import autoImport.*

  // bomOutputPath is read inside a dynamic task, which sbt lint does not see
  override def globalSettings = Seq(excludeLintKeys += bomOutputPath)

  override def projectSettings = Seq(
    bomFormat := "json",
    // sbt 2 target is target/out/jvm/scala-<version>/<project>, keep the SBOMs in the repo root target/sbom
    bomOutputPath := ((LocalRootProject / baseDirectory).value / "target" / "sbom").getAbsolutePath,
    makeBomGT := Def.taskIf {
      if ((publish / skip).value) None
      else Some(makeBom.value)
    }.value
  )
}
