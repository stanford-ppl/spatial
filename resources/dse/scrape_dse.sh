#!/bin/bash

# Scrape app directories for cycle counts

rm data/train
for file in `find ~/sp_dse/spatial/gen/VCS/ -name "sim.log"`; do
	in_loads=0
	max_load=0
	in_stores=0
	max_store=0
	in_gateds=0
	max_gated=0
	loads=`echo $file | sed "s/.*Load//g" | sed "s/Store.*//g"`
	stores=`echo $file | sed "s/.*Store//g" | sed "s/Gated.*//g"`
	gateds=`echo $file | sed "s/.*Gated//g" | sed "s/Dims.*//g"`
	while read -r line; do 
		if [[ in_loads -lt loads ]]; then
			this_load=`echo $line | sed "s/^.* - //g" | sed "s/ (.*//g"`
			if [[ this_load -gt max_load ]]; then max_load=${this_load}; fi
			in_loads=$((in_loads+1))
		elif [[ in_stores -lt stores ]]; then
			this_store=`echo $line | sed "s/^.* - //g" | sed "s/ (.*//g"`
			if [[ this_store -gt max_store ]]; then max_store=${this_store}; fi
			in_stores=$((in_stores+1))
		elif [[ in_gateds -lt gateds ]]; then
			this_gated=`echo $line | sed "s/^.* - //g" | sed "s/ (.*//g"`
			if [[ this_gated -gt max_gated ]]; then max_gated=${this_gated}; fi
			in_gateds=$((in_gateds+1))
		else 
			echo "ERROR"
		fi 
	done <<< $(cat $file | grep "^      x")
	# cycs=`cat $file | grep "Design ran for " | sed "s/^.*Design ran for //g" | sed "s/ cycles.*//g"`
	h=`echo $file | sed "s/.*Dims//g" | sed "s/x.*//g"`
	w=`echo $file | sed "s/.*Dims.*x//g" | sed "s/P.*//g"`
	bpc=`echo $file | sed "s/.*P//g" | sed "s/\/sim.log//g"`
	echo -e "$loads\t$stores\t$gateds\t$h\t$w\t$bpc\t${max_load}\t${max_store}\t${max_gated}" | tee -a data/train
done
