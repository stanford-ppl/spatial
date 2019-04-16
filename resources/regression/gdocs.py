# This is called by regression_run.sh / scrape.sh / regression_functions.sh / receive.sh / synth_launcher.sh / synth_regression.sh

import re
import gspread
import getpass
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
		if (wksh.title != "Probe"): # Never write to probe
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

def stampDestructiveCmd(sh,cmd):
	worksheet = sh.worksheet_by_title('STATUS')
	stamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
	write(worksheet,24,2,"Last Destructive Command:")
	write(worksheet,24,3,"User = " + getpass.getuser())
	write(worksheet,25,3,"Time = " + stamp)	
	write(worksheet,26,3,cmd)

def getColOrAppend(wksh, appname):
	try: 
		lol = readAllVals(wksh)
		if (appname in lol[0]):
			col=lol[0].index(appname)+1
		else:
			col=len(lol[0])+1
			write(wksh,1,col,appname)	
		return col
	except:
		print("ERROR: pygsheets failed getColOrAppend... -_-")	
		exit()

def getCols(wksh, appname):
	try: 
		lol = readAllVals(wksh)
		cols = [i+1 for i,x in enumerate(lol[0]) if (re.match("^"+appname+"$",x))]
		return cols
	except:
		print("ERROR: pygsheets failed getCols... -_-")	
		exit()

def getStampCol(wksh):
	try: 
		lol = readAllVals(wksh)
		cols = [i+1 for i,x in enumerate(lol[1]) if (re.match("Test Time",x))]
		return cols[0]
	except:
		return -1

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

def deleteRows(wksh, id):
	try:
		wksh.delete_rows(id)
	except:
		print("ERROR: pygsheets could not delete row %d" % id)

def deleteCols(wksh, id):
	try:
		wksh.delete_cols(id)
	except:
		print("ERROR: pygsheets could not delete row %d" % id)

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
			sh = gc.open_by_key("1gfJvD6QHxJ276wyvtApqSB6NHme1GPqmQXo_fp10t8A")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "vcs"):
		try: 
			sh = gc.open_by_key("1_bbJHrt6fvMvfCLyuSyy6-pQbJLiNY4kOSoKN3voSoM")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "scalasim"):
		try: 
			sh = gc.open_by_key("1BAf6e1_ckRwrJNW-t09pjGDFixt2GsobCYyzaue0qcA")
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
			sh = gc.open_by_key("1t9jSxurcFXgtrtCW5EZGiy9T3pApXfX1nsgA1U9i-Bs")
		except:
			print("ERROR: Couldn't get sheet")
			exit()
	elif (title == "ZCU"):
		try: 
			sh = gc.open_by_key("1HuaKHe0Gp5bEbM969IZqYzcJEUfOfpwb-nIcBXpmsgQ")
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
		print("No spreadsheet for " + title + "!")
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

def getRowByBranch(sh, branchname, start):
	if (branchname == "any"): 
		print("branch %s is row %d" % (branchname, start))
		return start
	else:
		worksheet = sh.worksheet('index', 0)
		lol = readAllVals(worksheet)
		row = -1
		for i in range(start, len(lol)):
			if (lol[i][1] == branchname):
				row = i
				break
		if (row == -1):	print("ERROR: Could not find row for %s, starting from %s" % (branchname, start))
		print("branch %s is row %d" % (branchname, row))
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
	elif (title == "vcs"):
		perf=True
	elif (title == "scalasim"):
		perf=True
	elif (title == "vcs-noretime"):
		perf=True
	else:
		print("No spreadsheet for " + title)
		exit()

	return perf




