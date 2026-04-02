CWD = $(shell pwd)

POM = -f pom.xml
# Maven Clean Install Skip ; skip tests, javadoc, scaladoc, etc
MS = mvn -DskipTests -Dmaven.javadoc.skip=true -Dskip
MCIS = $(MS) clean install
MCCS = $(MS) clean compile

VER = $(error specify VER=releasefile-name e.g. VER=1.9.7-rc2)
loud = echo "@@" $(1);$(1)

# Source: https://stackoverflow.com/questions/4219255/how-do-you-get-the-list-of-targets-in-a-makefile
.PHONY: help

.ONESHELL:
help:   ## Show these help instructions
	@sed -rn 's/^([a-zA-Z_-]+):.*?## (.*)$$/"\1" "\2"/p' < $(MAKEFILE_LIST) | xargs printf "make %-20s# %s\n"

uberjar: ## Create the self-contained RDFPatch Stream JAR
	$(MCCS) $(POM) package -Pbundle -pl :rdfpatch-stream-pkg-uberjar-cli -am $(ARGS)
	file=`find '$(CWD)/rdfpatch-stream-pkg-parent/rdfpatch-stream-pkg-uberjar-cli/target' -name 'rdfpatch-stream-cli-*.jar'`
	printf '\nCreated package:\n\n%s\n\n' "$$file"

deb-rebuild: ## Rebuild the deb package (minimal build of only required modules)
	$(MCIS) $(POM) -Pdeb -am -pl :jena-rdfpatch-stream-pkg-deb-cli $(ARGS)

deb-reinstall: ## Reinstall deb (requires prior build)
	@p1=`find jena-rdfpatch-stream-pkg-parent/jena-rdfpatch-stream-pkg-deb-cli/target | grep '\.deb$$'`
	sudo dpkg -i "$$p1"

deb-rere: deb-rebuild deb-reinstall ## Rebuild and reinstall deb package

#release-github: SHELL:=/bin/bash
#release-github: ## Create files for Github upload
#	@set -eu
#	ver=$(VER)
#	$(call loud,$(MAKE) fuseki-plugin)
#	file=`find '$(CWD)/graphql4sparql-pkg-parent/graphql4sparql-pkg-fuseki-plugin/target' -name '*-fuseki-plugin*.jar'`
#	$(call loud,cp "$$file" "graphql4sparql-fuseki-plugin-$$ver.jar")
#	$(call loud,gh release create v$$ver "graphql4sparql-fuseki-plugin-$$ver.jar")

