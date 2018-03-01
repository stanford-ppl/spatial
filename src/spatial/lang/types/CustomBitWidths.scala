package spatial.lang.types

sealed trait BOOL[T] {
  val v: Boolean
  override def toString: String = if (v) "TRUE" else "FALSE"
}
sealed trait INT[T] {
  val v: Int
  override def equals(x: Any): Boolean = x match {
    case that: INT[_] => this.v == that.v
    case _ => false
  }
  override def toString = s"_$v"
}

trait TRUE
trait FALSE

trait _0
trait _1
trait _2
trait _3
trait _4
trait _5
trait _6
trait _7
trait _8
trait _9
trait _10
trait _11
trait _12
trait _13
trait _14
trait _15
trait _16
trait _17
trait _18
trait _19
trait _20
trait _21
trait _22
trait _23
trait _24
trait _25
trait _26
trait _27
trait _28
trait _29
trait _30
trait _31
trait _32
trait _33
trait _34
trait _35
trait _36
trait _37
trait _38
trait _39
trait _40
trait _41
trait _42
trait _43
trait _44
trait _45
trait _46
trait _47
trait _48
trait _49
trait _50
trait _51
trait _52
trait _53
trait _54
trait _55
trait _56
trait _57
trait _58
trait _59
trait _60
trait _61
trait _62
trait _63
trait _64
trait _65
trait _66
trait _67
trait _68
trait _69
trait _70
trait _71
trait _72
trait _73
trait _74
trait _75
trait _76
trait _77
trait _78
trait _79
trait _80
trait _81
trait _82
trait _83
trait _84
trait _85
trait _86
trait _87
trait _88
trait _89
trait _90
trait _91
trait _92
trait _93
trait _94
trait _95
trait _96
trait _97
trait _98
trait _99
trait _100
trait _101
trait _102
trait _103
trait _104
trait _105
trait _106
trait _107
trait _108
trait _109
trait _110
trait _111
trait _112
trait _113
trait _114
trait _115
trait _116
trait _117
trait _118
trait _119
trait _120
trait _121
trait _122
trait _123
trait _124
trait _125
trait _126
trait _127
trait _128

/** Hack for working with customized bit widths, since Scala doesn't support integers as template parameters **/
object BOOL {
  def apply[T:BOOL]: BOOL[T] = implicitly[BOOL[T]]

  implicit object BOOL_TRUE extends BOOL[TRUE] { val v = true }
  implicit object BOOL_FALSE extends BOOL[FALSE] { val v = false }
}

object INT {
  def apply[T](implicit int: INT[T]): INT[T] = int
  def from[T](x: Int): INT[T] = new INT[T] { val v: Int = x }

