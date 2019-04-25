# Scrape xilinx synth directories for sram resource utilizations

import re
import csv
import os
from shutil import copyfile
from pyquery import PyQuery as pq
import argparse
import sys

################
## PROPERTIES ##
################
requirerpt = True # Filter out apps that have no synth log?
target = 'zcu' # i.e. out0/verilog-zcu/
capstarget = 'ZCU'  #i.e. gen/ZCU/
scrapetype = 'par' # either scrape synth_ or par_ log
spatialdir = '/home/mattfel/regression/synth/' + target + '/last-spatial/spatial/'
gendir = spatialdir + 'gen/' + capstarget
logdir = spatialdir + 'logs/' + capstarget
# spatialdir='/home/mattfel/sp_area/spatial/out0'
# gendir = spatialdir
# logdir = spatialdir + 'logs/'



##########
## CODE ##
##########
outputs = ["LUTs", "LaL", "LaM", "SRLs", "FFs", "RAMB32", "RAMB18", "URAM", "DSPs"]
setups = ["fullname", "localname", "nodetype"]
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
	tp_sgn = ""
	tp_dec = ""
	tp_frac = ""
	tp_srcsgn = ""
	tp_srcdec = ""
	tp_srcfrac = ""
	mem_bitwidth = ""
	mem_nbufs = ""
	mem_dim0 = ""
	mem_dim1 = ""
	mem_dim2 = ""
	mem_dim3 = ""
	mem_dim4 = ""
	mem_N0 = ""
	mem_N1 = ""
	mem_N2 = ""
	mem_N3 = ""
	mem_N4 = ""
	mem_B0 = ""
	mem_B1 = ""
	mem_B2 = ""
	mem_B3 = ""
	mem_B4 = ""
	mem_a0 = ""
	mem_a1 = ""
	mem_a2 = ""
	mem_a3 = ""
	mem_a4 = ""
	mem_p0 = ""
	mem_p1 = ""
	mem_p2 = ""
	mem_p3 = ""
	mem_p4 = ""
	mem_hist0muxwidth = ""
	mem_hist0rlanes = ""
	mem_hist0wlanes = ""
	mem_hist1muxwidth = ""
	mem_hist1rlanes = ""
	mem_hist1wlanes = ""
	mem_hist2muxwidth = ""
	mem_hist2rlanes = ""
	mem_hist2wlanes = ""
	op_consta = ""
	op_constb = ""
	op_constc = ""
	ctrl_level = ""
	ctrl_style = ""
	ctrl_children = ""
	ctrl_ii = ""
	ctrl_lat = ""
	cchain_ctr0start = ""
	cchain_ctr0stop = ""
	cchain_ctr0step = ""
	cchain_ctr0par = ""
	cchain_ctr1start = ""
	cchain_ctr1stop = ""
	cchain_ctr1step = ""
	cchain_ctr1par = ""
	cchain_ctr2start = ""
	cchain_ctr2stop = ""
	cchain_ctr2step = ""
	cchain_ctr2par = ""
	cchain_ctr3start = ""
	cchain_ctr3stop = ""
	cchain_ctr3step = ""
	cchain_ctr3par = ""
	cchain_ctr4start = ""
	cchain_ctr4stop = ""
	cchain_ctr4step = ""
	cchain_ctr4par = ""

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

	def _getAllFields(this): 
		return setups + [attr for attr in dir(this) if (not attr.startswith('_')) and (attr not in setups) and (attr not in outputs)] + outputs
	def _printAllFields(this): 
		print ','.join(this._getAllFields())
	def _printAll(this):
		payload = [str(getattr(this, attrname)) for attrname in this._getAllFields()]
		print ','.join(payload)



