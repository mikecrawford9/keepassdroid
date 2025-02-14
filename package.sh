#!/bin/bash

# This script should be run from the root of an Android project

KEYSTORE='../android.keystore'

if [ ! -f "${KEYSTORE}" ]
then
  keytool -genkey -v -alias release -keyalg RSA -keysize 4096 -validity 10000 -keystore ${KEYSTORE}
fi

if [ ! -f "local.properties" ]
then
  android update project --name KeePassDroid --target android-9 --path $PWD --subprojects
fi

# rebuild JNI shared objects, if applicable
if [ ! -f "./libs/armeabi/libfinal-key.so" ]
then
  rm -rf obj libs/*/*.so
  ndk-build
  if [ $? -ne 0 ]
  then
    echo "Native build failed, bailing"
    exit 1
  fi
fi

# rebuild APK
ant release

# sign and align APK
#jarsigner -verbose ./bin/davsync-release-unsigned.apk release
#zipalign -v 4 ./bin/davsync-release-unsigned.apk ./bin/davsync.apk

if [ "${1}" == "push" ]
then
  # push to device and wait for status messages
  adb install -r ./bin/KeePassDroid-release.apk
  adb logcat
fi
