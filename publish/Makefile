IMG  := geotrellis/publish-geotrellis-container
TAG  := "latest"

build: Dockerfile
	@cp ~/.ivy2/.credentials .credentials
	@mkdir -p gnupg
	@cp ~/.gnupg/*.conf gnupg
	@cp ~/.gnupg/*.gpg gnupg
	docker build --no-cache -t ${IMG}:${TAG} --env GEOTRELLIS_VERSION_SUFFIX="" --build-arg BRANCH=1.2 .
	@rm .credentials
	@rm -r ./gnupg

run:
	docker run -it ${IMG}:${TAG} /bin/bash

publish:
	docker run -it ${IMG}:${TAG} publish/publish-to-sonatype.sh
