#ifndef __DELITE_FILE_INPUT_STREAM__
#define __DELITE_FILE_INPUT_STREAM__

#include <cstdarg>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <vector>
#include <string.h>
#include <cstring>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <algorithm>
#include <string>
#include <errno.h>
#include <fcntl.h>
#include <dirent.h>
#include "DeliteNamespaces.h"
#include "DeliteCpp.h"
#include "DeliteMemory.h"

// each line of file is limited to 1M characters
#define MAX_BUFSIZE 1048576

// switch for printing debugging message
//#define DFS_DEBUG(...) fprintf(stderr, "[DEBUG-DeliteFS] "); fprintf(stderr, __VA_ARGS__)
#define DFS_DEBUG(...)

//TODO: check if need to compile with _FILE_OFFSET_BITS == 64?
class DeliteFileInputStream {
  private:
    std::vector<char*> files;
    std::vector<uint64_t> fileLengths;
    size_t idx;

    FILE *reader;
    char *text;

    void findFileOffset(uint64_t start, size_t &fileIdx, uint64_t &offset) {
      offset = start;
      fileIdx = 0;
      while (offset >= fileLengths.at(fileIdx)) {
        offset -= fileLengths.at(fileIdx);
        fileIdx += 1;
      }
    }

    void addFile(const char *pathname, uint64_t file_size) {
      // NOTE: explicit copy is required since the pathname (char*) given to constructor may be freed outside.
      DFS_DEBUG("adding file %s of size %lu\n", pathname, file_size);
      char *p = (char *)malloc(strlen(pathname)+1);
      strcpy(p, pathname);
      files.push_back(p);
      fileLengths.push_back(file_size);
      size += file_size;
    }

  public:
    uint64_t size;
    uint64_t position;
    uint64_t streamOffset;

    DeliteFileInputStream* openCopyAtNewLine(uint64_t start) {
      DeliteFileInputStream* copy = new DeliteFileInputStream(streamOffset, size, &files, &fileLengths);
      copy->openAtNewLine(start);
      return copy;
    }

    void openAtNewLine(uint64_t start) { 
      position = streamOffset + start;
      uint64_t offset;
      findFileOffset(position, idx, offset);
      reader = fopen(files.at(idx), "r");
      text = (char *)malloc(MAX_BUFSIZE*sizeof(char));

      if (reader == NULL) {
        fprintf(stderr, "error reading file %s (%s)\n", files.at(idx), strerror(errno));
        assert(false);
      }

      if (offset != 0) {
        // jump to the offset
        if(lseek(fileno(reader), offset-1, SEEK_SET) == -1) {
          assert(false && "lseek call failed");
        }
        // find the next newline after offset
        if (fgets(text, MAX_BUFSIZE, reader) == NULL) {
          assert(false && "first fgets failed");
        }
        position += strlen(text) - 1;
      }
    }

    string readLine(const resourceInfo_t *resourceInfo) {
      char *line = text;
      while (fgets(line, MAX_BUFSIZE, reader) == NULL) {
        // read the next file
        idx += 1;
        if (idx >= files.size()) 
          return "";
        else 
          fclose(reader);
        reader = fopen(files.at(idx), "r");
        if (reader == NULL) {
          fprintf(stderr, "error reading file %s (%s)\n", files.at(idx), strerror(errno));
          assert(false);
        }
      }
      size_t length = strlen(line);
      position += length;
      char *strptr = new (resourceInfo) char[length+1];
      strcpy(strptr, line);
      string str(strptr, length, 0);
      str.erase(std::remove(str.begin(), str.end(), '\n'), str.end());
      str.erase(std::remove(str.begin(), str.end(), '\r'), str.end());
      return str;
    }

    DeliteFileInputStream(uint64_t _offset, uint64_t _size, std::vector<char*> *_files, std::vector<uint64_t> *_fileLengths) {
      size = _size;
      streamOffset = _offset;
      //could share the files but then the free logic becomes confusing
      for (size_t i = 0; i < _files->size(); i++) {
        char *pathname = _files->at(i);
        char *p = (char *)malloc(strlen(pathname)+1);
        strcpy(p, pathname);
        files.push_back(p);
        fileLengths.push_back(_fileLengths->at(i));
      }
    }

    DeliteFileInputStream(uint64_t _offset, size_t num, ...) {
      size = 0;
      streamOffset = _offset;
      va_list arguments;
      DFS_DEBUG("number of paths is %d\n", num);
      va_start(arguments, num);
      for(size_t i=0; i<num; i++) {
        const char *pathname = va_arg(arguments, char *);
        DFS_DEBUG("pathname is %s\n", pathname);

        // check if file or directory
        struct stat st;
        lstat(pathname, &st);
        if (S_ISDIR(st.st_mode)) {
          DIR *dir = opendir(pathname);
          struct dirent *dp;
          while ((dp = readdir(dir)) != NULL) {
            struct stat st;
            string filename = string(pathname) + string("/") + string(dp->d_name);
            lstat(filename.c_str(), &st);
            if (S_ISREG(st.st_mode)) addFile(filename.c_str(), st.st_size);
          }
          closedir(dir);
        }
        else if (S_ISREG(st.st_mode)) {
          addFile(pathname, st.st_size);
        }
        else {
          fprintf(stderr, "[DeliteFileInputStream] Path %s does not appear to be a valid file or directory\n", pathname);
          exit(-1);
        }
      }
      va_end(arguments);

      if (size > 0) {
        openAtNewLine(0);
      }
      else {
        fprintf(stderr, "DeliteFileInputStream opened with size == 0. Paths were: [");
        for(int i=0; i<files.size(); i++) fprintf(stderr, "%s,", files[i]);
        fprintf(stderr, "]\n");
        exit(-1);
      }

      DFS_DEBUG("total size of file is %ld\n", size);
    }

    void close() { 
      fclose(reader);
    }

    ~DeliteFileInputStream() {
      free(reader);
      free(text);
      for(std::vector<char*>::iterator it = files.begin(); it != files.end(); ++it) {
        free(*it);
      }
    }
};

#endif

/* 
// main function for test
int main(void) {
  DeliteFileInputStream file(1, "hello.txt");
  file.openAtNewLine(0);
  file.openAtNewLine(1);

  string s;
  while((s = file.readLine(0)).length() != 0) {
    std::cout << "string is " << s << endl;
  }
  std::cout << "hline" << endl;
  while((s = file.readLine(1)).length() != 0) {
    std::cout << "string is " << s << endl;
  }

  return 0;
}
*/
