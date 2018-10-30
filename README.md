# Spatial
Spatial is an Argon DSL for programming reconfigurable hardware from a parameterized, high level abstraction.  

# Getting Started

We recommend using [spatial-quickstart](https://github.com/stanford-ppl/spatial-quickstart) rather than this repo, if you only intend to develop applications without modifying the compiler.  

If you prefer to install Spatial from source using this repo, follow these instructions.  The [Spatial website](https://spatial.stanford.edu) has more information and tutorials.

```
    $ git clone https://github.com/stanford-ppl/spatial.git
    $ cd spatial
    $ make install
```

To run an app:

```
    $ bin/spatial <app name> <options>
    $ cd gen/<app name>
    $ make
    $ bash run.sh <input args>
```

# Links

  * [Website](https://spatial.stanford.edu)
  * [Documentation](http://spatial-lang.readthedocs.io/en/latest/)
  * [Regressions](https://docs.google.com/spreadsheets/d/1_bbJHrt6fvMvfCLyuSyy6-pQbJLiNY4kOSoKN3voSoM/edit#gid=1748974351)

