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

class Fringe:
	def __init__(this, appname, streams):
		this.appname = appname
		this.streams = streams

	streams = []
	appname = ""

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
		return ['appname'] + outputs + sum([attr._getAllFields() for attr in this.streams], [])
	def _printAllFields(this): 
		print ','.join(this._getAllFields())
	def _printAll(this):
		payload = [this.appname] + [str(getattr(this, attrname)) for attrname in outputs] + sum([stream._getAll() for stream in this.streams], [])
		print ','.join(payload)

class Stream:
	def __init__(this, direction, wordwidth, wordpar):
		this.direction = direction
		this.wordwidth = wordwidth
		this.wordpar = wordpar

	## PROPERTIES
	direction = ''
	wordpar = ''
	wordwidth = ''

	def _getAllFields(this): 
		return [attr for attr in dir(this) if (not attr.startswith('_'))]
	def _printAllFields(this): 
		print ','.join([this._getAllFields()])
	def _printAll(this):
		payload = [str(getattr(this, attrname)) for attrname in this._getAllFields()]
		print ','.join(payload)
	def _getAll(this):
		return [str(getattr(this, attrname)) for attrname in this._getAllFields()]


def scrapeFor(fringe,rpt,sfx=''): 
	try:
		with open(rpt, 'r') as file:
			results = file.read().split('\n')
		sym = 'Fringe'
		ecode = 1
		for line in results:
			if (re.compile('^\|[ ]+' + sym + sfx + '.*').match(line)):
				results = line.replace(' ','').split('|')[3:-1]
				results = [re.sub('\(.*\)','',x) for x in results]
				fringe.LUTs = results[0]
				fringe.LaL = results[1]
				fringe.LaM = results[2]
				fringe.SRLs = results[3]
				fringe.FFs = results[4]
				fringe.RAMB32 = results[5]
				fringe.RAMB18 = results[6]
				fringe.URAM = results[7]
				fringe.DSPs = results[8]
				ecode = 0
				break
	except:
		ecode = 2


	if ecode == 0: return True
	elif ecode == 1: print("No results for " + fringe.appname); return False
	elif ecode == 2: print("Faulty results for " + fringe.appname); return False


def collectFringeData():
	# Collect other nodes data
	iochisel='IOModule.scala'
	app_dict = []
	allchiselfiles=[]
	for root, dirs, files in os.walk(gendir):
	    if iochisel in files:
	       f_name = os.path.join(root, iochisel)
	       allchiselfiles.append(f_name)
	for app in allchiselfiles:
		# Find PaR report
		rpt = '/'.join(app.split('/')[0:-2] + ['verilog-' + target, scrapetype + '_utilization_hierarchical.rpt'])

		if (not requirerpt or os.path.isfile(rpt)) and os.path.isfile(app): 
			app_streams = []
			appname = app.split('/')[-3]
			with open(app, 'r') as file:
				data = file.read().split('\n')
			for line in data:
				if (line.find("io_loadStreamInfo =") >= 0):
					loadstreams = re.findall('StreamParInfo\(([0-9]+), ([0-9]+), [0-9]+\)',line)
				elif (line.find("io_storeStreamInfo =") >= 0):
					storestreams = re.findall('StreamParInfo\(([0-9]+), ([0-9]+), [0-9]+\)',line)
				elif (line.find("io_gatherStreamInfo =") >= 0):
					gatherstreams = re.findall('StreamParInfo\(([0-9]+), ([0-9]+), [0-9]+\)',line)
				elif (line.find("io_scatterStreamInfo =") >= 0):
					scatterstreams = re.findall('StreamParInfo\(([0-9]+), ([0-9]+), [0-9]+\)',line)

			for stream in loadstreams:
				app_streams = app_streams + [Stream('load', stream[0], stream[1])]
			for stream in storestreams:
				app_streams = app_streams + [Stream('store', stream[0], stream[1])]
			for stream in gatherstreams:
				app_streams = app_streams + [Stream('gather', stream[0], stream[1])]
			for stream in scatterstreams:
				app_streams = app_streams + [Stream('scatter', stream[0], stream[1])]

			# Add srams to master list
			app_fringe = Fringe(appname, app_streams)
			success = scrapeFor(app_fringe, rpt)
			if (success): app_dict = app_dict + [app_fringe]

	return app_dict


def main():
	app_dict = []
	app_dict = app_dict + collectFringeData()

	streamprinter = Stream('1','1','1')
	fringeprinter = Fringe('1',[streamprinter])
	fringeprinter._printAllFields()
	for node in app_dict:
		node._printAll()

if __name__ == "__main__":
    main()
