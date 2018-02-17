package nova.traversal.transform

abstract class ExpandTransformer extends MutateTransformer {
  /*
   * Options when transforming a statement:
   *   0. Remove it: s -> Nil. Statement will not appear in resulting graph.
   *   1. Update it: s -> List(s). Statement will not change except for inputs.
   *   2. Subst. it: s -> List(s'). Substitution s' will appear instead.
   *   3. Expand it: s -> List(a,b,c). Substitutions will appear instead.
   *                                   Downstream users must specify which subst. to use.
   */


}
