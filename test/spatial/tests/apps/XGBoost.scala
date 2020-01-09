package spatial.tests.apps

object XGHelpers {
  def scalaIfElse[T](cond: scala.Boolean, ift: => T, iff: => T): T = {
    if (cond) ift else iff
  }
}

import spatial.dsl._

class XGBoost_Inline extends XGBoost(false)
class XGBoost_Blackbox extends XGBoost(true)

@spatial abstract class XGBoost(useBox: scala.Boolean) extends SpatialTest {
  override def runtimeArgs = "0.03578 20.0 3.33 0.0 0.4429 7.820 64.5 4.6947 5.0 216.0 14.9 387.31 3.76"
  type T = FixPt[TRUE,_32,_32]
  @struct case class FEATURES(CRIM: T, ZN: T, INDUS: T, CHAS: T, NOX: T, RM: T, AGE: T, DIS: T, RAD: T, TAX: T, PTRATIO: T, B: T, LSTAT: T)
  @struct case class INFERENCE(value: T)

  class TreeNode(field: java.lang.String, thresh: T, leftTree: Either[TreeNode, T], rightTree: Either[TreeNode, T]) {
    def evaluate(sample: FEATURES): T = {
      val point: T = field match {
        case "CRIM" => sample.CRIM.to[T]
        case "ZN" => sample.ZN.to[T]
        case "INDUS" => sample.INDUS.to[T]
        case "CHAS" => sample.CHAS.to[T]
        case "NOX" => sample.NOX.to[T]
        case "RM" => sample.RM.to[T]
        case "AGE" => sample.AGE.to[T]
        case "DIS" => sample.DIS.to[T]
        case "RAD" => sample.RAD.to[T]
        case "TAX" => sample.TAX.to[T]
        case "PTRATIO" => sample.PTRATIO.to[T]
        case "B" => sample.B.to[T]
        case "LSTAT" => sample.LSTAT.to[T]
      }
//      val point: T = sample.getField(field).to[T]
      mux(point < thresh,
        {
          leftTree match {
            case Left(tn) => tn.evaluate(sample)
            case Right(term) => term
          }
        },
        {
          rightTree match {
            case Left(tn) => tn.evaluate(sample)
            case Right(term) => term
          }
        }
      )
    }
  }

  def buildTree(idx: scala.Int, params: scala.List[(java.lang.String, T)]): Either[TreeNode, T] = {
    XGHelpers.scalaIfElse[Either[TreeNode, T]](params(idx)._1.equals(""),
      Right(params(idx)._2),
      {
        val leftIdx = idx * 2 + 1
        val rightIdx = idx * 2 + 2
        assert(leftIdx < params.length && rightIdx < params.length, "Your params are screwed up.  Is there an unterminated node?")
        val leftRoot = buildTree(leftIdx, params)
        val rightRoot = buildTree(rightIdx, params)

        Left(new TreeNode(params(idx)._1, params(idx)._2, leftRoot, rightRoot))
      }
    )
  }

