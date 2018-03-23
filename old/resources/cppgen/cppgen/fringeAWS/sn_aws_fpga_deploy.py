########################################################################################
# SambaNova Systems
# 
# Author: Bandish Shah
# Script: sn_aws_fpga_deploy.py 
#           - Given DCP, generate AFI and deploy AFI if running on F1 instance
#
########################################################################################

import os
import sys
import getopt
import time
import datetime
import subprocess
import requests
import boto3


def print_help(): 
  help_message = "sn_aws_fpga_deploy.py --bucket_name <AWS S3 Bucket Name> --region <us-west-1 | us-west-2 [default]> --dcp_image <dcp_tarball> --afi_name <AFI name> --afi_description <Describe AFI>"
  print help_message


def print_info(text):
  print "INFO: " + text


def s3_deploy_bucket(bucket_name, region, dcp_image):
  s3 = boto3.resource('s3')
  
  # Grab DCP filename
  dcp_filename = os.path.basename(dcp_image)

  # Create S3 bucket in us-west-2 region
  # TODO should probably check if bucket already exists, right now just errors out
  bucket = s3.create_bucket(Bucket=bucket_name, CreateBucketConfiguration={'LocationConstraint': region})

  # Upload DCP to bucket
  bucket.upload_file(dcp_image, 'dcp/' + dcp_filename)

  # Touch 'LOG_FILES_GO_HERE' file and upload log area
  subprocess.call(["touch", "LOG_FILES_GO_HERE.txt"])
  bucket.upload_file('LOG_FILES_GO_HERE.txt', 'logs/LOG_FILES_GO_HERE.txt')

  print_info("- Created S3 Bucket -")
  print_info("	  Name: " + bucket_name)
  print_info("	  Region: " + region)
  print_info("	  DCP TAR: dcp/" + dcp_filename)
  print_info("	  Logs: logs/")


def ec2_create_afi(bucket_name, dcp_image, afi_name, afi_description):
  ec2 = boto3.client('ec2')

  # Grab DCP filename
  dcp_filename = os.path.basename(dcp_image)
  
  response = ec2.create_fpga_image(
		  #DryRun=True,
		  InputStorageLocation={'Bucket': bucket_name, 'Key': "dcp/" + dcp_filename},
		  LogsStorageLocation={'Bucket': bucket_name, 'Key': "logs/"},
		  Description=afi_description,
		  Name=afi_name)
  
  return response


def ec2_poll_afi_status(afi_id):
  ec2 = boto3.client('ec2')
  image_status = ''
  try_count = 0

  print_info("- Polling AFI creation status (usually takes 30-45 min) -")
  while image_status != 'available':
    try_count += 1
    print_info("    Polling AFI status, try " + str(try_count))
    print_info("    AFI not available, sleeping for 5 min")
    time.sleep(300)
    response = ec2.describe_fpga_images(FpgaImageIds=[afi_id])
    image_status = response['FpgaImages'][0]['State']['Code']
    print_info("    AFI status: " + image_status)

  print_info("- AFI available - ")


def ec2_deploy_fpga(afi_gid):
  print_info("- Checking instance type for FPGA deployment -")
  
  # Curl request to JSON file that describes AWS instance
  # More information: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-identity-documents.html
  r = requests.get('http://169.254.169.254/latest/dynamic/instance-identity/document')

  # Check JSON dict to see if instance type is f1.2xlarge and load FPGA image, if not then do nothing
  if r.json()['instanceType'] != 'f1.2xlarge':
    print_info("    Instance type not f1.2xlarge, AFI will not be deployed")
  else:
    print_info("    Instance type is f1.2xlarge, deploying AFI")
    # Clear local FPGA image
    subprocess.check_output("sudo fpga-clear-local-image -S 0", shell=True)

    # Load FPGA image
    subprocess.check_output("sudo fpga-load-local-image -S 0 -I " + afi_gid, shell=True)

    # Perform driver reset
    print_info("    Performing driver reset")
    subprocess.check_output("sudo rmmod edma-drv && sudo insmod $AWS_HOME/sdk/linux_kernel_drivers/edma/edma-drv.ko", shell=True)
    
    print_info("- AFI deployed -")


def main(argv):
  date = datetime.datetime.now().strftime("%m-%d-%y")
  aws_s3_bucket_name = ''
  aws_region = 'us-west-2'
  aws_dcp_image = ''
  aws_afi_name = ''
  aws_afi_id = ''
  aws_afi_gid = ''
  aws_afi_description = "SambaNova AFI image, update with better description"

  # Argument parsing
  try:
    opts, args = getopt.getopt(argv, "hb:r:d:a:c:", ["help", "bucket_name=", "region=", "dcp_image=", "afi_name=", "afi_description="])
  except getopt.GetoptError as err:
    print str(err)
    print_help()
    sys.exit(2)
  for opt, arg in opts:
    if opt in ("-h", "--help"):
      print_help()
      sys.exit()
    elif opt in ("-b", "--bucket_name"):
      aws_s3_bucket_name = arg
    elif opt in ("-r", "--region"):
      aws_region = arg
    elif opt in ("-d", "--dcp_image"):
      aws_dcp_image = arg
    elif opt in ("-a", "--afi_name"):
      aws_afi_name = arg
    elif opt in ("-c", "--afi_description"):
      aws_afi_description = arg


  # 1) Create S3 bucket and deploy DCP tarball 
  s3_deploy_bucket(aws_s3_bucket_name, aws_region, aws_dcp_image)

  # 2) Submit DCP to AWS create AFI, return dict with AFI and AGFI IDs
  aws_afi_info = ec2_create_afi(aws_s3_bucket_name, aws_dcp_image, aws_afi_name, aws_afi_description)
  aws_afi_id = aws_afi_info['FpgaImageId']
  aws_afi_gid = aws_afi_info['FpgaImageGlobalId']
  print_info("    AFI-ID: " + aws_afi_id)
  print_info("    AFI-GID: " + aws_afi_gid)
  print_info("- AFI " + aws_afi_name + " submitted for creation -")

  # 3) Poll AWS EC2 to see if AFI is available
  ec2_poll_afi_status(aws_afi_id)

  # 4) Check if running on F1 instance and deploy FPGA image once AFI is available
  ec2_deploy_fpga(aws_afi_gid)


if __name__ == "__main__":
  if len(sys.argv) == 1:
    print_help()
    sys.exit()
  else:
    main(sys.argv[1:])
