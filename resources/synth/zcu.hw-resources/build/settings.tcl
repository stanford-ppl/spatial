## TARGET_ARCH must either be ZC706 or Zedboard or ZCU102
# set TARGET ZC706
set TARGET Zedboard

set ver [version -short]

switch $TARGET {
  "ZCU102" {
    # For 2017.4 or later?
    if {[string match 2018* $ver] || [string match 2019* $ver]} {
      set BOARD xilinx.com:zcu102:part0:3.1
      set PART xczu9eg-ffvb1156-2-e
    } else {
      set BOARD xilinx.com:zcu102:part0:3.0
      set PART xczu9eg-ffvb1156-2-i      
    }
  }
  default {
    puts "$TARGET" is not a valid target! Must either be 'ZCU102'
  }
}


