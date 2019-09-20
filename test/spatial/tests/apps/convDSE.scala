package spatial.tests.apps

import spatial.dsl._
import utils.implicits._


/*
https://docs.google.com/spreadsheets/d/1VyCRWnAEOnHyjE1x6U73b_W8fz2xaWnNAMDyVTVUx1I/edit#gid=0

layers:
513 x 385 x 3    str 2
257 x 193 x 32   str 2
129 x 97  x 32   str 1
129 x 97  x 64   str 2 
65  x 49  x 64   str 1
65  x 49  x 64   str 2
33  x 25  x 128  str 1
33  x 25  x 128  str 2
17  x 13  x 128  str 1
17  x 13  x 192  str 2 


conv1: LUT:  63634  (23.22%) Regs: 52170  (9.52%)  BRAM: 219 (24.01%) URAM: NA (NA%) DSP: 102 (4.05%)  LaL: 44126  (16.10%) LaM: 5952  (4.13%)  Synthtime: 2314 Tmg_Met: 1 ZCU
  1317.700681
  598.439581
  735.114509
  370.261747
  370.262287
  150.073226
  296.333909
  82.615611
  121.048035
  57.492679

conv2: LUT:  110596 (40.35%) Regs: 59349  (10.83%) BRAM: 272 (29.82%) URAM: NA (NA%) DSP: 154 (6.11%)  LaL: 88757  (32.38%) LaM: 7152  (4.97%)  Synthtime: 2366 Tmg_Met: 0 ZCU
  856.556048
  533.821955
  606.467855
  304.609180
  304.609559
  149.315505
  295.411820
  82.274412
  120.635632
  57.286682

conv3: LUT:  83407  (30.43%) Regs: 74044  (13.51%) BRAM: 225 (24.67%) URAM: NA (NA%) DSP: 87  (3.45%)  LaL: 57837  (21.10%) LaM: 6726  (4.67%)  Synthtime: 2118 Tmg_Met: 0 ZCU
  589.413946
  541.681254
  806.427885
  406.490753
  406.491642
  176.498836
  349.142912
  96.707033
  135.281185
  63.537875

conv4: LUT:  146832 (53.57%) Regs: 92369  (16.85%) BRAM: 411 (45.07%) URAM: NA (NA%) DSP: 171 (6.79%)  LaL: 116430 (42.48%) LaM: 9485  (6.59%)  Synthtime: 3251 Tmg_Met: 0 ZCU
  575.053270
  493.414840
  604.831643
  304.159365
  304.160504
  149.167449
  295.424034
  82.302368
  120.663348
  57.350111

conv5: LUT:  146006 (53.27%) Regs: 93147  (16.99%) BRAM: 371 (40.68%) URAM: NA (NA%) DSP: 231 (9.17%)  LaL: 114253 (41.69%) LaM: 9807  (6.81%)  Synthtime: 3274 Tmg_Met: 0 ZCU
  589.320512
  200.369622
  273.057326
  134.775295
  134.775685
  55.466601
  107.027772
  31.851993
  43.289801
  24.204002

conv6: LUT:  171366 (62.52%) Regs: 163498 (29.83%) BRAM: 383 (42.00%) URAM: NA (NA%) DSP: 285 (11.31%) LaL: 117922 (43.02%) LaM: 17470 (12.13%) Synthtime: 3823 Tmg_Met: 0 ZCU
  591.899355
  156.159231
  228.735287
  112.091821
  112.090882
  49.173304
  94.935220
  28.265119
  39.472240
  22.167404

conv7: LUT:  152847 (55.77%) Regs: 111034 (20.26%) BRAM: 281 (30.81%) URAM: NA (NA%) DSP: 225 (8.93%)  LaL: 115255 (42.05%) LaM: 13690 (9.51%)  Synthtime: 3401 Tmg_Met: 0 ZCU
  327.804855
  526.356656
  606.823413
  304.969966
  304.969996
  149.512458
  296.135264
  82.603394
  120.988603
  57.582964

conv8: LUT:  162413 (59.26%) Regs: 112614 (20.54%) BRAM: 299 (32.79%) URAM: NA (NA%) DSP: 225 (8.93%)  LaL: 124992 (45.60%) LaM: 13312 (9.24%)  Synthtime: 3318 Tmg_Met: 0 ZCU
  290.891921
  498.413159
  609.839978
  307.336445
  307.334166
  150.768538
  299.225783
  84.232443
  122.589575
  58.550360

conv9: LUT:  168077 (61.32%) Regs: 112545 (20.53%) BRAM: 308 (33.77%) URAM: NA (NA%) DSP: 225 (8.93%)  LaL: 130543 (47.63%) LaM: 12776 (8.87%)  Synthtime: 3327 Tmg_Met: 0 ZCU
  301.581947
  526.217548
  606.671019
  304.825730
  304.826609
  149.440019
  295.675746
  82.371697
  120.756648
  57.360710

conv10: LUT: 172271 (62.85%) Regs: 111519 (20.34%) BRAM: 410 (44.96%) URAM: NA (NA%) DSP: 225 (8.93%) LaL: 137530 (50.18%) LaM: 12461 (8.65%) Synthtime: 3596 Tmg_Met: 0 ZCU
  236.424465
  497.224025
  608.717609
  306.341020
  306.341558
  150.259615
  296.739496
  82.931222
  121.314678
  57.658787

conv11: LUT: 164396 (59.98%) Regs: 111116 (20.27%) BRAM: 394 (43.20%) URAM: NA (NA%) DSP: 225 (8.93%) LaL: 129602 (47.29%) LaM: 12350 (8.58%) Synthtime: 3610 Tmg_Met: 0 ZCU
  235.996166
  496.621297
  608.115608
  305.993762
  305.993922
  150.088313
  296.524864
  82.822055
  121.206921
  57.620229

conv12: LUT: 201169 (73.40%) Regs: 186120 (33.95%) BRAM: 588 (64.47%) URAM: NA (NA%) DSP: 231 (9.17%) LaL: 142519 (52.00%) LaM: 18482 (12.83%) Synthtime: 5199 Tmg_Met: 0 ZCU
  2350.254888
  6256.321651
  12459.318978
  6389.271784
  6389.262177
  3354.076159
  6704.706885
  1848.928004
  2771.134699
  1248.763794

conv13: LUT: 204231 (74.52%) Regs: 194919 (35.56%) BRAM: 471 (51.64%) URAM: NA (NA%) DSP: 264 (10.48%) LaL: 142721 (52.07%) LaM: 18572 (12.90%) Synthtime: 6022 Tmg_Met: 0 ZCU
  200.921068
  273.310518
  406.965823
  206.917817
  206.917766
  92.075179
  181.759235
  53.117373
  74.494825
  39.168089


conv14: LUT: 188412 (68.74%) Regs: 182147 (33.23%) BRAM: 489 (53.62%) URAM: NA (NA%) DSP: 264 (10.48%) LaL: 129837 (47.37%) LaM: 19081 (13.25%) Synthtime: 4590 Tmg_Met: 0 ZCU
  218.120709
  278.327727
  412.109791
  209.908894
  209.911336
  93.654464
  183.717236
  54.033830
  75.455337
  39.568437

conv15: LUT: 212788 (77.64%) Regs: 112028 (20.44%) BRAM: 378 (41.45%) URAM: NA (NA%) DSP: 399 (15.83%) LaL: 180839 (65.98%) LaM: 12442 (8.64%) Synthtime: 4992 Tmg_Met: 0 ZCU
conv15: LUT: 245576 (89.60%) Regs: 102667 (18.73%) BRAM: 546 (59.87%) URAM: NA (NA%) DSP: 399 (15.83%) LaL: 213423 (77.87%) LaM: 13323 (9.25%) Synthtime: 4766 Tmg_Met: 0 ZCU
  560.676628
  140.027310
  149.560434
  71.159455
  71.159415
  30.530354
  57.139889
  18.483119
  25.394477
  16.549242

conv16: LUT: 128633 (46.93%) Regs: 92122 (16.81%) BRAM: 546 (59.87%) URAM: NA (NA%) DSP: 273 (10.83%) LaL: 101868 (37.17%) LaM: 11330 (7.87%) Synthtime: 3516 Tmg_Met: 0 ZCU
  587.325101
  146.810813
  173.690003
  84.507752
  84.507291
  41.919200
  80.540122
  24.756756
  36.052108
  21.152665

conv17: LUT: 218473 (79.71%) Regs: 129949 (23.71%) BRAM: 754 (82.68%) URAM: NA (NA%) DSP: 489 (19.40%) LaL: 179729 (65.58%) LaM: 20879 (14.50%) Synthtime: 9592 Tmg_Met: 0 ZCU
  587.369447
  146.462133
  169.693729
  83.490624
  83.490147
  41.391840
  80.014505
  24.617809
  35.841938
  21.092336

conv18: LUT: 108052 (39.42%) Regs: 107737 (19.65%) BRAM: 185 (20.29%) URAM: NA (NA%) DSP: 195 (7.74%) LaL: 73216 (26.71%) LaM: 10911 (7.58%) Synthtime: 2610 Tmg_Met: 0 ZCU
  305.181313
  556.560256
  829.435154
  412.049098
  412.048306
  178.750766
  351.372015
  97.133617
  135.975411
  63.613195

conv19: LUT: 225112 (82.13%) Regs: 138385 (25.25%) BRAM: 309 (33.88%) URAM: NA (NA%) DSP: 471 (18.69%) LaL: 182608 (66.63%) LaM: 16682 (11.58%) Synthtime: 4887 Tmg_Met: 0 ZCU
  244.647703
  289.922628
  345.980784
  165.620479
  165.619729
  80.322341
  155.226114
  44.590284
  65.110729
  33.368496

conv20: LUT: 152285 (55.56%) Regs: 118571 (21.63%) BRAM: 275 (30.15%) URAM: NA (NA%) DSP: 222 (8.81%) LaL: 115658 (42.20%) LaM: 11604 (8.06%) Synthtime: 4100 Tmg_Met: 0 ZCU
  293.184850
  76.563960
  141.097790
  69.607940
  69.608500
  37.220820
  72.492710
  22.432310
  33.537240
  20.058010

OLDconv20:    LUT: 101376 (36.99%) Regs: 73058 (13.33%) BRAM: 269 (29.50%) URAM: NA (NA%) DSP: 168 (6.67%) LaL: 79659 (29.06%) LaM: 7285 (5.06%) Synthtime: 2678 Tmg_Met: 0 ZCU
  541.175150
  134.036270
  141.102540
  69.611010
  69.611070
  37.224010
  72.495240
  22.436220
  33.540680
  20.061410


conv21: LUT: 144729 (52.81%) Regs: 118156 (21.56%) BRAM: 301 (33.00%) URAM: NA (NA%) DSP: 222 (8.81%) LaL: 109316 (39.88%) LaM: 11433 (7.94%) Synthtime: 3592 Tmg_Met: 0 ZCU
  243.589250
  76.329250
  140.868190
  69.570410
  69.569980
  37.230070
  72.543390
  22.482150
  33.586860
  20.153930

conv22: LUT: 265328 (96.81%) Regs: 209565 (38.23%) BRAM: 620 (67.98%) URAM: NA (NA%) DSP: 426 (16.90%) LaL: 198350 (72.37%) LaM: 22311 (15.49%) Synthtime: 8254 Tmg_Met: 0 ZCU
  203.943140
  50.898650
  72.108510
  36.164920
  36.164650
  20.534500
  39.484590
  13.949740
  20.797240
  15.127940

conv23: LUT: 149955 (54.71%) Regs: 120061 (21.90%) BRAM: 307 (33.66%) URAM: NA (NA%) DSP: 222 (8.81%) LaL: 113866 (41.54%) LaM: 11929 (8.28%) Synthtime: 4178 Tmg_Met: 0 ZCU
  243.446930
  76.328460
  140.867620
  69.570140
  69.570020
  37.230490
  72.529390
  22.482250
  33.586730
  20.153680

conv24: LUT: 269563 (98.35%) Regs: 233339 (42.57%) BRAM: 304 (33.33%) URAM: NA (NA%) DSP: 471 (18.69%) LaL: 226966 (82.81%) LaM: 18540 (12.88%) Synthtime: 10760 Tmg_Met: 0 ZCU
  520.307480
  159.643140
  200.156560
  68.331160
  68.331440
  29.495360
  47.865980
  15.829750
  22.690720
  15.019490

conv25: LUT: 263972 (96.31%) Regs: 208587 (38.05%) BRAM: 611 (67.00%) URAM: NA (NA%) DSP: 426 (16.90%) LaL: 197352 (72.01%) LaM: 22137 (15.37%) Synthtime: 8100 Tmg_Met: 0 ZCU
  201.531710
  50.608830
  72.108570
  36.164110
  36.163610
  20.534900
  39.483880
  13.948520
  20.797460
  15.127690

conv26: LUT: 135621 (49.48%) Regs: 137013 (25.00%) BRAM: 672 (73.68%) URAM: NA (NA%) DSP: 423 (16.79%) LaL: 101690 (37.10%) LaM: 21707 (15.07%) Synthtime: 4086 Tmg_Met: 0 ZCU
  202.658700
  50.713620
  70.847240
  35.840400
  35.840580
  20.449180
  39.398790
  13.925600
  20.773180
  15.120620

conv27: LUT: 200164 (73.03%) Regs: 184093 (33.58%) BRAM: 793 (86.95%) URAM: NA (NA%) DSP: 468 (18.57%) LaL: 152180 (55.52%) LaM: 28941 (20.10%) Synthtime: 6105 Tmg_Met: 0 ZCU
  188.412830
  47.961700
  90.758880
  46.655630
  46.655890
  25.516750
  49.623440
  17.204660
  25.651700
  16.606980

conv28: LUT: 263472 (96.13%) Regs: 171681 (31.32%) BRAM: 740 (81.14%) URAM: NA (NA%) DSP: 423 (16.79%) LaL: 215505 (78.63%) LaM: 31019 (21.54%) Synthtime: 7122 Tmg_Met: 0 ZCU
  190.618970
  48.572460
  70.840010
  35.828770
  35.828750
  20.434680
  39.325960
  13.841720
  20.687510
  14.926940

conv29: LUT: 207207 (75.60%) Regs: 157995 (28.82%) BRAM: 765 (83.88%) URAM: NA (NA%) DSP: 642 (25.48%) LaL: 167210 (61.01%) LaM: 22348 (15.52%) Synthtime: 5059 Tmg_Met: 0 ZCU
  196.990330
  49.633950
  51.440770
  19.562840
  19.562840
  11.784870
  21.965860
  9.029670
  13.479130
  11.644440


conv30: LUT: 253466 (92.48%) Regs: 173341 (31.62%) BRAM: 900.5 (98.74%) URAM: NA (NA%) DSP: 642 (25.48%) LaL: 208268 (75.99%) LaM: 25828 (17.94%) Synthtime: 7073 Tmg_Met: 0 ZCU
  188.728840
  47.214632
  48.043221
  19.548295
  19.553022
  11.792153
  21.958335
  9.057193
  13.497431
  11.687618

conv31: LUT: 217512 (79.36%) Regs: 159543 (29.11%) BRAM: 753 (82.57%) URAM: NA (NA%) DSP: 642 (25.48%) LaL: 174104 (63.52%) LaM: 25964 (18.03%) Synthtime: 5454 Tmg_Met: 0 ZCU
  196.994800
  49.620540
  51.457980
  19.562910
  19.563040
  11.785000
  21.965560
  9.029340
  13.479240
  11.644380

conv32: LUT: 237868 (86.79%) Regs: 163886 (29.90%) BRAM: 499 (54.71%) URAM: NA (NA%) DSP: 753 (29.88%) LaL: 193415 (70.57%) LaM: 27821 (19.32%) Synthtime: 5703 Tmg_Met: 0 ZCU
  235.697353 ms
  55.291265 ms
  69.972921 ms
  35.520111 ms
  35.519702 ms
  20.364195 ms
  39.345534 ms
  13.928737 ms
  20.789447 ms
  15.187551 ms

*/
@spatial class convprobe extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1" 
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 1 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // debug
    val INPUT_CHUNK = DRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
    val KERNEL_CHUNK = DRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
    val BIAS_LOOP = DRAM[T](OUTCHANS_MAX_UPPERBOUND)
    val OUTCHAN_PROBE = ArgIn[Int]
    val OUTROW_PROBE = ArgIn[Int]
    val OUTCOL_PROBE = ArgIn[Int]
    val OUTACCUM = ArgOut[T2]
    val OUTBIAS = ArgOut[T]
    val OUTBITSHIFT = ArgOut[T]
    val OUTRESULT = ArgOut[T]
    setArg(OUTROW_PROBE, args(8).to[Int])
    setArg(OUTCOL_PROBE, args(9).to[Int])
    setArg(OUTCHAN_PROBE, args(10).to[Int])

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // printTensor3(input.reorder(Seq(2,1,0)), "Input")
    // printTensor4(kernel.reorder(Seq(0,3,2,1)), "Kernel")
    // printArray(bias, "Bias")

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    // setMem(KERNEL_COPY_CPU, kernel)
    // println("MANUALLY COPY KERNEL_DATA PTR TO ME!")

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1, 0 until 3 by 1){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
                             // else accum_line_upcast(oc).bits(15::0).as[T]
            // val rawadd = bitshifted + bias_sram(oc)
            // val satadd = if ((rawadd < 0) && (bitshifted > 0) && (bias_sram(oc) > 0)) 32767
            //              else if ((rawadd > 0) && (bitshifted < 0) && (bias_sram(oc) < 0)) -32768
            //              else rawadd
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
            // Pipe{
              if (C == OUTCOL_PROBE.value && R == OUTROW_PROBE.value && oc == OUTCHAN_PROBE.value) {
            //     INPUT_CHUNK store local_data
            //     val kernel_debug = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
            //     Foreach(3 by 1, 3 by 1, INCHANS_MAX_UPPERBOUND by 1){(i,j,k) => kernel_debug(i,j,k) = kernel_sram(oc,i,j,k)}
            //     KERNEL_CHUNK(0::3, 0::3, 0::INCHANS_MAX) store kernel_debug
            //     BIAS_LOOP store bias_sram
                OUTACCUM := accum_line_upcast(oc)
                OUTBITSHIFT := bitshifted
                OUTBIAS  := bias_sram(oc)
                OUTRESULT   := satadd
              }
            // }
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // // Compute Checks
    // val gold = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 
    //   val el = Array.tabulate(INCHANS_MAX){page => 
    //     Array.tabulate(KERNEL_COLS){ii => Array.tabulate(KERNEL_ROWS){jj => 
    //       val idx0 = i*stride - 1 + ii
    //       val idx1 = j*stride - 1 + jj

    //       val pxl = if (idx0 >= 0 && idx0 < IMAX_0 && idx1 >= 0 && idx1 < IMAX_1) input(idx0,idx1, page) else 0.to[T]
    //       val f = kernel(k, ii, jj, page)
    //       // println(r"for $idx0 $idx1: $pxl * $f")
    //       pxl.to[T2] * f.to[T2]
    //     }}.flatten.reduce{_+_}
    //   }.reduce{_+_} //>> 12
    //   if (el.to[T] + bias(k) < 0.to[T]) 0.to[T] else {el.to[T] + bias(k)}
    // }
  
    if ((print_data == 1)) {
      printTensor3(input.reorder(Seq(2,1,0)), "Input")
      printTensor4(kernel.reorder(Seq(0,3,2,1)), "Kernel")
      // printTensor3(gold.reorder(Seq(2,1,0)), "Gold")
      printTensor3(results.reorder(Seq(2,1,0)), "Extracted")  

    //   val bitmask = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) =>
    //     if (results(i,j,k) == gold(i,j,k)) 1.to[Int] else 0.to[Int]
    //   }
    //   val num_wrong = bitmask.length - bitmask.reduce{_+_}
    //   printTensor3(bitmask.reorder(Seq(2,1,0)), "Matchup")
    //   println("Error rate: " + num_wrong + " / " + bitmask.length + " incorrect")

    }
    printArray(getMem(BIAS_LOOP), "bias echo")
    printTensor3(getTensor3(INPUT_CHUNK), "inchunk")
    printTensor3(getTensor3(KERNEL_CHUNK), "kernelchunk")
    println(r"part1 is ${getArg(OUTACCUM)}")
    println(r"part2 is ${getArg(OUTBIAS)}")
    println(r"bitshifted is ${getArg(OUTBITSHIFT)}")
    println(r"value is ${getArg(OUTRESULT)}")

    // val cksum = gold.zip(results){_==_}.reduce{_&&_}    

    // println("PASS: " + cksum + " (SingleLayerConv_RCIO)")
    // assert(cksum)

  }
}



