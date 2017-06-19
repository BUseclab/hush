#!/bin/sh

# Helper script to dissessembl a directory of apks. A 
# directory of the apk name will be created in the output
# directory location and the disassebmled code placed there

# Use: 
# 	disassemble.sh [directory of apks] [output directory]

#Change this to where your apktool is
APKTOOL_HOME=
APK="$1/*.apk"
DEST=$2
for f in $APK
do
	#From https://stackoverflow.com/questions/2664740/extract-file-basename-without-path-and-extension-in-bash
	APKFILENAME=${f##*/}
	DIRNAME=${APKFILENAME%.apk}
	D="$DEST/$DIRNAME"

	echo $f
	$APKTOOL_HOME/apktool d $f -o $D -f
done
