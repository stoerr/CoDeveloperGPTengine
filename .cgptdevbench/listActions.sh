# Plugin Action: list all available actions
cd .cgptdevbench || exit 1
for fil in *.sh; do
  echo "${fil%.sh}"
done
