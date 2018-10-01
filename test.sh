#!/bin/bash

# Set data environment variable for running this test
# export TEST_DATA_HOME="$PWD/../data/"

type=$1
numthreads=$NUM_THREADS
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
      if [[ $LINE == spatial.tests.* ]]; then 
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

if [[ $numthreads == "" ]]; then
  echo -e "$WARN Defaulting to 4 threads. Set NUM_THREADS environment variable to change."
  threads=4
else
  threads=$numthreads
  echo -e "$INFO Using $threads thread(s) for testing."
fi

if [[ $TEST_DATA_HOME == "" ]]; then
  echo -e "$WARN TEST_DATA_HOME is not set. Set TEST_DATA_HOME for data-dependent tests to pass."
else 
  echo -e "$INFO Test Data Directory: $TEST_DATA_HOME"
fi

fileout="test_$(date +'%m_%d_%y_%H_%M_%S')_$type.log"
echo -e "$INFO Running tests $tests"
echo -e "$INFO Logging tests to $fileout"


# Basic tests
if [[ $type == "sim" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.Scala=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "vcs" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.VCS=true "testOnly $tests" 2>&1 | tee $fileout

# Synthesis tests
elif [[ $type == "zynq" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.Zynq=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "aws" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.AWS=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "zcu" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.ZCU=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "arria10" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.Arria10=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "pir" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.PIR=true "testOnly $tests" 2>&1 | tee $fileout

# Verilog tests that report to gdocs (https://docs.google.com/spreadsheets/d/1_bbJHrt6fvMvfCLyuSyy6-pQbJLiNY4kOSoKN3voSoM/edit#gid=1748974351)
elif [[ $type == "vcs-gdocs" ]]; then
  export GDOCS=1
  export FRINGE_PACKAGE="vcs-gdocs"
  make resources
  hash=`git rev-parse HEAD`
  branchname=`git rev-parse --abbrev-ref HEAD | sed "s/HEAD/unknown/g"`
  export timestamp=`git show -s --format=%ci`
  curpath="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  echo $hash > ${curpath}/reghash
  echo $branchname > ${curpath}/branchname
  echo "python3 ${curpath}/resources/regression/gdocs.py \"prepare_sheet\" \"$hash\" \"$branchname\" \"$timestamp\" \"vcs\""
  python3 ${curpath}/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "vcs"
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.VCS=true "testOnly $tests" 2>&1 | tee $fileout
  python3 ${curpath}/resources/regression/gdocs.py "report_changes" "vcs"
  python3 ${curpath}/resources/regression/gdocs.py "report_slowdowns" "runtime" "vcs"
else
  echo -e "$FAIL Usage: test_all.sh <test type> [test(s)]"
  echo -e "$FAIL Test type '$test_type' was not recognized"
  echo -e "$FAIL Supported types: [sim | vcs(-gdocs) | vcs-noretime(-gdocs) | zynq | aws | zcu | arria10]"
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
