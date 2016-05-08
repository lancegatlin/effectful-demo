#!/bin/bash 
set -x
echo Refreshing sbt template...
TGT_DIR=$(pwd)
cd ../s_mach.sbt_templates/single_project
cp .gitignore .travis.yml LICENSE common.sbt publish.sbt publish_checklist.asciidoc $TGT_DIR
cp project/plugins.sbt $TGT_DIR/project/plugins.sbt
cd $TGT_DIR