@spatial class conv1 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 2 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1, 0 until 3 by 1){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}


@spatial class conv2 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 16 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1, 0 until 3 by 1){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv3 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 1 //8
    val P5 = 1 //4
    val P6 = 1 //16
    val P7 = 1
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv4 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P7 = 3
    val P8 = 1
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv5 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 4 //2
    val P4 = 1 //8
    val P5 = 1 //4
    val P6 = 1 //16
    val P7 = 1
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv6 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 4 //2
    val P4 = 1 //8
    val P5 = 1 //4
    val P6 = 1 //16
    val P7 = 1
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_srams = List.tabulate(KERNEL_COLS){i => List.tabulate(KERNEL_ROWS){j => (SRAM[T](OUTCHANS_MAX_UPPERBOUND,1,1, INCHANS_MAX_UPPERBOUND), i,j) }}.flatten
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_srams.map{case (sram, i, j) => sram load KERNEL_DATA(0::OUTCHANS_MAX, i::i+1, j::j+1, 0::INCHANS_MAX)}
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = kernel_srams.map{case (sram, i,j) => sram(oc,0,0,ic).to[T2]}
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}



@spatial class conv7 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = List.fill(3)(SRAM[T](1,3,INCHANS_MAX_UPPERBOUND).hierarchical)
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P8){j => 
            if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(0)(0::1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          Foreach(0 until 3 by 1 par P8){j => 
            if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(1)(0::1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          Foreach(0 until 3 by 1 par P8){j => 
            if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(2)(0::1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(0,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}


@spatial class conv8 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}



@spatial class conv9 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 3
    val loadPar = 4 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = List.fill(3)(SRAM[T](1,3,INCHANS_MAX_UPPERBOUND).hierarchical)
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P8){j => 
            if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(0)(0::1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          Foreach(0 until 3 by 1 par P8){j => 
            if (idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(1)(0::1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          Foreach(0 until 3 by 1 par P8){j => 
            if (idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(2)(0::1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(0,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}


@spatial class conv10 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 3
    val loadPar = 4 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)
          local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)
          local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}


@spatial class conv11 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 3
    val loadPar = 4 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv12 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 1 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 2 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
          local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              // val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
              //   kernel_sram(oc,i,j,ic).to[T2]
              // }}.flatten
              // val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
              //   if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
              // }}.flatten
              // val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              val accum = Reduce(Reg[T2])(3 by 1, 3 by 1 par 3){(i,j) => 
                val data = if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) {
                  if (i == 0 && j == 0) local_data(0)(0)(0,0,ic).to[T2]
                  else if (i == 0 && j == 1) local_data(0)(1)(0,0,ic).to[T2]
                  else if (i == 0 && j == 2) local_data(0)(2)(0,0,ic).to[T2]
                  else if (i == 1 && j == 0) local_data(1)(0)(0,0,ic).to[T2]
                  else if (i == 1 && j == 1) local_data(1)(1)(0,0,ic).to[T2]
                  else if (i == 1 && j == 2) local_data(1)(2)(0,0,ic).to[T2]
                  else if (i == 2 && j == 0) local_data(2)(0)(0,0,ic).to[T2]
                  else if (i == 2 && j == 1) local_data(2)(1)(0,0,ic).to[T2]
                  else local_data(2)(2)(0,0,ic).to[T2]
                }
                else 0.to[T2]
                val kernel = kernel_sram(oc,i,j,ic).to[T2]
                data * kernel
              }{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // Compute Checks
    val gold = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 
      val el = Array.tabulate(INCHANS_MAX){page => 
        Array.tabulate(KERNEL_COLS){ii => Array.tabulate(KERNEL_ROWS){jj => 
          val idx0 = i*stride - 1 + ii
          val idx1 = j*stride - 1 + jj

          val pxl = if (idx0 >= 0 && idx0 < IMAX_0 && idx1 >= 0 && idx1 < IMAX_1) input(idx0,idx1, page) else 0.to[T]
          val f = kernel(k, ii, jj, page)
          // println(r"for $idx0 $idx1: $pxl * $f")
          pxl.to[T2] * f.to[T2]
        }}.flatten.reduce{_+_}
      }.reduce{_+_} //>> 12
      if (el.to[T] + bias(k) < 0.to[T]) 0.to[T] else {el.to[T] + bias(k)}
    }
    println(r"Pass: ${gold == results}")

  }
}

@spatial class conv13 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 1 //2
    val P5 = 1 //4
    val P6 = 2 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
          val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
            val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
              kernel_sram(oc,i,j,ic).to[T2]
            }}.flatten
            val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
              if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
            }}.flatten
            val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
            local_accum_line(oc) = accum
          }
          local_accum_line
        }{_+_}
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv14 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 1 //2
    val P5 = 1 //4
    val P6 = 2 //16
    val loadPar = 2 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}


