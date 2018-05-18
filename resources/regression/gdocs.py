# This is called by regression_run.sh / scrape.sh / regression_functions.sh / receive.sh / synth_launcher.sh / synth_regression.sh

import gspread
import pygsheets
import sys
import os
from oauth2client.service_account import ServiceAccountCredentials
import datetime
from datetime import datetime, timezone
import time
import socket

def write(wksh, row, col, txt):
	try:
		if (col > wksh.cols):
        		wksh.insert_cols(col-1, inherit=True)
		wksh.update_cell((row,col),txt)
	except:
		print("WARN: pygsheets failed write %s @ %d,%d... -_-" % (txt, row, col))

def readAllVals(wksh):
	try:
		return wksh.get_all_values()
	except:
		print("WARN: pygsheets failed readAllVals... -_-")
		exit()

def getCol(wksh, appname):
	try: 
		lol = readAllVals(wksh)
		if (appname in lol[0]):
			col=lol[0].index(appname)+1
		else:
			col=len(lol[0])+1
			write(wksh,1,col,appname)	
		return col
	except:
		print("ERROR: pygsheets failed getCol... -_-")	
		exit()

def getRuntimeCol(wksh, appname):
	try: 
		lol = readAllVals(wksh)
		if (appname in lol[0]):
			col=lol[0].index(appname)+1
		else:
			col=len(lol[0])+1
			write(wksh,1,col,appname)
		return col
	except:
		print("ERROR: pygsheets failed getRuntimeCol... -_-")	
		exit()

def getDoc(title):
	# # gspread auth
	# json_key = '/home/mattfel/regression/synth/key.json'
	# scope = [
	#     'https://spreadsheets.google.com/feeds',
	#     'https://www.googleapis.com/auth/drive'
	# ]
	# credentials = ServiceAccountCredentials.from_json_keyfile_name(json_key, scope)

	# pygsheets auth
	json_key = '/home/mattfel/regression/synth/pygsheets_key.json'
	gc = pygsheets.authorize(outh_file = json_key)

	if (title == "vcs-noretime"):
		try: 
			sh = gc.open_by_key("/")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "vcs"):
		try: 
			sh = gc.open_by_key("")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "Zynq"):
		try: 
			sh = gc.open_by_key("1nFzTcIFbw182cLUFZiGnezeR43ofV2NOYsRp2aemhAA")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "AWS"):
		try: 
			sh = gc.open_by_key("")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "ZCU"):
		try: 
			sh = gc.open_by_key("")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "Arria10"):
		try: 
			sh = gc.open_by_key("")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	else:
		print("No spreadsheet for " + title)
		exit()

	return sh

def getWord(title):
	if (title == "Zynq"):
		return "Slice"
	elif (title == "ZCU"):
		return "CLB"
	elif (title == "Arria10"):
		return "CLB"  # TODO: Tian
	elif (title == "AWS"):
		return "CLB"
	else:
		return "N/A"

def getRow(sh, hash, apphash):
	worksheet = sh.worksheet('index', 0)
	lol = readAllVals(worksheet)
	row = -1
	for i in range(2, len(lol)):
		if (lol[i][0] == hash and lol[i][1] == apphash and lol[i][4] == socket.gethostname()):
			row = i + 1
			break
	if (row == -1):	print("ERROR: Could not find row for %s, %s" % (hash, apphash))
	return row

def isPerf(title):
	if (title == "Zynq"):
		perf=False
	elif (title == "ZCU"):
		perf=False
	elif (title == "Arria10"):
		perf=False
	elif (title == "AWS"):
		perf=False
	elif (title == "fpga"):
		perf=True
	elif (title == "develop"):
		perf=True
	elif (title == "retime"):
		perf=True
	elif (title == "syncMem"):
		perf=True
	elif (title == "pre-master"):
		perf=True
	elif (title == "master"):
		perf=True
	else:
		print("No spreadsheet for " + title)
		exit()

	return perf




