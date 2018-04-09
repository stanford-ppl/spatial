;buildInfoPackage: chisel3, version: 3.0-SNAPSHOT_2017-10-06, scalaVersion: 2.11.11, sbtVersion: 0.13.16, builtAtString: 2017-10-06 19:00:02.237, builtAtMillis: 1507316402237
circuit SRAM : 
  module Mem1D : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip r : {banks : {0 : UInt<1>}, ofs : UInt<8>, en : UInt<1>}, flip rMask : UInt<1>, flip w : {banks : {0 : UInt<1>}, ofs : UInt<8>, data : UInt<32>, en : UInt<1>}, flip wMask : UInt<1>, flip flow : UInt<1>, output : {data : UInt<32>}, debug : {invalidRAddr : UInt<1>, invalidWAddr : UInt<1>, rwOn : UInt<1>, error : UInt<1>}}
    
    clock is invalid
    reset is invalid
    io is invalid
    node wInBound = lt(io.w.ofs, UInt<9>("h0100")) @[SRAM.scala 106:27]
    node rInBound = lt(io.r.ofs, UInt<9>("h0100")) @[SRAM.scala 107:27]
    cmem _T_20 : UInt<32>[256] @[SRAM.scala 136:18]
    node _T_21 = and(io.w.en, io.wMask) @[SRAM.scala 137:21]
    node _T_22 = and(_T_21, wInBound) @[SRAM.scala 137:32]
    when _T_22 : @[SRAM.scala 137:44]
      infer mport _T_23 = _T_20[io.w.ofs], clock
      _T_23 <= io.w.data @[SRAM.scala 137:57]
      skip @[SRAM.scala 137:44]
    infer mport _T_24 = _T_20[io.r.ofs], clock
    io.output.data <= _T_24 @[SRAM.scala 138:22]
    node _T_25 = not(rInBound) @[SRAM.scala 143:30]
    io.debug.invalidRAddr <= _T_25 @[SRAM.scala 143:27]
    node _T_26 = not(wInBound) @[SRAM.scala 144:30]
    io.debug.invalidWAddr <= _T_26 @[SRAM.scala 144:27]
    node _T_27 = and(io.w.en, io.r.en) @[SRAM.scala 145:30]
    node _T_28 = and(_T_27, io.wMask) @[SRAM.scala 145:40]
    node _T_29 = and(_T_28, io.rMask) @[SRAM.scala 145:51]
    io.debug.rwOn <= _T_29 @[SRAM.scala 145:19]
    node _T_30 = not(rInBound) @[SRAM.scala 146:23]
    node _T_31 = not(wInBound) @[SRAM.scala 146:35]
    node _T_32 = or(_T_30, _T_31) @[SRAM.scala 146:33]
    node _T_33 = and(io.w.en, io.r.en) @[SRAM.scala 146:56]
    node _T_34 = and(_T_33, io.wMask) @[SRAM.scala 146:66]
    node _T_35 = and(_T_34, io.rMask) @[SRAM.scala 146:77]
    node _T_36 = or(_T_32, _T_35) @[SRAM.scala 146:45]
    io.debug.error <= _T_36 @[SRAM.scala 146:20]
    
  module Mem1D_1 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip r : {banks : {0 : UInt<1>}, ofs : UInt<8>, en : UInt<1>}, flip rMask : UInt<1>, flip w : {banks : {0 : UInt<1>}, ofs : UInt<8>, data : UInt<32>, en : UInt<1>}, flip wMask : UInt<1>, flip flow : UInt<1>, output : {data : UInt<32>}, debug : {invalidRAddr : UInt<1>, invalidWAddr : UInt<1>, rwOn : UInt<1>, error : UInt<1>}}
    
    clock is invalid
    reset is invalid
    io is invalid
    node wInBound = lt(io.w.ofs, UInt<9>("h0100")) @[SRAM.scala 106:27]
    node rInBound = lt(io.r.ofs, UInt<9>("h0100")) @[SRAM.scala 107:27]
    cmem _T_20 : UInt<32>[256] @[SRAM.scala 136:18]
    node _T_21 = and(io.w.en, io.wMask) @[SRAM.scala 137:21]
    node _T_22 = and(_T_21, wInBound) @[SRAM.scala 137:32]
    when _T_22 : @[SRAM.scala 137:44]
      infer mport _T_23 = _T_20[io.w.ofs], clock
      _T_23 <= io.w.data @[SRAM.scala 137:57]
      skip @[SRAM.scala 137:44]
    infer mport _T_24 = _T_20[io.r.ofs], clock
    io.output.data <= _T_24 @[SRAM.scala 138:22]
    node _T_25 = not(rInBound) @[SRAM.scala 143:30]
    io.debug.invalidRAddr <= _T_25 @[SRAM.scala 143:27]
    node _T_26 = not(wInBound) @[SRAM.scala 144:30]
    io.debug.invalidWAddr <= _T_26 @[SRAM.scala 144:27]
    node _T_27 = and(io.w.en, io.r.en) @[SRAM.scala 145:30]
    node _T_28 = and(_T_27, io.wMask) @[SRAM.scala 145:40]
    node _T_29 = and(_T_28, io.rMask) @[SRAM.scala 145:51]
    io.debug.rwOn <= _T_29 @[SRAM.scala 145:19]
    node _T_30 = not(rInBound) @[SRAM.scala 146:23]
    node _T_31 = not(wInBound) @[SRAM.scala 146:35]
    node _T_32 = or(_T_30, _T_31) @[SRAM.scala 146:33]
    node _T_33 = and(io.w.en, io.r.en) @[SRAM.scala 146:56]
    node _T_34 = and(_T_33, io.wMask) @[SRAM.scala 146:66]
    node _T_35 = and(_T_34, io.rMask) @[SRAM.scala 146:77]
    node _T_36 = or(_T_32, _T_35) @[SRAM.scala 146:45]
    io.debug.error <= _T_36 @[SRAM.scala 146:20]
    
  module Mem1D_2 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip r : {banks : {0 : UInt<1>}, ofs : UInt<8>, en : UInt<1>}, flip rMask : UInt<1>, flip w : {banks : {0 : UInt<1>}, ofs : UInt<8>, data : UInt<32>, en : UInt<1>}, flip wMask : UInt<1>, flip flow : UInt<1>, output : {data : UInt<32>}, debug : {invalidRAddr : UInt<1>, invalidWAddr : UInt<1>, rwOn : UInt<1>, error : UInt<1>}}
    
    clock is invalid
    reset is invalid
    io is invalid
    node wInBound = lt(io.w.ofs, UInt<9>("h0100")) @[SRAM.scala 106:27]
    node rInBound = lt(io.r.ofs, UInt<9>("h0100")) @[SRAM.scala 107:27]
    cmem _T_20 : UInt<32>[256] @[SRAM.scala 136:18]
    node _T_21 = and(io.w.en, io.wMask) @[SRAM.scala 137:21]
    node _T_22 = and(_T_21, wInBound) @[SRAM.scala 137:32]
    when _T_22 : @[SRAM.scala 137:44]
      infer mport _T_23 = _T_20[io.w.ofs], clock
      _T_23 <= io.w.data @[SRAM.scala 137:57]
      skip @[SRAM.scala 137:44]
    infer mport _T_24 = _T_20[io.r.ofs], clock
    io.output.data <= _T_24 @[SRAM.scala 138:22]
    node _T_25 = not(rInBound) @[SRAM.scala 143:30]
    io.debug.invalidRAddr <= _T_25 @[SRAM.scala 143:27]
    node _T_26 = not(wInBound) @[SRAM.scala 144:30]
    io.debug.invalidWAddr <= _T_26 @[SRAM.scala 144:27]
    node _T_27 = and(io.w.en, io.r.en) @[SRAM.scala 145:30]
    node _T_28 = and(_T_27, io.wMask) @[SRAM.scala 145:40]
    node _T_29 = and(_T_28, io.rMask) @[SRAM.scala 145:51]
    io.debug.rwOn <= _T_29 @[SRAM.scala 145:19]
    node _T_30 = not(rInBound) @[SRAM.scala 146:23]
    node _T_31 = not(wInBound) @[SRAM.scala 146:35]
    node _T_32 = or(_T_30, _T_31) @[SRAM.scala 146:33]
    node _T_33 = and(io.w.en, io.r.en) @[SRAM.scala 146:56]
    node _T_34 = and(_T_33, io.wMask) @[SRAM.scala 146:66]
    node _T_35 = and(_T_34, io.rMask) @[SRAM.scala 146:77]
    node _T_36 = or(_T_32, _T_35) @[SRAM.scala 146:45]
    io.debug.error <= _T_36 @[SRAM.scala 146:20]
    
  module Mem1D_3 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip r : {banks : {0 : UInt<1>}, ofs : UInt<8>, en : UInt<1>}, flip rMask : UInt<1>, flip w : {banks : {0 : UInt<1>}, ofs : UInt<8>, data : UInt<32>, en : UInt<1>}, flip wMask : UInt<1>, flip flow : UInt<1>, output : {data : UInt<32>}, debug : {invalidRAddr : UInt<1>, invalidWAddr : UInt<1>, rwOn : UInt<1>, error : UInt<1>}}
    
    clock is invalid
    reset is invalid
    io is invalid
    node wInBound = lt(io.w.ofs, UInt<9>("h0100")) @[SRAM.scala 106:27]
    node rInBound = lt(io.r.ofs, UInt<9>("h0100")) @[SRAM.scala 107:27]
    cmem _T_20 : UInt<32>[256] @[SRAM.scala 136:18]
    node _T_21 = and(io.w.en, io.wMask) @[SRAM.scala 137:21]
    node _T_22 = and(_T_21, wInBound) @[SRAM.scala 137:32]
    when _T_22 : @[SRAM.scala 137:44]
      infer mport _T_23 = _T_20[io.w.ofs], clock
      _T_23 <= io.w.data @[SRAM.scala 137:57]
      skip @[SRAM.scala 137:44]
    infer mport _T_24 = _T_20[io.r.ofs], clock
    io.output.data <= _T_24 @[SRAM.scala 138:22]
    node _T_25 = not(rInBound) @[SRAM.scala 143:30]
    io.debug.invalidRAddr <= _T_25 @[SRAM.scala 143:27]
    node _T_26 = not(wInBound) @[SRAM.scala 144:30]
    io.debug.invalidWAddr <= _T_26 @[SRAM.scala 144:27]
    node _T_27 = and(io.w.en, io.r.en) @[SRAM.scala 145:30]
    node _T_28 = and(_T_27, io.wMask) @[SRAM.scala 145:40]
    node _T_29 = and(_T_28, io.rMask) @[SRAM.scala 145:51]
    io.debug.rwOn <= _T_29 @[SRAM.scala 145:19]
    node _T_30 = not(rInBound) @[SRAM.scala 146:23]
    node _T_31 = not(wInBound) @[SRAM.scala 146:35]
    node _T_32 = or(_T_30, _T_31) @[SRAM.scala 146:33]
    node _T_33 = and(io.w.en, io.r.en) @[SRAM.scala 146:56]
    node _T_34 = and(_T_33, io.wMask) @[SRAM.scala 146:66]
    node _T_35 = and(_T_34, io.rMask) @[SRAM.scala 146:77]
    node _T_36 = or(_T_32, _T_35) @[SRAM.scala 146:45]
    io.debug.error <= _T_36 @[SRAM.scala 146:20]
    
  module SRAM : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip xBarW : {banks : {1 : UInt<1>, 0 : UInt<1>}, ofs : UInt<8>, data : UInt<32>, en : UInt<1>}[1], flip xBarR : {banks : {1 : UInt<1>, 0 : UInt<1>}, ofs : UInt<8>, en : UInt<1>}[1], directW : {flip 3 : {ofs : UInt<8>, data : UInt<32>, en : UInt<1>}, flip 2 : {ofs : UInt<8>, data : UInt<32>, en : UInt<1>}, flip 1 : {ofs : UInt<8>, data : UInt<32>, en : UInt<1>}, flip 0 : {ofs : UInt<8>, data : UInt<32>, en : UInt<1>}}, directR : {flip 3 : {ofs : UInt<8>, en : UInt<1>}, flip 2 : {ofs : UInt<8>, en : UInt<1>}, flip 1 : {ofs : UInt<8>, en : UInt<1>}, flip 0 : {ofs : UInt<8>, en : UInt<1>}}, flip flow : UInt<1>[4], output : {data : UInt<32>[4]}}
    
    clock is invalid
    reset is invalid
    io is invalid
    inst Mem1D of Mem1D @[SRAM.scala 286:21]
    Mem1D.io is invalid
    Mem1D.clock <= clock
    Mem1D.reset <= reset
    inst Mem1D_1 of Mem1D_1 @[SRAM.scala 286:21]
    Mem1D_1.io is invalid
    Mem1D_1.clock <= clock
    Mem1D_1.reset <= reset
    inst Mem1D_2 of Mem1D_2 @[SRAM.scala 286:21]
    Mem1D_2.io is invalid
    Mem1D_2.clock <= clock
    Mem1D_2.reset <= reset
    inst Mem1D_3 of Mem1D_3 @[SRAM.scala 286:21]
    Mem1D_3.io is invalid
    Mem1D_3.clock <= clock
    Mem1D_3.reset <= reset
    node _T_47 = eq(io.xBarW[0].banks.0, UInt<1>("h00")) @[SRAM.scala 297:48]
    node _T_49 = eq(io.xBarW[0].banks.1, UInt<1>("h00")) @[SRAM.scala 297:48]
    node _T_50 = and(_T_47, _T_49) @[SRAM.scala 297:69]
    node _T_52 = and(_T_50, UInt<1>("h00")) @[SRAM.scala 297:74]
    node _T_53 = or(_T_52, io.directW.0.en) @[SRAM.scala 303:47]
    Mem1D.io.wMask <= _T_53 @[SRAM.scala 303:21]
    node _T_55 = mux(io.directW.0.en, io.directW.0.ofs, io.xBarW[0].ofs) @[SRAM.scala 305:27]
    Mem1D.io.w.ofs <= _T_55 @[SRAM.scala 305:21]
    node _T_57 = mux(io.directW.0.en, io.directW.0.data, io.xBarW[0].data) @[SRAM.scala 306:28]
    Mem1D.io.w.data <= _T_57 @[SRAM.scala 306:22]
    node _T_59 = mux(io.directW.0.en, io.directW.0.en, io.xBarW[0].en) @[SRAM.scala 307:26]
    Mem1D.io.w.en <= _T_59 @[SRAM.scala 307:20]
    node _T_61 = eq(io.xBarW[0].banks.0, UInt<1>("h00")) @[SRAM.scala 297:48]
    node _T_63 = eq(io.xBarW[0].banks.1, UInt<1>("h01")) @[SRAM.scala 297:48]
    node _T_64 = and(_T_61, _T_63) @[SRAM.scala 297:69]
    node _T_66 = and(_T_64, UInt<1>("h00")) @[SRAM.scala 297:74]
    node _T_67 = or(_T_66, io.directW.1.en) @[SRAM.scala 303:47]
    Mem1D_1.io.wMask <= _T_67 @[SRAM.scala 303:21]
    node _T_69 = mux(io.directW.1.en, io.directW.1.ofs, io.xBarW[0].ofs) @[SRAM.scala 305:27]
    Mem1D_1.io.w.ofs <= _T_69 @[SRAM.scala 305:21]
    node _T_71 = mux(io.directW.1.en, io.directW.1.data, io.xBarW[0].data) @[SRAM.scala 306:28]
    Mem1D_1.io.w.data <= _T_71 @[SRAM.scala 306:22]
    node _T_73 = mux(io.directW.1.en, io.directW.1.en, io.xBarW[0].en) @[SRAM.scala 307:26]
    Mem1D_1.io.w.en <= _T_73 @[SRAM.scala 307:20]
    node _T_75 = eq(io.xBarW[0].banks.0, UInt<1>("h01")) @[SRAM.scala 297:48]
    node _T_77 = eq(io.xBarW[0].banks.1, UInt<1>("h00")) @[SRAM.scala 297:48]
    node _T_78 = and(_T_75, _T_77) @[SRAM.scala 297:69]
    node _T_80 = and(_T_78, UInt<1>("h00")) @[SRAM.scala 297:74]
    node _T_81 = or(_T_80, io.directW.2.en) @[SRAM.scala 303:47]
    Mem1D_2.io.wMask <= _T_81 @[SRAM.scala 303:21]
    node _T_83 = mux(io.directW.2.en, io.directW.2.ofs, io.xBarW[0].ofs) @[SRAM.scala 305:27]
    Mem1D_2.io.w.ofs <= _T_83 @[SRAM.scala 305:21]
    node _T_85 = mux(io.directW.2.en, io.directW.2.data, io.xBarW[0].data) @[SRAM.scala 306:28]
    Mem1D_2.io.w.data <= _T_85 @[SRAM.scala 306:22]
    node _T_87 = mux(io.directW.2.en, io.directW.2.en, io.xBarW[0].en) @[SRAM.scala 307:26]
    Mem1D_2.io.w.en <= _T_87 @[SRAM.scala 307:20]
    node _T_89 = eq(io.xBarW[0].banks.0, UInt<1>("h01")) @[SRAM.scala 297:48]
    node _T_91 = eq(io.xBarW[0].banks.1, UInt<1>("h01")) @[SRAM.scala 297:48]
    node _T_92 = and(_T_89, _T_91) @[SRAM.scala 297:69]
    node _T_94 = and(_T_92, UInt<1>("h00")) @[SRAM.scala 297:74]
    node _T_95 = or(_T_94, io.directW.3.en) @[SRAM.scala 303:47]
    Mem1D_3.io.wMask <= _T_95 @[SRAM.scala 303:21]
    node _T_97 = mux(io.directW.3.en, io.directW.3.ofs, io.xBarW[0].ofs) @[SRAM.scala 305:27]
    Mem1D_3.io.w.ofs <= _T_97 @[SRAM.scala 305:21]
    node _T_99 = mux(io.directW.3.en, io.directW.3.data, io.xBarW[0].data) @[SRAM.scala 306:28]
    Mem1D_3.io.w.data <= _T_99 @[SRAM.scala 306:22]
    node _T_101 = mux(io.directW.3.en, io.directW.3.en, io.xBarW[0].en) @[SRAM.scala 307:26]
    Mem1D_3.io.w.en <= _T_101 @[SRAM.scala 307:20]
    node _T_103 = eq(io.xBarR[0].banks.0, UInt<1>("h00")) @[SRAM.scala 314:48]
    node _T_105 = eq(io.xBarR[0].banks.1, UInt<1>("h00")) @[SRAM.scala 314:48]
    node _T_106 = and(_T_103, _T_105) @[SRAM.scala 314:69]
    node _T_107 = and(_T_106, io.xBarR[0].en) @[SRAM.scala 314:74]
    node _T_108 = and(_T_107, io.directR.0.en) @[SRAM.scala 320:47]
    Mem1D.io.rMask <= _T_108 @[SRAM.scala 320:21]
    node _T_110 = mux(io.directR.0.en, io.directR.0.ofs, io.xBarR[0].ofs) @[SRAM.scala 322:27]
    Mem1D.io.r.ofs <= _T_110 @[SRAM.scala 322:21]
    node _T_112 = mux(io.directR.0.en, io.directR.0.en, io.xBarR[0].en) @[SRAM.scala 323:26]
    Mem1D.io.r.en <= _T_112 @[SRAM.scala 323:20]
    node _T_113 = and(io.flow[0], io.flow[1]) @[SRAM.scala 325:39]
    node _T_114 = and(_T_113, io.flow[2]) @[SRAM.scala 325:39]
    node _T_115 = and(_T_114, io.flow[3]) @[SRAM.scala 325:39]
    Mem1D.io.flow <= _T_115 @[SRAM.scala 325:20]
    node _T_117 = eq(io.xBarR[0].banks.0, UInt<1>("h00")) @[SRAM.scala 314:48]
    node _T_119 = eq(io.xBarR[0].banks.1, UInt<1>("h01")) @[SRAM.scala 314:48]
    node _T_120 = and(_T_117, _T_119) @[SRAM.scala 314:69]
    node _T_121 = and(_T_120, io.xBarR[0].en) @[SRAM.scala 314:74]
    node _T_122 = and(_T_121, io.directR.1.en) @[SRAM.scala 320:47]
    Mem1D_1.io.rMask <= _T_122 @[SRAM.scala 320:21]
    node _T_124 = mux(io.directR.1.en, io.directR.1.ofs, io.xBarR[0].ofs) @[SRAM.scala 322:27]
    Mem1D_1.io.r.ofs <= _T_124 @[SRAM.scala 322:21]
    node _T_126 = mux(io.directR.1.en, io.directR.1.en, io.xBarR[0].en) @[SRAM.scala 323:26]
    Mem1D_1.io.r.en <= _T_126 @[SRAM.scala 323:20]
    node _T_127 = and(io.flow[0], io.flow[1]) @[SRAM.scala 325:39]
    node _T_128 = and(_T_127, io.flow[2]) @[SRAM.scala 325:39]
    node _T_129 = and(_T_128, io.flow[3]) @[SRAM.scala 325:39]
    Mem1D_1.io.flow <= _T_129 @[SRAM.scala 325:20]
    node _T_131 = eq(io.xBarR[0].banks.0, UInt<1>("h01")) @[SRAM.scala 314:48]
    node _T_133 = eq(io.xBarR[0].banks.1, UInt<1>("h00")) @[SRAM.scala 314:48]
    node _T_134 = and(_T_131, _T_133) @[SRAM.scala 314:69]
    node _T_135 = and(_T_134, io.xBarR[0].en) @[SRAM.scala 314:74]
    node _T_136 = and(_T_135, io.directR.2.en) @[SRAM.scala 320:47]
    Mem1D_2.io.rMask <= _T_136 @[SRAM.scala 320:21]
    node _T_138 = mux(io.directR.2.en, io.directR.2.ofs, io.xBarR[0].ofs) @[SRAM.scala 322:27]
    Mem1D_2.io.r.ofs <= _T_138 @[SRAM.scala 322:21]
    node _T_140 = mux(io.directR.2.en, io.directR.2.en, io.xBarR[0].en) @[SRAM.scala 323:26]
    Mem1D_2.io.r.en <= _T_140 @[SRAM.scala 323:20]
    node _T_141 = and(io.flow[0], io.flow[1]) @[SRAM.scala 325:39]
    node _T_142 = and(_T_141, io.flow[2]) @[SRAM.scala 325:39]
    node _T_143 = and(_T_142, io.flow[3]) @[SRAM.scala 325:39]
    Mem1D_2.io.flow <= _T_143 @[SRAM.scala 325:20]
    node _T_145 = eq(io.xBarR[0].banks.0, UInt<1>("h01")) @[SRAM.scala 314:48]
    node _T_147 = eq(io.xBarR[0].banks.1, UInt<1>("h01")) @[SRAM.scala 314:48]
    node _T_148 = and(_T_145, _T_147) @[SRAM.scala 314:69]
    node _T_149 = and(_T_148, io.xBarR[0].en) @[SRAM.scala 314:74]
    node _T_150 = and(_T_149, io.directR.3.en) @[SRAM.scala 320:47]
    Mem1D_3.io.rMask <= _T_150 @[SRAM.scala 320:21]
    node _T_152 = mux(io.directR.3.en, io.directR.3.ofs, io.xBarR[0].ofs) @[SRAM.scala 322:27]
    Mem1D_3.io.r.ofs <= _T_152 @[SRAM.scala 322:21]
    node _T_154 = mux(io.directR.3.en, io.directR.3.en, io.xBarR[0].en) @[SRAM.scala 323:26]
    Mem1D_3.io.r.en <= _T_154 @[SRAM.scala 323:20]
    node _T_155 = and(io.flow[0], io.flow[1]) @[SRAM.scala 325:39]
    node _T_156 = and(_T_155, io.flow[2]) @[SRAM.scala 325:39]
    node _T_157 = and(_T_156, io.flow[3]) @[SRAM.scala 325:39]
    Mem1D_3.io.flow <= _T_157 @[SRAM.scala 325:20]
    node _T_160 = and(UInt<1>("h01"), io.directR.0.en) @[SRAM.scala 342:79]
    node _T_161 = or(UInt<1>("h00"), _T_160) @[SRAM.scala 344:17]
    node _T_164 = and(UInt<1>("h00"), io.directR.0.en) @[SRAM.scala 342:79]
    node _T_165 = or(UInt<1>("h00"), _T_164) @[SRAM.scala 344:17]
    node _T_168 = and(UInt<1>("h00"), io.directR.0.en) @[SRAM.scala 342:79]
    node _T_169 = or(UInt<1>("h00"), _T_168) @[SRAM.scala 344:17]
    node _T_172 = and(UInt<1>("h00"), io.directR.0.en) @[SRAM.scala 342:79]
    node _T_173 = or(UInt<1>("h00"), _T_172) @[SRAM.scala 344:17]
    node _T_174 = mux(_T_169, Mem1D_2.io.output.data, Mem1D_3.io.output.data) @[Mux.scala 31:69]
    node _T_175 = mux(_T_165, Mem1D_1.io.output.data, _T_174) @[Mux.scala 31:69]
    node _T_176 = mux(_T_161, Mem1D.io.output.data, _T_175) @[Mux.scala 31:69]
    io.output.data[0] <= _T_176 @[SRAM.scala 348:10]
    node _T_179 = and(UInt<1>("h00"), io.directR.1.en) @[SRAM.scala 342:79]
    node _T_180 = or(UInt<1>("h00"), _T_179) @[SRAM.scala 344:17]
    node _T_183 = and(UInt<1>("h01"), io.directR.1.en) @[SRAM.scala 342:79]
    node _T_184 = or(UInt<1>("h00"), _T_183) @[SRAM.scala 344:17]
    node _T_187 = and(UInt<1>("h00"), io.directR.1.en) @[SRAM.scala 342:79]
    node _T_188 = or(UInt<1>("h00"), _T_187) @[SRAM.scala 344:17]
    node _T_191 = and(UInt<1>("h00"), io.directR.1.en) @[SRAM.scala 342:79]
    node _T_192 = or(UInt<1>("h00"), _T_191) @[SRAM.scala 344:17]
    node _T_193 = mux(_T_188, Mem1D_2.io.output.data, Mem1D_3.io.output.data) @[Mux.scala 31:69]
    node _T_194 = mux(_T_184, Mem1D_1.io.output.data, _T_193) @[Mux.scala 31:69]
    node _T_195 = mux(_T_180, Mem1D.io.output.data, _T_194) @[Mux.scala 31:69]
    io.output.data[1] <= _T_195 @[SRAM.scala 348:10]
    node _T_198 = and(UInt<1>("h00"), io.directR.2.en) @[SRAM.scala 342:79]
    node _T_199 = or(UInt<1>("h00"), _T_198) @[SRAM.scala 344:17]
    node _T_202 = and(UInt<1>("h00"), io.directR.2.en) @[SRAM.scala 342:79]
    node _T_203 = or(UInt<1>("h00"), _T_202) @[SRAM.scala 344:17]
    node _T_206 = and(UInt<1>("h01"), io.directR.2.en) @[SRAM.scala 342:79]
    node _T_207 = or(UInt<1>("h00"), _T_206) @[SRAM.scala 344:17]
    node _T_210 = and(UInt<1>("h00"), io.directR.2.en) @[SRAM.scala 342:79]
    node _T_211 = or(UInt<1>("h00"), _T_210) @[SRAM.scala 344:17]
    node _T_212 = mux(_T_207, Mem1D_2.io.output.data, Mem1D_3.io.output.data) @[Mux.scala 31:69]
    node _T_213 = mux(_T_203, Mem1D_1.io.output.data, _T_212) @[Mux.scala 31:69]
    node _T_214 = mux(_T_199, Mem1D.io.output.data, _T_213) @[Mux.scala 31:69]
    io.output.data[2] <= _T_214 @[SRAM.scala 348:10]
    node _T_217 = and(UInt<1>("h00"), io.directR.3.en) @[SRAM.scala 342:79]
    node _T_218 = or(UInt<1>("h00"), _T_217) @[SRAM.scala 344:17]
    node _T_221 = and(UInt<1>("h00"), io.directR.3.en) @[SRAM.scala 342:79]
    node _T_222 = or(UInt<1>("h00"), _T_221) @[SRAM.scala 344:17]
    node _T_225 = and(UInt<1>("h00"), io.directR.3.en) @[SRAM.scala 342:79]
    node _T_226 = or(UInt<1>("h00"), _T_225) @[SRAM.scala 344:17]
    node _T_229 = and(UInt<1>("h01"), io.directR.3.en) @[SRAM.scala 342:79]
    node _T_230 = or(UInt<1>("h00"), _T_229) @[SRAM.scala 344:17]
    node _T_231 = mux(_T_226, Mem1D_2.io.output.data, Mem1D_3.io.output.data) @[Mux.scala 31:69]
    node _T_232 = mux(_T_222, Mem1D_1.io.output.data, _T_231) @[Mux.scala 31:69]
    node _T_233 = mux(_T_218, Mem1D.io.output.data, _T_232) @[Mux.scala 31:69]
    io.output.data[3] <= _T_233 @[SRAM.scala 348:10]
    