@spatial class conv15 extends SpatialTest { // from conv5
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 8 //2
    val P4 = 1 //8
    val P5 = 1 //4
    val P6 = 1 //16
    val P7 = 1
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv16 extends SpatialTest { // from conv5
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 4 //2
    val P4 = 2 //8
    val P5 = 1 //4
    val P6 = 1 //16
    val P7 = 1
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv17 extends SpatialTest { // from conv5
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 4 //2
    val P4 = 4 //8
    val P5 = 1 //4
    val P6 = 1 //16
    val P7 = 1
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                             else accum_line_upcast(oc).bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv18 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val loadTile = 64
    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 1 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 1
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    val donttouch = ArgOut[Bit]

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Stream.Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val local_data = List.fill(3)(List.fill(3)(FIFO[T](INCHANS_MAX_UPPERBOUND)))

        def fetchLine(i: scala.Int, j: scala.Int): Unit = {
          Pipe{
            val idx0 = C*STRIDE
            val idx1 = R*STRIDE
            Foreach(INCHANS_MAX by loadTile){ t => 
              val len = min(loadTile, INCHANS_MAX - t)
              local_data(i)(j) load INPUT_DATA(max(0,min(idx0-1+i,IMAX_0.value)), max(0,min(idx1-1+j,IMAX_1.value)), t::t+len par loadPar)
            }
          }
        }

        // Fetch data
        List.tabulate(3){i => List.tabulate(3){j => fetchLine(i,j) }}

        // Greedily consume data
        val store_ready = FIFO[Bit](8)
        val accum_line = FIFO[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            val data_elements_raw = List.tabulate(3){i => List.tabulate(3){j => local_data(i)(j).deq().to[T2] }}
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                mux(idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1, data_elements_raw(i)(j), 0.to[T2])
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val raw = accum_line_upcast(oc)
            val bitshifted = if (raw < (-536870912).to[T2]) -32768.to[T]
                             else if (raw > (536870911).to[T2]) 32767.to[T]
                             else raw.bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line.enq(max(0.to[T], satadd))
            if (oc == 0) store_ready.enq(true)
          }
        }

        // store
        Pipe{
          donttouch := store_ready.deq()
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
        
      }
    }
    println(r"donttouch: $donttouch")

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}