def report_regression_results(branch, appname, passed, cycles, hash, apphash, csv, args):
	sh = getDoc(branch)
	row = getRow(sh, hash, apphash)

	# Page 0 - Timestamps
	stamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
	worksheet = sh.worksheet_by_title('Timestamps')
	col = getCol(worksheet, appname)
	write(worksheet, row,col, stamp)

	# Page 1 - Runtime
	worksheet = sh.worksheet_by_title('Runtime')
	col = getRuntimeCol(worksheet, appname)
	write(worksheet, 2,  col,  args)
	write(worksheet, row,col,  cycles)
	write(worksheet, row,col+1,passed)

	# Page 2 - Properties
	worksheet = sh.worksheet_by_title('Properties')
	col = getCol(worksheet, appname)
	write(worksheet, row,col,passed)
	lol = readAllVals(worksheet)
	for prop in csv.split(","):
		# Find row
		found = False
		for i in range(2, len(lol)):
			if (lol[i][0] == prop):
				write(worksheet, i+1, col, prop)
				found = True
		if (found == False):
			write(worksheet, len(lol)+1,1, prop)
			write(worksheet, len(lol),col, prop)

	# Page 3 - STATUS
	worksheet = sh.worksheet_by_title('STATUS')
	worksheet = sh.worksheet_by_title('STATUS')
	write(worksheet,22,3,stamp)
	write(worksheet,22,4,appname)
	write(worksheet,22,5,os.uname()[1])

def report_board_runtime(appname, timeout, runtime, passed, args, backend, locked_board, hash, apphash):
	sh = getDoc(backend)
	row = getRow(sh, hash, apphash)

	# Page 10 - Results
	worksheet = sh.worksheet_by_title("Runtime")
	col = getCol(worksheet, appname)
	if (timeout == "1"):
		write(worksheet, row,col, args + "\nTimed Out!\nFAILED")
	elif (locked_board == "0"):
		write(worksheet, row,col, args + "\n" + runtime + "\n" + passed)
	else:
		write(worksheet, row,col, args + "\n" + locked_board + "\nUnknown?")

def report_synth_results(appname, lut, reg, ram, uram, dsp, lal, lam, synth_time, timing_met, backend, hash, apphash):
	sh = getDoc(backend)
	row = getRow(sh, hash, apphash)
	word = getWord(backend)

	# Page 0 - Timestamps
	stamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

	worksheet = sh.worksheet_by_title('Timestamps')
	col = getCol(worksheet, appname)
	write(worksheet,row,col, stamp)

	# Page 1 - Slice LUT
	worksheet = sh.worksheet_by_title(word + ' LUTs')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,lut)

	# Page 2 - Slice Reg
	worksheet = sh.worksheet_by_title(word + ' Regs')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,reg)

	# Page 3 - Mem
	worksheet = sh.worksheet_by_title('BRAMs')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,ram)

	if (backend == "AWS"):
		# Page 4 - URAM
		worksheet = sh.worksheet_by_title('URAMs')
		col = getCol(worksheet, appname)
		write(worksheet,row,col,uram)

	# Page 5 - DSP
	worksheet = sh.worksheet_by_title('DSPs')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,dsp)

	# Page 6 - LUT as Logic
	worksheet = sh.worksheet_by_title('LUT as Logic')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,lal)

	# Page 7 - LUT as Memory
	worksheet = sh.worksheet_by_title('LUT as Memory')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,lam)

	# Page 8 - Synth time
	worksheet = sh.worksheet_by_title('Synth Time')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,float(synth_time) / 3600.)

	# Page 9 - Timing met
	worksheet = sh.worksheet_by_title('Timing Met')
	col = getCol(worksheet, appname)
	write(worksheet,row,col,timing_met)

	# Tell last update
	worksheet = sh.worksheet_by_title('STATUS')
	write(worksheet,22,3,stamp)
	write(worksheet,22,4,appname)
	write(worksheet,22,5,os.uname()[1])

def dev(arg1, arg2, backend): 
	sh = getDoc(backend)
	perf = isPerf(backend)

	t=time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())

	worksheet = sh.worksheet_by_title("Timestamps")
	
	# Some nonsense to write
	numsheets = len(sh.worksheets())
	for x in range(0,numsheets):
		# worksheet = sh.get_worksheet(x)
		worksheet = sh.worksheet('index', x)
		if (worksheet.title != "STATUS" and worksheet.title != "Properties"):
			worksheet.insert_rows(row = 2, values = [arg1, arg2, t])
			worksheet.delete_rows(75)
		if (worksheet.title == "Properties" and perf):
			worksheet.update_cells('B3:DQ3', [[' ']*120]) # Clear old pass bitmask
	sys.stdout.write(str(3))

