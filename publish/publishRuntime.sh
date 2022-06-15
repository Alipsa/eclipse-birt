#!/usr/bin/env bash

#########################################################################
## Download, repackage, and publish the birt runtime artifact to nexus ##
#########################################################################
basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
buildDir="${basedir}/target"
srcZipDir=${basedir}/../build/birt-packages/birt-runtime/target/
libDir="${buildDir}/ReportEngine/lib"
publishGroupName=org.eclipse.birt.runtime
# The flute jar lacks version number so we handle it separately
fluteVersion=1.3

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[34m'
NC='\033[0m' # No Color

deployType=snapshot
doClean=false
doInstall=false
doUpload=false

while [[ $# -gt 0 ]]; do
    key="$1"
    case "$key" in
      -c|--clean)
        doClean=true
        ;;
      -i|--install)
        doInstall=true
        ;;
      -u|--upload)
        doUpload=true
        ;;
      -p|--prod)
        deployType=prod
        ;;
      *)
	    echo -e "${RED}Unknown option '$key', ignoring it.${NC}"
	    ;;
    esac
    # Shift after checking all the cases to get the next option
    shift
done

function info {
 echo -e "${BLUE}* ${1}${NC}"
}

function error {
  echo -e "${RED}${1}${NC}"
}

function deploy {
  filePath=$1
  file=$(basename "${filePath}")
  parts=(${file//_/ })
  artifactId=${parts[0]}
  if [[ "${artifactId}" == *.jar ]]; then
    artifactId=${artifactId%.jar}
  fi
  if [[ $# -eq 2 ]]; then
    version=${2}${versionSuffix}
  else
    version=${parts[1]%.*}${versionSuffix}
  fi
  if [[ ${doInstall} == true ]]; then
    mvn install:install-file \
      -DgroupId=${publishGroupName} \
      -DartifactId="${artifactId}" \
      -Dversion="${version}" \
      -Dpackaging=jar \
      -Dfile="${filePath}" || { error "Failed to run 'mvn install'"; exit 1; }
  fi
  if [[ ${doUpload} == true ]]; then
    mvn deploy:deploy-file \
    -DgroupId=${publishGroupName} \
    -DartifactId="${artifactId}" \
    -Dversion="${version}" \
    -Dpackaging=jar \
    -Dfile="${filePath}" \
    -DrepositoryId=ossrh \
    -Durl="${nexusUrl}" || { error "Failed to run 'mvn deploy'"; exit 1; }
  fi
}

if [[ "${deployType}" == prod ]]; then
  nexusUrl=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/
  versionSuffix=""
else
  nexusUrl=https://s01.oss.sonatype.org/content/repositories/snapshots/
  versionSuffix="-SNAPSHOT"
fi

if [[ ${doClean} == true ]]; then
  if [[ -d ${buildDir} ]]; then
    info "Cleaning ${buildDir}"
    rm -rf "${buildDir}" || { error "Failed to delete ${buildDir}"; exit 1; }
  fi
  mvn dependency:purge-local-repository -DmanualInclude=org.eclipse.birt.runtime || { error "Failed to purge maven local repo"; exit 1; }
fi

mkdir -p "${buildDir}"

srcZipPath=$(find "${srcZipDir}" -maxdepth 1 -name 'birt-runtime-*' | head -n 1)
if [[ ! -f ${srcZipPath} ]]; then
  error "Failed to find a runtime zip in ${srcZipDir}, you must bild runtime first"
  exit 1
fi

info "Unpacking the runtime zip"
unzip -q -n "${srcZipPath}" -d "${buildDir}"
birtJar=$(basename "$(find "${libDir}" -maxdepth 1 -name 'org.eclipse.birt.runtime_*' | head -n 1)")

if [[ ${doInstall} == false ]] && [[ ${doUpload} == false ]]; then
  info "Neither install or upload flags set, ending now!"
  exit
fi

info "Deploying birt dependency jars..."
for filePath in "${libDir}"/*.jar; do
  fileName=$(basename "${filePath}")
  if [[ "${fileName}" == "flute.jar" ]]; then
    deploy "${filePath}" "${fluteVersion}"
  elif [[ "${fileName}" != "${birtJar}" ]]; then
    deploy "${filePath}"
  fi
done

info "creating birt pom file"
birtPom=${buildDir}/birt-pom.xml
function printDependency {
  fileName=$1
  parts=(${fileName//_/ })
  artifactId=${parts[0]}
  if [[ "${artifactId}" == *.jar ]]; then
    artifactId=${artifactId%.jar}
  fi
  if [[ $# -eq 2 ]]; then
    version=$2${versionSuffix}
  else
    version=${parts[1]%.*}${versionSuffix}
  fi
  echo "  <dependency>
    <groupId>${publishGroupName}</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>${version}</version>
  </dependency>"
}
echo "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
               xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">
  <modelVersion>4.0.0</modelVersion>

  <groupId>${publishGroupName}</groupId>
  <artifactId>birt-runtime</artifactId>
  <version>4.9.0-20220502${versionSuffix}</version>
  <name>Eclipse :: Birt :: Runtime</name>

  <dependencies>" > "${birtPom}"

for f in "${libDir}"/*.jar; do
  fileName=$(basename "${f}")
  if [[ "${fileName}" == "flute.jar" ]]; then
    printDependency "${fileName}" "${fluteVersion}" >> "${birtPom}"
  elif [[ "${fileName}" != "${birtJar}" ]]; then
    printDependency "${fileName}" >> "${birtPom}"
  fi
done
echo "  </dependencies>
</project>" >> "${birtPom}"

if [[ ${doInstall} == true ]]; then
  info "Installing the birt jar to the local repository"
  mvn install:install-file \
  -Dfile="${libDir}/${birtJar}" \
  -DpomFile="${birtPom}"
fi
if [[ ${doUpload} == true ]]; then
  info "Deploying the birt jar"
  mvn deploy:deploy-file \
  -Dfile="${libDir}/${birtJar}" \
  -DpomFile="${birtPom}" \
  -DrepositoryId=ossrh \
  -Durl=${nexusUrl} || { error "Failed to run 'mvn deploy'"; exit 1; }
fi

echo -e "${GREEN}Publish Runtime Complete!${NC}"
