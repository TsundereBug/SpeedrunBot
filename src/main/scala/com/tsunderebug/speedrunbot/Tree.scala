package com.tsunderebug.speedrunbot

import java.util.function.Predicate

case class Tree[T](t: T, children: Seq[Tree[T]]) {

  def child(find: Predicate[T]): Option[Tree[T]] = {
    children.find((tree: Tree[T]) => find.test(tree.t))
  }

}