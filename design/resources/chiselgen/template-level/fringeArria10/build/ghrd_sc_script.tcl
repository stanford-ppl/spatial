# setting parameter for system infomation ######################################
# this section must be modified to represent the targeted memory map of the design
set sysid_base 0x0000
set pio_led_base 0x0010
set pio_dipsw_base 0x0030
set pio_button_base 0x0020
set intr_capture_base 0x0100
set onchip_mem_base 0x0
set f2sdram_base 0x0

set dipsw_int_bit 0
set button_int_bit 1

################################################################################

# set $mj as handler of dedicated master
#set mj [get_service_paths master]

# if system has multiple masters, dedicated master to be in variable
# so to know that return master service path with phy_1 is an non-secure jtag master,
# phy_2 is an secure jtag master in design, and phy_0 is JTAG master attached to F2SDRAM

set all_sp [get_service_paths master]
#puts "all ser.path: $all_sp"
puts "llength of all_sp: [ llength $all_sp ]"
for {set i 0} {$i<[llength $all_sp]} {incr i} {
  set sp($i) [lindex [get_service_paths master] $i]
  #puts $sp($i)

  if {[ regexp {/10AS.+/phy_0/} "$sp($i)" ]} {
    #set result [ regexp {/phy_0/} "$sp($i)" ]
    #puts "matched /phy_0/: $result"
    set mj_sdram "$sp($i)"
  }

  if {[ regexp {/10AS.+/phy_1/} "$sp($i)" ]} {
    #set result [ regexp {/phy_1/} "$sp($i)" ]
    #puts "matched /phy_1/: $result"
    set mj_nsec "$sp($i)"
  }
  
  if {[ regexp {/10AS.+/phy_2/} "$sp($i)" ]} {
    #set result [ regexp {/phy_2/} "$sp($i)" ]
    #puts "matched /phy_2/: $result"
    set mj_secu "$sp($i)"
  }

}
 
#set mj_nsec [lindex [get_service_paths master] 0]
open_service master $mj_nsec

#set mj_secu [lindex [get_service_paths master] 1]
open_service master $mj_secu

open_service master $mj_sdram

# display back the variable name
puts "non-secure jtag master: $mj_nsec"
puts "secure jtag master    : $mj_secu"
puts "sdram jtag master     : $mj_sdram"

set ser_path [get_service_paths monitor]
set mon_path [claim_service monitor $ser_path "my_lib" ""]
monitor_set_interval $mon_path 5000
monitor_set_callback $mon_path [list monitor_callback_bg $mon_path]
monitor_add_range $mon_path $mj_nsec $intr_capture_base 1

# disable the monitoring activity by default when script is sourced
#monitor_set_enabled $mon_path 0

proc monitor_callback_bg {mon_path} {
  global mj_nsec
  global intr_capture_base
  global dipsw_int_bit
  global button_int_bit

  set int_data [monitor_read_data $mon_path $mj_nsec $intr_capture_base 1]
  set interv [monitor_get_read_interval $mon_path $mj_nsec $intr_capture_base 1]
  puts "\nInterrupt capture value $int_data at interval of $interv ms"

  set dipsw_irq_locate [ expr { $int_data >> $dipsw_int_bit } ]
  #puts "located dipsw = $dipsw_irq_locate"
  set is_dipsw_irq [ expr {$dipsw_irq_locate & 1} ]
  puts "dipswitch irq value: $is_dipsw_irq"
  if { $is_dipsw_irq != 0 } {
    puts "     DIPSW_PIO got interrupt!"
  }

  set button_irq_locate [ expr { $int_data >> $button_int_bit } ]
  #puts "located button = $button_irq_locate"
  set is_button_irq [ expr {$button_irq_locate & 1} ]
  puts "button irq value: $is_button_irq"
  if { $is_button_irq != 0 } {
    puts "     BUTTON_PIO got interrupt!"
  }

  puts "wait for 5 seconds to next read..."
}

proc irq_monitor_on {} {
  global mon_path
  monitor_set_enabled $mon_path 1
}

