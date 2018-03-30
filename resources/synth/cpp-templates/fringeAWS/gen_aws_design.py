# ------------------------------------------------------------------------------
# Imports
# ------------------------------------------------------------------------------
import sys
import os

# ------------------------------------------------------------------------------
# Parse args
# ------------------------------------------------------------------------------
if len(sys.argv) != 2:
  print 'Usage:  $ python gen_aws_design.py ${AWS_HOME}/hdk/cl/examples/spatial_design'
  sys.exit(0)
aws_dir = sys.argv[1]

# ------------------------------------------------------------------------------
# Design
# ------------------------------------------------------------------------------

# Step 1: Parse Top.v until we get to module Top and extract # args
# We care about the following:
#  input   clock,
#  input   reset,
#  input  [31:0] io_enable,
#  output [31:0] io_done,
#  input  [31:0] io_scalarIns_0,
#  output [31:0] io_scalarOuts_0,
# The first 4 are always the same, the last 2 can occur multiple times (_0, _1, _2, ..) and need to be counted
design_dir = aws_dir + '/design/'
top_src = open(design_dir + 'Top.v')
state = 0
num_scalar_in = 0
num_scalar_out = 0
for line in top_src:
  if 'module Top(' in line:
    assert state == 0
    state = 1
    continue
  if state == 1:
    if 'io_scalarIns' in line:
      num_scalar_in += 1
    elif 'io_scalarOuts' in line:
      num_scalar_out += 1
    elif ');' in line:
      break      
top_src.close()

# Step 2: Replace scalars in two files

# cl_dram_dma.sv
src = open(design_dir + 'cl_dram_dma.sv_TEMPLATE')
dst = open(design_dir + 'cl_dram_dma.sv', 'w')
for line in src:
  if '{{{SPATIAL_INSERT_input_argnum}}}' in line:
    new_lines = ''
    for argnum in range(num_scalar_in):
      line_with_replacements = line
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_input_argnum}}}', str(argnum))
      new_lines += line_with_replacements
    dst.write(new_lines)
  elif '{{{SPATIAL_INSERT_output_argnum}}}' in line:
    new_lines = ''
    for argnum in range(num_scalar_out):
      line_with_replacements = line
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_output_argnum}}}', str(argnum))
      new_lines += line_with_replacements
    dst.write(new_lines)
  else:
    dst.write(line)
src.close()
dst.close()

# cl_ocl_slv.sv
src = open(design_dir + 'cl_ocl_slv.sv_TEMPLATE')
dst = open(design_dir + 'cl_ocl_slv.sv', 'w')
for line in src:
  if '{{{SPATIAL_INSERT_input_argnum}}}' in line:
    new_lines = ''
    for argnum in range(num_scalar_in):
      line_with_replacements = line
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_input_argnum}}}', str(argnum))
      # Replace address of lower 32 bits
      hex_addr = str(hex(argnum*64 + 65536))[2:]
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_input_argaddr_5h}}}', hex_addr[:-1])
      # Replace address of higher 32 bits
      hex_addr_plus_0x20 = str(hex(argnum*64 + 32 + 65536))[2:]
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_input_argaddr_5h_plus_0x20}}}', hex_addr_plus_0x20[:-1])
      new_lines += line_with_replacements
    dst.write(new_lines)
  elif '{{{SPATIAL_INSERT_output_argnum}}}' in line:
    new_lines = ''
    for argnum in range(num_scalar_out):
      line_with_replacements = line
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_output_argnum}}}', str(argnum))
      # Replace address of lower 32 bits
      hex_addr = str(hex(argnum*64 + 65536*8))[2:]
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_output_argaddr_5h}}}', hex_addr[:-1])
      # Replace address of higher 32 bits
      hex_addr_plus_0x20 = str(hex(argnum*64 + 32 + 65536*8))[2:]
      line_with_replacements = line_with_replacements.replace('{{{SPATIAL_INSERT_output_argaddr_5h_plus_0x20}}}', hex_addr_plus_0x20[:-1])
      new_lines += line_with_replacements
    dst.write(new_lines)
  else:
    dst.write(line)
src.close()
dst.close()

# Step 3: Temporary hack: Add 2 defines and replace {1{$random}} with 0
#src = open(design_dir + 'Top.v')
#dst = open(design_dir + 'Top.v_copy', 'w')
#dst.write('''// Temporary hack:
#`define RANDOMIZE_INVALID_ASSIGN
#`define RANDOMIZE_REG_INIT
#
#''')
#for line in src:
#  if '{1{$random}}' in line:
#    dst.write(line.replace('{1{$random}}', '0'))
#  else:
#    dst.write(line)
#src.close()
#dst.close()
#os.system('mv -f ' + design_dir + 'Top.v_copy ' + design_dir + 'Top.v')

