import spatial.dsl._

@spatial class MD_1 extends SpatialTest {

 /*
  
  Molecular Dynamics via K-nearest neighbors                                                                

                  ← N_NEIGHBORS →   
                 ___________________   
                |                   |  
            ↑   |                   |  
                |                   |  
                |                   |  
                |                   |  
      N_ATOMS   |                   |  
                |                   |  
                |                   |  
                |                   |  
            ↓   |                   |  
                |                   |  
                 ```````````````````   

                 For each atom (row), get id of its interactions (col), index into idx/y/z, compute potential energy, and sum them all up

                                                                                                           
 */

  // Max pos seems to be about 19
  type T = FixPt[TRUE, _12, _20]

  @struct case class XYZ(x: T, y: T, z: T)

  def main(args: Array[String]): Unit = {
    val PROBLEMS = ArgIn[Int]
    setArg(PROBLEMS, args(0).to[Int])

    val P1 = 1
    val P2 = 2
    val P3 = 3

    val N_ATOMS = 256
    val N_NEIGHBORS = 16 
    val lj1 = 1.5.to[T]
    val lj2 = 2.to[T]
    val raw_xpos = loadCSV1D[T](s"$DATA/MD/knn_x.csv", "\n")
    val raw_ypos = loadCSV1D[T](s"$DATA/MD/knn_y.csv", "\n")
    val raw_zpos = loadCSV1D[T](s"$DATA/MD/knn_z.csv", "\n")
    val raw_interactions_data = loadCSV1D[Int](s"$DATA/MD/knn_interactions.csv", "\n")
    val raw_interactions = raw_interactions_data.reshape(N_ATOMS, N_NEIGHBORS)

    val xpos_dram = DRAM[T](N_ATOMS)
    val ypos_dram = DRAM[T](N_ATOMS)
    val zpos_dram = DRAM[T](N_ATOMS)
    val xforce_dram = DRAM[T](N_ATOMS)
    val yforce_dram = DRAM[T](N_ATOMS)
    val zforce_dram = DRAM[T](N_ATOMS)
    val interactions_dram = DRAM[Int](N_ATOMS, N_NEIGHBORS)

    setMem(xpos_dram, raw_xpos)
    setMem(ypos_dram, raw_ypos)
    setMem(zpos_dram, raw_zpos)
    setMem(interactions_dram, raw_interactions)

    Accel{
      Foreach(PROBLEMS by 1 par P1){ problem => 
        val xpos_sram = SRAM[T](N_ATOMS)
        val ypos_sram = SRAM[T](N_ATOMS)
        val zpos_sram = SRAM[T](N_ATOMS)
        val xforce_sram = SRAM[T](N_ATOMS)
        val yforce_sram = SRAM[T](N_ATOMS)
        val zforce_sram = SRAM[T](N_ATOMS)
        val interactions_sram = SRAM[Int](N_ATOMS, N_NEIGHBORS) // Can also shrink sram and work row by row

        xpos_sram load xpos_dram
        ypos_sram load ypos_dram
        zpos_sram load zpos_dram
        interactions_sram load interactions_dram

        Foreach(N_ATOMS by 1 par P2) { atom =>
          val this_pos = XYZ(xpos_sram(atom), ypos_sram(atom), zpos_sram(atom))
          val total_force = Reg[XYZ](XYZ(0.to[T], 0.to[T], 0.to[T]))
          // total_force.reset // Probably unnecessary
          Reduce(total_force)(N_NEIGHBORS by 1 par P3) { neighbor => 
            val that_id = interactions_sram(atom, neighbor)
            val that_pos = XYZ(xpos_sram(that_id), ypos_sram(that_id), zpos_sram(that_id))
            val delta = XYZ(this_pos.x - that_pos.x, this_pos.y - that_pos.y, this_pos.z - that_pos.z)
            val r2inv = 1.0.to[T]/( delta.x*delta.x + delta.y*delta.y + delta.z*delta.z );
            // Assume no cutoff and aways account for all nodes in area
            val r6inv = r2inv * r2inv * r2inv;
            val potential = r6inv*(lj1*r6inv - lj2);
            val force = r2inv*potential;
            XYZ(delta.x*force, delta.y*force, delta.z*force)
          }{(a,b) => XYZ(a.x + b.x, a.y + b.y, a.z + b.z)}
          xforce_sram(atom) = total_force.x
          yforce_sram(atom) = total_force.y
          zforce_sram(atom) = total_force.z
        }
        xforce_dram store xforce_sram
        yforce_dram store yforce_sram
        zforce_dram store zforce_sram
      }
    }

    val xforce_received = getMem(xforce_dram)
    val yforce_received = getMem(yforce_dram)
    val zforce_received = getMem(zforce_dram)
    val xforce_gold = loadCSV1D[T](s"$DATA/MD/knn_x_gold.csv", "\n")
    val yforce_gold = loadCSV1D[T](s"$DATA/MD/knn_y_gold.csv", "\n")
    val zforce_gold = loadCSV1D[T](s"$DATA/MD/knn_z_gold.csv", "\n")

    printArray(xforce_gold, "Gold x:")
    printArray(xforce_received, "Received x:")
    printArray(yforce_gold, "Gold y:")
    printArray(yforce_received, "Received y:")
    printArray(zforce_gold, "Gold z:")
    printArray(zforce_received, "Received z:")

    val margin = 0.001.to[T]
    val cksumx = xforce_gold.zip(xforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumy = yforce_gold.zip(yforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumz = zforce_gold.zip(zforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksum = cksumx && cksumy && cksumz
    println("PASS: " + cksum + " (MD_KNN)")
    assert(cksum)
  }
}      


@spatial class MD_2 extends SpatialTest { // pipeline flattening and reordering via structural rewrite

 /*
  
  Molecular Dynamics via K-nearest neighbors                                                                

                  ← N_NEIGHBORS →   
                 ___________________   
                |                   |  
            ↑   |                   |  
                |                   |  
                |                   |  
                |                   |  
      N_ATOMS   |                   |  
                |                   |  
                |                   |  
                |                   |  
            ↓   |                   |  
                |                   |  
                 ```````````````````   

                 For each atom (row), get id of its interactions (col), index into idx/y/z, compute potential energy, and sum them all up

                                                                                                           
 */

  // Max pos seems to be about 19
  type T = FixPt[TRUE, _12, _20]

  @struct case class XYZ(x: T, y: T, z: T)

  def main(args: Array[String]): Unit = {
    val PROBLEMS = ArgIn[Int]
    setArg(PROBLEMS, args(0).to[Int])

    val P1 = 1
    val P2 = 16
    val PX = 1

    val N_ATOMS = 256
    val N_NEIGHBORS = 16 
    val lj1 = 1.5.to[T]
    val lj2 = 2.to[T]
    val raw_xpos = loadCSV1D[T](s"$DATA/MD/knn_x.csv", "\n")
    val raw_ypos = loadCSV1D[T](s"$DATA/MD/knn_y.csv", "\n")
    val raw_zpos = loadCSV1D[T](s"$DATA/MD/knn_z.csv", "\n")
    val raw_interactions_data = loadCSV1D[Int](s"$DATA/MD/knn_interactions.csv", "\n")
    val raw_interactions = raw_interactions_data.reshape(N_ATOMS, N_NEIGHBORS)

    val xpos_dram = DRAM[T](N_ATOMS)
    val ypos_dram = DRAM[T](N_ATOMS)
    val zpos_dram = DRAM[T](N_ATOMS)
    val xforce_dram = DRAM[T](N_ATOMS)
    val yforce_dram = DRAM[T](N_ATOMS)
    val zforce_dram = DRAM[T](N_ATOMS)
    val interactions_dram = DRAM[Int](N_ATOMS, N_NEIGHBORS)

    setMem(xpos_dram, raw_xpos)
    setMem(ypos_dram, raw_ypos)
    setMem(zpos_dram, raw_zpos)
    setMem(interactions_dram, raw_interactions)

    Accel{
      Foreach(PROBLEMS by 1 par P1){ problem => 
        val xpos_sram = SRAM[T](N_ATOMS)
        val ypos_sram = SRAM[T](N_ATOMS)
        val zpos_sram = SRAM[T](N_ATOMS)
        val xforce_sram = SRAM[T](N_ATOMS)
        val yforce_sram = SRAM[T](N_ATOMS)
        val zforce_sram = SRAM[T](N_ATOMS)
        val interactions_sram = SRAM[Int](N_ATOMS, N_NEIGHBORS) // Can also shrink sram and work row by row

        xpos_sram load xpos_dram
        ypos_sram load ypos_dram
        zpos_sram load zpos_dram
        interactions_sram load interactions_dram

        Foreach(N_NEIGHBORS by 1 par PX, N_ATOMS by 1 par P2) { (neighbor, atom) =>
          val this_pos = XYZ(xpos_sram(atom), ypos_sram(atom), zpos_sram(atom))
          val total_force = Reg[XYZ](XYZ(0.to[T], 0.to[T], 0.to[T]))
          // total_force.reset // Probably unnecessary
          val that_id = interactions_sram(atom, neighbor)
          val that_pos = XYZ(xpos_sram(that_id), ypos_sram(that_id), zpos_sram(that_id))
          val delta = XYZ(this_pos.x - that_pos.x, this_pos.y - that_pos.y, this_pos.z - that_pos.z)
          val r2inv = 1.0.to[T]/( delta.x*delta.x + delta.y*delta.y + delta.z*delta.z );
          // Assume no cutoff and aways account for all nodes in area
          val r6inv = r2inv * r2inv * r2inv;
          val potential = r6inv*(lj1*r6inv - lj2);
          val force = r2inv*potential;
          xforce_sram(atom) = delta.x*force + mux(neighbor == 0, 0, xforce_sram(atom))
          yforce_sram(atom) = delta.y*force + mux(neighbor == 0, 0, yforce_sram(atom))
          zforce_sram(atom) = delta.z*force + mux(neighbor == 0, 0, zforce_sram(atom))
        }
        xforce_dram store xforce_sram
        yforce_dram store yforce_sram
        zforce_dram store zforce_sram
      }
    }

    val xforce_received = getMem(xforce_dram)
    val yforce_received = getMem(yforce_dram)
    val zforce_received = getMem(zforce_dram)
    val xforce_gold = loadCSV1D[T](s"$DATA/MD/knn_x_gold.csv", "\n")
    val yforce_gold = loadCSV1D[T](s"$DATA/MD/knn_y_gold.csv", "\n")
    val zforce_gold = loadCSV1D[T](s"$DATA/MD/knn_z_gold.csv", "\n")

    printArray(xforce_gold, "Gold x:")
    printArray(xforce_received, "Received x:")
    printArray(yforce_gold, "Gold y:")
    printArray(yforce_received, "Received y:")
    printArray(zforce_gold, "Gold z:")
    printArray(zforce_received, "Received z:")

    val margin = 0.001.to[T]
    val cksumx = xforce_gold.zip(xforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumy = yforce_gold.zip(yforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumz = zforce_gold.zip(zforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksum = cksumx && cksumy && cksumz
    println("PASS: " + cksum + " (MD_KNN)")
    assert(cksum)
  }
}      


@spatial class MD_3 extends SpatialTest { // pipeline flattening via metaprogram

 /*
  
  Molecular Dynamics via K-nearest neighbors                                                                

                  ← N_NEIGHBORS →   
                 ___________________   
                |                   |  
            ↑   |                   |  
                |                   |  
                |                   |  
                |                   |  
      N_ATOMS   |                   |  
                |                   |  
                |                   |  
                |                   |  
            ↓   |                   |  
                |                   |  
                 ```````````````````   

                 For each atom (row), get id of its interactions (col), index into idx/y/z, compute potential energy, and sum them all up

                                                                                                           
 */

  // Max pos seems to be about 19
  type T = FixPt[TRUE, _12, _20]

  @struct case class XYZ(x: T, y: T, z: T)

  def main(args: Array[String]): Unit = {
    val PROBLEMS = ArgIn[Int]
    setArg(PROBLEMS, args(0).to[Int])

    val P1 = 1
    val P2 = 2

    val N_ATOMS = 256
    val N_NEIGHBORS = 16 
    val lj1 = 1.5.to[T]
    val lj2 = 2.to[T]
    val raw_xpos = loadCSV1D[T](s"$DATA/MD/knn_x.csv", "\n")
    val raw_ypos = loadCSV1D[T](s"$DATA/MD/knn_y.csv", "\n")
    val raw_zpos = loadCSV1D[T](s"$DATA/MD/knn_z.csv", "\n")
    val raw_interactions_data = loadCSV1D[Int](s"$DATA/MD/knn_interactions.csv", "\n")
    val raw_interactions = raw_interactions_data.reshape(N_ATOMS, N_NEIGHBORS)

    val xpos_dram = DRAM[T](N_ATOMS)
    val ypos_dram = DRAM[T](N_ATOMS)
    val zpos_dram = DRAM[T](N_ATOMS)
    val xforce_dram = DRAM[T](N_ATOMS)
    val yforce_dram = DRAM[T](N_ATOMS)
    val zforce_dram = DRAM[T](N_ATOMS)
    val interactions_dram = DRAM[Int](N_ATOMS, N_NEIGHBORS)

    setMem(xpos_dram, raw_xpos)
    setMem(ypos_dram, raw_ypos)
    setMem(zpos_dram, raw_zpos)
    setMem(interactions_dram, raw_interactions)

    Accel{
      Foreach(PROBLEMS by 1 par P1){ problem => 
        val xpos_sram = SRAM[T](N_ATOMS)
        val ypos_sram = SRAM[T](N_ATOMS)
        val zpos_sram = SRAM[T](N_ATOMS)
        val xforce_sram = SRAM[T](N_ATOMS)
        val yforce_sram = SRAM[T](N_ATOMS)
        val zforce_sram = SRAM[T](N_ATOMS)
        val interactions_sram = SRAM[Int](N_ATOMS, N_NEIGHBORS) // Can also shrink sram and work row by row

        xpos_sram load xpos_dram
        ypos_sram load ypos_dram
        zpos_sram load zpos_dram
        interactions_sram load interactions_dram

        Foreach(N_ATOMS by 1 par P2) { atom =>
          val this_pos = XYZ(xpos_sram(atom), ypos_sram(atom), zpos_sram(atom))
          // total_force.reset // Probably unnecessary
          val total_force = List.tabulate(N_NEIGHBORS) { neighbor => 
            val that_id = interactions_sram(atom, neighbor)
            val that_pos = XYZ(xpos_sram(that_id), ypos_sram(that_id), zpos_sram(that_id))
            val delta = XYZ(this_pos.x - that_pos.x, this_pos.y - that_pos.y, this_pos.z - that_pos.z)
            val r2inv = 1.0.to[T]/( delta.x*delta.x + delta.y*delta.y + delta.z*delta.z );
            // Assume no cutoff and aways account for all nodes in area
            val r6inv = r2inv * r2inv * r2inv;
            val potential = r6inv*(lj1*r6inv - lj2);
            val force = r2inv*potential;
            XYZ(delta.x*force, delta.y*force, delta.z*force)
          }.reduceTree{(a,b) => XYZ(a.x + b.x, a.y + b.y, a.z + b.z)}
          xforce_sram(atom) = total_force.x
          yforce_sram(atom) = total_force.y
          zforce_sram(atom) = total_force.z
        }
        xforce_dram store xforce_sram
        yforce_dram store yforce_sram
        zforce_dram store zforce_sram
      }
    }

    val xforce_received = getMem(xforce_dram)
    val yforce_received = getMem(yforce_dram)
    val zforce_received = getMem(zforce_dram)
    val xforce_gold = loadCSV1D[T](s"$DATA/MD/knn_x_gold.csv", "\n")
    val yforce_gold = loadCSV1D[T](s"$DATA/MD/knn_y_gold.csv", "\n")
    val zforce_gold = loadCSV1D[T](s"$DATA/MD/knn_z_gold.csv", "\n")

    printArray(xforce_gold, "Gold x:")
    printArray(xforce_received, "Received x:")
    printArray(yforce_gold, "Gold y:")
    printArray(yforce_received, "Received y:")
    printArray(zforce_gold, "Gold z:")
    printArray(zforce_received, "Received z:")

    val margin = 0.001.to[T]
    val cksumx = xforce_gold.zip(xforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumy = yforce_gold.zip(yforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumz = zforce_gold.zip(zforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksum = cksumx && cksumy && cksumz
    println("PASS: " + cksum + " (MD_KNN)")
    assert(cksum)
  }
}      