proc irq_monitor_off {} {
  global mon_path
  monitor_set_enabled $mon_path 0
}

proc regwr {addr char} {
  global mj_nsec
  master_write_32 $mj_nsec $addr $char
  puts "written to address: 0x[ format %x [ expr $addr ] ]"
}

proc regrd {addr} {
  global mj_nsec
  set rdata [master_read_32 $mj_nsec $addr 1]
  puts "readdata: $rdata, from addr: $addr"
  return $rdata
}

proc secwr {addr char} {
  global mj_secu
  master_write_32 $mj_secu $addr $char
  puts "written to address: 0x[ format %x [ expr $addr ] ]"
}

proc secrd {addr} {
  global mj_secu
  set rdata [master_read_32 $mj_secu $addr 1]
  puts "readdata: $rdata, from addr: $addr"
  return $rdata
}

proc int2bits {i} {
     #returns a bitslist, e.g. int2bits 10 => {1 0 1 0} 
     set res ""
     while {$i>0} {
         set res [expr {$i%2}]$res
         set i [expr {$i/2}]
     }
     if {$res==""} {set res 0}
     split $res ""
 }

# generate random integer number in the range [min,max]
proc randrange { min max } {
    set maxfactor [expr [expr $max + 1] - $min]
    set value [ expr int([expr rand() * $maxfactor])]
    set value [ expr $value + $min ]
   # set value [expr int([expr rand() * 100])]
   # set value [expr [expr $value % $maxfactor] + $min]
return $value
}

# procedure that read FPGA system ID
proc sysid_read {} {
  global mj_nsec
  global sysid_base
  regrd $sysid_base
}

# procedure that read push button value
proc button_data_read {} {
  global mj_nsec
  global pio_button_base
  regrd 0x[format %8.8x [ expr $pio_button_base+0xc ]]
}

# procedure that read push button value and write 1 to clear
proc button_data_read_clear {} {
  global mj_nsec
  global pio_button_base
  set rdata [ regrd 0x[format %8.8x [ expr $pio_button_base+0xc ]]]
  puts "Button value: 0x$rdata\n"
  regwr [ expr $pio_button_base+0xc ] $rdata
}

# procedure that enable interrupt of button peripheral
# "bit_enable" argument represent which bit of push button to generate IRQ
proc button_intr_enable {bit_enable} {
  global mj_nsec
  global pio_button_base
  set inhex [ expr 0x1<<$bit_enable ]
  #set inhex [format %x $bit_enable]
  set rintr [ regrd [ expr $pio_button_base+8 ] ]
  puts "read existing interrupt reg: $rintr"
  set wintr [ expr $rintr | $inhex ]
  puts "interrupt value to be written: 0x[format %8.8x $wintr]"
  regwr [ expr $pio_button_base+8 ] $wintr
}

# procedure that disable interrupt of button peripheral
proc button_intr_disable {} {
  global mj_nsec
  global pio_button_base
  regwr [ expr $pio_button_base+8 ] 0x0
}

# procedure that light on LED base on 4-bit hexadecimal value written
proc led_on {pattern} {
  global mj_nsec
  global pio_led_base
  regwr [ expr $pio_led_base ] $pattern
}

# procedure that light off the 4 LEDs. Value '1' at PIO will turn off the lighting
proc led_off {} {
  global mj_nsec
  global pio_led_base
  regwr [ expr $pio_led_base ] 0xf
}

# procedure set FPGA LED to run in single light-on pattern, with an interval set by user
proc led_run {interval occurance} {
  global mj_nsec
  global pio_led_base
  #regrd 0x0
  for {set y 0} {$y<$occurance} {incr y} {
    set value 1    
    for {set x 0} {$x<4} {incr x} {
      set dvalue [expr ~$value]
      regwr $pio_led_base $dvalue
      puts " value of LED PIO register~: 0b [int2bits $value]"
      set value [expr $value <<1 ]
      after $interval
    }
  }
}

