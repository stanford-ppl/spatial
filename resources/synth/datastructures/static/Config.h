#ifndef _DELITE_CONFIG_H_
#define _DELITE_CONFIG_H_

class Config {
public:
    int numThreads;
    int numCoresPerSocket;
    int numSockets;

    Config(int _numThreads) {
        numThreads = _numThreads;
        numCoresPerSocket = _numThreads;
        numSockets = 1;
    }

    Config(int _numThreads, int _numCoresPerSocket, int _numSockets) {
        numThreads = _numThreads;
        numCoresPerSocket = _numCoresPerSocket;
        numSockets = _numSockets;
    }

    int numCores() {
        return numCoresPerSocket * numSockets;
    }

    //current strategy is to spill threads to next socket when all cores are filled
    //then repeat for numThreads > numCores
    int threadToSocket(int threadId) {
        int socketId = threadId / numCoresPerSocket % numSockets;
        return socketId;
    }

    int activeSockets() {
        if (numThreads >= numCores()) return numSockets;
        else return threadToSocket(numThreads-1)+1;
    }

};

#endif
