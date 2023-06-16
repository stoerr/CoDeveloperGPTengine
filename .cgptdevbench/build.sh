# Plugin Action: maven build incl. running unit- and integrationtests
echo executing mvn clean install
mvn -B clean install 2>&1