# procedure to read LED PIO value
proc led_read {} {
  global mj_nsec
  global pio_led_base
  regrd 0x[format %8.8x [ expr $pio_led_base ]]
}

# procedure that read dip switch value
proc dipsw_data_read {} {
  global mj_nsec
  global pio_dipsw_base
  regrd 0x[format %8.8x [ expr $pio_dipsw_base+0x0 ]]
}

# procedure that read dip switch value
proc dipsw_data_read_toggle {} {
  global mj_nsec
  global pio_dipsw_base
  set data [regrd 0x[format %8.8x [ expr $pio_dipsw_base+0x0 ]]]
  puts "DIP Switch data         : $data"
  set toggle [regrd 0x[format %8.8x [ expr $pio_dipsw_base+0xc ]]]
  puts "DIP Switch toggled value: $toggle\n"
}

# procedure that read dip switch value
proc dipsw_data_read_toggle_clear {} {
  global mj_nsec
  global pio_dipsw_base
  set rdata [ regrd 0x[format %8.8x [ expr $pio_dipsw_base+0x0 ]]]
  puts "DIP Switch data         : $rdata\n"
  set rtoggle [ regrd 0x[format %8.8x [ expr $pio_dipsw_base+0xc ]]]
  puts "DIP Switch toggled value: $rtoggle\n"
  regwr [ expr $pio_dipsw_base+0xc ] $rtoggle
}

# procedure that enable interrupt of dip switch peripheral
# "bit_enable" argument represent which bit of dip switch to be enable for IRQ generation
proc dipsw_intr_enable {bit_enable} {
  global mj_nsec
  global pio_dipsw_base
  set inhex [ expr 0x1<<$bit_enable ]
#  set inhex [format %x $bit_enable]
  set rintr [ regrd [ expr $pio_dipsw_base+8 ] ]
  puts "read existing interrupt reg: $rintr"
  set wintr [ expr $rintr | $inhex ]
  puts "interrupt value to be written: 0x[format %8.8x $wintr]"
  regwr [ expr $pio_dipsw_base+8 ] $wintr
}

# procedure that disable interrupt of dip switch peripheral
proc dipsw_intr_disable {} {
  global mj_nsec
  global pio_dipsw_base
  regwr [ expr $pio_dipsw_base+8 ] 0x0
}


# procedure to write random data to random address of onchip memory and read back for comparison
# proc mem_rand_test {{count 1}} {
  # global mj_nsec
  # global onchip_mem_base
  # set mm_count 0
  # set mt_count 0
  # for {set oc 0} {$oc<$count} {incr oc} {
    # set dataarray($oc) [ expr {int(rand()*4294967296)} ]
    # set addrarray($oc) [ expr [ expr {int(rand()*16380)} ] *4 ]
    # regwr [ expr $onchip_mem_base+$addrarray($oc) ] $dataarray($oc)
  # }
  # puts "Done writting [expr $oc] set of data into Onchip Memory\n"
  # puts "Start reading back for comparison"
  # for {set oc 0} {$oc<$count} {incr oc} {
    # set data [ regrd 0x[ format %x [ expr $onchip_mem_base+$addrarray($oc) ] ] ]
    # if { $data != "0x[format %8.8x $dataarray($oc)]" } {
      # puts "Data mismatch!\nwritten:0x[format %8.8x $dataarray($oc)], read:$data, at address:0x[format %8.8x $addrarray($oc)]\n"
      # set mm_count [ expr $mm_count+1 ]
    # } else {
      # puts "Data($oc) matched: $data\n"
      # set mt_count [ expr $mt_count+1 ]
    # }
  # }
  # puts "Total word transferred: $count"
  # puts "Data missmatched: $mm_count"
  # puts "Data matched    : $mt_count" 
# }