@spatial class conv19 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val loadTile = 64
    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 2 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 1
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    val donttouch = ArgOut[Bit]

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Stream.Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val local_data = List.fill(3)(List.fill(3)(FIFO[T](INCHANS_MAX_UPPERBOUND)))

        def fetchLine(i: scala.Int, j: scala.Int): Unit = {
          Pipe{
            val idx0 = C*STRIDE
            val idx1 = R*STRIDE
            Foreach(INCHANS_MAX by loadTile){ t => 
              val len = min(loadTile, INCHANS_MAX - t)
              local_data(i)(j) load INPUT_DATA(max(0,min(idx0-1+i,IMAX_0.value)), max(0,min(idx1-1+j,IMAX_1.value)), t::t+len par loadPar)
            }
          }
        }

        // Fetch data
        List.tabulate(3){i => List.tabulate(3){j => fetchLine(i,j) }}

        // Greedily consume data
        val store_ready = FIFO[Bit](8)
        val accum_line = FIFO[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            val data_elements_raw = List.tabulate(3){i => List.tabulate(3){j => local_data(i)(j).deq().to[T2] }}
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                mux(idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1, data_elements_raw(i)(j), 0.to[T2])
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
              local_accum_line(oc) = accum
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val raw = accum_line_upcast(oc)
            val bitshifted = if (raw < (-536870912).to[T2]) -32768.to[T]
                             else if (raw > (536870911).to[T2]) 32767.to[T]
                             else raw.bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line.enq(max(0.to[T], satadd))
            if (oc == 0) store_ready.enq(true)
          }
        }

        // store
        Pipe{
          donttouch := store_ready.deq()
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
        
      }
    }
    println(r"donttouch: $donttouch")

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}

