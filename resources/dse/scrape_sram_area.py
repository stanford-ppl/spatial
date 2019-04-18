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
	sgn = ""
	dec = ""
	frac = ""
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
	consta = ""
	constb = ""
	constc = ""

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
		print("nbufs,bitwidth,sgn,dec,frac,dim0,dim1,dim2,dim3,dim4,N0,N1,N2,N3,N4,B0,B1,B2,B3,B4,a0,a1,a2,a3,a4,p0,p1,p2,p3,p4,hist0muxwidth,hist0rlanes,hist0wlanes,hist1muxwidth,hist1rlanes,hist1wlanes,hist2muxwidth,hist2rlanes,hist2wlanes,consta,constb,constc")
	def printProperties(this):
		print("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s" % (this.nbufs,this.bitwidth,this.sgn,this.dec,this.frac,this.dim0,this.dim1,this.dim2,this.dim3,this.dim4,this.N0,this.N1,this.N2,this.N3,this.N4,this.B0,this.B1,this.B2,this.B3,this.B4,this.a0,this.a1,this.a2,this.a3,this.a4,this.p0,this.p1,this.p2,this.p3,this.p4,this.hist0muxwidth,this.hist0rlanes,this.hist0wlanes,this.hist1muxwidth,this.hist1rlanes,this.hist1wlanes,this.hist2muxwidth,this.hist2rlanes,this.hist2wlanes,this.consta,this.constb,this.constc))
	def printAllFields(this):
		print("fullname,localname,nodetype,nbufs,bitwidth,sgn,dec,frac,dim0,dim1,dim2,dim3,dim4,N0,N1,N2,N3,N4,B0,B1,B2,B3,B4,a0,a1,a2,a3,a4,p0,p1,p2,p3,p4,hist0muxwidth,hist0rlanes,hist0wlanes,hist1muxwidth,hist1rlanes,hist1wlanes,hist2muxwidth,hist2rlanes,hist2wlanes,consta,constb,constc,LUTs,LaL,LaM,SRLs,FFs,RAMB32,RAMB18,URAM,DSPs")
	def printAll(this):
		print("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s" % (this.fullname, this.localname, this.nodetype, this.nbufs,this.bitwidth,this.sgn,this.dec,this.frac,this.dim0,this.dim1,this.dim2,this.dim3,this.dim4,this.N0,this.N1,this.N2,this.N3,this.N4,this.B0,this.B1,this.B2,this.B3,this.B4,this.a0,this.a1,this.a2,this.a3,this.a4,this.p0,this.p1,this.p2,this.p3,this.p4,this.hist0muxwidth,this.hist0rlanes,this.hist0wlanes,this.hist1muxwidth,this.hist1rlanes,this.hist1wlanes,this.hist2muxwidth,this.hist2rlanes,this.hist2wlanes,this.consta,this.constb,this.constc,this.LUTs, this.LaL, this.LaM, this.SRLs, this.FFs, this.RAMB32, this.RAMB18, this.URAM, this.DSPs))


################
## PROPERTIES ##
################
target = 'zcu' # i.e. out0/verilog-zcu/
capstarget = 'ZCU'  #i.e. gen/ZCU/
spatialdir = '/home/mattfel/regression/synth/' + target + '/current-spatial/spatial/'
gendir = spatialdir + 'gen/' + capstarget
logdir = spatialdir + 'logs/' + capstarget
# spatialdir='/home/mattfel/sp_area/spatial/out0'
# gendir = spatialdir
# logdir = spatialdir + 'logs/'


def scrapeFor(node,rpt): 
	try:
		with open(rpt, 'r') as file:
			results = file.read().split('\n')
		sym = node.localname
		# print "find %s in %s" % (sym, rpt)
		for line in results:
			if (re.compile('^\|[ ]+' + sym + '.*').match(line)):
				results = line.replace(' ','').split('|')[3:-1]
				results = [re.sub('\(.*\)','',x) for x in results]
				node.LUTs = results[0]
				node.LaL = results[1]
				node.LaM = results[2]
				node.SRLs = results[3]
				node.FFs = results[4]
				node.RAMB32 = results[5]
				node.RAMB18 = results[6]
				node.URAM = results[7]
				node.DSPs = results[8]
				break
		return True
	except:
		print("No results for " + node.fullname)
		return False

