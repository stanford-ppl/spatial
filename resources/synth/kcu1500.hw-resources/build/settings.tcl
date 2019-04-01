set TARGET KCU1500

switch $TARGET {
  "KCU1500" {
    set BOARD xilinx.com:kcu1500:part0:1.1
    set PART xcku115-flvb2104-2-e
  }
  default {
    puts "$TARGET" is not a valid target! Must either be 'KCU1500'
  }
}