@spatial class conv20 extends SpatialTest { // from conv5
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //8
    val P5 = 1 //4
    val P6 = 4 //16
    val P7 = 3
    val P8 = 3
    val loadPar = 1 (1 -> 16)
    val storePar = 4 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND).hierarchical
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Foreach(0 until 3 by 1 par P7, 0 until 3 by 1 par P8){(i,j) => 
          if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
        }
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i,j,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}


@spatial class conv21 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 4 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 4 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}


@spatial class conv22 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 4 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}


@spatial class conv23 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 8 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 4 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}


@spatial class conv24 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val loadTile = 64
    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 2 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val P8 = 1
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    val donttouch = ArgOut[Bit]

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Stream.Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val local_data = List.fill(3)(List.fill(3)(FIFO[T](INCHANS_MAX_UPPERBOUND)))

        def fetchLine(i: scala.Int, j: scala.Int): Unit = {
          Pipe{
            val idx0 = C*STRIDE
            val idx1 = R*STRIDE
            Foreach(INCHANS_MAX by loadTile){ t => 
              val len = min(loadTile, INCHANS_MAX - t)
              local_data(i)(j) load INPUT_DATA(max(0,min(idx0-1+i,IMAX_0.value)), max(0,min(idx1-1+j,IMAX_1.value)), t::t+len par loadPar)
            }
          }
        }

        // Fetch data
        List.tabulate(3){i => List.tabulate(3){j => fetchLine(i,j) }}

        val store_ready = FIFO[Bit](8)
        val accum_line = FIFO[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){(ic, oc) => 
            val holders = List.tabulate(3){i => List.tabulate(3){j => Reg[T2]}}
            val data_elements_raw = List.tabulate(3){i => List.tabulate(3){j => if (oc == 0) {val x = local_data(i)(j).deq().to[T2]; holders(i)(j) := x; x} else holders(i)(j).value }}
            val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
              kernel_sram(oc,i,j,ic).to[T2]
            }}.flatten
            val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
              mux(idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0 && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1, data_elements_raw(i)(j), 0.to[T2])
            }}.flatten

            val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
            accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)

          }
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val raw = accum_line_upcast(oc)
            val bitshifted = if (raw < (-536870912).to[T2]) -32768.to[T]
                             else if (raw > (536870911).to[T2]) 32767.to[T]
                             else raw.bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line.enq(max(0.to[T], satadd))
            if (oc == 0) store_ready.enq(true)
          }
        }

        // store
        Pipe{
          donttouch := store_ready.deq()
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
        
      }
    }
    println(r"donttouch: $donttouch")

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}

