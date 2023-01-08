package spatial.executor

package object scala {
  type SomeEmul = F forSome {type F <: EmulResult}
}
