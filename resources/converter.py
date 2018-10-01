#!bin/python

# Helpful script for converting fixed point signed numbers to decimal

def letter2num(x):
	if (x == "F" or x == "f"):
		return 15
	elif (x == "E" or x == "e"):
		return 14
	elif (x == "D" or x == "d"):
		return 13
	elif (x == "C" or x == "c"):
		return 12
	elif (x == "B" or x == "b"):
		return 11
	elif (x == "A" or x == "a"):
		return 10
	elif (x == "9"):
		return 9
	elif (x == "8"):
		return 8
	elif (x == "7"):
		return 7
	elif (x == "6"):
		return 6
	elif (x == "5"):
		return 5
	elif (x == "4"):
		return 4
	elif (x == "3"):
		return 3
	elif (x == "2"):
		return 2
	elif (x == "1"):
		return 1
	elif (x == "0"):
		return 0
	elif (x == "."):
		return 0

def num2letter(x):
	if (x == 15):
		return "F"
	elif (x == 14):
		return "E"
	elif (x == 13):
		return "D"
	elif (x == 12):
		return "C"
	elif (x == 11):
		return "B"
	elif (x == 10):
		return "A"
	elif (x == 9):
		return "9"
	elif (x == 8):
		return "8"
	elif (x == 7):
		return "7"
	elif (x == 6):
		return "6"
	elif (x == 5):
		return "5"
	elif (x == 4):
		return "4"
	elif (x == 3):
		return "3"
	elif (x == 2):
		return "2"
	elif (x == 1):
		return "1"
	elif (x == 0):
		return "0"
	else:
		return "."

def twoscomp(number, numdigits):
	s = ""
	for i in range(0, numdigits):
		if (number[i] != "."):
			s+=num2letter(15 - letter2num(number[i]))
		else:
			s+="."
	return s

def negative(number, dec):
	if (number[0] != "."):
		if (letter2num(number[0]) >= 8):
			return True
		else:
			return False
	else:
		if (letter2num(number[1]) >= 8):
			return True
		else:
			return False


def factor(digit, dec):
	if (digit < dec):
		dist = dec - digit - 1
	else:
		dist = dec - digit
	return 16**dist

def adder(numdigits, dec):
	if (numdigits == dec):
		return 1
	else:
		return 16**-(numdigits-dec-1)

tp = raw_input("Hex to dec? [Y/n]  (yes = hex -> dec, no = dec -> hex): ")
if (tp == "y" or tp == "Y" or tp == ""):
	hex2dec = True
else:
	hex2dec = False

while(1):
	if hex2dec:
		number = raw_input("Hex to dec: ")
		numdigits = len(number)
		position = number.find(".")
		if position == -1:
			position = numdigits

		isNeg = negative(number, position)
		if (isNeg):
			number = twoscomp(number, numdigits)
			scalar = -1
			add = adder(numdigits,position)
		else:
			scalar = 1
			add = 0

		converted = 0
		for i in range(0, numdigits):
			converted = converted + letter2num(number[i]) * factor(i,position)

		print (converted + add) * scalar
	else:
		numFbits = 16
		numDbits = 32
		number = float(raw_input("Dec to hex: "))
		number = int(number*(16**(numFbits/4)))
		if number > 0:
			converted = hex(number)
		else:
			number = 16**(numDbits/4 + numFbits/4) + number
			converted = hex(number)
		print converted[0:len(converted)-(numFbits/4)] + "." + converted[len(converted)-(numFbits/4):len(converted)]