@spatial class conv25 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 4 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv26 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 2 //2
    val P4 = 2 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv27 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 3 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 2 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

  }
}

@spatial class conv28 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 2 //2
    val P4 = 2 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 8 (1 -> 16)
    val storePar = 8 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}

@spatial class conv29 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 (1,2,8,16,32) //2
    val P4 = 8 (1,2,8,16,32) //2
    val P5 = 1 (1,2,8,16,32) //4
    val P6 = 1 (1,2,8,16,32) //16
    val loadPar = 4 (1,4,8,16,32)
    val storePar = 4 (1,4,8,16,32)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int] + 1; bound(imax_0) = 479
    val imax_1 = args(1).to[Int] + 1; bound(imax_1) = 580
    val omax_0 = args(2).to[Int] + 1; bound(omax_0) = 240
    val omax_1 = args(3).to[Int] + 1; bound(omax_1) = 290
    val inchans_max = args(4).to[Int] + 1; bound(inchans_max) = 3
    val outchans_max = args(5).to[Int] + 1; bound(outchans_max) = 16
    val stride = args(6).to[Int]; bound(stride) = 2
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0)
    setArg(IMAX_1, imax_1)
    setArg(OMAX_0, omax_0)
    setArg(OMAX_1, omax_1)
    setArg(INCHANS_MAX, inchans_max)
    setArg(OUTCHANS_MAX, outchans_max)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Parallel{
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        }
        Pipe.II(1).Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}