  implicit object INT0  extends INT[_0]  { val v = 0  }
  implicit object INT1  extends INT[_1]  { val v = 1  }
  implicit object INT2  extends INT[_2]  { val v = 2  }
  implicit object INT3  extends INT[_3]  { val v = 3  }
  implicit object INT4  extends INT[_4]  { val v = 4  }
  implicit object INT5  extends INT[_5]  { val v = 5  }
  implicit object INT6  extends INT[_6]  { val v = 6  }
  implicit object INT7  extends INT[_7]  { val v = 7  }
  implicit object INT8  extends INT[_8]  { val v = 8  }
  implicit object INT9  extends INT[_9]  { val v = 9  }
  implicit object INT10 extends INT[_10] { val v = 10 }
  implicit object INT11 extends INT[_11] { val v = 11 }
  implicit object INT12 extends INT[_12] { val v = 12 }
  implicit object INT13 extends INT[_13] { val v = 13 }
  implicit object INT14 extends INT[_14] { val v = 14 }
  implicit object INT15 extends INT[_15] { val v = 15 }
  implicit object INT16 extends INT[_16] { val v = 16 }
  implicit object INT17 extends INT[_17] { val v = 17 }
  implicit object INT18 extends INT[_18] { val v = 18 }
  implicit object INT19 extends INT[_19] { val v = 19 }
  implicit object INT20 extends INT[_20] { val v = 20 }
  implicit object INT21 extends INT[_21] { val v = 21 }
  implicit object INT22 extends INT[_22] { val v = 22 }
  implicit object INT23 extends INT[_23] { val v = 23 }
  implicit object INT24 extends INT[_24] { val v = 24 }
  implicit object INT25 extends INT[_25] { val v = 25 }
  implicit object INT26 extends INT[_26] { val v = 26 }
  implicit object INT27 extends INT[_27] { val v = 27 }
  implicit object INT28 extends INT[_28] { val v = 28 }
  implicit object INT29 extends INT[_29] { val v = 29 }
  implicit object INT30 extends INT[_30] { val v = 30 }
  implicit object INT31 extends INT[_31] { val v = 31 }
  implicit object INT32 extends INT[_32] { val v = 32 }
  implicit object INT33 extends INT[_33] { val v = 33 }
  implicit object INT34 extends INT[_34] { val v = 34 }
  implicit object INT35 extends INT[_35] { val v = 35 }
  implicit object INT36 extends INT[_36] { val v = 36 }
  implicit object INT37 extends INT[_37] { val v = 37 }
  implicit object INT38 extends INT[_38] { val v = 38 }
  implicit object INT39 extends INT[_39] { val v = 39 }
  implicit object INT40 extends INT[_40] { val v = 40 }
  implicit object INT41 extends INT[_41] { val v = 41 }
  implicit object INT42 extends INT[_42] { val v = 42 }
  implicit object INT43 extends INT[_43] { val v = 43 }
  implicit object INT44 extends INT[_44] { val v = 44 }
  implicit object INT45 extends INT[_45] { val v = 45 }
  implicit object INT46 extends INT[_46] { val v = 46 }
  implicit object INT47 extends INT[_47] { val v = 47 }
  implicit object INT48 extends INT[_48] { val v = 48 }
  implicit object INT49 extends INT[_49] { val v = 49 }
  implicit object INT50 extends INT[_50] { val v = 50 }
  implicit object INT51 extends INT[_51] { val v = 51 }
  implicit object INT52 extends INT[_52] { val v = 52 }
  implicit object INT53 extends INT[_53] { val v = 53 }
  implicit object INT54 extends INT[_54] { val v = 54 }
  implicit object INT55 extends INT[_55] { val v = 55 }
  implicit object INT56 extends INT[_56] { val v = 56 }
  implicit object INT57 extends INT[_57] { val v = 57 }
  implicit object INT58 extends INT[_58] { val v = 58 }
  implicit object INT59 extends INT[_59] { val v = 59 }
  implicit object INT60 extends INT[_60] { val v = 60 }
  implicit object INT61 extends INT[_61] { val v = 61 }
  implicit object INT62 extends INT[_62] { val v = 62 }
  implicit object INT63 extends INT[_63] { val v = 63 }
  implicit object INT64 extends INT[_64] { val v = 64 }
  implicit object INT65 extends INT[_65] { val v = 65 }
  implicit object INT66 extends INT[_66] { val v = 66 }
  implicit object INT67 extends INT[_67] { val v = 67 }
  implicit object INT68 extends INT[_68] { val v = 68 }
  implicit object INT69 extends INT[_69] { val v = 69 }
  implicit object INT70 extends INT[_70] { val v = 70 }
  implicit object INT71 extends INT[_71] { val v = 71 }
  implicit object INT72 extends INT[_72] { val v = 72 }
  implicit object INT73 extends INT[_73] { val v = 73 }
  implicit object INT74 extends INT[_74] { val v = 74 }
  implicit object INT75 extends INT[_75] { val v = 75 }
  implicit object INT76 extends INT[_76] { val v = 76 }
  implicit object INT77 extends INT[_77] { val v = 77 }
  implicit object INT78 extends INT[_78] { val v = 78 }
  implicit object INT79 extends INT[_79] { val v = 79 }
  implicit object INT80 extends INT[_80] { val v = 80 }
  implicit object INT81 extends INT[_81] { val v = 81 }
  implicit object INT82 extends INT[_82] { val v = 82 }
  implicit object INT83 extends INT[_83] { val v = 83 }
  implicit object INT84 extends INT[_84] { val v = 84 }
  implicit object INT85 extends INT[_85] { val v = 85 }
  implicit object INT86 extends INT[_86] { val v = 86 }
  implicit object INT87 extends INT[_87] { val v = 87 }
  implicit object INT88 extends INT[_88] { val v = 88 }
  implicit object INT89 extends INT[_89] { val v = 89 }
  implicit object INT90 extends INT[_90] { val v = 90 }
  implicit object INT91 extends INT[_91] { val v = 91 }
  implicit object INT92 extends INT[_92] { val v = 92 }
  implicit object INT93 extends INT[_93] { val v = 93 }
  implicit object INT94 extends INT[_94] { val v = 94 }
  implicit object INT95 extends INT[_95] { val v = 95 }
  implicit object INT96 extends INT[_96] { val v = 96 }
  implicit object INT97 extends INT[_97] { val v = 97 }
  implicit object INT98 extends INT[_98] { val v = 98 }
  implicit object INT99 extends INT[_99] { val v = 99 }

