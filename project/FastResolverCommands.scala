import sbt._
import sbt.Keys._

/**
  * Commands that run resolution-heavy tasks with the snapshot resolvers swapped out for speed.
  *
  * Snapshot repos re-validate version metadata on every `update`, which dominates the runtime of
  * `dependencyUpdates` / `dependencyList` across all modules. Each command swaps in the snapshot-free
  * resolver set, runs the task, then `reload`s to restore the regular resolvers.
  */
object FastResolverCommands extends AutoPlugin {
  override def trigger = allRequirements

  override def globalSettings: Seq[Setting[_]] =
    // Faster `dependencyUpdates`.
    addCommandAlias(
      "dependencyUpdatesFast",
      "; set every externalResolvers := Settings.Repositories.allNoSnapshots; dependencyUpdates; reload"
    ) ++ Seq(
      // Faster `dependencyListGT/toFile`; the target file is passed through, defaults to target/dependencies-list.txt.
      commands += Command.args("dependencyListGTFast", "<target-file>") { (state, args) =>
        val target = if (args.isEmpty) "target/dependencies-list.txt" else args.mkString(" ")
        "set every externalResolvers := Settings.Repositories.allNoSnapshots" ::
        s"dependencyListGT/toFile $target" ::
        "reload" ::
        state
      }
    )
}