@spatial class conv30 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 (1 -> 6) //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 (1 -> 6) //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 (1 -> 32) //2
    val P4 = 8 (1 -> 32) //2
    val P5 = 1 (1 -> 32) //4
    val P6 = 1 (1 -> 32) //16
    val loadPar = 4 (1,2,4,8,16,32)
    val storePar = 4 (1,2,4,8,16,32)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe.II(1).Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}


@spatial class conv31 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 8 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
        local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)
        local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)
        Pipe.II(1).Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}

@spatial class conv32 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 2 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 2 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 (1,2,8,16,32) //2
    val P4 = 1 (1,2,8,16,32) //2
    val P5 = 1 (1,2,8,16,32) //4
    val P6 = 1 (1,2,8,16,32) //16
    val loadPar = 1 (1,4,8,16,32)
    val storePar = 1 (1,4,8,16,32)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int] + 1; bound(imax_0) = 479
    val imax_1 = args(1).to[Int] + 1; bound(imax_1) = 580
    val omax_0 = args(2).to[Int] + 1; bound(omax_0) = 240
    val omax_1 = args(3).to[Int] + 1; bound(omax_1) = 290
    val inchans_max = args(4).to[Int] + 1; bound(inchans_max) = 3
    val outchans_max = args(5).to[Int] + 1; bound(outchans_max) = 16
    val stride = args(6).to[Int]; bound(stride) = 2
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0)
    setArg(IMAX_1, imax_1)
    setArg(OMAX_0, omax_0)
    setArg(OMAX_1, omax_1)
    setArg(INCHANS_MAX, inchans_max)
    setArg(OUTCHANS_MAX, outchans_max)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1, OMAX_1 par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.fill(3)(List.fill(3)(SRAM[T](1,1,INCHANS_MAX_UPPERBOUND).hierarchical))
        val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
        val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
        Parallel{
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(0)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 1 >= 0 && idx1 - 1 + 1 < IMAX_1) local_data(0)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 0 >= 0 && idx0 - 1 + 0 < IMAX_0.value && idx1 - 1 + 2 >= 0 && idx1 - 1 + 2 < IMAX_1) local_data(0)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+0::idx0+0, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 1 >= 0 && idx0 - 1 + 1 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(1)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(1)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(1)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+1::idx0+1, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
          Pipe{if (idx0 - 1 + 2 >= 0 && idx0 - 1 + 2 < IMAX_0.value && idx1 - 1 + 0 >= 0 && idx1 - 1 + 0 < IMAX_1) local_data(2)(0)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+0::idx1+0, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(2)(1)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+1::idx1+1, 0::INCHANS_MAX par loadPar)}
          Pipe{local_data(2)(2)(0::1,0::1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+2::idx0+2, idx1-1+2::idx1+2, 0::INCHANS_MAX par loadPar)}
        }
        Pipe.II(1).Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
            kernel_sram(oc,i,j,ic).to[T2]
          }}.flatten
          val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
          }}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          accum_line_upcast(oc) = mux(ic == 0, accum, accum_line_upcast(oc) + accum)
        }
        // RELU
        Foreach(OUTCHANS_MAX by 1 par P6){oc => 
          val bitshifted = if (accum_line_upcast(oc) < (-536870912).to[T2]) -32768.to[T]
                           else if (accum_line_upcast(oc) > (536870911).to[T2]) 32767.to[T]
                           else accum_line_upcast(oc).bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          accum_line(oc) = max(0.to[T], satadd)
        }
        OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}


