echo executing mvn clean install
mvn -B clean install
echo executing for testing src/test/resources/runtests.sh
src/test/resources/runtests.sh < /dev/null
