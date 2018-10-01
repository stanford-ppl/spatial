;buildInfoPackage: chisel3, version: 3.0-SNAPSHOT, scalaVersion: 2.11.7, sbtVersion: 0.13.12, builtAtString: 2017-03-24 17:25:51.022, builtAtMillis: 1490376351022
circuit Top : 
  module FF : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip input : {data : UInt<32>, init : UInt<32>, enable : UInt<1>, reset : UInt<1>}, output : {data : UInt<32>}}
    
    io is invalid
    io is invalid
    reg ff : UInt, clock with : (reset => (reset, io.input.init)) @[FF.scala 32:19]
    node _T_17 = mux(io.input.enable, io.input.data, ff) @[FF.scala 33:47]
    node _T_18 = mux(io.input.reset, io.input.init, _T_17) @[FF.scala 33:12]
    ff <= _T_18 @[FF.scala 33:6]
    node _T_19 = mux(io.input.reset, io.input.init, ff) @[FF.scala 34:24]
    io.output.data <= _T_19 @[FF.scala 34:18]
    
  module FF_1 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip input : {data : UInt<32>, init : UInt<32>, enable : UInt<1>, reset : UInt<1>}, output : {data : UInt<32>}}
    
    io is invalid
    io is invalid
    reg ff : UInt, clock with : (reset => (reset, io.input.init)) @[FF.scala 32:19]
    node _T_17 = mux(io.input.enable, io.input.data, ff) @[FF.scala 33:47]
    node _T_18 = mux(io.input.reset, io.input.init, _T_17) @[FF.scala 33:12]
    ff <= _T_18 @[FF.scala 33:6]
    node _T_19 = mux(io.input.reset, io.input.init, ff) @[FF.scala 34:24]
    io.output.data <= _T_19 @[FF.scala 34:18]
    
  module SRFF : 
    input clock : Clock
    input reset : UInt<1>
    output io : {input : {flip set : UInt<1>, flip reset : UInt<1>, flip asyn_reset : UInt<1>}, output : {data : UInt<1>}}
    
    io is invalid
    io is invalid
    reg _T_14 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[FF.scala 276:21]
    node _T_18 = mux(io.input.reset, UInt<1>("h00"), _T_14) @[FF.scala 278:48]
    node _T_19 = mux(io.input.set, UInt<1>("h01"), _T_18) @[FF.scala 277:48]
    node _T_20 = mux(io.input.asyn_reset, UInt<1>("h00"), _T_19) @[FF.scala 277:14]
    _T_14 <= _T_20 @[FF.scala 277:8]
    node _T_22 = mux(io.input.asyn_reset, UInt<1>("h00"), _T_14) @[FF.scala 279:26]
    io.output.data <= _T_22 @[FF.scala 279:20]
    
  module FF_2 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip input : {data : UInt<32>, init : UInt<32>, enable : UInt<1>, reset : UInt<1>}, output : {data : UInt<32>}}
    
    io is invalid
    io is invalid
    reg ff : UInt, clock with : (reset => (reset, io.input.init)) @[FF.scala 32:19]
    node _T_17 = mux(io.input.enable, io.input.data, ff) @[FF.scala 33:47]
    node _T_18 = mux(io.input.reset, io.input.init, _T_17) @[FF.scala 33:12]
    ff <= _T_18 @[FF.scala 33:6]
    node _T_19 = mux(io.input.reset, io.input.init, ff) @[FF.scala 34:24]
    io.output.data <= _T_19 @[FF.scala 34:18]
    
  module SingleCounter : 
    input clock : Clock
    input reset : UInt<1>
    output io : {input : {flip start : UInt<32>, flip max : UInt<32>, flip stride : UInt<32>, flip gap : UInt<32>, flip reset : UInt<1>, flip enable : UInt<1>, flip saturate : UInt<1>}, output : {count : UInt<32>[1], countWithoutWrap : UInt<32>[1], done : UInt<1>, extendedDone : UInt<1>, saturated : UInt<1>}}
    
    io is invalid
    io is invalid
    inst FF of FF_2 @[Counter.scala 124:22]
    FF.io is invalid
    FF.clock <= clock
    FF.reset <= reset
    FF.io.input.init <= io.input.start @[Counter.scala 126:24]
    node _T_32 = or(io.input.reset, io.input.enable) @[Counter.scala 127:44]
    FF.io.input.enable <= _T_32 @[Counter.scala 127:26]
    node _T_34 = mul(io.input.stride, UInt<1>("h01")) @[Counter.scala 130:43]
    node _T_35 = add(FF.io.output.data, _T_34) @[Counter.scala 130:24]
    node _T_36 = tail(_T_35, 1) @[Counter.scala 130:24]
    node _T_37 = add(_T_36, io.input.gap) @[Counter.scala 130:52]
    node _T_38 = tail(_T_37, 1) @[Counter.scala 130:52]
    node _T_39 = geq(_T_38, io.input.max) @[Counter.scala 131:24]
    reg _T_42 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Counter.scala 132:25]
    _T_42 <= _T_39 @[Counter.scala 132:25]
    reg _T_45 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Counter.scala 133:29]
    _T_45 <= io.input.enable @[Counter.scala 133:29]
    node _T_46 = mux(io.input.saturate, FF.io.output.data, io.input.start) @[Counter.scala 134:30]
    node _T_47 = mux(_T_39, _T_46, _T_38) @[Counter.scala 134:19]
    node _T_48 = mux(io.input.reset, io.input.start, _T_47) @[Counter.scala 135:30]
    FF.io.input.data <= _T_48 @[Counter.scala 135:24]
    node _T_50 = mul(UInt<1>("h00"), io.input.stride) @[Counter.scala 137:67]
    node _T_51 = add(FF.io.output.data, _T_50) @[Counter.scala 137:62]
    node _T_52 = tail(_T_51, 1) @[Counter.scala 137:62]
    io.output.count[0] <= _T_52 @[Counter.scala 137:53]
    node _T_54 = eq(FF.io.output.data, UInt<1>("h00")) @[Counter.scala 139:50]
    node _T_55 = mux(_T_54, io.input.max, FF.io.output.data) @[Counter.scala 139:43]
    node _T_57 = mul(UInt<1>("h00"), io.input.stride) @[Counter.scala 139:85]
    node _T_58 = add(_T_55, _T_57) @[Counter.scala 139:80]
    node _T_59 = tail(_T_58, 1) @[Counter.scala 139:80]
    io.output.countWithoutWrap[0] <= _T_59 @[Counter.scala 139:37]
    node _T_60 = and(io.input.enable, _T_39) @[Counter.scala 141:39]
    io.output.done <= _T_60 @[Counter.scala 141:20]
    node _T_61 = and(io.input.saturate, _T_39) @[Counter.scala 142:46]
    io.output.saturated <= _T_61 @[Counter.scala 142:25]
    node _T_62 = or(io.input.enable, _T_45) @[Counter.scala 143:48]
    node _T_63 = or(_T_39, _T_42) @[Counter.scala 143:71]
    node _T_64 = and(_T_62, _T_63) @[Counter.scala 143:62]
    io.output.extendedDone <= _T_64 @[Counter.scala 143:28]
    
  module FF_3 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip input : {data : UInt<1>, init : UInt<1>, enable : UInt<1>, reset : UInt<1>}, output : {data : UInt<1>}}
    
    io is invalid
    io is invalid
    reg ff : UInt, clock with : (reset => (reset, io.input.init)) @[FF.scala 32:19]
    node _T_17 = mux(io.input.enable, io.input.data, ff) @[FF.scala 33:47]
    node _T_18 = mux(io.input.reset, io.input.init, _T_17) @[FF.scala 33:12]
    ff <= _T_18 @[FF.scala 33:6]
    node _T_19 = mux(io.input.reset, io.input.init, ff) @[FF.scala 34:24]
    io.output.data <= _T_19 @[FF.scala 34:18]
    
  module Metapipe : 
    input clock : Clock
    input reset : UInt<1>
    output io : {input : {flip enable : UInt<1>, flip numIter : UInt<32>, flip stageDone : UInt<1>[1], flip rst : UInt<1>, flip forever : UInt<1>, flip nextState : UInt<32>}, output : {done : UInt<1>, stageEnable : UInt<1>[1], rst_en : UInt<1>, ctr_inc : UInt<1>, state : UInt<32>}}
    
    io is invalid
    io is invalid
    inst stateFF of FF @[Metapipe.scala 42:23]
    stateFF.io is invalid
    stateFF.clock <= clock
    stateFF.reset <= reset
    stateFF.io.input.enable <= UInt<1>("h01") @[Metapipe.scala 43:27]
    stateFF.io.input.init <= UInt<1>("h00") @[Metapipe.scala 44:25]
    stateFF.io.input.reset <= io.input.rst @[Metapipe.scala 45:26]
    inst maxFF of FF_1 @[Metapipe.scala 49:21]
    maxFF.io is invalid
    maxFF.clock <= clock
    maxFF.reset <= reset
    maxFF.io.input.enable <= io.input.enable @[Metapipe.scala 50:25]
    maxFF.io.input.data <= io.input.numIter @[Metapipe.scala 51:23]
    reg doneClear : UInt, clock with : (reset => (reset, UInt<1>("h00"))) @[Metapipe.scala 54:26]
    inst doneFF_0 of SRFF @[Metapipe.scala 56:20]
    doneFF_0.io is invalid
    doneFF_0.clock <= clock
    doneFF_0.reset <= reset
    doneFF_0.io.input.set <= io.input.stageDone[0] @[Metapipe.scala 57:21]
    doneFF_0.io.input.asyn_reset <= doneClear @[Metapipe.scala 58:28]
    inst ctr of SingleCounter @[Metapipe.scala 63:19]
    ctr.io is invalid
    ctr.clock <= clock
    ctr.reset <= reset
    ctr.io.input.enable <= doneClear @[Metapipe.scala 64:23]
    node _T_35 = eq(stateFF.io.output.data, UInt<2>("h03")) @[Metapipe.scala 65:32]
    ctr.io.input.reset <= _T_35 @[Metapipe.scala 65:22]
    ctr.io.input.saturate <= UInt<1>("h01") @[Metapipe.scala 66:25]
    ctr.io.input.max <= maxFF.io.output.data @[Metapipe.scala 67:20]
    ctr.io.input.stride <= UInt<1>("h01") @[Metapipe.scala 68:23]
    node _T_39 = eq(stateFF.io.output.data, UInt<1>("h01")) @[Metapipe.scala 69:30]
    io.output.rst_en <= _T_39 @[Metapipe.scala 69:20]
    inst cycsSinceDone of FF_3 @[Metapipe.scala 73:29]
    cycsSinceDone.io is invalid
    cycsSinceDone.clock <= clock
    cycsSinceDone.reset <= reset
    cycsSinceDone.io.input.init <= UInt<1>("h00") @[Metapipe.scala 74:31]
    node _T_42 = eq(stateFF.io.output.data, UInt<2>("h03")) @[Metapipe.scala 75:42]
    cycsSinceDone.io.input.reset <= _T_42 @[Metapipe.scala 75:32]
    when io.input.enable : @[Metapipe.scala 81:25]
      node _T_44 = eq(stateFF.io.output.data, UInt<1>("h00")) @[Metapipe.scala 82:16]
      when _T_44 : @[Metapipe.scala 82:33]
        stateFF.io.input.data <= UInt<1>("h01") @[Metapipe.scala 83:29]
        io.output.stageEnable[0] <= UInt<1>("h00") @[Metapipe.scala 84:46]
        skip @[Metapipe.scala 82:33]
      node _T_48 = eq(stateFF.io.output.data, UInt<1>("h01")) @[Metapipe.scala 85:23]
      node _T_50 = eq(_T_44, UInt<1>("h00")) @[Metapipe.scala 82:33]
      node _T_51 = and(_T_50, _T_48) @[Metapipe.scala 85:41]
      when _T_51 : @[Metapipe.scala 85:41]
        node _T_53 = eq(io.input.numIter, UInt<1>("h00")) @[Metapipe.scala 86:53]
        node _T_56 = mux(io.input.forever, UInt<2>("h02"), UInt<2>("h03")) @[Metapipe.scala 86:65]
        node _T_58 = mux(_T_53, _T_56, UInt<2>("h02")) @[Metapipe.scala 86:35]
        stateFF.io.input.data <= _T_58 @[Metapipe.scala 86:29]
        io.output.stageEnable[0] <= UInt<1>("h00") @[Metapipe.scala 87:46]
        skip @[Metapipe.scala 85:41]
      node _T_61 = lt(stateFF.io.output.data, UInt<2>("h02")) @[Metapipe.scala 88:23]
      node _T_63 = eq(_T_44, UInt<1>("h00")) @[Metapipe.scala 82:33]
      node _T_65 = eq(_T_48, UInt<1>("h00")) @[Metapipe.scala 85:41]
      node _T_66 = and(_T_63, _T_65) @[Metapipe.scala 85:41]
      node _T_67 = and(_T_66, _T_61) @[Metapipe.scala 88:40]
      when _T_67 : @[Metapipe.scala 88:40]
        skip @[Metapipe.scala 88:40]
      node _T_69 = eq(stateFF.io.output.data, UInt<2>("h02")) @[Metapipe.scala 119:23]
      node _T_71 = eq(_T_44, UInt<1>("h00")) @[Metapipe.scala 82:33]
      node _T_73 = eq(_T_48, UInt<1>("h00")) @[Metapipe.scala 85:41]
      node _T_74 = and(_T_71, _T_73) @[Metapipe.scala 85:41]
      node _T_76 = eq(_T_61, UInt<1>("h00")) @[Metapipe.scala 88:40]
      node _T_77 = and(_T_74, _T_76) @[Metapipe.scala 88:40]
      node _T_78 = and(_T_77, _T_69) @[Metapipe.scala 119:42]
      when _T_78 : @[Metapipe.scala 119:42]
        node _T_79 = not(doneFF_0.io.output.data) @[Metapipe.scala 120:78]
        io.output.stageEnable[0] <= _T_79 @[Metapipe.scala 120:75]
        doneClear <= doneFF_0.io.output.data @[Metapipe.scala 123:17]
        node _T_81 = eq(doneFF_0.io.output.data, UInt<1>("h01")) @[Metapipe.scala 124:22]
        when _T_81 : @[Metapipe.scala 124:31]
          node _T_83 = sub(maxFF.io.output.data, UInt<1>("h01")) @[Metapipe.scala 125:46]
          node _T_84 = asUInt(_T_83) @[Metapipe.scala 125:46]
          node _T_85 = tail(_T_84, 1) @[Metapipe.scala 125:46]
          node _T_86 = eq(ctr.io.output.count[0], _T_85) @[Metapipe.scala 125:37]
          when _T_86 : @[Metapipe.scala 125:54]
            node _T_89 = mux(io.input.forever, UInt<2>("h02"), UInt<2>("h03")) @[Metapipe.scala 126:39]
            stateFF.io.input.data <= _T_89 @[Metapipe.scala 126:33]
            skip @[Metapipe.scala 125:54]
          node _T_91 = eq(_T_86, UInt<1>("h00")) @[Metapipe.scala 125:54]
          when _T_91 : @[Metapipe.scala 127:21]
            stateFF.io.input.data <= stateFF.io.output.data @[Metapipe.scala 128:33]
            skip @[Metapipe.scala 127:21]
          skip @[Metapipe.scala 124:31]
        node _T_93 = eq(_T_81, UInt<1>("h00")) @[Metapipe.scala 124:31]
        when _T_93 : @[Metapipe.scala 130:19]
          stateFF.io.input.data <= stateFF.io.output.data @[Metapipe.scala 131:31]
          skip @[Metapipe.scala 130:19]
        skip @[Metapipe.scala 119:42]
      node _T_95 = lt(stateFF.io.output.data, UInt<2>("h03")) @[Metapipe.scala 133:23]
      node _T_97 = eq(_T_44, UInt<1>("h00")) @[Metapipe.scala 82:33]
      node _T_99 = eq(_T_48, UInt<1>("h00")) @[Metapipe.scala 85:41]
      node _T_100 = and(_T_97, _T_99) @[Metapipe.scala 85:41]
      node _T_102 = eq(_T_61, UInt<1>("h00")) @[Metapipe.scala 88:40]
      node _T_103 = and(_T_100, _T_102) @[Metapipe.scala 88:40]
      node _T_105 = eq(_T_69, UInt<1>("h00")) @[Metapipe.scala 119:42]
      node _T_106 = and(_T_103, _T_105) @[Metapipe.scala 119:42]
      node _T_107 = and(_T_106, _T_95) @[Metapipe.scala 133:38]
      when _T_107 : @[Metapipe.scala 133:38]
        skip @[Metapipe.scala 133:38]
      node _T_109 = eq(stateFF.io.output.data, UInt<2>("h03")) @[Metapipe.scala 149:23]
      node _T_111 = eq(_T_44, UInt<1>("h00")) @[Metapipe.scala 82:33]
      node _T_113 = eq(_T_48, UInt<1>("h00")) @[Metapipe.scala 85:41]
      node _T_114 = and(_T_111, _T_113) @[Metapipe.scala 85:41]
      node _T_116 = eq(_T_61, UInt<1>("h00")) @[Metapipe.scala 88:40]
      node _T_117 = and(_T_114, _T_116) @[Metapipe.scala 88:40]
      node _T_119 = eq(_T_69, UInt<1>("h00")) @[Metapipe.scala 119:42]
      node _T_120 = and(_T_117, _T_119) @[Metapipe.scala 119:42]
      node _T_122 = eq(_T_95, UInt<1>("h00")) @[Metapipe.scala 133:38]
      node _T_123 = and(_T_120, _T_122) @[Metapipe.scala 133:38]
      node _T_124 = and(_T_123, _T_109) @[Metapipe.scala 149:40]
      when _T_124 : @[Metapipe.scala 149:40]
        doneClear <= UInt<1>("h00") @[Metapipe.scala 150:17]
        stateFF.io.input.data <= UInt<1>("h00") @[Metapipe.scala 151:29]
        skip @[Metapipe.scala 149:40]
      node _T_128 = eq(_T_44, UInt<1>("h00")) @[Metapipe.scala 82:33]
      node _T_130 = eq(_T_48, UInt<1>("h00")) @[Metapipe.scala 85:41]
      node _T_131 = and(_T_128, _T_130) @[Metapipe.scala 85:41]
      node _T_133 = eq(_T_61, UInt<1>("h00")) @[Metapipe.scala 88:40]
      node _T_134 = and(_T_131, _T_133) @[Metapipe.scala 88:40]
      node _T_136 = eq(_T_69, UInt<1>("h00")) @[Metapipe.scala 119:42]
      node _T_137 = and(_T_134, _T_136) @[Metapipe.scala 119:42]
      node _T_139 = eq(_T_95, UInt<1>("h00")) @[Metapipe.scala 133:38]
      node _T_140 = and(_T_137, _T_139) @[Metapipe.scala 133:38]
      node _T_142 = eq(_T_109, UInt<1>("h00")) @[Metapipe.scala 149:40]
      node _T_143 = and(_T_140, _T_142) @[Metapipe.scala 149:40]
      when _T_143 : @[Metapipe.scala 152:17]
        stateFF.io.input.data <= stateFF.io.output.data @[Metapipe.scala 153:29]
        skip @[Metapipe.scala 152:17]
      skip @[Metapipe.scala 81:25]
    node _T_145 = eq(io.input.enable, UInt<1>("h00")) @[Metapipe.scala 81:25]
    when _T_145 : @[Metapipe.scala 155:15]
      io.output.stageEnable[0] <= UInt<1>("h00") @[Metapipe.scala 156:57]
      doneClear <= UInt<1>("h00") @[Metapipe.scala 157:15]
      stateFF.io.input.data <= UInt<1>("h00") @[Metapipe.scala 158:27]
      skip @[Metapipe.scala 155:15]
    node _T_149 = not(io.input.stageDone[0]) @[Metapipe.scala 162:60]
    reg _T_152 : UInt, clock with : (reset => (reset, UInt<1>("h00"))) @[Utils.scala 13:53]
    node _T_155 = mux(_T_149, UInt<1>("h01"), UInt<1>("h00")) @[Utils.scala 16:25]
    _T_152 <= _T_155 @[Utils.scala 16:19]
    node _T_157 = eq(_T_152, UInt<1>("h01")) @[Utils.scala 20:27]
    node _T_158 = and(io.input.stageDone[0], _T_157) @[Metapipe.scala 162:46]
    io.output.ctr_inc <= _T_158 @[Metapipe.scala 162:21]
    node _T_160 = eq(stateFF.io.output.data, UInt<2>("h03")) @[Metapipe.scala 163:27]
    io.output.done <= _T_160 @[Metapipe.scala 163:18]
    
  module SRFF_1 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {input : {flip set : UInt<1>, flip reset : UInt<1>, flip asyn_reset : UInt<1>}, output : {data : UInt<1>}}
    
    io is invalid
    io is invalid
    reg _T_14 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[FF.scala 276:21]
    node _T_18 = mux(io.input.reset, UInt<1>("h00"), _T_14) @[FF.scala 278:48]
    node _T_19 = mux(io.input.set, UInt<1>("h01"), _T_18) @[FF.scala 277:48]
    node _T_20 = mux(io.input.asyn_reset, UInt<1>("h00"), _T_19) @[FF.scala 277:14]
    _T_14 <= _T_20 @[FF.scala 277:8]
    node _T_22 = mux(io.input.asyn_reset, UInt<1>("h00"), _T_14) @[FF.scala 279:26]
    io.output.data <= _T_22 @[FF.scala 279:20]
    
  module FF_4 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip input : {data : UInt<32>, init : UInt<32>, enable : UInt<1>, reset : UInt<1>}, output : {data : UInt<32>}}
    
    io is invalid
    io is invalid
    reg ff : UInt, clock with : (reset => (reset, io.input.init)) @[FF.scala 32:19]
    node _T_17 = mux(io.input.enable, io.input.data, ff) @[FF.scala 33:47]
    node _T_18 = mux(io.input.reset, io.input.init, _T_17) @[FF.scala 33:12]
    ff <= _T_18 @[FF.scala 33:6]
    node _T_19 = mux(io.input.reset, io.input.init, ff) @[FF.scala 34:24]
    io.output.data <= _T_19 @[FF.scala 34:18]
    
  module SingleCounter_1 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {input : {flip start : UInt<32>, flip max : UInt<32>, flip stride : UInt<32>, flip gap : UInt<32>, flip reset : UInt<1>, flip enable : UInt<1>, flip saturate : UInt<1>}, output : {count : UInt<32>[1], countWithoutWrap : UInt<32>[1], done : UInt<1>, extendedDone : UInt<1>, saturated : UInt<1>}}
    
    io is invalid
    io is invalid
    inst FF of FF_4 @[Counter.scala 124:22]
    FF.io is invalid
    FF.clock <= clock
    FF.reset <= reset
    FF.io.input.init <= io.input.start @[Counter.scala 126:24]
    node _T_32 = or(io.input.reset, io.input.enable) @[Counter.scala 127:44]
    FF.io.input.enable <= _T_32 @[Counter.scala 127:26]
    node _T_34 = mul(io.input.stride, UInt<1>("h01")) @[Counter.scala 130:43]
    node _T_35 = add(FF.io.output.data, _T_34) @[Counter.scala 130:24]
    node _T_36 = tail(_T_35, 1) @[Counter.scala 130:24]
    node _T_37 = add(_T_36, io.input.gap) @[Counter.scala 130:52]
    node _T_38 = tail(_T_37, 1) @[Counter.scala 130:52]
    node _T_39 = geq(_T_38, io.input.max) @[Counter.scala 131:24]
    reg _T_42 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Counter.scala 132:25]
    _T_42 <= _T_39 @[Counter.scala 132:25]
    reg _T_45 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Counter.scala 133:29]
    _T_45 <= io.input.enable @[Counter.scala 133:29]
    node _T_46 = mux(io.input.saturate, FF.io.output.data, io.input.start) @[Counter.scala 134:30]
    node _T_47 = mux(_T_39, _T_46, _T_38) @[Counter.scala 134:19]
    node _T_48 = mux(io.input.reset, io.input.start, _T_47) @[Counter.scala 135:30]
    FF.io.input.data <= _T_48 @[Counter.scala 135:24]
    node _T_50 = mul(UInt<1>("h00"), io.input.stride) @[Counter.scala 137:67]
    node _T_51 = add(FF.io.output.data, _T_50) @[Counter.scala 137:62]
    node _T_52 = tail(_T_51, 1) @[Counter.scala 137:62]
    io.output.count[0] <= _T_52 @[Counter.scala 137:53]
    node _T_54 = eq(FF.io.output.data, UInt<1>("h00")) @[Counter.scala 139:50]
    node _T_55 = mux(_T_54, io.input.max, FF.io.output.data) @[Counter.scala 139:43]
    node _T_57 = mul(UInt<1>("h00"), io.input.stride) @[Counter.scala 139:85]
    node _T_58 = add(_T_55, _T_57) @[Counter.scala 139:80]
    node _T_59 = tail(_T_58, 1) @[Counter.scala 139:80]
    io.output.countWithoutWrap[0] <= _T_59 @[Counter.scala 139:37]
    node _T_60 = and(io.input.enable, _T_39) @[Counter.scala 141:39]
    io.output.done <= _T_60 @[Counter.scala 141:20]
    node _T_61 = and(io.input.saturate, _T_39) @[Counter.scala 142:46]
    io.output.saturated <= _T_61 @[Counter.scala 142:25]
    node _T_62 = or(io.input.enable, _T_45) @[Counter.scala 143:48]
    node _T_63 = or(_T_39, _T_42) @[Counter.scala 143:71]
    node _T_64 = and(_T_62, _T_63) @[Counter.scala 143:62]
    io.output.extendedDone <= _T_64 @[Counter.scala 143:28]
    
  module Innerpipe : 
    input clock : Clock
    input reset : UInt<1>
    output io : {input : {flip enable : UInt<1>, flip ctr_done : UInt<1>, flip ctr_maxIn : UInt<32>[1], flip forever : UInt<1>, flip nextState : UInt<32>, flip initState : UInt<32>, flip doneCondition : UInt<1>}, output : {done : UInt<1>, ctr_en : UInt<1>, ctr_inc : UInt<1>, rst_en : UInt<1>, ctr_maxOut : UInt<32>[1], state : UInt<32>}}
    
    io is invalid
    io is invalid
    reg _T_36 : UInt, clock with : (reset => (reset, UInt<1>("h00"))) @[Innerpipe.scala 41:24]
    reg _T_39 : UInt, clock with : (reset => (reset, UInt<1>("h00"))) @[Innerpipe.scala 42:55]
    inst SingleCounter of SingleCounter_1 @[Innerpipe.scala 45:24]
    SingleCounter.io is invalid
    SingleCounter.clock <= clock
    SingleCounter.reset <= reset
    node _T_41 = eq(_T_36, UInt<1>("h01")) @[Innerpipe.scala 46:37]
    SingleCounter.io.input.enable <= _T_41 @[Innerpipe.scala 46:28]
    node _T_43 = neq(_T_36, UInt<1>("h01")) @[Innerpipe.scala 47:37]
    SingleCounter.io.input.reset <= _T_43 @[Innerpipe.scala 47:27]
    SingleCounter.io.input.saturate <= UInt<1>("h01") @[Innerpipe.scala 48:30]
    SingleCounter.io.input.max <= UInt<4>("h0a") @[Innerpipe.scala 49:25]
    SingleCounter.io.input.stride <= UInt<1>("h01") @[Innerpipe.scala 50:28]
    when io.input.enable : @[Innerpipe.scala 53:28]
      node _T_48 = eq(_T_36, UInt<1>("h00")) @[Innerpipe.scala 55:19]
      when _T_48 : @[Innerpipe.scala 55:36]
        io.output.done <= UInt<1>("h00") @[Innerpipe.scala 56:24]
        io.output.ctr_en <= UInt<1>("h00") @[Innerpipe.scala 57:26]
        io.output.ctr_inc <= UInt<1>("h00") @[Innerpipe.scala 58:27]
        io.output.rst_en <= UInt<1>("h00") @[Innerpipe.scala 59:26]
        _T_39 <= io.input.ctr_maxIn[0] @[Innerpipe.scala 60:52]
        _T_36 <= UInt<1>("h01") @[Innerpipe.scala 61:15]
        skip @[Innerpipe.scala 55:36]
      node _T_55 = eq(_T_36, UInt<1>("h01")) @[Innerpipe.scala 62:25]
      node _T_57 = eq(_T_48, UInt<1>("h00")) @[Innerpipe.scala 55:36]
      node _T_58 = and(_T_57, _T_55) @[Innerpipe.scala 62:43]
      when _T_58 : @[Innerpipe.scala 62:43]
        io.output.rst_en <= UInt<1>("h01") @[Innerpipe.scala 63:26]
        io.output.ctr_maxOut[0] <= _T_39 @[Innerpipe.scala 64:67]
        node _T_62 = mux(io.input.ctr_done, UInt<2>("h03"), UInt<1>("h01")) @[Innerpipe.scala 65:21]
        _T_36 <= _T_62 @[Innerpipe.scala 65:15]
        when SingleCounter.io.output.done : @[Innerpipe.scala 66:38]
          io.output.rst_en <= UInt<1>("h00") @[Innerpipe.scala 67:28]
          node _T_66 = mux(io.input.ctr_done, UInt<2>("h03"), UInt<2>("h02")) @[Innerpipe.scala 68:23]
          _T_36 <= _T_66 @[Innerpipe.scala 68:17]
          skip @[Innerpipe.scala 66:38]
        skip @[Innerpipe.scala 62:43]
      node _T_68 = eq(_T_36, UInt<2>("h02")) @[Innerpipe.scala 70:25]
      node _T_70 = eq(_T_48, UInt<1>("h00")) @[Innerpipe.scala 55:36]
      node _T_72 = eq(_T_55, UInt<1>("h00")) @[Innerpipe.scala 62:43]
      node _T_73 = and(_T_70, _T_72) @[Innerpipe.scala 62:43]
      node _T_74 = and(_T_73, _T_68) @[Innerpipe.scala 70:41]
      when _T_74 : @[Innerpipe.scala 70:41]
        io.output.ctr_en <= UInt<1>("h01") @[Innerpipe.scala 71:26]
        io.output.ctr_inc <= UInt<1>("h01") @[Innerpipe.scala 72:27]
        when io.input.ctr_done : @[Innerpipe.scala 73:34]
          io.output.ctr_inc <= UInt<1>("h00") @[Innerpipe.scala 74:29]
          _T_39 <= UInt<1>("h00") @[Innerpipe.scala 75:54]
          _T_36 <= UInt<2>("h03") @[Innerpipe.scala 76:17]
          skip @[Innerpipe.scala 73:34]
        node _T_81 = eq(io.input.ctr_done, UInt<1>("h00")) @[Innerpipe.scala 73:34]
        when _T_81 : @[Innerpipe.scala 77:21]
          _T_36 <= UInt<2>("h02") @[Innerpipe.scala 78:17]
          skip @[Innerpipe.scala 77:21]
        skip @[Innerpipe.scala 70:41]
      node _T_84 = eq(_T_36, UInt<2>("h03")) @[Innerpipe.scala 80:25]
      node _T_86 = eq(_T_48, UInt<1>("h00")) @[Innerpipe.scala 55:36]
      node _T_88 = eq(_T_55, UInt<1>("h00")) @[Innerpipe.scala 62:43]
      node _T_89 = and(_T_86, _T_88) @[Innerpipe.scala 62:43]
      node _T_91 = eq(_T_68, UInt<1>("h00")) @[Innerpipe.scala 70:41]
      node _T_92 = and(_T_89, _T_91) @[Innerpipe.scala 70:41]
      node _T_93 = and(_T_92, _T_84) @[Innerpipe.scala 80:42]
      when _T_93 : @[Innerpipe.scala 80:42]
        node _T_96 = mux(io.input.forever, UInt<1>("h00"), UInt<1>("h01")) @[Innerpipe.scala 81:30]
        io.output.done <= _T_96 @[Innerpipe.scala 81:24]
        _T_36 <= UInt<1>("h01") @[Innerpipe.scala 82:15]
        skip @[Innerpipe.scala 80:42]
      node _T_99 = eq(_T_36, UInt<3>("h04")) @[Innerpipe.scala 83:25]
      node _T_101 = eq(_T_48, UInt<1>("h00")) @[Innerpipe.scala 55:36]
      node _T_103 = eq(_T_55, UInt<1>("h00")) @[Innerpipe.scala 62:43]
      node _T_104 = and(_T_101, _T_103) @[Innerpipe.scala 62:43]
      node _T_106 = eq(_T_68, UInt<1>("h00")) @[Innerpipe.scala 70:41]
      node _T_107 = and(_T_104, _T_106) @[Innerpipe.scala 70:41]
      node _T_109 = eq(_T_84, UInt<1>("h00")) @[Innerpipe.scala 80:42]
      node _T_110 = and(_T_107, _T_109) @[Innerpipe.scala 80:42]
      node _T_111 = and(_T_110, _T_99) @[Innerpipe.scala 83:46]
      when _T_111 : @[Innerpipe.scala 83:46]
        _T_36 <= UInt<3>("h04") @[Innerpipe.scala 84:15]
        skip @[Innerpipe.scala 83:46]
      skip @[Innerpipe.scala 53:28]
    node _T_114 = eq(io.input.enable, UInt<1>("h00")) @[Innerpipe.scala 53:28]
    when _T_114 : @[Innerpipe.scala 86:17]
      node _T_117 = mux(io.input.ctr_done, UInt<1>("h01"), UInt<1>("h00")) @[Innerpipe.scala 87:28]
      io.output.done <= _T_117 @[Innerpipe.scala 87:22]
      io.output.ctr_en <= UInt<1>("h00") @[Innerpipe.scala 88:24]
      io.output.ctr_inc <= UInt<1>("h00") @[Innerpipe.scala 89:25]
      io.output.rst_en <= UInt<1>("h00") @[Innerpipe.scala 90:24]
      _T_36 <= UInt<1>("h00") @[Innerpipe.scala 91:13]
      skip @[Innerpipe.scala 86:17]
    
  module AccelTop : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip enable : UInt<1>, done : UInt<1>, flip memStreams : {flip cmd : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<32>[16], isWr : UInt<1>, size : UInt<32>}}, flip wdata : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>[16]}, rdata : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>[16]}}[0], flip argIns : UInt<32>[0], argOuts : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>}[0], flip stream_in_data : UInt<24>, flip stream_in_startofpacket : UInt<1>, flip stream_in_endofpacket : UInt<1>, flip stream_in_empty : UInt<2>, flip stream_in_valid : UInt<1>, flip stream_out_ready : UInt<1>, stream_in_ready : UInt<1>, stream_out_data : UInt<16>, stream_out_startofpacket : UInt<1>, stream_out_endofpacket : UInt<1>, stream_out_empty : UInt<1>, stream_out_valid : UInt<1>}
    
    io is invalid
    io is invalid
    wire AccelController_done : UInt<1> @[GlobalWires.scala 6:32]
    AccelController_done is invalid @[GlobalWires.scala 6:32]
    wire x180_UnitPipe_done : UInt<1> @[GlobalWires.scala 7:30]
    x180_UnitPipe_done is invalid @[GlobalWires.scala 7:30]
    wire x180_UnitPipe_en : UInt<1> @[GlobalWires.scala 8:28]
    x180_UnitPipe_en is invalid @[GlobalWires.scala 8:28]
    wire x180_UnitPipe_resetter : UInt<1> @[GlobalWires.scala 9:34]
    x180_UnitPipe_resetter is invalid @[GlobalWires.scala 9:34]
    wire x178_tuple : UInt<16> @[GlobalWires.scala 10:22]
    x178_tuple is invalid @[GlobalWires.scala 10:22]
    wire x169_valid : UInt<1> @[GlobalWires.scala 11:22]
    x169_valid is invalid @[GlobalWires.scala 11:22]
    wire x169_data : UInt<16> @[GlobalWires.scala 12:21]
    x169_data is invalid @[GlobalWires.scala 12:21]
    wire converted_data : UInt<16> @[GlobalWires.scala 13:26]
    converted_data is invalid @[GlobalWires.scala 13:26]
    when reset : @[RootController.scala 10:16]
      io.stream_out_data <= UInt<1>("h00") @[RootController.scala 11:34]
      io.stream_out_startofpacket <= UInt<1>("h00") @[RootController.scala 12:34]
      io.stream_out_endofpacket <= UInt<1>("h00") @[RootController.scala 13:34]
      io.stream_out_empty <= UInt<1>("h00") @[RootController.scala 14:34]
      io.stream_out_valid <= UInt<1>("h00") @[RootController.scala 15:34]
      skip @[RootController.scala 10:16]
    node _T_746 = not(io.stream_out_valid) @[RootController.scala 16:38]
    node _T_747 = or(io.stream_out_ready, _T_746) @[RootController.scala 16:36]
    node _T_749 = eq(reset, UInt<1>("h00")) @[RootController.scala 10:16]
    node _T_750 = and(_T_749, _T_747) @[RootController.scala 16:60]
    when _T_750 : @[RootController.scala 16:60]
      io.stream_out_data <= converted_data @[RootController.scala 17:34]
      io.stream_out_startofpacket <= io.stream_in_startofpacket @[RootController.scala 18:34]
      io.stream_out_endofpacket <= io.stream_in_endofpacket @[RootController.scala 19:34]
      io.stream_out_empty <= io.stream_in_empty @[RootController.scala 20:34]
      io.stream_out_valid <= io.stream_in_valid @[RootController.scala 21:34]
      skip @[RootController.scala 16:60]
    node _T_752 = eq(io.done, UInt<1>("h00")) @[RootController.scala 24:40]
    node AccelController_en = and(io.enable, _T_752) @[RootController.scala 24:38]
    inst AccelController_sm of Metapipe @[RootController.scala 28:34]
    AccelController_sm.io is invalid
    AccelController_sm.clock <= clock
    AccelController_sm.reset <= reset
    AccelController_sm.io.input.enable <= AccelController_en @[RootController.scala 29:38]
    AccelController_done <= AccelController_sm.io.output.done @[RootController.scala 30:24]
    AccelController_sm.io.input.numIter <= UInt<1>("h01") @[RootController.scala 32:39]
    AccelController_sm.io.input.rst <= UInt<1>("h00") @[RootController.scala 33:35]
    AccelController_sm.io.input.forever <= UInt<1>("h00") @[RootController.scala 37:39]
    AccelController_sm.io.input.stageDone[0] <= x180_UnitPipe_done @[RootController.scala 39:44]
    x180_UnitPipe_en <= AccelController_sm.io.output.stageEnable[0] @[RootController.scala 40:20]
    x180_UnitPipe_resetter <= AccelController_sm.io.output.rst_en @[RootController.scala 41:26]
    inst done_latch of SRFF_1 @[RootController.scala 42:26]
    done_latch.io is invalid
    done_latch.clock <= clock
    done_latch.reset <= reset
    done_latch.io.input.set <= AccelController_sm.io.output.done @[RootController.scala 43:27]
    done_latch.io.input.reset <= UInt<1>("h00") @[RootController.scala 44:29]
    io.done <= done_latch.io.output.data @[RootController.scala 45:11]
    inst x180_UnitPipe_sm of Innerpipe @[RootController.scala 48:32]
    x180_UnitPipe_sm.io is invalid
    x180_UnitPipe_sm.clock <= clock
    x180_UnitPipe_sm.reset <= reset
    x180_UnitPipe_sm.io.input.enable <= x180_UnitPipe_en @[RootController.scala 49:36]
    x180_UnitPipe_done <= x180_UnitPipe_sm.io.output.done @[RootController.scala 50:22]
    reg _T_757 : UInt, clock with : (reset => (reset, UInt<1>("h00"))) @[Utils.scala 13:53]
    node _T_760 = mux(x180_UnitPipe_sm.io.output.ctr_en, UInt<1>("h01"), UInt<1>("h00")) @[Utils.scala 16:25]
    _T_757 <= _T_760 @[Utils.scala 16:19]
    node _T_762 = eq(_T_757, UInt<1>("h01")) @[Utils.scala 20:27]
    x180_UnitPipe_sm.io.input.ctr_done <= _T_762 @[RootController.scala 54:38]
    x180_UnitPipe_sm.io.input.forever <= UInt<1>("h00") @[RootController.scala 56:37]
    node x172_apply = bits(io.stream_in_data, 23, 16) @[x180_UnitPipe.scala 8:24]
    node x174_apply = bits(io.stream_in_data, 15, 8) @[x180_UnitPipe.scala 10:24]
    node x176_apply = bits(io.stream_in_data, 7, 0) @[x180_UnitPipe.scala 12:24]
    node _T_764 = bits(x172_apply, 4, 0) @[x180_UnitPipe.scala 14:31]
    node _T_765 = bits(x174_apply, 5, 0) @[x180_UnitPipe.scala 14:41]
    node _T_766 = bits(x176_apply, 4, 0) @[x180_UnitPipe.scala 14:51]
    node _T_767 = cat(_T_764, _T_765) @[Cat.scala 30:58]
    node _T_768 = cat(_T_767, _T_766) @[Cat.scala 30:58]
    x178_tuple <= _T_768 @[x180_UnitPipe.scala 14:14]
    node _T_770 = and(x180_UnitPipe_done, UInt<1>("h01")) @[x180_UnitPipe.scala 15:36]
    x169_valid <= _T_770 @[x180_UnitPipe.scala 15:14]
    x169_data <= x178_tuple @[x180_UnitPipe.scala 16:13]
    converted_data <= x169_data @[x180_UnitPipe.scala 17:18]
    
  module FF_5 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<32>, flip init : UInt<32>, out : UInt<32>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<32> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module FF_6 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<32>, flip init : UInt<32>, out : UInt<32>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<32> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module MuxN : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip ins : UInt<32>[2], flip sel : UInt<1>, out : UInt<32>}
    
    io is invalid
    io is invalid
    io.out <= io.ins[io.sel] @[MuxN.scala 15:10]
    
  module RegFile : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip raddr : UInt<1>, flip wen : UInt<1>, flip waddr : UInt<1>, flip wdata : UInt<32>, rdata : UInt<32>, argIns : UInt<32>[2], flip argOuts : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>}[1]}
    
    io is invalid
    io is invalid
    inst regs_0 of FF_5 @[RegFile.scala 46:20]
    regs_0.io is invalid
    regs_0.clock <= clock
    regs_0.reset <= reset
    regs_0.io.init <= UInt<1>("h00") @[RegFile.scala 47:18]
    regs_0.io.in <= io.wdata @[RegFile.scala 48:16]
    node _T_51 = eq(io.waddr, UInt<1>("h00")) @[RegFile.scala 49:147]
    node _T_52 = and(io.wen, _T_51) @[RegFile.scala 49:135]
    regs_0.io.enable <= _T_52 @[RegFile.scala 49:18]
    inst regs_1 of FF_6 @[RegFile.scala 46:20]
    regs_1.io is invalid
    regs_1.clock <= clock
    regs_1.reset <= reset
    regs_1.io.init <= UInt<1>("h00") @[RegFile.scala 47:18]
    node _T_54 = mux(io.argOuts[0].valid, io.argOuts[0].bits, io.wdata) @[RegFile.scala 48:51]
    regs_1.io.in <= _T_54 @[RegFile.scala 48:16]
    node _T_56 = eq(io.waddr, UInt<1>("h01")) @[RegFile.scala 49:113]
    node _T_57 = and(io.wen, _T_56) @[RegFile.scala 49:101]
    node _T_58 = or(io.argOuts[0].valid, _T_57) @[RegFile.scala 49:91]
    regs_1.io.enable <= _T_58 @[RegFile.scala 49:18]
    inst rport of MuxN @[RegFile.scala 53:21]
    rport.io is invalid
    rport.clock <= clock
    rport.reset <= reset
    wire regOuts : UInt<32>[2] @[RegFile.scala 54:20]
    regOuts is invalid @[RegFile.scala 54:20]
    regOuts[0] <= regs_0.io.out @[RegFile.scala 54:20]
    regOuts[1] <= regs_1.io.out @[RegFile.scala 54:20]
    rport.io.ins[0] <= regOuts[0] @[RegFile.scala 55:16]
    rport.io.ins[1] <= regOuts[1] @[RegFile.scala 55:16]
    rport.io.sel <= io.raddr @[RegFile.scala 56:16]
    io.rdata <= rport.io.out @[RegFile.scala 57:12]
    wire _T_67 : UInt<32>[2] @[RegFile.scala 59:19]
    _T_67 is invalid @[RegFile.scala 59:19]
    _T_67[0] <= regOuts[0] @[RegFile.scala 59:19]
    _T_67[1] <= regOuts[1] @[RegFile.scala 59:19]
    io.argIns[0] <= _T_67[0] @[RegFile.scala 59:13]
    io.argIns[1] <= _T_67[1] @[RegFile.scala 59:13]
    
  module FF_7 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<1>, flip init : UInt<1>, out : UInt<1>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<1> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module Depulser : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<1>, flip rst : UInt<1>, out : UInt<1>}
    
    io is invalid
    io is invalid
    inst r of FF_7 @[Depulser.scala 15:17]
    r.io is invalid
    r.clock <= clock
    r.reset <= reset
    node _T_9 = mux(io.rst, UInt<1>("h00"), io.in) @[Depulser.scala 16:17]
    r.io.in <= _T_9 @[Depulser.scala 16:11]
    r.io.init <= UInt<1>("h00") @[Depulser.scala 17:13]
    node _T_11 = or(io.in, io.rst) @[Depulser.scala 18:24]
    r.io.enable <= _T_11 @[Depulser.scala 18:15]
    io.out <= r.io.out @[Depulser.scala 19:10]
    
  module FF_8 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<1>, flip init : UInt<1>, out : UInt<1>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<1> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module FIFOArbiter : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip enq : UInt<32>[16][0], flip enqVld : UInt<1>[0], full : UInt<1>[0], deq : UInt<32>[16], flip deqVld : UInt<1>, empty : UInt<1>, flip forceTag : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<1>}, tag : UInt<1>, flip config : {chainWrite : UInt<1>, chainRead : UInt<1>}}
    
    io is invalid
    io is invalid
    inst tagFF of FF_8 @[FIFOArbiter.scala 30:21]
    tagFF.io is invalid
    tagFF.clock <= clock
    tagFF.reset <= reset
    tagFF.io.init <= UInt<1>("h00") @[FIFOArbiter.scala 31:17]
    node tag = mux(io.forceTag.valid, io.forceTag.bits, tagFF.io.out) @[FIFOArbiter.scala 32:16]
    io.tag <= UInt<1>("h00") @[FIFOArbiter.scala 72:12]
    wire _T_162 : UInt<32>[16] @[FIFOArbiter.scala 73:18]
    _T_162 is invalid @[FIFOArbiter.scala 73:18]
    _T_162[0] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[1] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[2] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[3] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[4] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[5] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[6] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[7] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[8] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[9] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[10] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[11] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[12] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[13] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[14] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[15] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    io.deq[0] <= _T_162[0] @[FIFOArbiter.scala 73:12]
    io.deq[1] <= _T_162[1] @[FIFOArbiter.scala 73:12]
    io.deq[2] <= _T_162[2] @[FIFOArbiter.scala 73:12]
    io.deq[3] <= _T_162[3] @[FIFOArbiter.scala 73:12]
    io.deq[4] <= _T_162[4] @[FIFOArbiter.scala 73:12]
    io.deq[5] <= _T_162[5] @[FIFOArbiter.scala 73:12]
    io.deq[6] <= _T_162[6] @[FIFOArbiter.scala 73:12]
    io.deq[7] <= _T_162[7] @[FIFOArbiter.scala 73:12]
    io.deq[8] <= _T_162[8] @[FIFOArbiter.scala 73:12]
    io.deq[9] <= _T_162[9] @[FIFOArbiter.scala 73:12]
    io.deq[10] <= _T_162[10] @[FIFOArbiter.scala 73:12]
    io.deq[11] <= _T_162[11] @[FIFOArbiter.scala 73:12]
    io.deq[12] <= _T_162[12] @[FIFOArbiter.scala 73:12]
    io.deq[13] <= _T_162[13] @[FIFOArbiter.scala 73:12]
    io.deq[14] <= _T_162[14] @[FIFOArbiter.scala 73:12]
    io.deq[15] <= _T_162[15] @[FIFOArbiter.scala 73:12]
    io.empty <= UInt<1>("h01") @[FIFOArbiter.scala 74:14]
    
  module FF_9 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<1>, flip init : UInt<1>, out : UInt<1>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<1> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module FIFOArbiter_1 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip enq : UInt<1>[16][0], flip enqVld : UInt<1>[0], full : UInt<1>[0], deq : UInt<1>[16], flip deqVld : UInt<1>, empty : UInt<1>, flip forceTag : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<1>}, tag : UInt<1>, flip config : {chainWrite : UInt<1>, chainRead : UInt<1>}}
    
    io is invalid
    io is invalid
    inst tagFF of FF_9 @[FIFOArbiter.scala 30:21]
    tagFF.io is invalid
    tagFF.clock <= clock
    tagFF.reset <= reset
    tagFF.io.init <= UInt<1>("h00") @[FIFOArbiter.scala 31:17]
    node tag = mux(io.forceTag.valid, io.forceTag.bits, tagFF.io.out) @[FIFOArbiter.scala 32:16]
    io.tag <= UInt<1>("h00") @[FIFOArbiter.scala 72:12]
    wire _T_162 : UInt<1>[16] @[FIFOArbiter.scala 73:18]
    _T_162 is invalid @[FIFOArbiter.scala 73:18]
    _T_162[0] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[1] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[2] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[3] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[4] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[5] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[6] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[7] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[8] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[9] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[10] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[11] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[12] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[13] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[14] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[15] <= UInt<1>("h00") @[FIFOArbiter.scala 73:18]
    io.deq[0] <= _T_162[0] @[FIFOArbiter.scala 73:12]
    io.deq[1] <= _T_162[1] @[FIFOArbiter.scala 73:12]
    io.deq[2] <= _T_162[2] @[FIFOArbiter.scala 73:12]
    io.deq[3] <= _T_162[3] @[FIFOArbiter.scala 73:12]
    io.deq[4] <= _T_162[4] @[FIFOArbiter.scala 73:12]
    io.deq[5] <= _T_162[5] @[FIFOArbiter.scala 73:12]
    io.deq[6] <= _T_162[6] @[FIFOArbiter.scala 73:12]
    io.deq[7] <= _T_162[7] @[FIFOArbiter.scala 73:12]
    io.deq[8] <= _T_162[8] @[FIFOArbiter.scala 73:12]
    io.deq[9] <= _T_162[9] @[FIFOArbiter.scala 73:12]
    io.deq[10] <= _T_162[10] @[FIFOArbiter.scala 73:12]
    io.deq[11] <= _T_162[11] @[FIFOArbiter.scala 73:12]
    io.deq[12] <= _T_162[12] @[FIFOArbiter.scala 73:12]
    io.deq[13] <= _T_162[13] @[FIFOArbiter.scala 73:12]
    io.deq[14] <= _T_162[14] @[FIFOArbiter.scala 73:12]
    io.deq[15] <= _T_162[15] @[FIFOArbiter.scala 73:12]
    io.empty <= UInt<1>("h01") @[FIFOArbiter.scala 74:14]
    
  module FF_10 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<1>, flip init : UInt<1>, out : UInt<1>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<1> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module FIFOArbiter_2 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip enq : UInt<32>[16][0], flip enqVld : UInt<1>[0], full : UInt<1>[0], deq : UInt<32>[16], flip deqVld : UInt<1>, empty : UInt<1>, flip forceTag : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<1>}, tag : UInt<1>, flip config : {chainWrite : UInt<1>, chainRead : UInt<1>}}
    
    io is invalid
    io is invalid
    inst tagFF of FF_10 @[FIFOArbiter.scala 30:21]
    tagFF.io is invalid
    tagFF.clock <= clock
    tagFF.reset <= reset
    tagFF.io.init <= UInt<1>("h00") @[FIFOArbiter.scala 31:17]
    node tag = mux(io.forceTag.valid, io.forceTag.bits, tagFF.io.out) @[FIFOArbiter.scala 32:16]
    io.tag <= UInt<1>("h00") @[FIFOArbiter.scala 72:12]
    wire _T_162 : UInt<32>[16] @[FIFOArbiter.scala 73:18]
    _T_162 is invalid @[FIFOArbiter.scala 73:18]
    _T_162[0] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[1] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[2] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[3] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[4] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[5] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[6] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[7] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[8] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[9] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[10] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[11] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[12] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[13] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[14] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[15] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    io.deq[0] <= _T_162[0] @[FIFOArbiter.scala 73:12]
    io.deq[1] <= _T_162[1] @[FIFOArbiter.scala 73:12]
    io.deq[2] <= _T_162[2] @[FIFOArbiter.scala 73:12]
    io.deq[3] <= _T_162[3] @[FIFOArbiter.scala 73:12]
    io.deq[4] <= _T_162[4] @[FIFOArbiter.scala 73:12]
    io.deq[5] <= _T_162[5] @[FIFOArbiter.scala 73:12]
    io.deq[6] <= _T_162[6] @[FIFOArbiter.scala 73:12]
    io.deq[7] <= _T_162[7] @[FIFOArbiter.scala 73:12]
    io.deq[8] <= _T_162[8] @[FIFOArbiter.scala 73:12]
    io.deq[9] <= _T_162[9] @[FIFOArbiter.scala 73:12]
    io.deq[10] <= _T_162[10] @[FIFOArbiter.scala 73:12]
    io.deq[11] <= _T_162[11] @[FIFOArbiter.scala 73:12]
    io.deq[12] <= _T_162[12] @[FIFOArbiter.scala 73:12]
    io.deq[13] <= _T_162[13] @[FIFOArbiter.scala 73:12]
    io.deq[14] <= _T_162[14] @[FIFOArbiter.scala 73:12]
    io.deq[15] <= _T_162[15] @[FIFOArbiter.scala 73:12]
    io.empty <= UInt<1>("h01") @[FIFOArbiter.scala 74:14]
    
  module FF_11 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<1>, flip init : UInt<1>, out : UInt<1>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<1> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module FIFOArbiter_3 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip enq : UInt<32>[16][0], flip enqVld : UInt<1>[0], full : UInt<1>[0], deq : UInt<32>[16], flip deqVld : UInt<1>, empty : UInt<1>, flip forceTag : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<1>}, tag : UInt<1>, flip config : {chainWrite : UInt<1>, chainRead : UInt<1>}}
    
    io is invalid
    io is invalid
    inst tagFF of FF_11 @[FIFOArbiter.scala 30:21]
    tagFF.io is invalid
    tagFF.clock <= clock
    tagFF.reset <= reset
    tagFF.io.init <= UInt<1>("h00") @[FIFOArbiter.scala 31:17]
    node tag = mux(io.forceTag.valid, io.forceTag.bits, tagFF.io.out) @[FIFOArbiter.scala 32:16]
    io.tag <= UInt<1>("h00") @[FIFOArbiter.scala 72:12]
    wire _T_162 : UInt<32>[16] @[FIFOArbiter.scala 73:18]
    _T_162 is invalid @[FIFOArbiter.scala 73:18]
    _T_162[0] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[1] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[2] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[3] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[4] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[5] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[6] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[7] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[8] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[9] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[10] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[11] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[12] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[13] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[14] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    _T_162[15] <= UInt<32>("h00") @[FIFOArbiter.scala 73:18]
    io.deq[0] <= _T_162[0] @[FIFOArbiter.scala 73:12]
    io.deq[1] <= _T_162[1] @[FIFOArbiter.scala 73:12]
    io.deq[2] <= _T_162[2] @[FIFOArbiter.scala 73:12]
    io.deq[3] <= _T_162[3] @[FIFOArbiter.scala 73:12]
    io.deq[4] <= _T_162[4] @[FIFOArbiter.scala 73:12]
    io.deq[5] <= _T_162[5] @[FIFOArbiter.scala 73:12]
    io.deq[6] <= _T_162[6] @[FIFOArbiter.scala 73:12]
    io.deq[7] <= _T_162[7] @[FIFOArbiter.scala 73:12]
    io.deq[8] <= _T_162[8] @[FIFOArbiter.scala 73:12]
    io.deq[9] <= _T_162[9] @[FIFOArbiter.scala 73:12]
    io.deq[10] <= _T_162[10] @[FIFOArbiter.scala 73:12]
    io.deq[11] <= _T_162[11] @[FIFOArbiter.scala 73:12]
    io.deq[12] <= _T_162[12] @[FIFOArbiter.scala 73:12]
    io.deq[13] <= _T_162[13] @[FIFOArbiter.scala 73:12]
    io.deq[14] <= _T_162[14] @[FIFOArbiter.scala 73:12]
    io.deq[15] <= _T_162[15] @[FIFOArbiter.scala 73:12]
    io.empty <= UInt<1>("h01") @[FIFOArbiter.scala 74:14]
    
  module FF_12 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<32>, flip init : UInt<32>, out : UInt<32>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<32> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module Counter : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip max : UInt<32>, flip stride : UInt<32>, out : UInt<32>, next : UInt<32>, flip reset : UInt<1>, flip enable : UInt<1>, flip saturate : UInt<1>, done : UInt<1>}
    
    io is invalid
    io is invalid
    inst reg of FF_12 @[Counter.scala 24:19]
    reg.io is invalid
    reg.clock <= clock
    reg.reset <= reset
    reg.io.init <= UInt<32>("h00") @[Counter.scala 26:15]
    node _T_18 = or(io.reset, io.enable) @[Counter.scala 27:29]
    reg.io.enable <= _T_18 @[Counter.scala 27:17]
    node count = cat(UInt<1>("h00"), reg.io.out) @[Cat.scala 30:58]
    node _T_20 = add(count, io.stride) @[Counter.scala 30:22]
    node newval = tail(_T_20, 1) @[Counter.scala 30:22]
    node isMax = geq(newval, io.max) @[Counter.scala 31:22]
    node _T_21 = mux(io.saturate, count, UInt<32>("h00")) @[Counter.scala 32:28]
    node next = mux(isMax, _T_21, newval) @[Counter.scala 32:17]
    when io.reset : @[Counter.scala 33:19]
      reg.io.in <= UInt<32>("h00") @[Counter.scala 34:15]
      skip @[Counter.scala 33:19]
    node _T_23 = eq(io.reset, UInt<1>("h00")) @[Counter.scala 33:19]
    when _T_23 : @[Counter.scala 35:16]
      reg.io.in <= next @[Counter.scala 36:15]
      skip @[Counter.scala 35:16]
    io.out <= count @[Counter.scala 39:10]
    io.next <= next @[Counter.scala 40:11]
    node _T_24 = and(io.enable, isMax) @[Counter.scala 41:24]
    io.done <= _T_24 @[Counter.scala 41:11]
    
  module SRFF_2 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {input : {flip set : UInt<1>, flip reset : UInt<1>, flip asyn_reset : UInt<1>}, output : {data : UInt<1>}}
    
    io is invalid
    io is invalid
    reg _T_14 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[FF.scala 276:21]
    node _T_18 = mux(io.input.reset, UInt<1>("h00"), _T_14) @[FF.scala 278:48]
    node _T_19 = mux(io.input.set, UInt<1>("h01"), _T_18) @[FF.scala 277:48]
    node _T_20 = mux(io.input.asyn_reset, UInt<1>("h00"), _T_19) @[FF.scala 277:14]
    _T_14 <= _T_20 @[FF.scala 277:8]
    node _T_22 = mux(io.input.asyn_reset, UInt<1>("h00"), _T_14) @[FF.scala 279:26]
    io.output.data <= _T_22 @[FF.scala 279:20]
    
  module FF_13 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip in : UInt<11>, flip init : UInt<11>, out : UInt<11>, flip enable : UInt<1>}
    
    io is invalid
    io is invalid
    wire d : UInt<11> @[FF.scala 18:15]
    d is invalid @[FF.scala 18:15]
    reg ff : UInt, clock with : (reset => (reset, io.init)) @[FF.scala 19:19]
    ff <= d @[FF.scala 19:19]
    when io.enable : @[FF.scala 20:20]
      d <= io.in @[FF.scala 21:7]
      skip @[FF.scala 20:20]
    node _T_13 = eq(io.enable, UInt<1>("h00")) @[FF.scala 20:20]
    when _T_13 : @[FF.scala 22:16]
      d <= ff @[FF.scala 23:7]
      skip @[FF.scala 22:16]
    io.out <= ff @[FF.scala 25:10]
    
  module Counter_1 : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip max : UInt<11>, flip stride : UInt<11>, out : UInt<11>, next : UInt<11>, flip reset : UInt<1>, flip enable : UInt<1>, flip saturate : UInt<1>, done : UInt<1>}
    
    io is invalid
    io is invalid
    inst reg of FF_13 @[Counter.scala 24:19]
    reg.io is invalid
    reg.clock <= clock
    reg.reset <= reset
    reg.io.init <= UInt<11>("h00") @[Counter.scala 26:15]
    node _T_18 = or(io.reset, io.enable) @[Counter.scala 27:29]
    reg.io.enable <= _T_18 @[Counter.scala 27:17]
    node count = cat(UInt<1>("h00"), reg.io.out) @[Cat.scala 30:58]
    node _T_20 = add(count, io.stride) @[Counter.scala 30:22]
    node newval = tail(_T_20, 1) @[Counter.scala 30:22]
    node isMax = geq(newval, io.max) @[Counter.scala 31:22]
    node _T_21 = mux(io.saturate, count, UInt<11>("h00")) @[Counter.scala 32:28]
    node next = mux(isMax, _T_21, newval) @[Counter.scala 32:17]
    when io.reset : @[Counter.scala 33:19]
      reg.io.in <= UInt<11>("h00") @[Counter.scala 34:15]
      skip @[Counter.scala 33:19]
    node _T_23 = eq(io.reset, UInt<1>("h00")) @[Counter.scala 33:19]
    when _T_23 : @[Counter.scala 35:16]
      reg.io.in <= next @[Counter.scala 36:15]
      skip @[Counter.scala 35:16]
    io.out <= count @[Counter.scala 39:10]
    io.next <= next @[Counter.scala 40:11]
    node _T_24 = and(io.enable, isMax) @[Counter.scala 41:24]
    io.done <= _T_24 @[Counter.scala 41:11]
    
  module MAGCore : 
    input clock : Clock
    input reset : UInt<1>
    output io : {app : {flip cmd : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<32>[16], isWr : UInt<1>, size : UInt<32>}}, flip wdata : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>[16]}, rdata : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>[16]}}[0], dram : {cmd : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<32>, isWr : UInt<1>, tag : UInt<32>, streamId : UInt<32>, wdata : UInt<32>[16]}}, flip resp : {flip ready : UInt<1>, valid : UInt<1>, bits : {rdata : UInt<32>[16], tag : UInt<32>, streamId : UInt<32>}}}, flip config : {scatterGather : UInt<1>}}
    
    io is invalid
    io is invalid
    inst addrFifo of FIFOArbiter @[MAGCore.scala 119:24]
    addrFifo.io is invalid
    addrFifo.clock <= clock
    addrFifo.reset <= reset
    wire addrFifoConfig : {chainWrite : UInt<1>, chainRead : UInt<1>} @[MAGCore.scala 120:28]
    addrFifoConfig is invalid @[MAGCore.scala 120:28]
    addrFifoConfig.chainRead <= UInt<1>("h01") @[MAGCore.scala 121:28]
    node _T_536 = not(io.config.scatterGather) @[MAGCore.scala 122:32]
    addrFifoConfig.chainWrite <= _T_536 @[MAGCore.scala 122:29]
    addrFifo.io.config.chainRead <= addrFifoConfig.chainRead @[MAGCore.scala 123:22]
    addrFifo.io.config.chainWrite <= addrFifoConfig.chainWrite @[MAGCore.scala 123:22]
    addrFifo.io.forceTag.valid <= UInt<1>("h00") @[MAGCore.scala 125:30]
    node burstAddrs_0 = bits(addrFifo.io.deq[0], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_1 = bits(addrFifo.io.deq[1], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_2 = bits(addrFifo.io.deq[2], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_3 = bits(addrFifo.io.deq[3], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_4 = bits(addrFifo.io.deq[4], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_5 = bits(addrFifo.io.deq[5], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_6 = bits(addrFifo.io.deq[6], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_7 = bits(addrFifo.io.deq[7], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_8 = bits(addrFifo.io.deq[8], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_9 = bits(addrFifo.io.deq[9], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_10 = bits(addrFifo.io.deq[10], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_11 = bits(addrFifo.io.deq[11], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_12 = bits(addrFifo.io.deq[12], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_13 = bits(addrFifo.io.deq[13], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_14 = bits(addrFifo.io.deq[14], 31, 6) @[MAGCore.scala 104:9]
    node burstAddrs_15 = bits(addrFifo.io.deq[15], 31, 6) @[MAGCore.scala 104:9]
    node wordOffsets_0 = bits(addrFifo.io.deq[0], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_1 = bits(addrFifo.io.deq[1], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_2 = bits(addrFifo.io.deq[2], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_3 = bits(addrFifo.io.deq[3], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_4 = bits(addrFifo.io.deq[4], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_5 = bits(addrFifo.io.deq[5], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_6 = bits(addrFifo.io.deq[6], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_7 = bits(addrFifo.io.deq[7], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_8 = bits(addrFifo.io.deq[8], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_9 = bits(addrFifo.io.deq[9], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_10 = bits(addrFifo.io.deq[10], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_11 = bits(addrFifo.io.deq[11], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_12 = bits(addrFifo.io.deq[12], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_13 = bits(addrFifo.io.deq[13], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_14 = bits(addrFifo.io.deq[14], 6, 5) @[MAGCore.scala 115:9]
    node wordOffsets_15 = bits(addrFifo.io.deq[15], 6, 5) @[MAGCore.scala 115:9]
    inst isWrFifo of FIFOArbiter_1 @[MAGCore.scala 134:24]
    isWrFifo.io is invalid
    isWrFifo.clock <= clock
    isWrFifo.reset <= reset
    wire isWrFifoConfig : {chainWrite : UInt<1>, chainRead : UInt<1>} @[MAGCore.scala 135:28]
    isWrFifoConfig is invalid @[MAGCore.scala 135:28]
    isWrFifoConfig.chainRead <= UInt<1>("h01") @[MAGCore.scala 136:28]
    isWrFifoConfig.chainWrite <= UInt<1>("h01") @[MAGCore.scala 137:29]
    isWrFifo.io.config.chainRead <= isWrFifoConfig.chainRead @[MAGCore.scala 138:22]
    isWrFifo.io.config.chainWrite <= isWrFifoConfig.chainWrite @[MAGCore.scala 138:22]
    isWrFifo.io.forceTag.valid <= UInt<1>("h00") @[MAGCore.scala 140:30]
    inst sizeFifo of FIFOArbiter_2 @[MAGCore.scala 145:24]
    sizeFifo.io is invalid
    sizeFifo.clock <= clock
    sizeFifo.reset <= reset
    wire sizeFifoConfig : {chainWrite : UInt<1>, chainRead : UInt<1>} @[MAGCore.scala 146:28]
    sizeFifoConfig is invalid @[MAGCore.scala 146:28]
    sizeFifoConfig.chainRead <= UInt<1>("h01") @[MAGCore.scala 147:28]
    sizeFifoConfig.chainWrite <= UInt<1>("h01") @[MAGCore.scala 148:29]
    sizeFifo.io.config.chainRead <= sizeFifoConfig.chainRead @[MAGCore.scala 149:22]
    sizeFifo.io.config.chainWrite <= sizeFifoConfig.chainWrite @[MAGCore.scala 149:22]
    sizeFifo.io.forceTag.valid <= UInt<1>("h00") @[MAGCore.scala 150:30]
    node _T_554 = bits(sizeFifo.io.deq[0], 31, 6) @[MAGCore.scala 104:9]
    node _T_555 = bits(sizeFifo.io.deq[0], 5, 0) @[MAGCore.scala 109:9]
    node _T_557 = neq(_T_555, UInt<1>("h00")) @[MAGCore.scala 163:79]
    node _T_558 = add(_T_554, _T_557) @[MAGCore.scala 163:48]
    node sizeInBursts = tail(_T_558, 1) @[MAGCore.scala 163:48]
    inst dataFifo of FIFOArbiter_3 @[MAGCore.scala 170:24]
    dataFifo.io is invalid
    dataFifo.clock <= clock
    dataFifo.reset <= reset
    wire dataFifoConfig : {chainWrite : UInt<1>, chainRead : UInt<1>} @[MAGCore.scala 171:28]
    dataFifoConfig is invalid @[MAGCore.scala 171:28]
    dataFifoConfig.chainRead <= UInt<1>("h00") @[MAGCore.scala 172:28]
    dataFifoConfig.chainWrite <= io.config.scatterGather @[MAGCore.scala 173:29]
    dataFifo.io.config.chainRead <= dataFifoConfig.chainRead @[MAGCore.scala 174:22]
    dataFifo.io.config.chainWrite <= dataFifoConfig.chainWrite @[MAGCore.scala 174:22]
    inst burstCounter of Counter @[MAGCore.scala 176:28]
    burstCounter.io is invalid
    burstCounter.clock <= clock
    burstCounter.reset <= reset
    inst wrPhase of SRFF_2 @[MAGCore.scala 177:23]
    wrPhase.io is invalid
    wrPhase.clock <= clock
    wrPhase.reset <= reset
    node _T_565 = not(sizeFifo.io.empty) @[MAGCore.scala 179:18]
    node _T_566 = bits(isWrFifo.io.deq[0], 0, 0) @[MAGCore.scala 179:112]
    node _T_567 = or(wrPhase.io.output.data, _T_566) @[MAGCore.scala 179:91]
    node _T_568 = not(dataFifo.io.empty) @[MAGCore.scala 179:118]
    node _T_570 = mux(_T_567, _T_568, UInt<1>("h01")) @[MAGCore.scala 179:67]
    node burstVld = and(_T_565, _T_570) @[MAGCore.scala 179:62]
    dataFifo.io.forceTag.bits <= addrFifo.io.tag @[MAGCore.scala 182:29]
    dataFifo.io.forceTag.valid <= UInt<1>("h01") @[MAGCore.scala 183:30]
    node _T_573 = mux(io.config.scatterGather, UInt<1>("h01"), sizeInBursts) @[MAGCore.scala 188:29]
    burstCounter.io.max <= _T_573 @[MAGCore.scala 188:23]
    burstCounter.io.stride <= UInt<1>("h01") @[MAGCore.scala 189:26]
    burstCounter.io.reset <= UInt<1>("h00") @[MAGCore.scala 190:25]
    node _T_576 = not(addrFifo.io.empty) @[MAGCore.scala 191:58]
    node _T_577 = mux(io.config.scatterGather, _T_576, burstVld) @[MAGCore.scala 191:32]
    node _T_578 = and(_T_577, io.dram.cmd.ready) @[MAGCore.scala 191:88]
    burstCounter.io.enable <= _T_578 @[MAGCore.scala 191:26]
    burstCounter.io.saturate <= UInt<1>("h00") @[MAGCore.scala 192:28]
    inst burstTagCounter of Counter_1 @[MAGCore.scala 195:31]
    burstTagCounter.io is invalid
    burstTagCounter.clock <= clock
    burstTagCounter.reset <= reset
    burstTagCounter.io.max <= UInt<11>("h0400") @[MAGCore.scala 196:26]
    burstTagCounter.io.stride <= UInt<1>("h01") @[MAGCore.scala 197:29]
    burstTagCounter.io.reset <= UInt<1>("h00") @[MAGCore.scala 198:28]
    node _T_583 = not(addrFifo.io.empty) @[MAGCore.scala 199:61]
    node _T_584 = mux(io.config.scatterGather, _T_583, burstVld) @[MAGCore.scala 199:35]
    node _T_585 = and(_T_584, io.dram.cmd.ready) @[MAGCore.scala 199:91]
    burstTagCounter.io.enable <= _T_585 @[MAGCore.scala 199:29]
    burstCounter.io.saturate <= UInt<1>("h00") @[MAGCore.scala 200:28]
    node elementID = bits(burstTagCounter.io.out, 3, 0) @[MAGCore.scala 201:41]
    addrFifo.io.deqVld <= burstCounter.io.done @[MAGCore.scala 213:22]
    isWrFifo.io.deqVld <= burstCounter.io.done @[MAGCore.scala 214:22]
    sizeFifo.io.deqVld <= burstCounter.io.done @[MAGCore.scala 215:22]
    node _T_587 = and(burstVld, isWrFifo.io.deq[0]) @[MAGCore.scala 216:34]
    node _T_588 = and(_T_587, io.dram.cmd.ready) @[MAGCore.scala 216:55]
    dataFifo.io.deqVld <= _T_588 @[MAGCore.scala 216:22]
    wire tagOut : {streamTag : UInt<1>, burstTag : UInt<31>} @[MAGCore.scala 321:20]
    tagOut is invalid @[MAGCore.scala 321:20]
    tagOut.streamTag <= addrFifo.io.tag @[MAGCore.scala 322:20]
    node _T_594 = mux(io.config.scatterGather, burstAddrs_0, burstTagCounter.io.out) @[MAGCore.scala 323:25]
    tagOut.burstTag <= _T_594 @[MAGCore.scala 323:19]
    node _T_595 = add(burstAddrs_0, burstCounter.io.out) @[MAGCore.scala 325:47]
    node _T_596 = tail(_T_595, 1) @[MAGCore.scala 325:47]
    node _T_598 = cat(_T_596, UInt<6>("h00")) @[Cat.scala 30:58]
    io.dram.cmd.bits.addr <= _T_598 @[MAGCore.scala 325:25]
    node _T_599 = cat(tagOut.streamTag, tagOut.burstTag) @[Cat.scala 30:58]
    io.dram.cmd.bits.tag <= _T_599 @[MAGCore.scala 326:24]
    io.dram.cmd.bits.streamId <= tagOut.streamTag @[MAGCore.scala 327:29]
    io.dram.cmd.bits.wdata[0] <= dataFifo.io.deq[0] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[1] <= dataFifo.io.deq[1] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[2] <= dataFifo.io.deq[2] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[3] <= dataFifo.io.deq[3] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[4] <= dataFifo.io.deq[4] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[5] <= dataFifo.io.deq[5] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[6] <= dataFifo.io.deq[6] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[7] <= dataFifo.io.deq[7] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[8] <= dataFifo.io.deq[8] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[9] <= dataFifo.io.deq[9] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[10] <= dataFifo.io.deq[10] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[11] <= dataFifo.io.deq[11] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[12] <= dataFifo.io.deq[12] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[13] <= dataFifo.io.deq[13] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[14] <= dataFifo.io.deq[14] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.wdata[15] <= dataFifo.io.deq[15] @[MAGCore.scala 328:26]
    io.dram.cmd.bits.isWr <= isWrFifo.io.deq[0] @[MAGCore.scala 330:25]
    wrPhase.io.input.set <= isWrFifo.io.deq[0] @[MAGCore.scala 331:24]
    reg _T_602 : UInt, clock with : (reset => (reset, UInt<1>("h00"))) @[Utils.scala 13:53]
    node _T_605 = mux(burstVld, UInt<1>("h01"), UInt<1>("h00")) @[Utils.scala 16:25]
    _T_602 <= _T_605 @[Utils.scala 16:19]
    node _T_607 = eq(_T_602, UInt<1>("h01")) @[Utils.scala 20:27]
    wrPhase.io.input.reset <= _T_607 @[MAGCore.scala 332:26]
    io.dram.cmd.valid <= burstVld @[MAGCore.scala 333:21]
    node streamTagFromDRAM = bits(io.dram.resp.bits.tag, 31, 31) @[MAGCore.scala 319:32]
    
  module Fringe : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip raddr : UInt<1>, flip wen : UInt<1>, flip waddr : UInt<1>, flip wdata : UInt<32>, rdata : UInt<32>, enable : UInt<1>, flip done : UInt<1>, argIns : UInt<32>[0], flip argOuts : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>}[0], memStreams : {flip cmd : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<32>[16], isWr : UInt<1>, size : UInt<32>}}, flip wdata : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>[16]}, rdata : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>[16]}}[0], dram : {cmd : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<32>, isWr : UInt<1>, tag : UInt<32>, streamId : UInt<32>, wdata : UInt<32>[16]}}, flip resp : {flip ready : UInt<1>, valid : UInt<1>, bits : {rdata : UInt<32>[16], tag : UInt<32>, streamId : UInt<32>}}}}
    
    io is invalid
    io is invalid
    inst regs of RegFile @[Fringe.scala 48:20]
    regs.io is invalid
    regs.clock <= clock
    regs.reset <= reset
    regs.io.raddr <= io.raddr @[Fringe.scala 49:17]
    regs.io.waddr <= io.waddr @[Fringe.scala 50:17]
    regs.io.wen <= io.wen @[Fringe.scala 51:15]
    regs.io.wdata <= io.wdata @[Fringe.scala 52:17]
    io.rdata <= regs.io.rdata @[Fringe.scala 53:12]
    node _T_570 = bits(regs.io.argIns[0], 0, 0) @[Fringe.scala 57:23]
    node _T_571 = bits(regs.io.argIns[1], 0, 0) @[Fringe.scala 57:39]
    node _T_572 = not(_T_571) @[Fringe.scala 57:29]
    node _T_573 = and(_T_570, _T_572) @[Fringe.scala 57:27]
    io.enable <= _T_573 @[Fringe.scala 57:13]
    inst depulser of Depulser @[Fringe.scala 60:24]
    depulser.io is invalid
    depulser.clock <= clock
    depulser.reset <= reset
    depulser.io.in <= io.done @[Fringe.scala 61:18]
    wire status : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>} @[Fringe.scala 62:20]
    status is invalid @[Fringe.scala 62:20]
    node _T_588 = and(regs.io.argIns[0], depulser.io.out) @[Fringe.scala 63:26]
    status.bits <= _T_588 @[Fringe.scala 63:15]
    status.valid <= depulser.io.out @[Fringe.scala 64:16]
    regs.io.argOuts[0].bits <= status.bits @[Fringe.scala 70:22]
    regs.io.argOuts[0].valid <= status.valid @[Fringe.scala 71:23]
    inst mag of MAGCore @[Fringe.scala 79:19]
    mag.io is invalid
    mag.clock <= clock
    mag.reset <= reset
    wire magConfig : {scatterGather : UInt<1>} @[Fringe.scala 80:23]
    magConfig is invalid @[Fringe.scala 80:23]
    magConfig.scatterGather <= UInt<1>("h00") @[Fringe.scala 81:27]
    mag.io.config.scatterGather <= magConfig.scatterGather @[Fringe.scala 82:17]
    io.dram.cmd.bits.wdata[0] <= mag.io.dram.cmd.bits.wdata[0] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[1] <= mag.io.dram.cmd.bits.wdata[1] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[2] <= mag.io.dram.cmd.bits.wdata[2] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[3] <= mag.io.dram.cmd.bits.wdata[3] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[4] <= mag.io.dram.cmd.bits.wdata[4] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[5] <= mag.io.dram.cmd.bits.wdata[5] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[6] <= mag.io.dram.cmd.bits.wdata[6] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[7] <= mag.io.dram.cmd.bits.wdata[7] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[8] <= mag.io.dram.cmd.bits.wdata[8] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[9] <= mag.io.dram.cmd.bits.wdata[9] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[10] <= mag.io.dram.cmd.bits.wdata[10] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[11] <= mag.io.dram.cmd.bits.wdata[11] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[12] <= mag.io.dram.cmd.bits.wdata[12] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[13] <= mag.io.dram.cmd.bits.wdata[13] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[14] <= mag.io.dram.cmd.bits.wdata[14] @[Fringe.scala 90:20]
    io.dram.cmd.bits.wdata[15] <= mag.io.dram.cmd.bits.wdata[15] @[Fringe.scala 90:20]
    io.dram.cmd.bits.streamId <= mag.io.dram.cmd.bits.streamId @[Fringe.scala 90:20]
    io.dram.cmd.bits.tag <= mag.io.dram.cmd.bits.tag @[Fringe.scala 90:20]
    io.dram.cmd.bits.isWr <= mag.io.dram.cmd.bits.isWr @[Fringe.scala 90:20]
    io.dram.cmd.bits.addr <= mag.io.dram.cmd.bits.addr @[Fringe.scala 90:20]
    io.dram.cmd.valid <= mag.io.dram.cmd.valid @[Fringe.scala 91:21]
    mag.io.dram.cmd.ready <= io.dram.cmd.ready @[Fringe.scala 92:25]
    mag.io.dram.resp.bits.streamId <= io.dram.resp.bits.streamId @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.tag <= io.dram.resp.bits.tag @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[0] <= io.dram.resp.bits.rdata[0] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[1] <= io.dram.resp.bits.rdata[1] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[2] <= io.dram.resp.bits.rdata[2] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[3] <= io.dram.resp.bits.rdata[3] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[4] <= io.dram.resp.bits.rdata[4] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[5] <= io.dram.resp.bits.rdata[5] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[6] <= io.dram.resp.bits.rdata[6] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[7] <= io.dram.resp.bits.rdata[7] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[8] <= io.dram.resp.bits.rdata[8] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[9] <= io.dram.resp.bits.rdata[9] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[10] <= io.dram.resp.bits.rdata[10] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[11] <= io.dram.resp.bits.rdata[11] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[12] <= io.dram.resp.bits.rdata[12] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[13] <= io.dram.resp.bits.rdata[13] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[14] <= io.dram.resp.bits.rdata[14] @[Fringe.scala 94:25]
    mag.io.dram.resp.bits.rdata[15] <= io.dram.resp.bits.rdata[15] @[Fringe.scala 94:25]
    mag.io.dram.resp.valid <= io.dram.resp.valid @[Fringe.scala 95:26]
    io.dram.resp.ready <= mag.io.dram.cmd.ready @[Fringe.scala 96:22]
    
  module FringeDE1SoC : 
    input clock : Clock
    input reset : UInt<1>
    output io : {S_AVALON : {readdata : UInt<32>, flip address : UInt<16>, flip chipselect : UInt<1>, flip write_n : UInt<1>, flip writedata : UInt<32>}, enable : UInt<1>, flip done : UInt<1>, argIns : UInt<32>[0], flip argOuts : {flip ready : UInt<1>, valid : UInt<1>, bits : UInt<32>}[0]}
    
    io is invalid
    io is invalid
    inst fringeCommon of Fringe @[FringeDE1SoC.scala 30:28]
    fringeCommon.io is invalid
    fringeCommon.clock <= clock
    fringeCommon.reset <= reset
    fringeCommon.reset <= reset @[FringeDE1SoC.scala 34:22]
    fringeCommon.io.raddr <= io.S_AVALON.address @[FringeDE1SoC.scala 35:25]
    node _T_46 = not(io.S_AVALON.write_n) @[FringeDE1SoC.scala 36:28]
    node _T_47 = and(_T_46, io.S_AVALON.chipselect) @[FringeDE1SoC.scala 36:49]
    fringeCommon.io.wen <= _T_47 @[FringeDE1SoC.scala 36:25]
    fringeCommon.io.waddr <= io.S_AVALON.address @[FringeDE1SoC.scala 38:25]
    fringeCommon.io.wdata <= io.S_AVALON.writedata @[FringeDE1SoC.scala 39:25]
    io.S_AVALON.readdata <= fringeCommon.io.rdata @[FringeDE1SoC.scala 40:24]
    io.enable <= fringeCommon.io.enable @[FringeDE1SoC.scala 42:13]
    fringeCommon.io.done <= io.done @[FringeDE1SoC.scala 43:24]
    
  module Top : 
    input clock : Clock
    input reset : UInt<1>
    output io : {flip raddr : UInt<1>, flip wen : UInt<1>, flip waddr : UInt<1>, flip wdata : UInt<1>, rdata : UInt<1>, S_AVALON : {readdata : UInt<32>, flip address : UInt<16>, flip chipselect : UInt<1>, flip write_n : UInt<1>, flip writedata : UInt<32>}, S_STREAM : {flip stream_in_data : UInt<24>, flip stream_in_startofpacket : UInt<1>, flip stream_in_endofpacket : UInt<1>, flip stream_in_empty : UInt<2>, flip stream_in_valid : UInt<1>, flip stream_out_ready : UInt<1>, stream_in_ready : UInt<1>, stream_out_data : UInt<16>, stream_out_startofpacket : UInt<1>, stream_out_endofpacket : UInt<1>, stream_out_empty : UInt<1>, stream_out_valid : UInt<1>}}
    
    io is invalid
    io is invalid
    inst accel of AccelTop @[Top.scala 88:21]
    accel.io is invalid
    accel.clock <= clock
    accel.reset <= reset
    inst FringeDE1SoC of FringeDE1SoC @[Top.scala 117:26]
    FringeDE1SoC.io is invalid
    FringeDE1SoC.clock <= clock
    FringeDE1SoC.reset <= reset
    FringeDE1SoC.io.S_AVALON.writedata <= io.S_AVALON.writedata @[Top.scala 121:26]
    FringeDE1SoC.io.S_AVALON.write_n <= io.S_AVALON.write_n @[Top.scala 121:26]
    FringeDE1SoC.io.S_AVALON.chipselect <= io.S_AVALON.chipselect @[Top.scala 121:26]
    FringeDE1SoC.io.S_AVALON.address <= io.S_AVALON.address @[Top.scala 121:26]
    io.S_AVALON.readdata <= FringeDE1SoC.io.S_AVALON.readdata @[Top.scala 121:26]
    accel.io.stream_in_data <= io.S_STREAM.stream_in_data @[Top.scala 126:47]
    accel.io.stream_in_startofpacket <= io.S_STREAM.stream_in_startofpacket @[Top.scala 127:47]
    accel.io.stream_in_endofpacket <= io.S_STREAM.stream_in_endofpacket @[Top.scala 128:47]
    accel.io.stream_in_empty <= io.S_STREAM.stream_in_empty @[Top.scala 129:47]
    accel.io.stream_in_valid <= io.S_STREAM.stream_in_valid @[Top.scala 130:47]
    accel.io.stream_out_ready <= io.S_STREAM.stream_out_ready @[Top.scala 131:47]
    io.S_STREAM.stream_in_ready <= accel.io.stream_in_ready @[Top.scala 134:47]
    io.S_STREAM.stream_out_data <= accel.io.stream_out_data @[Top.scala 135:47]
    io.S_STREAM.stream_out_startofpacket <= accel.io.stream_out_startofpacket @[Top.scala 136:47]
    io.S_STREAM.stream_out_endofpacket <= accel.io.stream_out_endofpacket @[Top.scala 137:47]
    io.S_STREAM.stream_out_empty <= accel.io.stream_out_empty @[Top.scala 138:47]
    io.S_STREAM.stream_out_valid <= accel.io.stream_out_valid @[Top.scala 139:47]
    accel.io.enable <= FringeDE1SoC.io.enable @[Top.scala 146:23]
    FringeDE1SoC.io.done <= accel.io.done @[Top.scala 147:22]
    accel.reset <= reset @[Top.scala 149:19]
    
