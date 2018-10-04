#ifndef __DELITE_FILE_SYSTEM__
#define __DELITE_FILE_SYSTEM__

#include <cerrno>
#include <stdio.h>
#include <stdlib.h>
#include <dirent.h>
#include <sys/stat.h>
#include "DeliteNamespaces.h"

class DeliteFileSystem {

  public:

    // deletes the given path recursively
    static void deleteRecursive(string pathname) {
      if (!exists(pathname)) return;
      struct stat st;
      const char *path = pathname.c_str();
      lstat(path, &st);
      if (S_ISDIR(st.st_mode)) {
          DIR *dir = opendir(path);
          struct dirent *dp;
          while ((dp = readdir(dir)) != NULL) {
            struct stat st;
            string filename = string(path) + string("/") + string(dp->d_name);
            lstat(filename.c_str(), &st);
            if (S_ISREG(st.st_mode)) remove(filename.c_str());
            // TODO: Make sure code below is safe for all platforms! (Need to be careful about parent directory)
            //else if(S_ISDIR(st.st_mode) && dp->d_name[0] != '.') deleteRecursive(filename);
          }
          closedir(dir);
          remove(path);
      }
      else if (S_ISREG(st.st_mode)) {
        remove(path);
      }
      else {
        fprintf(stderr, "[DeliteFileSystem] Path %s does not appear to be a valid file or directory\n", path);
        exit(-1);
      }
    }

    // checks existence of a file/directory
    static bool exists(string pathname) {
      struct stat st;
      if (lstat(pathname.c_str(),&st) != 0) {
        if(errno == ENOENT) return false;
        else {
          fprintf(stderr, "Path %s exists but cannot be accessed\n", pathname.c_str());
          exit(-1);
        }
     }
     else return true;
   }
};

#endif