def report_regression_results(branch, appname, passed, cycles, hash, apphash, spatialcompile, backendcompile, csv, args):
	sh = getDoc(branch)
	row = getRow(sh, hash, apphash)

	# Page 0 - Timestamps
	stamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
	worksheet = sh.worksheet_by_title('Timestamps')
	col = getColOrAppend(worksheet, appname)
	write(worksheet, row,col, stamp)

	# Page 1 - Runtime
	worksheet = sh.worksheet_by_title('Runtime')
	col = getRuntimeCol(worksheet, appname)
	write(worksheet, 2,  col,  args)
	write(worksheet, row,col,  cycles)
	write(worksheet, row,col+1,passed)

	# Page 2 - Spatial compile time
	worksheet = sh.worksheet_by_title('SpatialCompile')
	col = getColOrAppend(worksheet, appname)
	write(worksheet, row, col, spatialcompile)

	# Page 3 - Backend compile time
	worksheet = sh.worksheet_by_title('BackendCompile')
	col = getColOrAppend(worksheet, appname)
	write(worksheet, row, col, backendcompile)

	# Page 4 - Properties
	worksheet = sh.worksheet_by_title('Properties')
	col = getColOrAppend(worksheet, appname)
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
	write(worksheet,22,3,stamp)
	write(worksheet,22,4,appname)
	write(worksheet,22,5,os.uname()[1])

def report_model_results(branch, appname, hash, apphash, true_runtime, dse_runtime, final_runtime, args):
	sh = getDoc(branch)
	row = getRow(sh, hash, apphash)

	final_error = 100.0 * float(final_runtime) / float(true_runtime)
	dse_error = 100.0 * float(dse_runtime) / float(true_runtime)
	
	worksheet = sh.worksheet_by_title('Models')
	col = getColOrAppend(worksheet, appname)
	write(worksheet, 2,  col,  args)
	write(worksheet, row,col, "%s (%.1f%%)\n%s (%.1f%%)\n%s" % (dse_runtime, dse_error, final_runtime, final_error, true_runtime))


def report_board_runtime(appname, timeout, runtime, passed, args, backend, locked_board, hash, apphash):
	sh = getDoc(backend)
	row = getRow(sh, hash, apphash)

	# Page 10 - Results
	worksheet = sh.worksheet_by_title("Runtime")
	col = getColOrAppend(worksheet, appname)
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
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col, stamp)

	# Page 1 - Slice LUT
	worksheet = sh.worksheet_by_title(word + ' LUTs')
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col,lut)

	# Page 2 - Slice Reg
	worksheet = sh.worksheet_by_title(word + ' Regs')
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col,reg)

	# Page 3 - Mem
	worksheet = sh.worksheet_by_title('BRAMs')
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col,ram)

	if (backend == "AWS"):
		# Page 4 - URAM
		worksheet = sh.worksheet_by_title('URAMs')
		col = getColOrAppend(worksheet, appname)
		write(worksheet,row,col,uram)

	# Page 5 - DSP
	worksheet = sh.worksheet_by_title('DSPs')
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col,dsp)

	# Page 6 - LUT as Logic
	worksheet = sh.worksheet_by_title('LUT as Logic')
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col,lal)

	# Page 7 - LUT as Memory
	worksheet = sh.worksheet_by_title('LUT as Memory')
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col,lam)

	# Page 8 - Synth time
	worksheet = sh.worksheet_by_title('Synth Time')
	col = getColOrAppend(worksheet, appname)
	write(worksheet,row,col,float(synth_time) / 3600.)

	# Page 9 - Timing met
	worksheet = sh.worksheet_by_title('Timing Met')
	col = getColOrAppend(worksheet, appname)
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

	if (len(lol) < 3): 
		lasthash="NA"
		lastapphash="NA"
		lasttime="2000-01-08 21:15:36"
	else:
		lasthash=lol[2][hcol]
		lastapphash=lol[2][acol]
		lasttime=lol[2][ttcol]

	if (len(lol) >= 74 and len(lol[73]) >= 6 and lol[73][5] == "KEEP"):
		keep_row_75 = True
	else:
		keep_row_75 = False

