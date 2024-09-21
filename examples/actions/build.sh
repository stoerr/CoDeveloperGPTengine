# Plugin Action: maven build incl. running unit- and integrationtests
echo executing mvn clean install
mvn -B clean install > build.log 2>&1
if [ $? -ne 0 ]; then
  echo "mvn clean install failed, see build.log. You can use the grep action with some context lines to look for errors and exceptions in file build.log"
  exit 1
else
  echo "successful; build log is in build.log"
  exit 0
fi
