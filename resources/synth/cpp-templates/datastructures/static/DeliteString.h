#include <string.h>
#include <assert.h>
#include <iostream>

namespace delite
{

//TODO: compare http://code.google.com/p/re2/source/browse/re2/stringpiece.h
class string {
private:
  const char *_ptr;
  size_t _length;
  int _offset;
  
public:
  string() {
    _ptr = NULL;
    _length = 0;
    _offset = 0;
  }

  string(const char *sptr) {
    _length = strlen(sptr);
    char *buf = new char[_length+1];
    strcpy(buf,sptr);
    _ptr = buf;
    _offset = 0;
  }

  string(const char *sptr, int length) {
    _length = length;
    char *buf = new char[_length+1];
    strncpy(buf,sptr,length);
    buf[length] = 0;
    _ptr = buf;
    _offset = 0;
  }

  string(const char *sptr, int length, int offset) {
    _ptr = sptr;
    _length = length;
    _offset = offset;
  }

  const char *c_str(void) const {
    return _ptr+_offset;
  }

  size_t length(void) const {
    return _length;
  }

  int compare(const string &str) const {
    int ret = strncmp(str.c_str(),c_str(),_length);
    if (ret == 0)
      return length() - str.length();
    else
      return ret;
  }

  int compare(int offset, int length, const string &str) const {
    return strncmp(c_str()+offset,str.c_str(),length);
  }

  char at(int idx) const {
    return _ptr[idx+_offset];
  }

  string operator+(const string &str) const {
    int result_length = length() + str.length();
    char *buf = new char[result_length+1];
    strncpy(buf,c_str(),_length);
    strncpy(buf+_length,str.c_str(),str.length());
    buf[result_length] = 0;
    return string(buf,result_length,0);
  }

  bool operator==(const string &str) const {
    return (compare(str) == 0);
  }

  bool operator<(const string &str) const {
    return (compare(str) < 0);
  }

  bool operator<=(const string &str) const {
    return (compare(str) <= 0);
  }
  
  bool operator>(const string &str) const {
    return (compare(str) > 0);
  }

  bool operator>=(const string &str) const {
    return (compare(str) >= 0);
  }

  string substr(int offset) const {
    return string(c_str(), length()-offset, offset);
  }

  string substr(int offset, int length) const {
    if (_offset + length < _length)
      return string(c_str()+offset, length);
    else
      return string(c_str()+offset, length, 0);
  }

  //TODO: what if the caller mutates the returned pointer? should return const char *?
  char *begin(void) {
    return (char *)c_str();
  }

  char *end(void) {
    return (char *)c_str() + length();
  }

  void erase(char *start, char *end) {
    for(char *ptr = start; ptr < end; ptr++) {
      *ptr = 0;
    }
    _length -= end-start;
  }

  friend std::ostream& operator<< (std::ostream &out, const string &str) {
    out << str.c_str();
    return out;
  }

  //http://www.docjar.com/html/api/java/lang/String.java.html
  string trim(void) const {
    int st = 0;
    int len = _length;
    int off = _offset;
    const char *val = _ptr;
    while ((st < len) && (val[off + st] <= ' ')) {
      st++;
    }
    while ((st < len) && (val[off + len - 1] <= ' ')) {
      len--;
    }
    return ((st > 0) || (len < _length)) ? substr(st, len-st) : *this;
  }
};

} // namespace delite
