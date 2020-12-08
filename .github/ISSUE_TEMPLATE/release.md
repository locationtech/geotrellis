---
name: Release
about: Release checklist for maintainers
title: 'Release X.Y.Z'
labels: ''
assignees: ''

---

- [ ] Check out the `master` branch and ensure it contains all commits to be released
- [ ] Update CHANGELOG.md, version.sbt, and the `version` and `release` fields in `docs/conf.py` to the release version as a single commit to `master`. [Example](https://github.com/locationtech/geotrellis/commit/e8fbbe2668aa0e99b7a631e9dc7802bc270c2bda)
- [ ] Create a new tag at the previous commit with `git tag -a vX.Y.Z -m "vX.Y.Z"`
- [ ] Push the tag to `locationtech/geotrellis` with `git push --tags`
- [ ] Follow the instructions in the setup section of [publish/README.md](https://github.com/locationtech/geotrellis/blob/master/publish/README.md#setup) to ensure that your local environment is configured for publishing the release
- [ ] Set the `RELEASE_TAG` variable in `./publish/Makefile` to `vX.Y.Z`, substituting x/y/z for the current release version
- [ ] **Before Continuing:** Ensure you have pushed the tag you created in step 3 before continuing. Otherwise the next step will fail.
- [ ] Execute `make build` in `./publish` to build the release container
- [ ] Execute `make publish` in `./publish` to build and push release artifacts to Sonatype. This will take an hour or so.
- [ ] Go to https://oss.sonatype.org and follow the [Releasing the Deployment](https://central.sonatype.org/pages/releasing-the-deployment.html) instructions to actually publish the release to Maven. Before clicking "publish" ensure that artifacts were generated for each scala version + geotrellis package combination supported by the release.
- [ ] Update `version.sbt` with the new SNAPSHOT version to begin publishing and add an empty `[Unreleased]` section to the CHANGELOG.md. Commit as as a single commit directly to the `master` branch. Typically you only need to increment the bugfix version. [Example](https://github.com/locationtech/geotrellis/commit/be47659e533f771bf9ffba54d59fca3cdcb4bf16)
- [ ] Push the previous commit to `locationtech/geotrellis` directly.
- [ ] Create a new [GitHub Release](https://github.com/locationtech/geotrellis/releases/new). Include the CHANGELOG entries for the release in the release notes, and state if the release breaks binary compatibility. [Example](https://github.com/locationtech/geotrellis/releases/tag/v3.4.0)
- [ ] Once the release is merged to master, log in to the Azavea readthedocs.org account and select the GeoTrellis project. Go to the Builds tab, find the Build Version button, ensure that the associated dropdown is set to "latest", and click the button. Within a few minutes, master should build and the docs site should be updated at https://geotrellis.readthedocs.io/en/latest/
