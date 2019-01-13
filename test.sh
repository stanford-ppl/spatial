#!/bin/bash

# Set data environment variable for running this test
# export TEST_DATA_HOME="$PWD/../data/"

set -e # die if anything fails

type=$1
NUM_THREADS=${NUM_THREADS:-4}
CLOCK_FREQ_MHZ=${CLOCK_FREQ_MHZ:-125}
file_or_tests=$2

WARN="[\e[33mwarn\e[39m]"
INFO="[\e[34minfo\e[39m]"
FAIL="[\e[31merror\e[39m]"

if [[ $file_or_tests == "" ]]; then
   tests="spatial.tests.*" 
elif [ -f $file_or_tests ]; then
   tests=""
   while read LINE 
   do  
      if [[ $LINE == \#* ]]; then 
        :
      elif [[ $LINE == spatial.tests.* ]]; then 
         tests="$tests $LINE"
        echo "$LINE"
      else 
        tests="$tests spatial.tests.$LINE"
        echo "spatial.tests.$LINE"
      fi
   done <  $file_or_tests
elif [[ $file_or_tests == spatial.tests.* ]]; then
   tests="$file_or_tests"
else 
   tests="spatial.tests.$file_or_tests"
fi

echo -e "$INFO Using ${NUM_THREADS} thread(s) for testing."

if [[ $TEST_DATA_HOME == "" ]]; then
  echo -e "$WARN TEST_DATA_HOME is not set. Set TEST_DATA_HOME for data-dependent tests to pass."
else 
  echo -e "$INFO Test Data Directory: $TEST_DATA_HOME"
fi

starttime="$(date +'%m_%d_%y_%H_%M_%S')"
fileout="test_${starttime}_$type.log"
echo -e "$INFO Running tests $tests"
echo -e "$INFO Logging tests to $fileout"


# Basic tests
if [[ $type == "sim" ]]; then
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.Scala=true "; project test; testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "vcs" ]]; then
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.VCS=true "; project test; testOnly $tests" 2>&1 | tee $fileout

# Synthesis tests
elif [[ $type == "zynq" ]]; then
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.Zynq=true "; project test; testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "aws" ]]; then
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.AWS=true "; project test; testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "zcu" ]]; then
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.ZCU=true "; project test; testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "arria10" ]]; then
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.Arria10=true "; project test; testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "pir" ]]; then
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.PIR=true "; project test; testOnly $tests" 2>&1 | tee $fileout

# Verilog tests that report to gdocs (https://docs.google.com/spreadsheets/d/1_bbJHrt6fvMvfCLyuSyy6-pQbJLiNY4kOSoKN3voSoM/edit#gid=1748974351)
elif [[ $type == "vcs-gdocs" ]]; then
  curpath="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  echo "$$(date +%s)" > ${curpath}start${starttime}.log
  export GDOCS=1
  branchname=`git rev-parse --abbrev-ref HEAD | sed "s/HEAD/unknown/g"`
  export FRINGE_PACKAGE="vcs-gdocs-${branchname}"
  make resources
  make publish
  hash=`git rev-parse HEAD`
  export timestamp=`git show -s --format=%ci`
  echo $hash > ${curpath}/reghash
  echo $branchname > ${curpath}/branchname
  rm -rf gen/VCS
  echo "python3 ${curpath}/resources/regression/gdocs.py \"prepare_sheet\" \"$hash\" \"$branchname\" \"$timestamp\" \"vcs\""
  python3 ${curpath}/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "vcs"
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.VCS=true "; project test; testOnly $tests" 2>&1 | tee $fileout
  echo "$$(date +%s)" > ${curpath}end${starttime}.log
  if [[ -f ${curpath}/${curpath}end${starttime}.log ]]; then endtime=`cat \`pwd\`/${curpath}end${starttime}.log`; else endtime=1; fi
  if [[ -f ${curpath}/${curpath}start${starttime}.log ]]; then starttime=`cat \`pwd\`/${curpath}start${starttime}.log`; else starttime=0; fi
  testtime=$((endtime-starttime))
  python3 ${curpath}/resources/regression/gdocs.py "finish_test" "vcs" "$branchname" "${testtime}"
  python3 ${curpath}/resources/regression/gdocs.py "report_changes" "vcs" "any" "any"
  python3 ${curpath}/resources/regression/gdocs.py "report_slowdowns" "runtime" "vcs" "any" "any"
elif [[ $type == "scalasim-gdocs" ]]; then
  curpath="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  echo "$$(date +%s)" > ${curpath}start${starttime}.log
  export GDOCS=1
  branchname=`git rev-parse --abbrev-ref HEAD | sed "s/HEAD/unknown/g"`
  export FRINGE_PACKAGE="scalasim-gdocs-${branchname}"
  make resources
  make publish
  hash=`git rev-parse HEAD`
  export timestamp=`git show -s --format=%ci`
  echo $hash > ${curpath}/reghash
  echo $branchname > ${curpath}/branchname
  rm -rf gen/Scala
  echo "python3 ${curpath}/resources/regression/gdocs.py \"prepare_sheet\" \"$hash\" \"$branchname\" \"$timestamp\" \"scalasim\""
  python3 ${curpath}/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "scalasim"
  nice -n 20 sbt -Dmaxthreads=${NUM_THREADS} -Dtest.Scala=true "; project test; testOnly $tests" 2>&1 | tee $fileout
  if [[ -f ${curpath}/${curpath}end${starttime}.log ]]; then endtime=`cat \`pwd\`/${curpath}end${starttime}.log`; else endtime=1; fi
  if [[ -f ${curpath}/${curpath}start${starttime}.log ]]; then starttime=`cat \`pwd\`/${curpath}start${starttime}.log`; else starttime=0; fi
  testtime=$((endtime-starttime))
  python3 ${curpath}/resources/regression/gdocs.py "finish_test" "scalasim" "$branchname" "${testtime}"
  python3 ${curpath}/resources/regression/gdocs.py "report_changes" "scalasim" "any" "any"
  python3 ${curpath}/resources/regression/gdocs.py "report_slowdowns" "runtime" "scalasim" "any" "any"
else
  echo -e "$FAIL Usage: test_all.sh <test type> [test(s)]"
  echo -e "$FAIL Test type '$test_type' was not recognized"
  echo -e "$FAIL Supported types: [sim | [vcs/scalasim](-gdocs) | [vcs/scalasim]-noretime(-gdocs) | zynq | aws | zcu | arria10]"
  echo "  sim          - Scala function simulation"
  echo "  vcs 	       - Cycle accurate simulation with VCS"
  echo "  vcs-gdocs    - Cycle accurate simulation with VCS (+google docs)"
  echo "  vcs-noretime - Cycle accurate simulation with VCS (no retiming)" 
  echo "  zynq         - Target the ZC706 board"
  echo "  aws	       - Target the AWS F1 board"
  echo "  zcu	       - Target the ZCU board"
  echo "  arria10      - Target the Arria 10 board"
  echo "  pir          - Plasticine IR compilation"
  exit 1
fi

sbtExitCode=${PIPESTATUS[0]}
exit $sbtExitCode
