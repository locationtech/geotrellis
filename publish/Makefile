IMG  := geotrellis/publish-geotrellis-container
TAG  := latest
RELEASE_TAG := 2.0

build: Dockerfile
	docker build \
	--build-arg RELEASE_TAG=$(RELEASE_TAG) \
	--no-cache -t ${IMG}:${TAG} .

run:
	docker run -it --rm \
	-v ~/.ivy2/.credentials:/home/geotrellis/.ivy2/.credentials \
	-v ~/.gnupg:/home/geotrellis/.gnupg \
	$(IMG):$(TAG) /bin/bash

publish:
	docker run -it $(IMG):$(TAG) publish/publish-to-sonatype.sh