  def main(args: Array[String]): Unit = {
    val par = 5

    val CRIM = Seq.fill(par)(ArgIn[T])
    val ZN = Seq.fill(par)(ArgIn[T])
    val INDUS = Seq.fill(par)(ArgIn[T])
    val CHAS = Seq.fill(par)(ArgIn[T])
    val NOX = Seq.fill(par)(ArgIn[T])
    val RM = Seq.fill(par)(ArgIn[T])
    val AGE = Seq.fill(par)(ArgIn[T])
    val DIS = Seq.fill(par)(ArgIn[T])
    val RAD = Seq.fill(par)(ArgIn[T])
    val TAX = Seq.fill(par)(ArgIn[T])
    val PTRATIO = Seq.fill(par)(ArgIn[T])
    val B = Seq.fill(par)(ArgIn[T])
    val LSTAT = Seq.fill(par)(ArgIn[T])

    CRIM.foreach{x => setArg(x,    args(0).to[T])}
    ZN.foreach{x => setArg(x,      args(1).to[T])}
    INDUS.foreach{x => setArg(x,   args(2).to[T])}
    CHAS.foreach{x => setArg(x,    args(3).to[T])}
    NOX.foreach{x => setArg(x,     args(4).to[T])}
    RM.foreach{x => setArg(x,      args(5).to[T])}
    AGE.foreach{x => setArg(x,     args(6).to[T])}
    DIS.foreach{x => setArg(x,     args(7).to[T])}
    RAD.foreach{x => setArg(x,     args(8).to[T])}
    TAX.foreach{x => setArg(x,     args(9).to[T])}
    PTRATIO.foreach{x => setArg(x, args(10).to[T])}
    B.foreach{x => setArg(x,       args(11).to[T])}
    LSTAT.foreach{x => setArg(x,   args(12).to[T])}

    val predictions = Seq.tabulate(par){_ => ArgOut[T]}

    // Complete binary tree of parameters for each tree
    val tree0 = List(("RM",6.978000.to[T]),("PTRATIO",19.900001.to[T]),("RM",7.437000.to[T]),("RM",6.543000.to[T]),("RM",5.863000.to[T]),("",3.133333.to[T]),("",4.387600.to[T]),("RM",5.403500.to[T]),("",2.706667.to[T]),("RM",4.985000.to[T]),("",1.650093.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.256250.to[T]),("",2.080000.to[T]),("",0.to[T]),("",0.to[T]),("",1.716364.to[T]),("PTRATIO",20.549999.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.041923.to[T]),("",1.466000.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree1 = List(("NOX",0.657000.to[T]),("NOX",0.512500.to[T]),("CRIM",7.194850.to[T]),("CRIM",0.035235.to[T]),("NOX",0.627500.to[T]),("",1.505970.to[T]),("CRIM",13.518949.to[T]),("",2.804707.to[T]),("",2.294779.to[T]),("NOX",0.607000.to[T]),("NOX",0.651000.to[T]),("",0.to[T]),("",0.to[T]),("AGE",77.949997.to[T]),("",0.658218.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("NOX",0.601000.to[T]),("",1.378824.to[T]),("",3.242220.to[T]),("",1.203327.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.366933.to[T]),("",1.039556.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.733546.to[T]),("",2.622254.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree2 = List(("PTRATIO",19.650001.to[T]),("DIS",1.757250.to[T]),("CRIM",9.280695.to[T]),("",1.123251.to[T]),("PTRATIO",14.750000.to[T]),("DIS",1.385950.to[T]),("CRIM",33.503799.to[T]),("",0.to[T]),("",0.to[T]),("CRIM",2.126275.to[T]),("DIS",2.572050.to[T]),("",3.306839.to[T]),("DIS",2.083550.to[T]),("",0.914875.to[T]),("",0.422977.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",3.124567.to[T]),("",1.459820.to[T]),("",1.394504.to[T]),("",2.055823.to[T]),("",0.to[T]),("",0.to[T]),("",1.102603.to[T]),("",1.525688.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree3 = List(("CRIM",6.686320.to[T]),("TAX",267.500000.to[T]),("CRIM",15.718000.to[T]),("ZN",8.750000.to[T]),("TAX",688.500000.to[T]),("",0.983602.to[T]),("",0.586802.to[T]),("CRIM",0.111385.to[T]),("",2.439931.to[T]),("",1.627606.to[T]),("CRIM",0.167115.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.925538.to[T]),("",1.112527.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.879763.to[T]),("",0.235110.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree4 = List(("CRIM",6.686320.to[T]),("B",353.465027.to[T]),("CRIM",15.718000.to[T]),("CRIM",4.445610.to[T]),("ZN",15.000000.to[T]),("B",105.239998.to[T]),("CRIM",33.503799.to[T]),("",1.019345.to[T]),("",0.538700.to[T]),("B",386.154999.to[T]),("B",383.044983.to[T]),("",0.612148.to[T]),("B",388.970001.to[T]),("",0.590439.to[T]),("CRIM",39.940498.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("CRIM",0.246790.to[T]),("CRIM",0.121760.to[T]),("",1.358384.to[T]),("",1.941117.to[T]),("",0.to[T]),("",0.to[T]),("",1.146066.to[T]),("CRIM",13.798000.to[T]),("",0.to[T]),("",0.to[T]),("",0.089504.to[T]),("",0.362660.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.259448.to[T]),("",1.968650.to[T]),("",1.632589.to[T]),("",1.303996.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.808372.to[T]),("",0.124661.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree5 = List(("RM",7.015000.to[T]),("NOX",0.657000.to[T]),("RM",7.437000.to[T]),("RM",6.179000.to[T]),("NOX",0.706500.to[T]),("NOX",0.680000.to[T]),("",3.107415.to[T]),("PTRATIO",20.950001.to[T]),("RM",6.543000.to[T]),("",0.491223.to[T]),("RM",6.786500.to[T]),("",2.106014.to[T]),("",0.473588.to[T]),("",0.to[T]),("",0.to[T]),("",1.086030.to[T]),("",0.778858.to[T]),("RM",6.528000.to[T]),("",1.629769.to[T]),("",0.to[T]),("",0.to[T]),("",0.894799.to[T]),("",0.168617.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.298171.to[T]),("",0.343522.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree6 = List(("INDUS",6.660000.to[T]),("INDUS",3.985000.to[T]),("AGE",70.149994.to[T]),("AGE",23.349999.to[T]),("INDUS",6.145000.to[T]),("",1.173793.to[T]),("CHAS",0.500000.to[T]),("INDUS",1.800000.to[T]),("",1.824298.to[T]),("AGE",35.849998.to[T]),("AGE",89.900002.to[T]),("",0.to[T]),("",0.to[T]),("INDUS",26.695000.to[T]),("AGE",98.750000.to[T]),("",0.350732.to[T]),("",1.333067.to[T]),("",0.to[T]),("",0.to[T]),("",1.335961.to[T]),("AGE",90.949997.to[T]),("",1.918057.to[T]),("",0.537588.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("INDUS",18.840000.to[T]),("AGE",95.349998.to[T]),("AGE",93.850006.to[T]),("",0.259406.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.977094.to[T]),("",0.276376.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.812497.to[T]),("",1.020087.to[T]),("",0.645129.to[T]),("",0.193561.to[T]),("",0.955908.to[T]),("",1.728316.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree7 = List(("RM",7.015000.to[T]),("LSTAT",15.065001.to[T]),("RM",7.437000.to[T]),("LSTAT",4.915000.to[T]),("LSTAT",19.830000.to[T]),("LSTAT",13.165000.to[T]),("",2.645434.to[T]),("",1.641776.to[T]),("RM",6.098500.to[T]),("RM",5.401500.to[T]),("PTRATIO",19.650001.to[T]),("",1.741641.to[T]),("",0.409283.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.864674.to[T]),("",1.134813.to[T]),("",0.228557.to[T]),("",0.723362.to[T]),("RM",4.914500.to[T]),("PTRATIO",20.150001.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.128402.to[T]),("",0.599836.to[T]),("",0.066174.to[T]),("",0.373870.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree8 = List(("NOX",0.657000.to[T]),("TAX",267.500000.to[T]),("NOX",0.706500.to[T]),("B",381.335022.to[T]),("NOX",0.627500.to[T]),("B",92.500000.to[T]),("B",251.235016.to[T]),("",0.660675.to[T]),("B",395.809998.to[T]),("TAX",407.000000.to[T]),("NOX",0.643000.to[T]),("",0.133055.to[T]),("NOX",0.686000.to[T]),("",0.355326.to[T]),("NOX",0.820500.to[T]),("",0.to[T]),("",0.to[T]),("",1.543581.to[T]),("TAX",222.500000.to[T]),("NOX",0.595000.to[T]),("B",26.720001.to[T]),("B",248.785004.to[T]),("",0.665806.to[T]),("",0.to[T]),("",0.to[T]),("",0.436639.to[T]),("NOX",0.696500.to[T]),("",0.to[T]),("",0.to[T]),("B",310.119995.to[T]),("",0.548859.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.464993.to[T]),("",0.915844.to[T]),("",0.994272.to[T]),("",1.477355.to[T]),("",0.235884.to[T]),("",0.746686.to[T]),("",0.591684.to[T]),("",2.533344.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.204651.to[T]),("",0.394723.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.186217.to[T]),("",0.841008.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]))
    val tree9 = List(("LSTAT",4.915000.to[T]),("CRIM",0.256140.to[T]),("LSTAT",11.705000.to[T]),("",1.614560.to[T]),("",2.305500.to[T]),("CRIM",0.424965.to[T]),("LSTAT",19.830000.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("LSTAT",7.630000.to[T]),("LSTAT",9.535000.to[T]),("ZN",20.500000.to[T]),("CRIM",33.503799.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",1.025722.to[T]),("LSTAT",7.945000.to[T]),("",1.469506.to[T]),("",0.884190.to[T]),("LSTAT",15.065001.to[T]),("",0.378632.to[T]),("LSTAT",23.740002.to[T]),("CRIM",39.940498.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.281294.to[T]),("",0.800874.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.to[T]),("",0.727427.to[T]),("",0.579149.to[T]),("",0.to[T]),("",0.to[T]),("",0.393499.to[T]),("",0.293810.to[T]),("",-0.009083.to[T]),("",0.126826.to[T]),("",0.to[T]))
    val trees = List(tree0,tree1,tree2,tree3,tree4,tree5,tree6,tree7,tree8,tree9)
    val base_score = 0.5

    // Put each tree in its own blackbox, for fun!
    val treeBoxes = trees.map{t =>
      Blackbox.SpatialPrimitive[FEATURES, INFERENCE] { in: FEATURES =>
        val treeRoot = buildTree(0, t)
        INFERENCE(treeRoot.left.get.evaluate(in))
      }
    }

    Accel {
      Seq.tabulate(par){i =>
        val crim = CRIM(i).value
        val zn = ZN(i).value
        val indus = INDUS(i).value
        val chas = CHAS(i).value
        val nox = NOX(i).value
        val rm = RM(i).value
        val age = AGE(i).value
        val dis = DIS(i).value
        val rad = RAD(i).value
        val tax = TAX(i).value
        val ptratio = PTRATIO(i).value
        val b = B(i).value
        val lstat = LSTAT(i).value

        val feat = FEATURES(crim, zn, indus, chas, nox, rm, age, dis, rad, tax, ptratio, b, lstat)

        // Blackboxed
        if (useBox) predictions(i) := treeBoxes.map(_(feat).value).reduceTree{_+_} + base_score
        // Inlined
        else {
           val treeRoots = trees.map{t => buildTree(0, t)}
           predictions(i) := treeRoots.map{t => t.left.get.evaluate(feat)}.reduceTree{_+_} + base_score
        }
      }

    }

    val gold = 24.354538.to[T]
    val got = predictions.foreach{p =>
      val got = getArg(p)
      println(r"Got $got, Wanted: $gold")
      assert(abs(gold - got) < 0.05)
    }

  }

}

