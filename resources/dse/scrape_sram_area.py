# Scrape xilinx synth directories for sram resource utilizations

import re
import csv
import os
from shutil import copyfile
from pyquery import PyQuery as pq
import argparse
import sys

class Node:
	def __init__(this, full, local, type):
		this.fullname = full
		this.localname = local
		this.nodetype = type

	## PROPERTIES
	fullname = ""
	localname = ""
	nodetype = ""

	# Mem specific
	nbufs = ""
	bitwidth = ""
	dim0 = ""
	dim1 = ""
	dim2 = ""
	dim3 = ""
	dim4 = ""
	N0 = ""
	N1 = ""
	N2 = ""
	N3 = ""
	N4 = ""
	B0 = ""
	B1 = ""
	B2 = ""
	B3 = ""
	B4 = ""
	a0 = ""
	a1 = ""
	a2 = ""
	a3 = ""
	a4 = ""
	p0 = ""
	p1 = ""
	p2 = ""
	p3 = ""
	p4 = ""
	hist0muxwidth = ""
	hist0rlanes = ""
	hist0wlanes = ""
	hist1muxwidth = ""
	hist1rlanes = ""
	hist1wlanes = ""
	hist2muxwidth = ""
	hist2rlanes = ""
	hist2wlanes = ""

	## RESULTS
	LUTs = 0
	LaL = 0
	LaM = 0
	SRLs = 0
	FFs = 0
	RAMB32 = 0
	RAMB18 = 0
	URAM = 0
	DSPs = 0

	def printIDFields(this): 
		print("fullname,localname,nodetype")
	def printID(this): 
		print("%s,%s,%s" % (this.fullname, this.localname, this.nodetype))
	def printResultsFields(this):
		print("LUTs,LaL,LaM,SRLs,FFs,RAMB32,RAMB18,URAM,DSPs")
	def printResults(this):
		print("%s,%s,%s,%s,%s,%s,%s,%s,%s" % (this.LUTs, this.LaL, this.LaM, this.SRLs, this.FFs, this.RAMB32, this.RAMB18, this.URAM, this.DSPs))
	def printPropertiesFields(this):
		print("nbufs,bitwidth,dim0,dim1,dim2,dim3,dim4,N0,N1,N2,N3,N4,B0,B1,B2,B3,B4,a0,a1,a2,a3,a4,p0,p1,p2,p3,p4,hist0muxwidth,hist0rlanes,hist0wlanes,hist1muxwidth,hist1rlanes,hist1wlanes,hist2muxwidth,hist2rlanes,hist2wlanes")
	def printProperties(this):
		print("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s" % (this.bitwidth,this.nbufs,this.dim0,this.dim1,this.dim2,this.dim3,this.dim4,this.N0,this.N1,this.N2,this.N3,this.N4,this.B0,this.B1,this.B2,this.B3,this.B4,this.a0,this.a1,this.a2,this.a3,this.a4,this.p0,this.p1,this.p2,this.p3,this.p4,this.hist0muxwidth,this.hist0rlanes,this.hist0wlanes,this.hist1muxwidth,this.hist1rlanes,this.hist1wlanes,this.hist2muxwidth,this.hist2rlanes,this.hist2wlanes))
	def printAllFields(this):
		print("fullname,localname,nodetype,nbufs,bitwidth,dim0,dim1,dim2,dim3,dim4,N0,N1,N2,N3,N4,B0,B1,B2,B3,B4,a0,a1,a2,a3,a4,p0,p1,p2,p3,p4,hist0muxwidth,hist0rlanes,hist0wlanes,hist1muxwidth,hist1rlanes,hist1wlanes,hist2muxwidth,hist2rlanes,hist2wlanesLUTs,LaL,LaM,SRLs,FFs,RAMB32,RAMB18,URAM,DSPs")
	def printAll(this):
		print("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s" % (this.fullname, this.localname, this.nodetype, this.nbufs,this.bitwidth,this.dim0,this.dim1,this.dim2,this.dim3,this.dim4,this.N0,this.N1,this.N2,this.N3,this.N4,this.B0,this.B1,this.B2,this.B3,this.B4,this.a0,this.a1,this.a2,this.a3,this.a4,this.p0,this.p1,this.p2,this.p3,this.p4,this.hist0muxwidth,this.hist0rlanes,this.hist0wlanes,this.hist1muxwidth,this.hist1rlanes,this.hist1wlanes,this.hist2muxwidth,this.hist2rlanes,this.hist2wlanes,this.LUTs, this.LaL, this.LaM, this.SRLs, this.FFs, this.RAMB32, this.RAMB18, this.URAM, this.DSPs))






target = 'zcu'
capstarget = 'ZCU'

