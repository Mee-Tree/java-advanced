#!/bin/bash

(( $# != 1 )) \
      && echo "Usage: $0 <hw>" \
      && exit

rsync -av --exclude='*.iml' \
      --exclude='*module-info.java' \
      --exclude='*package-info.java' \
      ./modules/info.kgeorgiy.ja.polchinsky."$1"/ \
      ../java-advanced/java-solutions

pushd ../java-advanced || exit

git add -f .
git status
git commit -m "Add $1 [Automated commit]"
#git commit -m "Suppress unchecked warning"
#git commit -m "Update $1"
git push origin master

popd || exit
