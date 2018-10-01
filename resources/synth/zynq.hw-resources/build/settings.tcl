## TARGET_ARCH must either be ZC706 or Zedboard or ZCU102
set TARGET ZC706
#set TARGET Zedboard

switch $TARGET {
  "ZC706" {
    set BOARD xilinx.com:zc706:part0:1.4
    set PART xc7z045ffg900-2
  }
  "Zedboard" {
    set BOARD em.avnet.com:zed:part0:1.3
    set PART xc7z020clg484-1
  }
  "ZCU102" {
    set BOARD xilinx.com:zcu102:part0:3.0
    set PART xczu9eg-ffvb1156-2-i
  }
  default {
    puts "$TARGET" is not a valid target! Must either be 'ZC706' or 'Zedboard' or 'ZCU102'
  }
}