def prepare_sheet(hash, apphash, timestamp, backend):
	sh = getDoc(backend)
	perf = isPerf(backend)

	t=time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())

	worksheet = sh.worksheet_by_title("Timestamps")
	lol = worksheet.get_all_values()
	lolhash = [x[0] for x in lol if x[0] != '']
	# id = len(lolhash) + 1
	freq = os.environ['CLOCK_FREQ_MHZ']
	numthreads = os.environ['NUM_THREADS']
	if ("hash" in lol[1]):
		hcol=lol[1].index("hash")
	if ("app hash" in lol[1]):
		acol=lol[1].index("app hash")
	if ("test timestamp" in lol[1]):
		ttcol=lol[1].index("test timestamp")

	if (len(lol) < 3): 
		lasthash="NA"
		lastapphash="NA"
		lasttime="2000-01-08 21:15:36"
	else:
		lasthash=lol[2][hcol]
		lastapphash=lol[2][acol]
		lasttime=lol[2][ttcol]

	if (len(lol) >= 74 and len(lol[73]) >= 6 and lol[73][5] == "KEEP"):
		keep_row_75 = True
	else:
		keep_row_75 = False

	if (perf):
		new_entry=True
	else:
		new_entry=(lasthash != hash or lastapphash != apphash)
	if (new_entry):
		link='=HYPERLINK("https://github.com/stanford-ppl/spatial/tree/' + hash + '", "' + hash + '")'
		alink=apphash
		count_success="=sum ( COUNTIF ( k3:3, \"=Y\" ) )"
		count_fail="=sum ( COUNTIF ( k3:3, \"=N\" ) )"
		count_crash="=sum ( COUNTIF ( k3:3, \"\" ) ) / 2"
		numsheets = len(sh.worksheets())
		for x in range(0,numsheets):
			# worksheet = sh.get_worksheet(x)
			worksheet = sh.worksheet('index', x)
			if (worksheet.title != "STATUS" and worksheet.title != "Properties" and worksheet.title != "Probe"):
				if (worksheet.title == "Runtime" and isPerf): worksheet.insert_rows(row = 2, values = [link, alink, t, freq + ' MHz (' + numthreads + " threads)" , os.uname()[1], count_success, count_fail, count_crash ])
				else: worksheet.insert_rows(row = 2, values = [link, alink, t, freq + ' MHz (' + numthreads + " threads)" , os.uname()[1] ])
				if (not keep_row_75): deleteRows(worksheet, 75)
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
				if (worksheet.title != "STATUS" and worksheet.title != "Properties" and worksheet.title != "Probe"):
					worksheet.insert_rows(row = 2, values = [link, alink, t, freq + ' MHz (' + numthreads + " threads)", os.uname()[1] ])
					if (not keep_row_75): deleteRows(worksheet, 75)
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

def finish_test(backend, branch, runtime):
	sh = getDoc(backend)
	numsheets = len(sh.worksheets())
	for x in range(0,numsheets):
		worksheet = sh.worksheet('index', x)
		print("Stamping %s" % worksheet.title)
		lol = worksheet.get_all_values()
		stampcol = getStampCol(worksheet)
		if (stampcol >= 0):
			row = getRowByBranch(sh, branch, 2) + 1
			write(worksheet, row, stampcol, runtime)
		else:
			print("No Test Time field for %s" % worksheet.title)
