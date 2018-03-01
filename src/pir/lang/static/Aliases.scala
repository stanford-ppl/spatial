package pir.lang.static

// No aliases of the form type X = pir.lang.X (creates a circular reference)
// Everything else is ok.
trait InternalAliases {

}

trait ExternalAliases extends InternalAliases {
  type In[A] = pir.lang.In[A]
  type Out[A] = pir.lang.Out[A]
  type Lanes = pir.lang.Lanes
  type Word = pir.lang.Word
  type PMUPtr = pir.lang.PMUPtr
  type PCUPtr = pir.lang.PCUPtr
  lazy val PMU = pir.lang.PMU
  lazy val PCU = pir.lang.PCU
  lazy val curPtr = pir.lang.curPtr

  lazy val BBox = pir.lang.BBox
}