# procedure to write random data to incremental address of onchip memory and read back for comparison
# proc mem_incr_test {startaddr {count 1}} {
  # global mj_nsec
  # global onchip_mem_base
  # set mm_count 0
  # set mt_count 0
  # for {set oc 0} {$oc<$count} {incr oc} {
    # set dataarray($oc) [ expr {int(rand()*4294967296)} ]
    # set addrarray($oc) [ expr $startaddr + [ expr $oc*4 ]]
    # regwr [ expr $onchip_mem_base+$addrarray($oc) ] $dataarray($oc)
  # }
  # puts "Done writting [expr $oc] set of data into Onchip Memory\n"
  # puts "Start reading back for comparison"
  # for {set oc 0} {$oc<$count} {incr oc} {
    # set data [ regrd 0x[ format %x [ expr $onchip_mem_base+$addrarray($oc) ] ] ]
    # if { $data != "0x[format %8.8x $dataarray($oc)]" } {
      # puts "Data mismatch!\nwritten:0x[format %8.8x $dataarray($oc)], read:$data, at address:0x[format %8.8x $addrarray($oc)]\n"
      # set mm_count [ expr $mm_count+1 ]
    # } else {
      # puts "Data($oc) matched: $data\n"
      # set mt_count [ expr $mt_count+1 ]
    # }
  # }
  # puts "Total word transferred: $count"
  # puts "Data missmatched: $mm_count"
  # puts "Data matched    : $mt_count" 
# }


# procedure to write random data to random address of hps sdram and read back for comparison
proc mem_rand_test {{count 1}} {
  global mj_sdram
  global f2sdram_base
  set mm_count 0
  set mt_count 0
  for {set oc 0} {$oc<$count} {incr oc} {
    set dataarray($oc) [ expr {int(rand()*4294967296)} ]
    set addrarray($oc) [ expr [ expr {int(rand()*16380)} ] *4 ]
    regwr [ expr $f2sdram_base+$addrarray($oc) ] $dataarray($oc)
  }
  puts "Done writting [expr $oc] set of data into Onchip Memory\n"
  puts "Start reading back for comparison"
  for {set oc 0} {$oc<$count} {incr oc} {
    set data [ regrd 0x[ format %x [ expr $f2sdram_base+$addrarray($oc) ] ] ]
    if { $data != "0x[format %8.8x $dataarray($oc)]" } {
      puts "Data mismatch!\nwritten:0x[format %8.8x $dataarray($oc)], read:$data, at address:0x[format %8.8x $addrarray($oc)]\n"
      set mm_count [ expr $mm_count+1 ]
    } else {
      puts "Data($oc) matched: $data\n"
      set mt_count [ expr $mt_count+1 ]
    }
  }
  puts "Total word transferred: $count"
  puts "Data missmatched: $mm_count"
  puts "Data matched    : $mt_count" 
}

# procedure to write random data to incremental address of hps sdram and read back for comparison
proc mem_incr_test {startaddr {count 1}} {
  global mj_sdram
  global f2sdram_base
  set mm_count 0
  set mt_count 0
  for {set oc 0} {$oc<$count} {incr oc} {
    set dataarray($oc) [ expr {int(rand()*4294967296)} ]
    set addrarray($oc) [ expr $startaddr + [ expr $oc*4 ]]
    regwr [ expr $f2sdram_base+$addrarray($oc) ] $dataarray($oc)
  }
  puts "Done writting [expr $oc] set of data into Onchip Memory\n"
  puts "Start reading back for comparison"
  for {set oc 0} {$oc<$count} {incr oc} {
    set data [ regrd 0x[ format %x [ expr $f2sdram_base+$addrarray($oc) ] ] ]
    if { $data != "0x[format %8.8x $dataarray($oc)]" } {
      puts "Data mismatch!\nwritten:0x[format %8.8x $dataarray($oc)], read:$data, at address:0x[format %8.8x $addrarray($oc)]\n"
      set mm_count [ expr $mm_count+1 ]
    } else {
      puts "Data($oc) matched: $data\n"
      set mt_count [ expr $mt_count+1 ]
    }
  }
  puts "Total word transferred: $count"
  puts "Data missmatched: $mm_count"
  puts "Data matched    : $mt_count" 
}

######################### information #################################
# add your new procedure here to access newly added component in system