  implicit object INT100 extends INT[_100] { val v = 100 }
  implicit object INT101 extends INT[_101] { val v = 101 }
  implicit object INT102 extends INT[_102] { val v = 102 }
  implicit object INT103 extends INT[_103] { val v = 103 }
  implicit object INT104 extends INT[_104] { val v = 104 }
  implicit object INT105 extends INT[_105] { val v = 105 }
  implicit object INT106 extends INT[_106] { val v = 106 }
  implicit object INT107 extends INT[_107] { val v = 107 }
  implicit object INT108 extends INT[_108] { val v = 108 }
  implicit object INT109 extends INT[_109] { val v = 109 }
  implicit object INT110 extends INT[_110] { val v = 110 }
  implicit object INT111 extends INT[_111] { val v = 111 }
  implicit object INT112 extends INT[_112] { val v = 112 }
  implicit object INT113 extends INT[_113] { val v = 113 }
  implicit object INT114 extends INT[_114] { val v = 114 }
  implicit object INT115 extends INT[_115] { val v = 115 }
  implicit object INT116 extends INT[_116] { val v = 116 }
  implicit object INT117 extends INT[_117] { val v = 117 }
  implicit object INT118 extends INT[_118] { val v = 118 }
  implicit object INT119 extends INT[_119] { val v = 119 }
  implicit object INT120 extends INT[_120] { val v = 120 }
  implicit object INT121 extends INT[_121] { val v = 121 }
  implicit object INT122 extends INT[_122] { val v = 122 }
  implicit object INT123 extends INT[_123] { val v = 123 }
  implicit object INT124 extends INT[_124] { val v = 124 }
  implicit object INT125 extends INT[_125] { val v = 125 }
  implicit object INT126 extends INT[_126] { val v = 126 }
  implicit object INT127 extends INT[_127] { val v = 127 }
  implicit object INT128 extends INT[_128] { val v = 128 }
}

trait CustomBitWidths {
  type INT[T] = spatial.lang.types.INT[T]
  lazy val INT = spatial.lang.types.INT
  type BOOL[T] = spatial.lang.types.BOOL[T]
  lazy val BOOL = spatial.lang.types.BOOL

  type TRUE = spatial.lang.types.TRUE
  type FALSE = spatial.lang.types.FALSE

