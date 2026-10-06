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
    val makeBomGT = taskKey[Option[File]]("Generate CycloneDX SBOMs of published artifacts into target/sbom")
  }

  val autoImport = Keys
  import autoImport.*

  // bomOutputPath is read inside a dynamic task, which sbt lint does not see
  override def globalSettings = Seq(excludeLintKeys += bomOutputPath)

  override def projectSettings = Seq(
    bomFormat := "json",
    bomOutputPath := ((LocalRootProject / target).value / "sbom").getAbsolutePath,
    makeBomGT := Def.taskIf {
      if ((publish / skip).value) None
      else Some(makeBom.value)
    }.value
  )
}