def report_changes(backend, newbranch, oldbranch):
	sh = getDoc(backend)

	worksheet = sh.worksheet_by_title("Runtime")
	lol = worksheet.get_all_values()

	start = getCols(worksheet, "Test:")[0]
	tests = list(filter(None, lol[0][start:]))
	newrow = getRowByBranch(sh, newbranch, 2)
	oldrow = getRowByBranch(sh, oldbranch, newrow+1)
	pass_list = []
	fail_list = []
	nocompile_list = []
	improved_list = []
	worsened_list = []
	for t in tests:
		col = lol[0].index(t) + 1
		if (len(lol[0]) > col): 
			now_pass = (lol[newrow][col] == 'Y') or (lol[newrow][col] == '1')
			now_fail = (lol[newrow][col] == 'N') or (lol[newrow][col] == '0')
			now_nocompile = lol[newrow][col] == ''
			b4_pass = (lol[oldrow][col] == 'Y') or (lol[oldrow][col] == '1')
			b4_fail = (lol[oldrow][col] == 'N') or (lol[oldrow][col] == '0')
			b4_nocompile = lol[oldrow][col] == ''
			if (now_pass): pass_list.append(t)
			if (now_fail): fail_list.append(t)
			if (now_nocompile): nocompile_list.append(t)
			if (now_pass and (not b4_pass)): improved_list.append(t)
			if ((not now_pass) and b4_pass): worsened_list.append(t)
	print("SUMMARY FOR %s" % backend)
	print("-------")
	print("Improved: %d" % len(improved_list))
	print("Worsened: %d" % len(worsened_list))
	print("Total # Passing: %d" % len(pass_list))
	print("Total # Failing: %d" % len(fail_list))
	print("Total # Not Compiling: %d" % len(nocompile_list))
	print("BREAKDOWN")
	print("---------")
	print("Passing:")
	print(sorted(pass_list))
	print("Failing:")
	print(sorted(fail_list))
	print("Not Compiling:")
	print(sorted(nocompile_list))
	print("Improved:")
	print(sorted(improved_list))
	print("Worsened:")
	print(sorted(worsened_list))
	print("Diffed rows %d (%s) and %d (%s)" % (newrow, newbranch, oldrow, oldbranch))

def combine_and_strip_prefixes(backend):
	sh = getDoc(backend)

	numsheets = len(sh.worksheets())
	for x in range(0,numsheets):
		# worksheet = sh.get_worksheet(x)
		worksheet = sh.worksheet('index', x)
		if (worksheet.title != "STATUS" and worksheet.title != "Probe"):
			print("Scrubbing %s" % worksheet.title)
			lol = worksheet.get_all_values()
			start = getCols(worksheet, "Test:")[0]
			tests = list(filter(None, lol[0][start:]))
			for t in tests:
				if "spatial." in t:
					col = lol[0].index(t)
					purename = re.sub(r".*\.","",t)
					merge_apps_columns(purename, t, backend, True)
					print("Replace %s with %s" % (t, purename))
	stampDestructiveCmd(sh,"combine_and_strip_prefixes %s" % (backend))


def report_slowdowns(prop, backend, newbranch, oldbranch):
	sh = getDoc(backend)

	if (prop == "runtime"):
		worksheet = sh.worksheet_by_title("Runtime")
	elif (prop == "spatial"):
		worksheet = sh.worksheet_by_title("SpatialCompile")
	else:
		worksheet = sh.worksheet_by_title("BackendCompile")

	lol = worksheet.get_all_values()
	start = getCols(worksheet, "Test:")[0]
	tests = list(filter(None, lol[0][start:]))
	newrow = getRowByBranch(sh, newbranch, 2)
	oldrow = getRowByBranch(sh, oldbranch, newrow+1)
	better_apps = []
	better_raw = []
	better_change = []
	worse_apps = []
	worse_raw = []
	worse_change = []
	for t in tests:
		col = lol[0].index(t)
		if (len(lol[0]) > col and lol[newrow][col] != "" and lol[oldrow][col] != ""): 
			nowtime = float(lol[newrow][col])
			try:
				lasttime = float(lol[oldrow][col])
				percent_change = ((nowtime - lasttime) / lasttime) * 100
				if (percent_change < -2):
					better_apps.append(t)
					better_raw.append(nowtime)
					better_change.append(percent_change)
				elif (percent_change > 2):
					worse_apps.append(t)
					worse_raw.append(nowtime)
					worse_change.append(percent_change)
			except:
				1

	print("SUMMARY FOR %s: %s" % (backend, prop))
	print("-------")
	better_apps_s = [x for _,x in sorted(zip(better_change,better_apps))]
	better_raw_s = [x for _,x in sorted(zip(better_change,better_raw))]
	better_change_s = sorted(better_change)
	for i in range(0,len(better_apps)):
		print("    %-30s: %.1f%% faster (now %.1f)" % (better_apps_s[i], -better_change_s[i], better_raw_s[i]))
	print("-----------------------------------------")
	worse_apps_s = [x for _,x in sorted(zip(worse_change,worse_apps))]
	worse_raw_s = [x for _,x in sorted(zip(worse_change,worse_raw))]
	worse_change_s = sorted(worse_change)
	for i in range(0,len(worse_apps)):
		print("    %-30s: %.1f%% slower (now %.1f)" % (worse_apps_s[i], worse_change_s[i], worse_raw_s[i]))
	print("Improved: %d" % len(better_apps))
	print("Worsened: %d" % len(worse_apps))