  type _0 = spatial.lang.types._0
  type _1 = spatial.lang.types._1
  type _2 = spatial.lang.types._2
  type _3 = spatial.lang.types._3
  type _4 = spatial.lang.types._4
  type _5 = spatial.lang.types._5
  type _6 = spatial.lang.types._6
  type _7 = spatial.lang.types._7
  type _8 = spatial.lang.types._8
  type _9 = spatial.lang.types._9
  type _10 = spatial.lang.types._10
  type _11 = spatial.lang.types._11
  type _12 = spatial.lang.types._12
  type _13 = spatial.lang.types._13
  type _14 = spatial.lang.types._14
  type _15 = spatial.lang.types._15
  type _16 = spatial.lang.types._16
  type _17 = spatial.lang.types._17
  type _18 = spatial.lang.types._18
  type _19 = spatial.lang.types._19
  type _20 = spatial.lang.types._20
  type _21 = spatial.lang.types._21
  type _22 = spatial.lang.types._22
  type _23 = spatial.lang.types._23
  type _24 = spatial.lang.types._24
  type _25 = spatial.lang.types._25
  type _26 = spatial.lang.types._26
  type _27 = spatial.lang.types._27
  type _28 = spatial.lang.types._28
  type _29 = spatial.lang.types._29
  type _30 = spatial.lang.types._30
  type _31 = spatial.lang.types._31
  type _32 = spatial.lang.types._32
  type _33 = spatial.lang.types._33
  type _34 = spatial.lang.types._34
  type _35 = spatial.lang.types._35
  type _36 = spatial.lang.types._36
  type _37 = spatial.lang.types._37
  type _38 = spatial.lang.types._38
  type _39 = spatial.lang.types._39
  type _40 = spatial.lang.types._40
  type _41 = spatial.lang.types._41
  type _42 = spatial.lang.types._42
  type _43 = spatial.lang.types._43
  type _44 = spatial.lang.types._44
  type _45 = spatial.lang.types._45
  type _46 = spatial.lang.types._46
  type _47 = spatial.lang.types._47
  type _48 = spatial.lang.types._48
  type _49 = spatial.lang.types._49
  type _50 = spatial.lang.types._50
  type _51 = spatial.lang.types._51
  type _52 = spatial.lang.types._52
  type _53 = spatial.lang.types._53
  type _54 = spatial.lang.types._54
  type _55 = spatial.lang.types._55
  type _56 = spatial.lang.types._56
  type _57 = spatial.lang.types._57
  type _58 = spatial.lang.types._58
  type _59 = spatial.lang.types._59
  type _60 = spatial.lang.types._60
  type _61 = spatial.lang.types._61
  type _62 = spatial.lang.types._62
  type _63 = spatial.lang.types._63
  type _64 = spatial.lang.types._64
  type _65 = spatial.lang.types._65
  type _66 = spatial.lang.types._66
  type _67 = spatial.lang.types._67
  type _68 = spatial.lang.types._68
  type _69 = spatial.lang.types._69
  type _70 = spatial.lang.types._70
  type _71 = spatial.lang.types._71
  type _72 = spatial.lang.types._72
  type _73 = spatial.lang.types._73
  type _74 = spatial.lang.types._74
  type _75 = spatial.lang.types._75
  type _76 = spatial.lang.types._76
  type _77 = spatial.lang.types._77
  type _78 = spatial.lang.types._78
  type _79 = spatial.lang.types._79
  type _80 = spatial.lang.types._80
  type _81 = spatial.lang.types._81
  type _82 = spatial.lang.types._82
  type _83 = spatial.lang.types._83
  type _84 = spatial.lang.types._84
  type _85 = spatial.lang.types._85
  type _86 = spatial.lang.types._86
  type _87 = spatial.lang.types._87
  type _88 = spatial.lang.types._88
  type _89 = spatial.lang.types._89
  type _90 = spatial.lang.types._90
  type _91 = spatial.lang.types._91
  type _92 = spatial.lang.types._92
  type _93 = spatial.lang.types._93
  type _94 = spatial.lang.types._94
  type _95 = spatial.lang.types._95
  type _96 = spatial.lang.types._96
  type _97 = spatial.lang.types._97
  type _98 = spatial.lang.types._98
  type _99 = spatial.lang.types._99
  type _100 = spatial.lang.types._100
  type _101 = spatial.lang.types._101
  type _102 = spatial.lang.types._102
  type _103 = spatial.lang.types._103
  type _104 = spatial.lang.types._104
  type _105 = spatial.lang.types._105
  type _106 = spatial.lang.types._106
  type _107 = spatial.lang.types._107
  type _108 = spatial.lang.types._108
  type _109 = spatial.lang.types._109
  type _110 = spatial.lang.types._110
  type _111 = spatial.lang.types._111
  type _112 = spatial.lang.types._112
  type _113 = spatial.lang.types._113
  type _114 = spatial.lang.types._114
  type _115 = spatial.lang.types._115
  type _116 = spatial.lang.types._116
  type _117 = spatial.lang.types._117
  type _118 = spatial.lang.types._118
  type _119 = spatial.lang.types._119
  type _120 = spatial.lang.types._120
  type _121 = spatial.lang.types._121
  type _122 = spatial.lang.types._122
  type _123 = spatial.lang.types._123
  type _124 = spatial.lang.types._124
  type _125 = spatial.lang.types._125
  type _126 = spatial.lang.types._126
  type _127 = spatial.lang.types._127
  type _128 = spatial.lang.types._128
}