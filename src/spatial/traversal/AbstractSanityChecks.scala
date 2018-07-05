package spatial.traversal

import argon._
import spatial.metadata.control.allowSharingBetweenHostAndAccel

abstract class AbstractSanityChecks extends AccelTraversal {

  def disallowedInputs(stms: Set[Sym[_]], ins: Iterator[Sym[_]], allowArgInference: Boolean): Iterator[(Sym[_],Sym[_])] = {
    ins.filterNot{s => allowSharingBetweenHostAndAccel(s, allowArgInference) }
      .flatMap{in => stms.find(_.inputs.contains(in)).map{use => (in,use) }}
  }

}