# ofs = 0 means start deleting from spreadsheet "row 3" and down
def delete_n_rows(n, ofs, backend):
	sh = getDoc(backend)

	numsheets = len(sh.worksheets())
	for x in range(0,numsheets):
		# worksheet = sh.get_worksheet(x)
		worksheet = sh.worksheet('index', x)
		if (worksheet.title != "STATUS" and worksheet.title != "Properties"):
			print("Scrubbing page %s" % worksheet.title)
			for i in range(0,int(n)):
				deleteRows(worksheet, 3 + int(ofs))

	stampDestructiveCmd(sh,"delete_n_rows %s %s %s" % (n,ofs,backend))

def delete_app_column(appname, backend):
	sh = getDoc(backend)
	perf = isPerf(backend)

	numsheets = len(sh.worksheets())
	for x in range(0,numsheets):
		worksheet = sh.worksheet('index', x)
		cols = sorted(getCols(worksheet, appname), reverse=True)
		for col in cols: 			
			if (worksheet.title != "STATUS" and worksheet.title != "Properties"):
				if (col == max(cols)): print("Scrubbing page %s" % worksheet.title)
				if (col >= 0):
					deleteCols(worksheet, col)
					if (perf and worksheet.title == "Runtime"):
						# Delete the bit column also
						deleteCols(worksheet, col)
				else:
					print("ERROR: App %s not found on sheet %s" % (appname, worksheet.title))

	stampDestructiveCmd(sh,"delete_app_column %s %s" % (appname, backend))

# This will take rows from new_appname column 0 until the last row with data and paste it into old_appname column
def merge_apps_columns(old_appname, new_appname, backend, keepOldname = False):
	sh = getDoc(backend)
	perf = isPerf(backend)

	numsheets = len(sh.worksheets())
	for x in range(0,numsheets):
		worksheet = sh.worksheet('index', x)
		lol = readAllVals(worksheet)
		old_col_in_sheet = getCols(worksheet, old_appname)
		new_col_in_sheet = getCols(worksheet, new_appname)
		if (len(old_col_in_sheet) != 1):
			print("ERROR: %s not found on sheet %s" % (old_appname, worksheet.title))
		elif (len(new_col_in_sheet) != 1):
			print("ERROR: %s not found on sheet %s" % (new_appname, worksheet.title))
		else:
			new_col = lol[0].index(new_appname)

			if (perf and worksheet.title == "Runtime"):
				data = [x[new_col] for x in lol]
				passes = [x[new_col+1] for x in lol]
				for i in range(0,len(data)):
					if (data[i] != ''):
						write(worksheet, i+1, old_col_in_sheet[0], data[i])
						# print("write(%d %d, %s)" %(i,old_col_in_sheet[0], data[i]))
					if (passes[i] != ''):
						write(worksheet, i+1, old_col_in_sheet[0]+1, passes[i])
						# print("write(%d %d, %s)" %(i,old_col_in_sheet[0]+1, passes[i]))
				deleteCols(worksheet, new_col_in_sheet[0]+1)
				deleteCols(worksheet, new_col_in_sheet[0])
			else:
				data = [x[new_col] for x in lol]
				for i in range(0,len(data)):
					if (data[i] != ''):
						write(worksheet, i+1, old_col_in_sheet[0], data[i])
						# print("write(%d %d, %s)" %(i,old_col_in_sheet[0], data[i]))
				deleteCols(worksheet, new_col_in_sheet[0])
			if keepOldname:
				write(worksheet, 1, old_col_in_sheet[0], old_appname)

			# print("delete app column")
	stampDestructiveCmd(sh,"merge_apps_columns %s %s %s %s %s" % (old_appname, new_appname, backend, keepOldname))





