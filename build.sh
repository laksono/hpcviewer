#!/bin/bash
#
# Copyright (c) 2002-2021, Rice University.
# See the file edu.rice.cs.hpcviewer.ui/License.txt for details.
#
# Build hpcviewer and generate tar or zip files
# This script is only for Unix and X windows.
#

#------------------------------------------------------------
# Error messages
#------------------------------------------------------------

die()
{
    echo "$0: error: $*" 1>&2
    exit 1
}


#------------------------------------------------------------
# Check maven
#------------------------------------------------------------
if ! command -v mvn &> /dev/null
then
	die "Apache Maven (mvn) command is not found"
fi

#------------------------------------------------------------
# Check the java version.
#------------------------------------------------------------
JAVA=`type -p java`

if [ "$JAVA"x != "x" ]; then
    JAVA=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME     
    JAVA="$JAVA_HOME/bin/java"
else
    die "unable to find program 'java' on your PATH"
fi

JAVA_MAJOR_VERSION=`java -version 2>&1 \
	  | head -1 \
	  | cut -d'"' -f2 \
	  | sed 's/^1\.//' \
	  | cut -d'.' -f1`

echo "Java version $JAVA_MAJOR_VERSION"

# we need Java 11 at least
jvm_required=11

if [ "$JAVA_MAJOR_VERSION" -lt "$jvm_required" ]; then
	die "$name requires Java $jvm_required"
fi

#------------------------------------------------------------
# start to build
#------------------------------------------------------------
NOTARIZE=0
RELEASE=`date +"%Y.%m"`

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -n|--notarize)
    NOTARIZE=1
    shift # past argument
    ;;
    -r|--release)
    RELEASE="$2"
    shift # past argument
    shift # past value
    ;;
    --default)
    DEFAULT=YES
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

# 
# Update the release file 
#
GITC=`git rev-parse --short HEAD`

echo "Release ${RELEASE}. Commit $GITC" > edu.rice.cs.hpcviewer.ui/release.txt
rm -rf hpcviewer-${RELEASE}*

#
# build the viewer
#
echo "=================================="
echo " Building the viewer"
echo "=================================="
mvn clean package

# The result should be:
#

echo "=================================="
echo " Repackaging the viewer"
echo "=================================="

# wrap the generated files into standard hpcviewer product
#

# repackage 
repackage_linux(){
	prefix=$1
	platform=$2
	package=`ls edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-${prefix}.${platform}*`
	extension="tgz"

	[[ -z $package ]] && { echo "$package doesn't exist"; exit 1;  }

	mkdir -p tmp/hpcviewer
	cd tmp/hpcviewer

	tar xzf  ../../$package
	cp ../../scripts/hpcviewer.sh .
	cp ../../scripts/install.sh .
	cp ../../scripts/README .

	cd ..

	output="hpcviewer-${RELEASE}-${prefix}.${platform}.$extension"
	tar czf ../$output hpcviewer/
	chmod 664 ../$output

	cd ..
	#ls -l $output
	rm -rf tmp
}


repackage_mac() {
      input=$1
      output=$2
      cp $input $output
      chmod 664 $output
}


repackage_windows() {
      input=$1
      output=$2
      # for windows, we need to create a special hpcviewer directory
      if [ -e hpcviewer ]; then
         echo "File or directory hpcviewer already exist. Do you want to remove it? (y/n) "
         read tobecontinue
         if [ $tobecontinue != "y" ]; then
	    exit
	 fi
      fi
      rm -rf hpcviewer
      mkdir hpcviewer
      cd hpcviewer
      unzip -q ../$input
      cd ..
      zip -q -r -y $output hpcviewer/
      rm -rf hpcviewer
      
      chmod 664 $output
}

# The result should be:
#
# Building zip: edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-win32.win32.x86_64.zip
# Building zip: edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-macosx.cocoa.x86_64.zip
# Building tar: edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-linux.gtk.aarch64.tar.gz
# ...

repackage_linux linux.gtk x86_64
repackage_linux linux.gtk aarch64
repackage_linux linux.gtk ppc64le 

# copy and rename windows package
output="hpcviewer-${RELEASE}-win32.win32.x86_64.zip"
input=edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-win32.win32.x86_64.zip
repackage_windows $input $output 

# copy and rename mac x86_64 package
output="hpcviewer-${RELEASE}-macosx.cocoa.x86_64.zip"
input=edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-macosx.cocoa.x86_64.zip 
repackage_mac $input $output

# copy and rename mac aarch64 package
output="hpcviewer-${RELEASE}-macosx.cocoa.aarch64.zip"
input=edu.rice.cs.hpcviewer.product/target/products/edu.rice.cs.hpcviewer-macosx.cocoa.aarch64.zip 
repackage_mac $input $output


###################################################################
# special treatement for mac OS
###################################################################
OS=`uname`
if [[ "$OS" == "Darwin" && "$NOTARIZE" == "1" ]]; then 
        macPkgs="hpcviewer-${RELEASE}-macosx.cocoa.x86_64.zip"
	macos/notarize.sh $macPkgs
	
        macPkgs="hpcviewer-${RELEASE}-macosx.cocoa.aarch64.zip"
	macos/notarize.sh $macPkgs
fi


echo "=================================="
echo " Done" 
echo "=================================="

ls -l hpcviewer-${RELEASE}-*
