GEOTRELLIS_VERSION ?= 1.2.0
GEOTRELLIS_VERSION_SUFFIX ?= -SNAPSHOT
ASSEMBLY := ../../spark-etl/target/scala-2.11/geotrellis-spark-etl-assembly-${GEOTRELLIS_VERSION}${GEOTRELLIS_VERSION_SUFFIX}.jar

${ASSEMBLY}: $(call rwildcard, ../../spark-etl/src, *.scala) ../../spark-etl/build.sbt ../../build.sbt
	cd ../../; ./sbt spark-etl/assembly -no-colors
	@touch -m ${ASSEMBLY}

ifndef CLUSTER_ID
CLUSTER_ID=$(shell cat terraform/terraform.tfstate | jq -r .modules[].resources[\"aws_emr_cluster.emr-spark-cluster\"].primary.id)
endif
ifndef KEY_PAIR_FILE
ifndef TF_VAR_pem_path
KEY_PAIR_FILE=$(shell cat terraform/variables.tf.json | jq -r ".variable.pem_path.default")
else
KEY_PAIR_FILE=${TF_VAR_pem_path}
endif
endif

ifndef TF_VAR_user
export TF_VAR_user=${USER}
endif

terraform-init:
	cd terraform; terraform init

create-cluster:
	cd terraform; TF_VAR_s3_notebook_bucket="blah" TF_VAR_s3_notebook_prefix="blah" terraform apply

create-jupyter-cluster:
	cd terraform ; TF_VAR_install_jupyter="true" terraform apply

destroy-cluster:
	cd terraform; TF_VAR_s3_notebook_bucket="blah" TF_VAR_s3_notebook_prefix="blah" terraform destroy

proxy:
	cd terraform; aws emr socks --cluster-id ${CLUSTER_ID} --key-pair-file ${KEY_PAIR_FILE}

ssh:
	cd terraform; aws emr ssh --cluster-id ${CLUSTER_ID} --key-pair-file ${KEY_PAIR_FILE}

cleanup-zeppelin:
	cd terraform; aws emr ssh --cluster-id ${CLUSTER_ID} --key-pair-file ${KEY_PAIR_FILE} \
	--command 'rm -r /usr/lib/zeppelin/local-repo/*/geotrellis*'

restart-zeppelin:
	cd terraform; aws emr ssh --cluster-id ${CLUSTER_ID} --key-pair-file ${KEY_PAIR_FILE} \
	--command 'sudo restart zeppelin'

stop-zeppelin:
	cd terraform; aws emr ssh --cluster-id ${CLUSTER_ID} --key-pair-file ${KEY_PAIR_FILE} \
	--command 'sudo stop zeppelin'

start-zeppelin:
	cd terraform; aws emr ssh --cluster-id ${CLUSTER_ID} --key-pair-file ${KEY_PAIR_FILE} \
	--command 'sudo start zeppelin'

upload-assembly: ${ASSEMBLY}
	cd terraform; aws emr put --cluster-id ${CLUSTER_ID} --key-pair-file ${KEY_PAIR_FILE} \
	--src ../${ASSEMBLY} --dest /tmp/geotrellis-spark-etl-assembly-${GEOTRELLIS_VERSION}${GEOTRELLIS_VERSION_SUFFIX}.jar