if (sys.argv[1] == "report_regression_results"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! report_regression_results('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10], sys.argv[11]))
	report_regression_results(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10], sys.argv[11])
if (sys.argv[1] == "report_model_results"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! report_regression_results('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9]))
	report_model_results(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9])
elif (sys.argv[1] == "report_board_runtime"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! report_board_runtime('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10]))
	report_board_runtime(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10])
elif (sys.argv[1] == "report_synth_results"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! report_synth_results('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10], sys.argv[11], sys.argv[12], sys.argv[13], sys.argv[14]))
	report_synth_results(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8], sys.argv[9], sys.argv[10], sys.argv[11], sys.argv[12], sys.argv[13], sys.argv[14])
elif (sys.argv[1] == "prepare_sheet"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! prepare_sheet('%s', '%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5]))
	prepare_sheet(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
elif (sys.argv[1] == "report_changes"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! report_changes('%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4]))
	report_changes(sys.argv[2], sys.argv[3], sys.argv[4])
elif (sys.argv[1] == "finish_test"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! finish_test('%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4]))
	finish_test(sys.argv[2], sys.argv[3], sys.argv[4])
elif (sys.argv[1] == "report_slowdowns"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! report_slowdowns('%s', '%s')" % (sys.argv[2], sys.argv[3]))
	report_slowdowns(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
elif (sys.argv[1] == "delete_n_rows"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! delete_n_rows('%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4]))
	delete_n_rows(sys.argv[2], sys.argv[3], sys.argv[4])
elif (sys.argv[1] == "delete_app_column"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! delete_app_column('%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4]))
	delete_app_column(sys.argv[2], sys.argv[3])
elif (sys.argv[1] == "merge_apps_columns"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! merge_apps_columns('%s', '%s', '%s')" % (sys.argv[2], sys.argv[3], sys.argv[4]))
	merge_apps_columns(sys.argv[2], sys.argv[3], sys.argv[4])
elif (sys.argv[1] == "combine_and_strip_prefixes"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! combine_and_strip_prefixes('%s')" % (sys.argv[2]))
	combine_and_strip_prefixes(sys.argv[2])
elif (sys.argv[1] == "dev"):
	# print("WARNING: THIS PRINT WILL BREAK REGRESSION. PLEASE COMMENT IT OUT! dev('%s', '%s', '%s')" % (sys.argv[2], sys.argv[3]))
	dev(sys.argv[2], sys.argv[3], sys.argv[4])
else:
	print("Commands:")
	print(" - report_regression_results(branch, appname, passed, cycles, hash, apphash, csv, args)")
	print(" - report_model_results(branch, appname, hash, apphash, true_runtime, dse_runtime, final_runtime, args)")
	print(" - report_board_runtime(appname, timeout, runtime, passed, args, backend, locked_board, hash, apphash)")
	print(" - report_synth_results(appname, lut, reg, ram, uram, dsp, lal, lam, synth_time, timing_met, backend, hash, apphash)")
	print(" - prepare_sheet(hash, apphash, timestamp, backend)")
	print(" - combine_and_strip_prefixes(backend)")
	print(" - report_changes(backend, newbranch, oldbranch (master, misc_fixes, any, etc.))")
	print(" - finish_test(backend, branch, runtime)")
	print(" - report_slowdowns(property [runtime, spatial, backend], newbranch, oldbranch, backend)")
	print(" - delete_n_rows(n, ofs (use 0 for row 3, 1 for row 4, etc...), backend (vcs, scalasim, vcs-noretime, Zynq, etc...))")
	print(" - delete_app_column(appname (regex supported), backend (vcs, scalasim, vcs-noretime, Zynq, etc...))")
	print(" - merge_apps_columns(old appname, new appname, backend)")
	exit()
