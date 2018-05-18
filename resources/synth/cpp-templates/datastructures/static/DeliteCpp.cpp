#include "DeliteCpp.h"

bool regex_metachar(char c) {
  switch (c) {
    case '\\': case '^': case '$': case '.': case '|': case '?':
    case '*': case '+': case '(': case ')': case '[': case '{':
      return true;
    default:
      return false;
  }
}

char find_delim(const string &pattern) {
  if (pattern.length()==1 && !regex_metachar(pattern.at(0))) {
    return pattern.at(0);
  }
  else if (pattern.length()==2 && pattern.at(0)=='\\' && regex_metachar(pattern.at(1))) {
    return pattern.at(1);
  }
  else
    return -1;
}

string *growStringArray(const resourceInfo_t *resourceInfo, string *input, int &length) {
  string *result = new (resourceInfo) string[length * 4];
  for(int i=0; i<length; i++) {
    result[i] = input[i];
  }
  length *= 4;
  return result;
}

#ifdef __USE_STD_STRING__
#ifdef MEMMGR_REFCNT
std::shared_ptr<cppDeliteArraystring> string_split(const resourceInfo_t *resourceInfo, const string &str, const string &pattern, int32_t lim) {
#else
cppDeliteArraystring *string_split(const resourceInfo_t *resourceInfo, const string &str, const string &pattern, int32_t lim) {
#endif
  //TODO: current g++ does not fully support c++11 regex, 
  //      so below code does not work.
  /*
  std::string s(str);
  std::regex e(pattern.c_str());
  std::vector<std::string> *elems = new std::vector<std::string>();

  const std::sregex_token_iterator endOfSequence;
  std::sregex_token_iterator token(s.begin(), s.end(), e, -1);
  while(token != endOfSequence) {
    elems->push_back(*token++);
    std::cout << *token++ << std::endl;
  }

  cppDeliteArray<string> *ret = new cppDeliteArray<string>(elems->size());
  for(int i=0; i<elems->size(); i++)
    ret->update(i,elems->at(i));
  return ret;
  */

  //Since above code is not working, we currently only support simple regular expressions
  // http://stackoverflow.com/questions/236129/how-to-split-a-string-in-c
  std::vector<std::string> elems;
  int num_tokens = 0;
  std::string token;
  std::stringstream ss(str);
  char delim;
  if (pattern.compare("\\s+")==0) {
    while (ss >> token) {
      num_tokens += 1;
      if (num_tokens == lim) {
        std::string remainder;
        getline(ss, remainder, (char)NULL);
        elems.push_back(token+remainder);
        break;
      }
      else {
        elems.push_back(token);
      }
    }
  }
  else if ((delim = find_delim(pattern)) != -1) {
    while (getline(ss, token, delim)) {
      num_tokens += 1;
      if (num_tokens == lim) {
        std::string remainder;
        getline(ss, remainder, (char)NULL);
        elems.push_back(token+remainder);
        break;
      }
      else {
        elems.push_back(token);
      }
    }
  }
  else {
    assert(false && "Given regex is not supported");
  }
  
  // remove the trailing empty strings when the limit is 0
  if (lim == 0) {
    while(elems.back().compare("") == 0) {
      elems.pop_back();
    }
  }

  //construct cppDeliteArray from vector
#ifdef MEMMGR_REFCNT
  std::shared_ptr<cppDeliteArraystring> ret(new cppDeliteArraystring(elems.size()), cppDeliteArraystringD());
#else
  cppDeliteArraystring *ret = new cppDeliteArraystring(elems.size());
  //cppDeliteArraystring *ret = new cppDeliteArraystring(tokens, num_tokens);
#endif
#ifdef __USE_STD_STRING__
  for(int i=0; i<elems.size(); i++)
    ret->update(i,elems.at(i));
#else
  for(int i=0; i<elems.size(); i++)
    ret->update(i,string(elems.at(i).c_str()));
#endif
  return ret;
}
#else // of __USE_STD_STRING__
#ifdef MEMMGR_REFCNT
std::shared_ptr<cppDeliteArraystring> string_split(const resourceInfo_t *resourceInfo, const string &str, const string &pattern, int32_t lim) {
#else
cppDeliteArraystring *string_split(const resourceInfo_t *resourceInfo, const string &str, const string &pattern, int32_t lim) {
#endif
  if (lim > 0) assert(false && "string_split with lim > 0 is not implemented yet");

  int strarrlen = 8; // default length for tokens array
  string *tokens = new (resourceInfo) string[strarrlen];
  int num_tokens = 0;
  int length = str.length();
  char *strptr = new (resourceInfo) char[length+1];
  strcpy(strptr, str.c_str());
  char delim;
  char *savePtr;
  if (pattern.compare("\\s+")==0) {
    //NOTE: strtok() is not thread-safe, so use strtok_r()
    char *ptr = strtok_r(strptr, " \t", &savePtr);
    while (ptr != NULL) {
      tokens[num_tokens++] = string(ptr, strlen(ptr), 0);
      ptr = strtok_r(NULL, " \t", &savePtr);
      if(num_tokens == strarrlen) {
        tokens = growStringArray(resourceInfo, tokens, strarrlen);
      }
    }
  }
  else if ((delim = find_delim(pattern)) != -1) {
    int offset = 0;
    for (int i=0; i<length; i++) {
      if (strptr[i] == delim) {
        strptr[i] = 0;
        tokens[num_tokens++] = string(strptr+offset,i-offset,0);
        offset = i + 1;
        if(num_tokens == strarrlen) {
          tokens = growStringArray(resourceInfo, tokens, strarrlen);
        }
      }
    }
    tokens[num_tokens++] = string(strptr+offset, length-offset, 0);
    //remove the trailing empty strings when the limit is 0
    if (lim == 0) {
      int i = num_tokens-1;
      while (tokens[i].length() == 0 && i >= 0) {
        tokens[i] = string();
        i--;
      }
    }
  }
  else {
    fprintf(stderr, "regex: %s\n", pattern.c_str());
    assert(false && "Given regex is not supported");
  }

#ifdef MEMMGR_REFCNT
  std::shared_ptr<cppDeliteArraystring> ret(new cppDeliteArraystring(tokens, num_tokens), cppDeliteArraystringD());
#else
  cppDeliteArraystring *ret = new cppDeliteArraystring(tokens, num_tokens);
#endif
  return ret;
}
#endif // of __USE_STD_STRING__

int32_t string_toInt(const string &str) {
  return atoi(str.c_str());
}

float string_toFloat(const string &str) {
  return strtof(str.c_str(),NULL);
}

double string_toDouble(const string &str) {
  return strtod(str.c_str(),NULL);
}

bool string_toBoolean(const string &str) {
  string b = str;
  std::transform(b.begin(), b.end(), b.begin(), ::tolower);
  if (str.compare("true") == 0)
    return true;
  else if (str.compare("false") == 0)
    return false;
  else
    assert(false && "Cannot parse boolean string");
}

#ifdef __USE_STD_STRING__
// Code from http://stackoverflow.com/questions/216823/whats-the-best-way-to-trim-stdstring
string &ltrim(string &s) {
  s.erase(s.begin(), std::find_if(s.begin(), s.end(), std::not1(std::ptr_fun<int, int>(std::isspace))));
  return s;
}

string &rtrim(string &s) {
  s.erase(std::find_if(s.rbegin(), s.rend(), std::not1(std::ptr_fun<int, int>(std::isspace))).base(), s.end());
  return s;
}

string string_trim(const string &str) {
  string ret = str;
  return ltrim(rtrim(ret));
}
#else
string string_trim(const string &str) {
  return str.trim();
}
#endif

int8_t string_charAt(const string &str, int idx) {
  return str.at(idx);
}

bool string_startsWith(const string &str, const string &substr) {
  if (str.compare(0,substr.length(),substr) == 0)
    return true;
  else
    return false;
}

string string_plus(const string &str1, const string &str2) {
  return str1 + str2;
}

string string_substr(const string &str, int32_t offset, int32_t end) {
  return str.substr(offset,end-offset+1);
}

string string_substr(const string &str, int32_t offset) {
  return str.substr(offset);
}

int32_t string_length(const string &str) {
  return str.length();
}

template<class T> string convert_to_string(T in) {
  std::ostringstream convert;
  convert << in;
#ifdef __USE_STD_STRING__
  return convert.str();
#else
  return string(convert.str().c_str());
#endif
}

// Explicit instantiation of template functions to enable separate compilation
template string convert_to_string<bool>(bool);
template string convert_to_string<int8_t>(int8_t);
template string convert_to_string<uint16_t>(uint16_t);
template string convert_to_string<int16_t>(int16_t);
template string convert_to_string<int32_t>(int32_t);
template string convert_to_string<int64_t>(int64_t);
template string convert_to_string<float>(float);
template string convert_to_string<double>(double);
template string convert_to_string<void*>(void*);
template<> string convert_to_string<string>(string str) {
  return str;
}

string readFirstLineFile(const string &filename) {
#ifdef __USE_STD_STRING__
  std::ifstream fs(filename.c_str());
  std::string line;
  if (fs.good()) {
    getline(fs, line);
  }
  fs.close();
  return line;
#else
  std::ifstream fs(filename.c_str());
  std::string line;
  if (fs.good()) {
    getline(fs, line);
  }
  fs.close();
  return string(line.c_str());
#endif
}

template <class K>
uint32_t delite_hashcode(K key) {
  return key->hashcode();
}

template<> uint32_t delite_hashcode<bool>(bool key) { return (uint32_t) key; }
template<> uint32_t delite_hashcode<int8_t>(int8_t key) { return (uint32_t) key; }
template<> uint32_t delite_hashcode<uint16_t>(uint16_t key) { return (uint32_t) key; }
template<> uint32_t delite_hashcode<int32_t>(int32_t key) { return (uint32_t) key; }
template<> uint32_t delite_hashcode<int64_t>(int64_t key) { return (uint32_t) key; }
template<> uint32_t delite_hashcode<float>(float key) { return (uint32_t) key; }
template<> uint32_t delite_hashcode<double>(double key) { return (uint32_t) key; }
template<> uint32_t delite_hashcode<string>(string key) {
  //http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/String.html#hashCode%28%29
  int32_t multiplier = 1;
  int32_t hc = 0;
  int n = key.length();
  for(int i=n-1; i>=0; i--) {
    hc += multiplier * key.at(i);
    multiplier *= 31;
  }
  return (uint32_t)hc;
}

template <class K>
bool delite_equals(K key1, K key2) {
  return key1->equals(key2);
}

template<> bool delite_equals<bool>(bool key1, bool key2) { return key1 == key2; }
template<> bool delite_equals<int8_t>(int8_t key1, int8_t key2) { return key1 == key2; }
template<> bool delite_equals<uint16_t>(uint16_t key1, uint16_t key2) { return key1 == key2; }
template<> bool delite_equals<int32_t>(int32_t key1, int32_t key2) { return key1 == key2; }
template<> bool delite_equals<int64_t>(int64_t key1, int64_t key2) { return key1 == key2; }
template<> bool delite_equals<float>(float key1, float key2) { return key1 == key2; }
template<> bool delite_equals<double>(double key1, double key2) { return key1 == key2; }
template<> bool delite_equals<string>(string key1, string key2) { return key1.compare(key2) == 0; }

template<class T> T cppDeepCopy(const resourceInfo_t *resourceInfo, T in) {
  assert(false);
}
void cppDeepCopy(const resourceInfo_t *resourceInfo) { }

/* helper methods and data structures only required for execution with Delite */
#ifndef __DELITE_CPP_STANDALONE__
pthread_mutex_t lock_objmap = PTHREAD_MUTEX_INITIALIZER;
std::map<int,jobject> *JNIObjectMap = new std::map<int,jobject>();
jobject JNIObjectMap_find(int key) {
  pthread_mutex_lock (&lock_objmap);
  jobject ret = JNIObjectMap->find(key)->second;
  pthread_mutex_unlock (&lock_objmap);
  return ret;
}
void JNIObjectMap_insert(int key, jobject value) {
  pthread_mutex_lock (&lock_objmap);
  std::map<int,jobject>::iterator it = JNIObjectMap->find(key);
  if(it != JNIObjectMap->end()) 
    it->second = value;
  else
    JNIObjectMap->insert(std::pair<int,jobject>(key,value));
  pthread_mutex_unlock (&lock_objmap);
}
#endif