@spatial class convlb1 extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 (1 -> 6) //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 (1 -> 6) //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 (1 -> 32) //2
    val P4 = 1 (1 -> 32) //2
    val P5 = 1 (1 -> 32) //4
    val P6 = 1 (1 -> 32) //16
    val loadPar = 4 (1,2,4,8,16,32)
    val storePar = 4 (1,2,4,8,16,32)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 514
    val OUTPUT_COLS_MAX = 258
    val INCHANS_MAX_UPPERBOUND = 192
    val OUTCHANS_MAX_UPPERBOUND = 192

    // Memories
    val INPUT_DATA = DRAM[T](INCHANS_MAX, IMAX_0, IMAX_1)
    val OUTPUT_DATA = DRAM[T](OUTCHANS_MAX, OMAX_0, OMAX_1)
    val TEMP = DRAM[T2](OUTCHANS_MAX, OMAX_0, OMAX_1)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::INCHANS_MAX, 0::IMAX_0, 0::IMAX_1) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OUTCHANS_MAX, 0::OMAX_0, 0::OMAX_1){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND).hierarchical
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(INCHANS_MAX by 1 par P3){ic => 
        val local_data_1 = LineBuffer[T](3,INPUT_COLS_MAX)
        val local_data_2 = LineBuffer.strided[T](3,INPUT_COLS_MAX,2)
        Foreach(OMAX_0 par P1){ R =>
          val accum_line_upcast = SRAM[T2](OUTPUT_COLS_MAX, OUTCHANS_MAX_UPPERBOUND).buffer
          val accum_line = SRAM[T](OUTPUT_COLS_MAX, OUTCHANS_MAX_UPPERBOUND)
          if (ic != 0) accum_line_upcast load TEMP(R, 0::OMAX_1, 0::OUTCHANS_MAX par loadPar)
          if (STRIDE.value == 1) local_data_1 load INPUT_DATA(ic, R, 0::INCHANS_MAX par loadPar)
          else                   local_data_2 load INPUT_DATA(ic, R*2::R*2 + 2, 0::INCHANS_MAX par loadPar)
          Foreach(OMAX_1 par P2, OUTCHANS_MAX par P4) { (C,oc) => 
            val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
              kernel_sram(oc,i,j,ic).to[T2]
            }}.flatten
            val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
              if (STRIDE.value == 1) local_data_1(i,C + j).to[T2]
              else                   local_data_2(i,C*2 + j).to[T2]
              // if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1.value) local_data(i)(j)(0,0,ic).to[T2] else 0.to[T2]
            }}.flatten
            val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
            if (ic == 0) accum_line_upcast(C,oc) = accum
            else if (ic == (INCHANS_MAX.value - 1)) {
              val bitshifted = if (accum_line_upcast(C,oc) < (-536870912).to[T2]) -32768.to[T]
                               else if (accum_line_upcast(C,oc) > (536870911).to[T2]) 32767.to[T]
                               else accum_line_upcast(C,oc).bits(29::14).as[T]
              val satadd = bitshifted +! bias_sram(oc)
              accum_line(C,oc) = max(0.to[T], satadd)
            }
            else accum_line_upcast(C,oc) = accum_line_upcast(C,oc) + accum
          }
          if (ic == INCHANS_MAX.value-1) OUTPUT_DATA(R,0::OMAX_1,0::OUTCHANS_MAX par storePar) store accum_line
          else TEMP(R, 0::OMAX_1, 0::OUTCHANS_MAX par storePar) store accum_line_upcast
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}
