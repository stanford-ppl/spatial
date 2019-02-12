# ------------------------------------------------------------------------------
# Imports
# ------------------------------------------------------------------------------
import sys
import os

# ------------------------------------------------------------------------------
# Parse args
# ------------------------------------------------------------------------------
if len(sys.argv) != 2:
  print 'Usage:  $ python gen_aws_tb.py ${AWS_HOME}/hdk/cl/examples/spatial_design'
  sys.exit(0)
aws_dir = sys.argv[1]

# ------------------------------------------------------------------------------
# Simulation Software
# ------------------------------------------------------------------------------
software_dir = aws_dir + '/software/src/'
src = open(software_dir + 'TopHost.cpp')
dst = open(software_dir + 'test_spatial_main.cpp', 'w')
global_buf_count = 0
for line in src:
  if 'int main' in line:  # TODO: Use regex, or just sed on cmd line
    dst.write(line.replace('int main', 'int spatial_main'))
  elif 'std::to_string' in line:
    dst.write('  char tmp_buf' + str(global_buf_count) + '[1024]; ' + "\n")
    dst.write(line.replace('string', 'std::string', 1).replace('std::to_string', 'convert_to_string_int32_t').replace(');', ',tmp_buf' + str(global_buf_count) + ');'))
    global_buf_count += 1
  elif 'string_plus' in line:
    num_pluses = line.count('string_plus')
    t = line.replace('string ', 'std::string ').replace('string(', 'std::string(').replace('string_plus(', '').replace(',', ' + ')
    for i in range(num_pluses):
      t = t.replace(');', ';')
    dst.write(t)
  # elif ' = std::string(argv[i]);' in line:
  #   dst.write(line.replace(' = std::string(argv[i]);', ' = string(argv[i]);', 1))
  elif '>>' in line and 'vector' in line:
    dst.write(line.replace('>>', '> >'))
  elif ' string x' in line:
    dst.write(line.replace(' string', ' std::string'))
  elif line.startswith('string x'):
    dst.write(line.replace('string', ' std::string', 1).replace(' string', ' std::string'))
  elif 'vector<string>' in line:
    dst.write(line.replace('<string>', '<std::string>'))
  elif '(string' in line:
    dst.write(line.replace('(string', '(std::string'))
  elif ' string(argv' in line:
    dst.write(line.replace(' string(argv', ' std::string(argv'))
  else:
    dst.write(line)
src.close()
dst.write('''
#include <cstring>
extern "C" void test_main(uint32_t *exit_code) {
  
  // cwd is .../verif/sim/test_spatial_main/
  // char cwd[1024];
  // getcwd(cwd, sizeof(cwd));
  // std::cout << cwd << "\\n";

  std::ifstream infile;
  infile.open("../../scripts/arg_file.txt");
  int argc;
  infile >> argc;
  infile.close();
  
  char **argv = (char **)malloc(sizeof(char*)*(argc + 1));
  
  infile.open("../../scripts/arg_file.txt");
  std::string line;
  bool first_iter = true;
  int argc_it = 0;
  while (std::getline(infile, line))
  {
    if (first_iter) {
      first_iter = false;
      continue;
    }
    argc_it += 1;
    argv[argc_it] = (char *)malloc(sizeof(char)*(line.length() + 1));
    strcpy(argv[argc_it], line.c_str());
    // std::cout <<  argv[argc_it];
  }
  infile.close();
  // std::cout << ".\\n";

  *exit_code = spatial_main(argc+1, argv);
}
''')
dst.close()
