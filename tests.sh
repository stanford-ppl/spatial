#!/bin/bash

# Set data environment variable for running this test
# export TEST_DATA_HOME="$PWD/../data/"

type=$1
numthreads=$NUM_THREADS
file_or_tests=$2

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
  echo "Defaulting to 4 threads. Set NUM_THREADS variable to change."
  threads=4
else
  threads=$numthreads
fi

fileout="test_$(date +'%m_%d_%y_%H_%M_%S').log"
echo "Running tests $tests"
echo "Logging tests to $fileout"


# Basic tests
if [[ $type == "sim" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.Scala=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "vcs" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.VCS=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "vcs-noretime" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.VCS_noretime=true "testOnly $tests" 2>&1 | tee $fileout

# Synthesis tests
elif [[ $type == "zynq" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.Zynq=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "aws" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.AWS=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "zcu" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.ZCU=true "testOnly $tests" 2>&1 | tee $fileout
elif [[ $type == "arria10" ]]; then
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.Arria10=true "testOnly $tests" 2>&1 | tee $fileout

# Verilog tests that report to gdocs (https://docs.google.com/spreadsheets/d/1_bbJHrt6fvMvfCLyuSyy6-pQbJLiNY4kOSoKN3voSoM/edit#gid=1748974351)
elif [[ $type == "vcs-gdocs" ]]; then
  export GDOCS=1
  hash=`git rev-parse HEAD`
  export timestamp=`git show -s --format=%ci`
  curpath="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  echo "python3 ${curpath}/../resources/regression/gdocs.py \"prepare_sheet\" \"$hash\" \"nova-spatial\" \"$timestamp\" \"vcs\""
  python3 ${curpath}/../resources/regression/gdocs.py "prepare_sheet" "$hash" "nova-spatial" "$timestamp" "vcs"
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.VCS=true "testOnly $tests" 2>&1 | tee $fileout
  python3 ${curpath}/../resources/regression/gdocs.py "report_changes" "vcs"
elif [[ $type == "vcs-noretime-gdocs" ]]; then
  export GDOCS=1
  hash=`git rev-parse HEAD`
  export timestamp=`git show -s --format=%ci`
  curpath="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  python3 ${curpath}/../resources/regression/gdocs.py "prepare_sheet" "$hash" "nova-spatial" "$timestamp" "vcs-noretime"
  nice -n 20 sbt -Dmaxthreads=$threads -Dtest.VCS_noretime=true "testOnly $tests" 2>&1 | tee $fileout
  python3 ${curpath}/../resources/regression/gdocs.py "report_changes" "vcs-noretime"
else
  echo "Test type '$type' not recognized" 
  echo "Supported types: [sim | vcs(-gdocs) | vcs-noretime(-gdocs) | zynq | aws | zcu | arria10]"
  exit 1
fi

sbtExitCode=${PIPESTATUS[0]}
exit $sbtExitCode
