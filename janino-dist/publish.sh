#!/bin/bash

# -------------- BEGIN CONFIGURATION

# The JANINO project directory in the workspace.
JANINO_DIR=`dirname $0`/../janino;

# The RSYNC command.
RSYNC='rsync --chmod=ugo=rwX --rsh=ssh';
#RSYNC="echo $RSYNC";
#RSYNC="$RSYNC --stats";

# -------------- END CONFIGURATION

# Determine the version number.
VERSION=`sed -r -e 's/^ *version *= *([0-9\.]+) *|.*$/\1/' -e '/^$/d' <$JANINO_DIR/build.properties`;
test "$VERSION" || { echo >&2 "Cannot determine version, exiting."; exit 1; };
echo "*** Version to publish is '$VERSION'.";

PAD_FILE="pad_file.xml";
CHANGELOG="changelog.html";
FAVICON="favicon.ico";
JANINO_ZIP="$JANINO_DIR/janino-$VERSION.zip";
JANINO_JAR="$JANINO_DIR/build/lib/janino.jar";

sed -e "s,@VERSION@,$VERSION,g" -e "s,@SIZE@,`wc -c <$JANINO_ZIP`,g" <pad_file-template.xml >pad_file.xml

FILES="$FAVICON $JANINO_ZIP $JANINO_JAR $CHANGELOG $PAD_FILE";
SAMPLES="$JANINO_DIR/src/org/codehaus/janino/samples";
JAVADOC_DIR="$JANINO_DIR/build/javadoc";
 
# Check that the files exist.
for i in $FILES; do
  test -f $i || { echo >&2 "'$i' missing, exiting."; exit 1; }
done;
echo "*** '$FILES' are in place, fine.";

# Check that the JAVADOC was generated and is complete.
test -d $JAVADOC_DIR || { echo >&2 "'$JAVADOC_DIR' mising, exiting."; exit 1; }
test `find $JAVADOC_DIR -type f | wc -l` -ge 280 || { echo >&2 "JAVADOC seems to be incomplete, exiting."; exit 1; }
echo "*** '$JAVADOC_DIR' is correct, fine.";

# Verify that the version is documented in the change log.
V2=`echo $VERSION | sed -e 's/\./\\\\./g'`;
grep -Eq '<h4>Version '$V2', \w+ [0-9]+, [0-9]{4} - <a href="janino-'$V2'.zip">Download</a></h4>' $CHANGELOG || { echo >&2 "No entry for version '$VERSION' in '$CHANGELOG', exiting."; exit 1; }
echo "*** Version '$VERSION' is documented in '$CHANGELOG', fine.";

echo;
echo "*** Everything is ready to publish JANINO version $VERSION.";
echo -n "*** Enter your XIRCLES user name to proceed: ";
read XIRCLES_USER;

# Where to RSYNC to.
DESTINATION="$XIRCLES_USER@janino.codehaus.org:/projects/janino/dist";

echo;
echo "*** Publishing '$FILES'...";
$RSYNC $FILES $DESTINATION/ ||
  { echo >&2 "RSYNC failed - distribution may now be inconsistent!"; exit 1; }
echo "*** ...done";

echo;
echo "*** Publishing '$SAMPLES'...";
$RSYNC --recursive $SAMPLES $DESTINATION/ ||
  { echo >&2 "RSYNC failed - distribution may now be inconsistent!"; exit 1; }
echo "*** ...done";

echo;
echo "*** Publishing '$JAVADOC_DIR'...";
$RSYNC --recursive --compress $JAVADOC_DIR $DESTINATION/ ||
  { echo >&2 "RSYNC failed - distribution is now inconsistent!"; exit 1; }
echo "*** ...done";

rm -r $PAD_FILE;

