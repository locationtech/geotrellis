IMG  := geotrellis/publish-geotrellis-container
TAG  := "latest"

build: Dockerfile
	@cp ~/.ivy2/.credentials .credentials
	@cp ~/.sbt/0.13/local.sbt local.sbt
	@cp ~/.sbt/0.13/plugins/plugins.sbt plugins.sbt
	@cp -r ~/.gnupg ./gnupg
	docker build --no-cache -t ${IMG}:${TAG} .
	@rm .credentials
	@rm -r ./gnupg
	@rm local.sbt

run:
	docker run -it ${IMG}:${TAG} /bin/bash

publish:
	docker run -it ${IMG}:${TAG} publish/publish-to-sonatype.sh
