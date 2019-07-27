Note: This is a temporary location for this documentation. It may later be moved to another location.

# Running Spatial on AWS F1 Instances

This tutorial explains how to compile a Spatial application and run it on the Amazon F1.

## Contents

  * [Prerequisites](#prerequisites)
  * [Setting up aws-fpga](#setting-up-aws-fpga)
  * [Running on F1](#running-on-f1)
    + [Step 1: Authenticating your AWS account](#step-1-authenticating-your-aws-account)
    + [Step 2: Compile and Synthesize your application](#step-2-compile-and-synthesize-your-application)
    + [Step 3: Creating the AFI](#step-3-creating-the-afi)
    + [Step 4: Opening an F1 instance](#step-4-opening-an-f1-instance)
    + [Step 5: Running the Spatial application](#step-5-running-the-spatial-application)
  * [Frequently Asked Questions](#frequently-asked-questions)

## Prerequisites

Running on EC2 FPGAs requires the following prerequisites:

* An installation of Spatial

* Vivado and a license to run on the desired Amazon FPGA. We tested this tutorial using [Amazon?s FPGA Developer AMI](https://aws.amazon.com/marketplace/pp/B06VVYBLZZ#), version 1.5.0, which contains all the required software tools.
If you want to run locally, see [these instructions](https://github.com/aws/aws-fpga/blob/0c8aceaa5a36f34879ab431955c66ad4cae3ff42/hdk/docs/on_premise_licensing_help.md).


## Setting up aws-fpga

Clone [Amazon?s EC2 FPGA Hardware and Software Development Kit](https://github.com/aws/aws-fpga/) to any location:

```
$ git clone https://github.com/aws/aws-fpga.git
```

Spatial was most recently tested with version 1.4.7 of this repository (git commit a9c3a00d78cf539cdc597d2d42ea5dd268a9bc1a).

Set the `AWS_HOME` environment variable to point to the cloned directory. Also source the AWS setup scripts. The HDK script is needed for simulation and synthesis, and the SDK is needed to create the host binary:

```
$ export AWS_HOME=/path/to/aws-fpga
$ cd /path/to/aws-fpga/
$ source /path/to/aws-fpga/hdk_setup.sh
$ source /path/to/aws-fpga/sdk_setup.sh
```

For example, you can add the 4 commands above to your `.bashrc` and source that.

You will then need to make the following edits to aws-fpga source files in order to compile the Spatial C++ host:

To file `sdk/userspace/fpga_mgmt_tools/src/fpga_local_cmd.c`:
```
-                       printf(long_help);
+                       printf("%s", long_help);
```

To file `sdk/userspace/include/fpga_dma.h`:
```
-    char device_file[static FPGA_DEVICE_FILE_NAME_MAX_LEN]);
+    char device_file[FPGA_DEVICE_FILE_NAME_MAX_LEN]);
```

## Running on F1

Running on the F1 requires a few simple manual steps. These depend on your personal AWS account (EC2 and S3). Specifically, following synthesis Amazon requires the bitstream to be uploaded to your S3 account, and an EC2 account is needed to launch an F1 instance to run the spatial application in hardware.

This tutorial describes the following steps:

* Authenticating your AWS account
* Generating and synthesizing a Spatial design. In our experience, synthesis/place/route takes 4-12 hours depending on design size
* Uploading the bitstream (AKA design checkpoint, or DCP) to Amazon S3 and waiting approximately 1 hour for the Amazon FPGA Image (AFI) associated with this bitstream to become valid
* Opening an F1 instance through your EC2 account
* Running the spatial application

### Step 1: Authenticating your AWS account

Follow [these steps](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_root-user.html#id_root-user_manage_add-key) to create a file `rootkey.csv`. This file can be placed anywhere, and will be needed for later steps to run commands associated with your AWS account.

Then add the path to that file to your .bashrc as follows:

```
export AWS_CONFIG_FILE=/path/to/rootkey.csv
```

### Step 2: Compile and Synthesize your application

Compile your application using the following command:

```
  $ bin/spatial example --synth --fpga=AWS_F1 && cd gen/example && make aws-F1
```

This step requires Vivado. We tested using Amazon?s FPGA [Developer AMI](https://aws.amazon.com/marketplace/pp/B06VVYBLZZ#) version 1.5.0, as mentioned above, however this can also be run locally using [these instructions](https://github.com/aws/aws-fpga/blob/0c8aceaa5a36f34879ab431955c66ad4cae3ff42/hdk/docs/on_premise_licensing_help.md).

Notice that once this command completes, Vivado synthesis will be running in the background. A script called `create_spatial_AFI_instructions.sh` has also been created. Follow the instructions in this text file (also described below) once Vivado completes to upload the Design Checkpoint (DCP) and finish creating the AFI.

### Step 3: Creating the AFI

Once Vivado has finished, run:

```
bash create_spatial_AFI_instructions.sh
```

This is an executable bash script that uploads the DCP to S3 and then runs the `create-fpga-image` command.
Running this will require the [AWS Command Line Interface](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html). We tested with version 1.11.78.

Once `create-fpga-image` has been run by the script above, it will print the agfi ID.
Modify the file `load.sh` in `software/runtime` to paste in the agfi ID returned above.
Now wait approximately 1-2 hours for the `logs` directory in S3 to be filled, and ensure that the AFI state in the file called `State` is ?available?.


### Step 4: Opening an F1 instance

Start an F1 instance in the AWS console. We tested with 
[Amazon?s FPGA Developer AMI](https://aws.amazon.com/marketplace/pp/B06VVYBLZZ#), version 1.5.0, 
in order to ensure the right SDK tools are compiled (e.g. `fpga-load-local-image`).

If you already have an existing F1 instance (e.g. for a previous Spatial application), skip to Step 5. If this is your first time starting the F1 instance, follow the one-time setup steps below.

Clone [Amazon?s EC2 FPGA Hardware and Software Development Kit](https://github.com/aws/aws-fpga/) to any location, e.g. `/home/centos/src/project_data`:

```
git clone https://github.com/aws/aws-fpga.git
```

Put the following (replacing with your chosen path above) in your `.bashrc`:

```
cd /home/centos/src/project_data/aws-fpga/
source /home/centos/src/project_data/aws-fpga/sdk_setup.sh
```

Source the `.bashrc`:

```
source ~/.bashrc
```

Then follow [these instructions](https://github.com/aws/aws-fpga/blob/9791342cd752ca3b7a631afec1f4ab3c6390d274/sdk/linux_kernel_drivers/xdma/xdma_install.md) to build and install the required XDMA driver. Verify it is running using:

```
lsmod | grep xdma
```


### Step 5: Running the Spatial application

The command `make aws-F1` above should have created the `Top` software binary.

You can do this on your local machine and copy over the binary to the F1 (this might require changing permissions to run it), or compile the binary on the F1 instance. To do it on the F1 instance, you only need the `software/runtime` and `software/include` directories of the generated Spatial AWS application, and can compile using `make all` in `software/runtime`.

Load the afi to the FPGA using the command below. `load.sh` can be found in the `runtime` directory.

```
bash load.sh
```

Now you an run the application as many times as needed using the command below. `Top` can also be found in the `runtime` directory.

```
sudo ./Top arg1 arg2 ...
```


## Important Tips

* Do not include capital letters or underscores in your Spatial application name. This name is used to make the S3 bucket, and S3 does not allow these characters.

## Frequently Asked Questions

Q. How do these steps differ from the F1 tutorial on the Spatial webpage?

A. Those steps are out of date, these steps are current.

Q. How do I fix the following error? `TopHost.cpp:816:8: error: ‘memcpy’ is not a member of ‘std’`

A. Edit TopHost.cpp and replace `std::memcpy` with just `memcpy`