# Collect SRAM Data
# datadir='/home/mattfel/regression/synth/' + target + '/current-spatial/spatial/gen/' + capstarget
datadir='/home/mattfel/sp_area/spatial/out0'
ctrltree='controller_tree.html'
allctrltrees=[]
for root, dirs, files in os.walk(datadir):
    if ctrltree in files:
       f_name = os.path.join(root, ctrltree)
       allctrltrees.append(f_name)

node_dict = []

for app in allctrltrees:
	# Get html
	appname = app.split('/')[-3]
	with open(app, 'r') as file:
		data = file.read().split('\n')

	# Extract memory section from html
	nb_start = data.index("NBuf Mems")
	# sb_start = data.index("Single-Buffered Mems")
	entries = data[nb_start:]

	current = None
	app_sram_dict = []
	# Crawl and collect info on each sram
	for line in entries:
		if line.find('BankedSRAM') >= 0:
			sym = re.search('x[0-9]+', line).group(0)
			current = Node(sym + "_" + appname, sym, "SRAMNew")
			current.nbufs = '1' # default of 1

		if (current != None): 
			if line.find('nBufs =') >= 0:
				current.nbufs = int(re.search('nBufs = ([0-9]+)', line).group(1))
			if line.find('>volume =') >= 0:
				dims = re.search('dims List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
				try: current.dim0 = dims[0]; current.dim1 = dims[1]; current.dim2 = dims[2]; current.dim3 = dims[3]; current.dim4 = dims[4]
				except: pass
				bitwidth = re.search('bw = ([0-9]+)', line).group(1)
				try: current.bitwidth = bitwidth
				except: pass
				print bitwidth
			if line.find('nBanks') >= 0:
				Ns = re.search('nBanks = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
				try: current.N0 = Ns[0]; current.N1 = Ns[1]; current.N2 = Ns[2]; current.N3 = Ns[3]; current.N4 = Ns[4]
				except: pass
				Bs = re.search('B = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
				try: current.B0 = Bs[0]; current.B1 = Bs[1]; current.B2 = Bs[2]; current.B3 = Bs[3]; current.B4 = Bs[4]
				except: pass
				As = re.search('a = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
				try: current.a0 = As[0]; current.a1 = As[1]; current.a2 = As[2]; current.a3 = As[3]; current.a4 = As[4]
				except: pass
				ps = re.search('p = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
				try: current.p0 = ps[0]; current.p1 = ps[1]; current.p2 = ps[2]; current.p3 = ps[3]; current.p4 = ps[4]
				except: pass
			if line.find('muxwidth') >= 0:
				hist = re.findall('>([0-9]+)<', line)
				if len(hist) > 9:
					print "ERROR: %s in %s has more than 3 groups in its histogram!!! Dropping it..."
					current = None
				try: current.hist0muxwidth = hist[0]; current.hist0rlanes = hist[1]; current.hist0wlanes = hist[2]; current.hist1muxwidth = hist[3]; current.hist1rlanes = hist[4]; current.hist1wlanes = hist[5]; current.hist2muxwidth = hist[6]; current.hist2rlanes = hist[7]; current.hist2wlanes = hist[8]
				except: pass
			if line.find('</font></div></p>') >= 0:
				app_sram_dict = app_sram_dict + [current]
				current = None


	# Find PaR report
	rpt = '/'.join(app.split('/')[0:-2] + ['verilog-' + target, 'par_utilization_hierarchical.rpt'])
	# Collect resource utilization for each sram
	for i in reversed(range(0,len(app_sram_dict))):
		mem = app_sram_dict[i]
		try:
			with open(rpt, 'r') as file:
				results = file.read().split('\n')
			sym = mem.localname
			for line in results:
				if (re.compile('^\|[ ]+' + sym + '.*').match(line)):
					results = line.replace(' ','').split('|')[3:-1]
					results = [re.sub('\(.*\)','',x) for x in results]
					mem.LUTs = results[0]
					mem.LaL = results[1]
					mem.LaM = results[2]
					mem.SRLs = results[3]
					mem.FFs = results[4]
					mem.RAMB32 = results[5]
					mem.RAMB18 = results[6]
					mem.URAM = results[7]
					mem.DSPs = results[8]
		except:
			print("No results for " + mem.fullname)
			del app_sram_dict[i]

	# Add srams to master list
	node_dict = node_dict + app_sram_dict

# Collect other nodes data
datadir='/home/mattfel/regression/synth/' + target + '/current-spatial/spatial/gen/' + capstarget
irhtml='IR.html'
allctrltrees=[]
for root, dirs, files in os.walk(datadir):
    if ctrltree in files:
       f_name = os.path.join(root, ctrltree)
       allctrltrees.append(f_name)
for app in allctrltrees:
	# Get html
	appname = app.split('/')[-3]
	with open(app, 'r') as file:
		data = file.read().split('\n')
	for line in data:
		if (line.find("Fix") >= 0):
			print line



node_dict[0].printAllFields()
for node in node_dict:
	node.printAll()

