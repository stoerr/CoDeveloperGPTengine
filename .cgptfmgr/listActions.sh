cd .cgptfmgr || exit 1
for fil in *.sh; do
  echo "${fil%.sh}"
done