# Collect Mem Data
def collectMemData():
	ctrltree='controller_tree.html'
	allctrltrees=[]
	for root, dirs, files in os.walk(gendir):
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
		app_mem_dict = []
		# Crawl and collect info on each sram
		for line in entries:
			# Start new node
			if line.find('BankedSRAM') >= 0:
				sym = re.search('x[0-9]+', line).group(0)
				current = Node(sym + "_" + appname, sym, "SRAMNew")
				current.nbufs = '1' # default of 1
			elif line.find('LineBuffer') >= 0:
				sym = re.search('x[0-9]+', line).group(0)
				current = Node(sym + "_" + appname, sym, "LineBufferNew")
				current.nbufs = '1' # default of 1
			elif line.find('FIFO ') >= 0:
				sym = re.search('x[0-9]+', line).group(0)
				current = Node(sym + "_" + appname, sym, "FIFONew")
				current.nbufs = '1' # default of 1
			elif line.find('LIFO') >= 0:
				sym = re.search('x[0-9]+', line).group(0)
				current = Node(sym + "_" + appname, sym, "LIFONew")
				current.nbufs = '1' # default of 1
			elif line.find('(FF') >= 0:
				sym = re.search('x[0-9]+', line).group(0)
				current = Node(sym + "_" + appname, sym, "RegNew")
				current.nbufs = '1' # default of 1
			elif line.find('ShiftRegFile') >= 0:
				sym = re.search('x[0-9]+', line).group(0)
				current = Node(sym + "_" + appname, sym, "RegFileNew")
				current.nbufs = '1' # default of 1


			# Scrape info if currently scanning node
			if (current != None): 
				# Get buffer info
				if line.find('nBufs =') >= 0:
					current.nbufs = int(re.search('nBufs = ([0-9]+)', line).group(1))
				# Get bitwidth/dims info
				if line.find('>volume =') >= 0 and current.nodetype != "RegNew":
					dims = re.search('dims List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
					try: current.dim0 = dims[0]; current.dim1 = dims[1]; current.dim2 = dims[2]; current.dim3 = dims[3]; current.dim4 = dims[4]
					except: pass
				if line.find('>volume =') >= 0:
					bitwidth = re.search('bw = ([0-9]+)', line).group(1)
					try: current.bitwidth = bitwidth
					except: pass
				# Get banking info
				if line.find('nBanks') >= 0 and current.nodetype != "RegNew":
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
				# Get histogram info
				if line.find('muxwidth') >= 0:
					hist = re.findall('>([0-9]+)<', line)
					if len(hist) > 9:
						print "ERROR: %s in %s has more than 3 groups in its histogram!!! Dropping it..."
						current = None
					try: current.hist0muxwidth = hist[0]; current.hist0rlanes = hist[1]; current.hist0wlanes = hist[2]; current.hist1muxwidth = hist[3]; current.hist1rlanes = hist[4]; current.hist1wlanes = hist[5]; current.hist2muxwidth = hist[6]; current.hist2rlanes = hist[7]; current.hist2wlanes = hist[8]
					except: pass
				if line.find('</font></div></p>') >= 0:
					app_mem_dict = app_mem_dict + [current]
					current = None


		# Find PaR report
		rpt = '/'.join(app.split('/')[0:-2] + ['verilog-' + target, 'par_utilization_hierarchical.rpt'])
		# Collect resource utilization for each sram
		for i in reversed(range(0,len(app_mem_dict))):
			mem = app_mem_dict[i]
			success = scrapeFor(mem,rpt)
			if (not success): del app_mem_dict[i]

		# Add srams to master list
		node_dict = node_dict + app_mem_dict

	return node_dict

def collectIRNodeData():
	# Collect other nodes data
	irhtml='IR.html'
	node_dict = []
	allirhtmls=[]
	for root, dirs, files in os.walk(gendir):
	    if irhtml in files:
	       f_name = os.path.join(root, irhtml)
	       allirhtmls.append(f_name)
	for app in allirhtmls:
		current = None
		app_node_dict = []
		appname = app.split('/')[-3]
		with open(app, 'r') as file:
			data = file.read().split('\n')
		for line in data:
			# Start new node
			if (line.find("FixMul") >= 0):
				sym = re.search('id=(x[0-9]+)>', line).group(1)
				current = Node(sym + "_" + appname, sym, "FixMul")
				current.nbufs = '1' # default of 1
			elif (line.find("FixDiv") >= 0):
				sym = re.search('id=(x[0-9]+)>', line).group(1)
				current = Node(sym + "_" + appname, sym, "FixDiv")
				current.nbufs = '1' # default of 1
			elif (line.find("FixAdd") >= 0):
				sym = re.search('id=(x[0-9]+)>', line).group(1)
				current = Node(sym + "_" + appname, sym, "FixAdd")
				current.nbufs = '1' # default of 1
			elif (line.find("FixSub") >= 0):
				sym = re.search('id=(x[0-9]+)>', line).group(1)
				current = Node(sym + "_" + appname, sym, "FixSub")
				current.nbufs = '1' # default of 1

			# Scrape info if currently scanning node
			if (current != None): 
				# Get buffer info
				if line.find('>Type<') >= 0:
					dec = re.search(',\_([0-9]+),',line).group(1)
					frac = re.search(',\_([0-9]+)\]',line).group(1)
					sgn = '1' if (line.find('[TRUE,') >= 0) else '0'
					try: current.dec = dec; current.frac = frac; current.sgn = sgn
					except: pass
				if line.find('<h3 id=') >= 0 and line.find('id=' + sym) == -1:
					app_node_dict = app_node_dict + [current]
					current = None

		# Find PaR report
		rpt = '/'.join(app.split('/')[0:-2] + ['verilog-' + target, 'par_utilization_hierarchical.rpt'])
		# Collect resource utilization for each sram
		for i in reversed(range(0,len(app_node_dict))):
			mem = app_node_dict[i]
			success = scrapeFor(mem,rpt)
			if (not success): del app_node_dict[i]

		# Add srams to master list
		node_dict = node_dict + app_node_dict

	return node_dict


def main():
	node_dict = []
	node_dict = collectMemData()
	# node_dict = node_dict + collectIRNodeData()

	node_dict[0].printAllFields()
	for node in node_dict:
		node.printAll()

if __name__ == "__main__":
    main()
