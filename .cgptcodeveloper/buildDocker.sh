# Plugin Action: maven build incl. running unit- and integrationtests and building the docker image
echo executing mvn -Pdocker clean install
mvn -B -Pdocker clean install > build.log 2>&1
if [ $? -ne 0 ]; then
  echo "mvn clean install failed, see build.log. You can use the grep action with some context lines to look for errors and exceptions in file build.log"
  echo "Here is an excerpt of the build.log file using \`grep -C 5 -E 'ERROR|Exception|FAILURE' build.log | head -n 100\` :"
  grep -C 5 -E "ERROR|Exception|FAILURE" build.log | head -n 100
  exit 1
else
  echo "successful; build log is in build.log"
  exit 0
fi
