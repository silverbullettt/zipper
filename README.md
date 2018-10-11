This is the implementation of the technique proposed in our OOPSLA'18 paper "[Precision-Guided Context Sensitivity for Pointer Analysis](https://silverbullettt.bitbucket.io/papers/oopsla2018.pdf)", called ZIPPER. 
ZIPPER is the first research work that shows how and where most imprecision is introduced in a context-insensitive pointer analysis. By applying context sensitivity only to the precision-critical methods identified by ZIPPER (context insensitivity to other methods), ZIPPER enables a conventional context-sensitive pointer analysis (e.g., 2-object-sensitivity) to run much faster while retaining essentially all of its precision.
The artifact of our paper can be downloaded from [here](http://www.brics.dk/zipper/OOPSLA18-Artifact-Zipper.tar.gz).

To demonstrate the usefulness of ZIPPER to pointer analysis, we have integrated ZIPPER with [DOOP](https://bitbucket.org/yanniss/doop) ([PLDI'14 artifact version](http://cgi.di.uoa.gr/~smaragd/pldi14ae/pldi14-ae.tgz)), a state-of-the-art context-sensitive points-to analysis framework for Java. For your convenience, this repository also contains the DOOP framework with ZIPPER integrated.

This tutorial introduces how to build and use ZIPPER together with DOOP.


## Requirements

- A 64-bit Ubuntu system
- A Java 8 (or later) distribution
- A Python 2.x interpreter

It is recommended to set your JAVA_HOME environment variable to point to your Java installation.


## Building ZIPPER

We have provided a pre-compiled jar of ZIPPER, i.e., `zipper.jar` in the directory `zipper/build/`. To build ZIPPER by yourself, you just need to switch to the `zipper/` directory and run script:

`$ ./compile.sh`

The generated `zipper.jar` will be placed in `zipper/build/` and overwrite the previous one.


## DOOP Framework

Now we introduce how to use DOOP together with ZIPPER.

### Installing Datalog Engine

To run DOOP framework, you need to install a LogicBlox engine for interpreting the Datalog rules used in DOOP. If you already have such engine installed (e.g., LogicBlox v3.9.0), you can skip this section. Otherwise, you can use PA-Datalog engine, a port available for academic use. The download link and installation instructions of PA-Datalog can be found on [this page](http://snf-705535.vm.okeanos.grnet.gr/agreement.html) (We recommend `.deb` package installation).

### Running DOOP

Please first change your current directory to `doop/` folder.

The command of running DOOP is:

`$ ./run -jre1.6 <analysis> <program-jar-to-analyze>`

For example, to analyze `foo.jar` with 2-object-sensitive analysis, just type:

`$ ./run -jre1.6 2-object-sensitive+heap foo.jar`

You can check all the supported `<analysis>` and other options with `./run -h`.


### Running DOOP with ZIPPER

The usage of running ZIPPER-guided pointer analysis is exactly same as the DOOP's usage, except that you need to change the driver script from `run` to `run-zipper.py`.

For example, the command to run ZIPPER-guided 2-object-sensitive analysis for `foo.jar` is:

`$ ./run-zipper.py -jre1.6 2-object-sensitive+heap foo.jar`

Such command first runs a context-insensitive pointer analysis as pre-analysis, then executes ZIPPER to compute and output precision-critical methods, and finally runs the main analysis (`2-object-sensitive+heap` in this case), which applies context sensitivity to only the precision-critical methods output by ZIPPER.
