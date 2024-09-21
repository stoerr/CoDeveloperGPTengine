# Plugin Action: list all available actions
cd $(dirname $(realpath "$0")) || (
  echo "ERROR: Could not cd to $(dirname $(realpath "$0"))"
  exit 1
)
echo "Available actionNames for GPT operation executeExternalAction"
echo "============================================================"
for fil in *.sh; do
  echo "${fil%.sh} :  " $(egrep -o "Plugin Action: .*" "$fil" | sed -e 's/Plugin Action: //')
done