def prepare_sheet(hash, apphash, timestamp, backend):
	sh = getDoc(backend)
	perf = isPerf(backend)

	t=time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())

	worksheet = sh.worksheet_by_title("Timestamps")
	lol = worksheet.get_all_values()
	lolhash = [x[0] for x in lol if x[0] != '']
	# id = len(lolhash) + 1
	freq = os.environ['CLOCK_FREQ_MHZ']
	if ("hash" in lol[1]):
		hcol=lol[1].index("hash")
	if ("app hash" in lol[1]):
		acol=lol[1].index("app hash")
	if ("test timestamp" in lol[1]):
		ttcol=lol[1].index("test timestamp")

	# Oldest first
	if (len(lol) < 3): 
		lasthash="NA"
		lastapphash="NA"
		lasttime="2000-01-08 21:15:36"
	else:
		lasthash=lol[2][hcol]
		lastapphash=lol[2][acol]
		lasttime=lol[2][ttcol]

	if (perf):
		new_entry=True
	else:
		new_entry=(lasthash != hash or lastapphash != apphash)
	if (new_entry):
		link='=HYPERLINK("https://github.com/stanford-ppl/spatial/tree/' + hash + '", "' + hash + '")'
		alink=apphash
		numsheets = len(sh.worksheets())
		for x in range(0,numsheets):
			# worksheet = sh.get_worksheet(x)
			worksheet = sh.worksheet('index', x)
			if (worksheet.title != "STATUS" and worksheet.title != "Properties"):
				worksheet.insert_rows(row = 2, values = [link, alink, t, freq + ' MHz', os.uname()[1] ])
				worksheet.delete_rows(75)
				# worksheet.update_cell(id,1, link)
				# worksheet.update_cell(id,2, alink)
				# worksheet.update_cell(id,3, t)
				# worksheet.update_cell(id,4, freq + ' MHz')
				# worksheet.update_cell(id,5, os.uname()[1])
			if (worksheet.title == "Properties" and perf):
				worksheet.update_cells('B3:DQ3', [[' ']*120]) # Clear old pass bitmask
		sys.stdout.write(str(3))
	else:
		# get time difference
		FMT = '%Y-%m-%d %H:%M:%S'
		tdelta = datetime.strptime(t, FMT) - datetime.strptime(lasttime, FMT)
		# Do new test anyway if results are over 24h old
		if (tdelta.total_seconds() > 129600):
			link='=HYPERLINK("https://github.com/stanford-ppl/spatial/tree/' + hash + '", "' + hash + '")'
			alink=apphash
			numsheets = len(sh.worksheets())
			for x in range(0,numsheets):
				# worksheet = sh.get_worksheet(x)
				worksheet = sh.worksheet('index', x)
				if (worksheet.title != "STATUS" and worksheet.title != "Properties"):
					worksheet.insert_rows(row = 2, values = [link, alink, t, freq + ' MHz', os.uname()[1] ])
					worksheet.delete_rows(75)
					# worksheet.update_cell(id,1, link)
					# worksheet.update_cell(id,2, alink)
					# worksheet.update_cell(id,3, t)
					# worksheet.update_cell(id,4, freq + ' MHz')
					# worksheet.update_cell(id,5, os.uname()[1])
			sys.stdout.write(str(3))
		else:
			worksheet = sh.worksheet_by_title("STATUS")
			udates = [x[0] for x in worksheet.get_all_values() if x[0] != '']
			st=len(udates) + 1
			if (st > 20):
				last = udates[-1]
				for x in range(1, st):
					worksheet.update_cell((x,1), '')
				worksheet.update_cell((1,1),last)
				st=2
			worksheet.update_cell((st,1), 'Skipped test at ' + t + ' on ' + os.uname()[1] + ' because hashes (' + hash + ' and ' + apphash + ') match and only ' + str(float(tdelta.total_seconds()) / 3600.0) + ' hours elapsed since last test (' + lasttime + ') and 24 hours are required')
			worksheet.update_cell((st+1,1), '')
			sys.stdout.write("-1")


	# sh.share('feldman.matthew1@gmail.com', perm_type='user', role='writer')





if (sys.argv[1] == "report_regression_results"):
	print("report_regression_results('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9]))
	report_regression_results(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9])
elif (sys.argv[1] == "report_board_runtime"):
	print("report_board_runtime('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10]))
	report_board_runtime(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10])
elif (sys.argv[1] == "report_synth_results"):
	print("report_synth_results('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10], sys.argv[11], sys.argv[12], sys.argv[13], sys.argv[14]))
	report_synth_results(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10], sys.argv[11], sys.argv[12], sys.argv[13], sys.argv[14])
elif (sys.argv[1] == "prepare_sheet"):
	# print("prepare_sheet('%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5]))
	prepare_sheet(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
elif (sys.argv[1] == "dev"):
	print("dev('%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4]))
	dev(sys.argv[2], sys.argv[3], sys.argv[4])
else:
	print("ERROR: Not a valid spreadsheet interaction! %s" % sys.argv[1])
	exit()
