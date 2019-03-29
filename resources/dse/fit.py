import numpy as np
import matplotlib.pyplot as plt
from scipy.optimize import curve_fit
from scipy import stats
import csv
import sys

def params(x):
	return x[0], x[1], x[2], x[3], x[4], x[5]


def fitFunc0(x, congestion, stallPenalty, idle, startup, parFactor, a, b, c, d, e, f, g, h, i, j):
	loads, stores, gateds, outerIters, innerIters, bitsPerCycle = params(x)
	countersContribution = outerIters * (innerIters + idle)
	congestionContribution = (loads + stores + gateds)*congestion
	parallelizationScale = bitsPerCycle * parFactor
	return (countersContribution * stallPenalty * congestionContribution + startup) / parallelizationScale

def fitFunc1(x, congestion, stallPenalty, idle, startup, parFactor, a, b, c, d, e, f, g, h, i, j):
	loads, stores, gateds, outerIters, innerIters, bitsPerCycle = params(x)
	countersContribution = outerIters * (innerIters + idle)
	congestionContribution = (loads + stores + gateds)*congestion
	parallelizationScale = bitsPerCycle * parFactor
	return (countersContribution * stallPenalty * congestionContribution + startup) * parallelizationScale

def fitFunc2(x, congestion, stallPenalty, idle, startup, parFactor, a, b, c, d, e, f, g, h, i, j):
	loads, stores, gateds, outerIters, innerIters, bitsPerCycle = params(x)
	countersContribution = outerIters * (innerIters + idle)
	congestionContribution = (loads*a + stores*b + gateds*c)
	parallelizationScale = bitsPerCycle * parFactor
	return (countersContribution * stallPenalty * congestionContribution + startup) / parallelizationScale

def fitFunc3(x, congestion, stallPenalty, idle, startup, parFactor, a, b, c, d, e, f, g, h, i, j):
	loads, stores, gateds, outerIters, innerIters, bitsPerCycle = params(x)
	countersContribution = outerIters * (innerIters + idle)
	congestionContribution = (loads*a + stores*b + gateds*c)*congestion
	parallelizationScale = bitsPerCycle * parFactor
	return (countersContribution * stallPenalty * congestionContribution + startup) * parallelizationScale

def fitFunc4(x, congestion, stallPenalty, idle, startup, parFactor, a, b, c, d, e, f, g, h, i, j):
	loads, stores, gateds, outerIters, innerIters, bitsPerCycle = params(x)
	countersContribution = outerIters * (innerIters + idle)
	congestionContribution = (loads*a + stores*b + gateds*c) * e
	parallelizationScale = bitsPerCycle * parFactor
	return (countersContribution * stallPenalty * congestionContribution + startup) * parallelizationScale + d

def fitFunc(i, data):
	if (i == 0):       return optimize(fitFunc0, data)
	elif (i == 1):     return optimize(fitFunc1, data)
	elif (i == 2):     return optimize(fitFunc2, data)
	elif (i == 3):     return optimize(fitFunc3, data)
	else:              return optimize(fitFunc4, data)

def optimize(func, data): 
	# congestion, stallPenalty, idle, startup, parFactor, a, b, c, d, e, f, g, h, i, j
	lower_bounds = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
	upper_bounds = [np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf, np.inf]
	try:
		fitParams, fitCovariances = curve_fit(func, 
											  data[0:6,:], 
											  data[col],
											  bounds = (lower_bounds, upper_bounds)
											  # method='lm'
											  )
		# print ' fit coefficients:\n', fitParams

		avg = []
		for x in data.transpose():
			predict = func(x[0:6], *fitParams)
			gold = x[col]
			err = 100.0 * predict / gold
			pe = abs(predict - gold) #/ gold
			avg.append(pe)
			# print("got: %.2f, want: %.2f, %%: %.2f" % (predict, gold, err))

		st = stats.describe(avg)
		print(st)
		return st.mean, fitParams
	except: 
		return 99999999999, []


CSV_COLUMNS = ['loads','stores','gateds','outerIters','innerIters','bitsPerCycle','DenseLoad','DenseStore','GatedDenseStore']
competitors = ['loads', 'stores', 'gateds']
payloads = ['outerIters', 'innerIters', 'bitsPerCycle']
targets = ['DenseLoad', 'DenseStore', 'GatedDenseStore']

# Fetch data
datafile='data/train'
scala = []
raw = []
besties = []

# Fit for all targets
for fit in targets:
	means = []
	results = []
	col = CSV_COLUMNS.index(fit)
	data = np.asarray(list(csv.reader(open(datafile), delimiter='\t')))
	data = [[float(y) for y in x] for x in data if x[col] != "0"]
	data = np.asarray(data).transpose()

	# Try different models
	print("TARGET %s" % fit)
	for i in range(0, 4): 
		sys.stdout.write("func%d: " % i)
		(m, p) = fitFunc(i,data)
		means.append(m)
		results.append(p)
	best = np.argmin(means)
	besties.append(best)
	raw.append(results[best])
	print("Best func = %d (%f)" % (best, min(means)))
	scala.append("    case \"%s\" => Seq(%s)" % (fit,','.join([str(x) for x in results[best]])))
	print "\n\n"


# Print results
print("Paste this into models/src/models/ModelData.scala")
print("**********************************")
for l in scala:
	print(l)
print("**********************************\n\n\n")


if (len(list(set(besties))) > 1): 
	print("")
	print("WARNING: DIFFERENT MODEL FOR DIFFERENT TARGETS!!")
	print(besties)

# Run sample data point
x = [4,0,0,8,8,256]
# x = [1,2,1,1,195,512]
# x = [2,0,0,32,512,32]
print(x)
for (fit,p,best) in zip(targets,raw,besties):
	sys.stdout.write("%s: " % fit)
	if best == 0: print(fitFunc0(x, *p))
	elif best == 1: print(fitFunc1(x, *p))
	elif best == 2: print(fitFunc2(x, *p))
	elif best == 3: print(fitFunc3(x, *p))
	else: print(fitFunc4(x, *p))

