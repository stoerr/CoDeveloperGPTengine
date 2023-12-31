# Plugin Action: builds the site, for testing site creation only.
echo executing mvn clean install javadoc:aggregate site site:stage
mvn -B clean install javadoc:aggregate site site:stage > build.log 2>&1
if [ $? -ne 0 ]; then
  echo "mvn clean install javadoc:aggregate site site:stage failed, see build.log. You can use the grep action with some context lines to look for errors and exceptions in file build.log"
  exit 1
else
  echo "successful; build log is in build.log"
  exit 0
fi
