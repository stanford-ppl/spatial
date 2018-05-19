#ifndef __DELITE_FILE_OUTPUT_STREAM__
#define __DELITE_FILE_OUTPUT_STREAM__

#include <iostream>
#include <fstream>
#include <vector>
#include <sys/stat.h>
#include "DeliteFileSystem.h"

class DeliteFileOutputStream {
  private:
    uint32_t numFiles;
    std::vector<string> paths;
    std::vector<std::ofstream *> writers;
    bool append;

    void setFilePaths(string path, uint32_t numFiles) {
      
      if (DeliteFileSystem::exists(path) && !append)
        DeliteFileSystem::deleteRecursive(path);

      if (numFiles == 1) {
        paths.push_back(path);
        writers.push_back(NULL);
      }
      else {
        // create file (755 permission)
        mkdir(path.c_str(), S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH);
        int i = path.length() - 1;
        while (i >= 0 && path.at(i) != '/') i--;
        string baseFileName = path.substr(i+1);
        for (int i=0; i<numFiles; i++) {
          char postfix[5];
          sprintf(postfix, "%04d", i);
          string filename = baseFileName + string(postfix);
          paths.push_back(path + "/" + filename);
          writers.push_back(NULL);
        }
      }
    }

    uint32_t getFileIdx(const resourceInfo_t *resourceInfo) {
      return (numFiles == 1) ? 0 : resourceInfo->threadId;
    }

  public: 
    DeliteFileOutputStream(string path, bool sequential, bool append, const resourceInfo_t *resourceInfo) {
      numFiles = sequential ? 1 : resourceInfo->numThreads;
      this->append = append;
      setFilePaths(path, numFiles);
    }

    void writeLine(const resourceInfo_t *resourceInfo, string line) {
      uint32_t fileIdx = getFileIdx(resourceInfo);
      if (fileIdx < 0 || fileIdx >= writers.size()) {
          fprintf(stderr, "error: file index %d\n", fileIdx);
          assert(false);
      }

      if (writers[fileIdx] == NULL) {
        string chunkPath = paths[fileIdx];
        if (append) writers[fileIdx] = new std::ofstream(chunkPath.c_str(), std::ofstream::out | std::ofstream::app);
        else writers[fileIdx] = new std::ofstream(chunkPath.c_str());
      }

      *writers[fileIdx] << line << std::endl;;
      //writers[fileIdx]->flush();
    }

    void close(const resourceInfo_t *resourceInfo) {
      uint32_t fileIdx = getFileIdx(resourceInfo);
      if (writers[fileIdx] != NULL) {
        writers[fileIdx]->close();
      }
    }

    void close() {
      for (int i=0; i<writers.size(); i++) {
        if (writers[i] != NULL) writers[i]->close();
      }
    }
};

#endif
