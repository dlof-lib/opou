#!/bin/sh

#
# gradlew — سكربت تشغيل Gradle Wrapper القياسي (Linux/macOS)
# يقوم بتنزيل نسخة Gradle المحددة في gradle/wrapper/gradle-wrapper.properties
# تلقائيًا عند أول تشغيل، ثم يستخدمها لبناء المشروع.
#

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
APP_NAME="Gradle"

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# تحديد أمر جافا
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ]; then
        die "ERROR: JAVA_HOME معرّف لكن '$JAVACMD' غير قابل للتنفيذ."
    fi
else
    JAVACMD="java"
    command -v java >/dev/null 2>&1 || die "ERROR: JAVA_HOME غير معرّف ولم يتم العثور على 'java' في PATH."
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
    die "ERROR: gradle/wrapper/gradle-wrapper.jar غير موجود.
شغّل 'gradle wrapper --gradle-version 8.7' مرة واحدة (بوجود Gradle مثبت محليًا أو عبر Android Studio)
لتوليد هذا الملف، ثم أعد تشغيل ./gradlew."
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