def scrapeFor(node,rpt,sfx=''): 
	try:
		with open(rpt, 'r') as file:
			results = file.read().split('\n')
		sym = node.localname
		ecode = 1
		for line in results:
			if (re.compile('^\|[ ]+' + sym + sfx + '.*').match(line)):
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
				ecode = 0
				break
	except:
		ecode = 2


	if ecode == 0: return True
	elif ecode == 1: print("No results for " + node.fullname + " (" + node.nodetype + ")"); return False
	elif ecode == 2: print("Faulty results for " + node.fullname + " (" + node.nodetype + ")"); return False



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
		# Find PaR report
		rpt = '/'.join(app.split('/')[0:-2] + ['verilog-' + target, scrapetype + '_utilization_hierarchical.rpt'])
		# Get html
		appname = app.split('/')[-3]

		if (not requirerpt or os.path.isfile(rpt)) and os.path.isfile(app): 
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
					current.mem_nbufs = '1' # default of 1
				elif line.find('LineBuffer') >= 0:
					sym = re.search('x[0-9]+', line).group(0)
					current = Node(sym + "_" + appname, sym, "LineBufferNew")
					current.mem_nbufs = '1' # default of 1
				elif line.find('FIFO ') >= 0:
					sym = re.search('x[0-9]+', line).group(0)
					current = Node(sym + "_" + appname, sym, "FIFONew")
					current.mem_nbufs = '1' # default of 1
				elif line.find('LIFO') >= 0:
					sym = re.search('x[0-9]+', line).group(0)
					current = Node(sym + "_" + appname, sym, "LIFONew")
					current.mem_nbufs = '1' # default of 1
				elif line.find('(FF') >= 0:
					sym = re.search('x[0-9]+', line).group(0)
					current = Node(sym + "_" + appname, sym, "RegNew")
					current.mem_nbufs = '1' # default of 1
				elif line.find('ShiftRegFile') >= 0:
					sym = re.search('x[0-9]+', line).group(0)
					current = Node(sym + "_" + appname, sym, "RegFileNew")
					current.mem_nbufs = '1' # default of 1


				# Scrape info if currently scanning node
				if (current != None): 
					# Get buffer info
					if line.find('nBufs =') >= 0:
						current.mem_nbufs = int(re.search('nBufs = ([0-9]+)', line).group(1))
					# Get bitwidth/dims info
					if line.find('>volume =') >= 0 and current.nodetype != "RegNew":
						dims = re.search('dims List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
						try: current.mem_dim0 = dims[0]; current.mem_dim1 = dims[1]; current.mem_dim2 = dims[2]; current.mem_dim3 = dims[3]; current.mem_dim4 = dims[4]
						except: pass
					if line.find('>volume =') >= 0:
						bitwidth = re.search('bw = ([0-9]+)', line).group(1)
						try: current.mem_bitwidth = bitwidth
						except: pass
					# Get banking info
					if line.find('nBanks') >= 0 and current.nodetype != "RegNew":
						Ns = re.search('nBanks = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
						try: current.mem_N0 = Ns[0]; current.mem_N1 = Ns[1]; current.mem_N2 = Ns[2]; current.mem_N3 = Ns[3]; current.mem_N4 = Ns[4]
						except: pass
						Bs = re.search('B = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
						try: current.mem_B0 = Bs[0]; current.mem_B1 = Bs[1]; current.mem_B2 = Bs[2]; current.mem_B3 = Bs[3]; current.mem_B4 = Bs[4]
						except: pass
						As = re.search('a = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
						try: current.mem_a0 = As[0]; current.mem_a1 = As[1]; current.mem_a2 = As[2]; current.mem_a3 = As[3]; current.mem_a4 = As[4]
						except: pass
						ps = re.search('p = List\(([0-9, ]+)\)',line).group(1).replace(' ','').split(',')
						try: current.mem_p0 = ps[0]; current.mem_p1 = ps[1]; current.mem_p2 = ps[2]; current.mem_p3 = ps[3]; current.mem_p4 = ps[4]
						except: pass
					# Get histogram info
					if line.find('muxwidth') >= 0:
						hist = re.findall('>([0-9]+)<', line)
						if len(hist) > 9:
							print "ERROR: %s in %s has more than 3 groups in its histogram!!! Dropping it..."
							current = None
						try: current.mem_hist0muxwidth = hist[0]; current.mem_hist0rlanes = hist[1]; current.mem_hist0wlanes = hist[2]; current.mem_hist1muxwidth = hist[3]; current.mem_hist1rlanes = hist[4]; current.mem_hist1wlanes = hist[5]; current.mem_hist2muxwidth = hist[6]; current.mem_hist2rlanes = hist[7]; current.mem_hist2wlanes = hist[8]
						except: pass
					if line.find('</font></div></p>') >= 0:
						app_mem_dict = app_mem_dict + [current]
						current = None


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
		# Find PaR report
		rpt = '/'.join(app.split('/')[0:-2] + ['verilog-' + target, scrapetype + '_utilization_hierarchical.rpt'])
		if (not requirerpt or os.path.isfile(rpt)) and os.path.isfile(app): 

			current = None
			app_node_dict = []
			appname = app.split('/')[-3]
			with open(app, 'r') as file:
				data = file.read().split('\n')
			for line in data:
				# Start new node
				declaration = False
				if (line.find("FixMul") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixMul")
				elif (line.find("FixDiv") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixDiv")
				elif (line.find("FixAdd") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixAdd")
				elif (line.find("FixSub") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixSub")
				elif (line.find("FixFMA") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixFMA")
				elif (line.find("UnbSatMul") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "UnbSatMul")
				elif (line.find("SatMul") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "SatMul")
				elif (line.find("UnbMul") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "UnbMul")
				elif (line.find("FixToFix(") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixToFix")
				elif (line.find("FixToFixUnb(") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixToFixUnb")
				elif (line.find("FixToFixSat(") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixToFixSat")
				elif (line.find("FixToFixUnbSat(") >= 0):
					declaration = True
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "FixToFixUnbSat")

				# Pull out constants:
				if declaration: 
					try: current.op_consta = re.search('a=([0-9\.]+)',line).group(1)
					except: pass
					try: current.op_constb = re.search('b=([0-9\.]+)',line).group(1)
					except: pass
					try: current.op_constc = re.search('c=([0-9\.]+)',line).group(1)
					except: pass
					try: current.tp_sgn = sgn = '1' if (line.find('[TRUE,') >= 0) else '0'; current.tp_dec = re.search(',\_([0-9]+),',line).group(1); current.tp_frac = frac = re.search(',\_([0-9]+)\]',line).group(1)
					except: pass
					try: current.tp_sgn = sgn = '1' if (line.find('s=TRUE') >= 0) else '0'; current.tp_dec = re.search(',i=\_([0-9]+),',line).group(1); current.tp_frac = frac = re.search(',f=\_([0-9]+)',line).group(1)
					except: pass

				# Scrape info if currently scanning node
				if (current != None): 
					# Get buffer info
					if line.find('>Type<') >= 0:
						dec = re.search(',\_([0-9]+),',line).group(1)
						frac = re.search(',\_([0-9]+)\]',line).group(1)
						sgn = '1' if (line.find('[TRUE,') >= 0) else '0'
						try: current.tp_dec = dec; current.tp_frac = frac; current.tp_sgn = sgn
						except: pass
					if line.find('>SrcType<') >= 0:
						dec = re.search(',\_([0-9]+),',line).group(1)
						frac = re.search(',\_([0-9]+)\]',line).group(1)
						sgn = '1' if (line.find('[TRUE,') >= 0) else '0'
						try: current.tp_srcdec = dec; current.tp_srcfrac = frac; current.tp_srcsgn = sgn
						except: pass
					if line.find('<h3 id=') >= 0 and line.find('id=' + sym) == -1:
						app_node_dict = app_node_dict + [current]
						current = None

			# Collect resource utilization for each sram
			for i in reversed(range(0,len(app_node_dict))):
				mem = app_node_dict[i]
				success = scrapeFor(mem,rpt)
				if (not success): del app_node_dict[i]

			# Add srams to master list
			node_dict = node_dict + app_node_dict

	return node_dict

def collectSMData():
	# Collect other nodes data
	irhtml='IR.html'
	node_dict = []
	allirhtmls=[]
	for root, dirs, files in os.walk(gendir):
	    if irhtml in files:
	       f_name = os.path.join(root, irhtml)
	       allirhtmls.append(f_name)
	for app in allirhtmls:
		# Create holding pen for CounterNew 
		ctr_pool = []
		# Find PaR report
		rpt = '/'.join(app.split('/')[0:-2] + ['verilog-' + target, scrapetype + '_utilization_hierarchical.rpt'])
		if (not requirerpt or os.path.isfile(rpt)) and os.path.isfile(app): 
			current = None
			app_node_dict = []
			appname = app.split('/')[-3]
			with open(app, 'r') as file:
				data = file.read().split('\n')
			for line in data:
				# Handle CounterNew and CounterChains
				if (line.find("CounterNew") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "CounterNew"); 
					setattr(current, "cchain_ctr0par", re.search('par=([0-9]+)', line).group(1))
					try: setattr(current, "cchain_ctr0start", re.search('start=([0-9]+)', line).group(1))
					except: pass
					try: setattr(current, "cchain_ctr0stop", re.search('end=([0-9]+)', line).group(1))
					except: pass
					try: setattr(current, "cchain_ctr0step", re.search('step=([0-9]+)', line).group(1))
					except: pass
					ctr_pool = ctr_pool + [current]
					current = None
				if (line.find("ForeverNew") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "ForeverNew"); 
					setattr(current, "cchain_ctr0par", '1')
					setattr(current, "cchain_ctr0start", '0')
					setattr(current, "cchain_ctr0stop", 'inf')
					setattr(current, "cchain_ctr0step", '1')
					ctr_pool = ctr_pool + [current]
					current = None
				elif (line.find("CounterChainNew") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "CounterChainNew"); 
					ctrs = [x for x in list(set(re.findall('x[0-9]+',line))) if x != sym]
					print "want %s" % ','.join(ctrs) 
					inctr = 0
					for ctr in ctrs:
						cc = [x for x in ctr_pool if x.localname == ctr][0]
						setattr(current, "cchain_ctr" + str(inctr) + "par", cc.cchain_ctr0par)
						setattr(current, "cchain_ctr" + str(inctr) + "start", cc.cchain_ctr0start)
						setattr(current, "cchain_ctr" + str(inctr) + "stop", cc.cchain_ctr0stop)
						setattr(current, "cchain_ctr" + str(inctr) + "step", cc.cchain_ctr0step)
						inctr = inctr + 1
					app_node_dict = app_node_dict + [current]
					current = None
				elif (line.find("UnrolledForeach") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "UnrolledForeach"); current.ctrl_style = "Pipelined"
				elif (line.find("ParallelPipe") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "ParallelPipe"); current.ctrl_style = "ForkJoin"
				elif (line.find("UnitPipe") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "UnitPipe"); current.ctrl_style = "Pipelined"
				elif (line.find("UnrolledReduce") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "UnrolledReduce"); current.ctrl_style = "Pipelined"
				elif (line.find("StateMachine") >= 0):
					sym = re.search('id=(x[0-9]+)>', line).group(1)
					current = Node(sym + "_" + appname, sym, "StateMachine"); current.ctrl_style = "Sequenced"

				# Scrape info if currently scanning node
				if (current != None and current.nodetype != "CounterNew" and current.nodetype != "CounterChainNew"): 
					# Get buffer info
					if line.find('>InitiationInterval<') >= 0:
						ii = re.search('([0-9]+)', line).group(1) # should be only number in this line
						current.ctrl_ii = ii
					elif line.find('>BodyLatency<') >= 0:
						lat = re.search('([0-9]+)', line).group(1) # should be only number in this line
						current.ctrl_lat = lat
					elif line.find('>ControlLevel<') >= 0:
						level = "Inner" if ("Inner" in line) else "Outer"
						current.ctrl_level = level
					elif line.find('>Children<') >= 0:
						children = line.count("Ctrl")
						current.ctrl_children = children
					elif line.find('>UserScheduleDirective<') >= 0:
						style = re.search(': ([a-zA-Z]+)', line).group(1)
						current.ctrl_style = style
					elif line.find('<h3 id=') >= 0 and line.find('id=' + sym) == -1:
						app_node_dict = app_node_dict + [current]
						current = None

			# Collect resource utilization for each sram
			for i in reversed(range(0,len(app_node_dict))):
				mem = app_node_dict[i]
				if mem.nodetype != "CounterChainNew": success = scrapeFor(mem,rpt,".*sm")
				else: success = scrapeFor(mem,rpt)
				if (not success): del app_node_dict[i]

			# Add srams to master list
			node_dict = node_dict + app_node_dict

	return node_dict


def main():
	node_dict = []
	node_dict = node_dict + collectMemData()
	node_dict = node_dict + collectIRNodeData()
	node_dict = node_dict + collectSMData()

	node_dict[0]._printAllFields()
	for node in node_dict:
		node._printAll()

if __name__ == "__main__":
    main()
