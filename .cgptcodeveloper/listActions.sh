# Plugin Action: list all available actions
cd .cgptcodeveloper || exit 1
for fil in *.sh; do
  echo "${fil%.sh} :  " $(egrep -o "Plugin Action: .*" "$fil" | sed -e 's/Plugin Action: //')
done
