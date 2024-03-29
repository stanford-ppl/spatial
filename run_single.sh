cd /home/dam/spatial/
echo "Running the simulation for $1...The simulation log will be printed in run_$1.txt" 
echo "This log will be deleted after the simulation"
sbt -Dtest.CS217=true "; project test; testOnly spatial.tests.apps.StreamedAttention$1" > ./run_$1.txt
rm ./run_$1.txt

VARA="$(grep ExecutorPass logs/CS217/StreamedAttention$1/9999_Timing.log)"
valueA1="${VARA#*:}" # take everything after the ':'
valueA2="${valueA1%(*}" # take everything before the '('
echo "Real Time: $valueA2"
echo "Real Time to simualte Spatial (length: $1)=$valueA2" >> /home/dam/dam-experiments/stream-attn-r-dam/spatial_sweep.txt
rm -rf ./logs/CS217/StreamedAttention$1/

egrep "x311</a>: Streaming UnitPipe" gen/CS217/StreamedAttention$1/info/PostExecution.html -A 3 > ./cycle_$1.txt
VARB="$(grep '<font color="red"> ' ./cycle_$1.txt)"
valueB1="${VARB#*>}" # take everything after the '>'
valueB2="${valueB1%/*}" # take everything before the '/'
echo "Simulated Cycles: $valueB2"
rm ./cycle_$1.txt