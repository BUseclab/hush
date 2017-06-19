#!/bin/bash

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters: [APK file list] [Output dir] [Init source/sink file]"
fi

#Let each analysis run for an hour
APKS=$1
OUTPUT=$2
SINKSOURCE=$3
LOG_LEVEL=info
#Location where android platforms installed, ex:  path/to/android-sdk-linux/platforms
ANDROID_PLATFORMS=
TIMEOUT=120m
memory=40g
pathalgo=sourcesonly 

timestamp() {
	date +"%s"
}
trap "exit" INT
while IFS='' read -r line || [[ -n $line ]]; do
	echo "$line"
	apkname=$(basename $line)
	mkdir -p "$OUTPUT/$apkname"
	log="$OUTPUT/$apkname/out-$(date +%s).log"
	timeout -s SIGINT -k 10 $TIMEOUT /usr/bin/time -v java -d64 -XX:+UseConcMarkSweepGC -Xmx$memory -Xms$memory -Dorg.slf4j.simpleLogger.defaultLogLevel=$LOG_LEVEL -cp .:lib/* edu.bu.android.hiddendata.FindHidden $line $ANDROID_PLATFORMS  --pathalgo $pathalgo --aplength 1 --aliasflowins --layoutmode none --fragments --sourcessinks $SINKSOURCE  --output $OUTPUT  > $log 2>&1 
	#This is a hack to make sure the java process actually is killed before continueing
	#This solves the out of memory issue that can occur with FLowDroid
	while ps aux  | grep $apkname | grep -v "grep" > /dev/null; do sleep 1; done
done < "$APKS"
