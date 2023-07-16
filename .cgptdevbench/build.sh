# Plugin Action: maven build incl. running unit- and integrationtests
echo executing mvn clean install
mvn -B clean install > build.log 2>&1
if [ $? -ne 0 ]; then
  echo "mvn clean install failed, see build.log"
  exit 1
else
  echo "mvn clean install successful"
  exit 0
fi
