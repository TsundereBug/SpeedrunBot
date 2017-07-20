package com.tsunderebug.speedrunbot

import java.util.function.Predicate

case class Tree[T](t: T, children: Seq[Tree[T]]) {

  def treeString(toString: (T, Int) => String, level: Int): String = ("  " * level) + (toString(t, level) + "\n" + children.foldLeft("")((s, t) => s + t.treeString(toString, level + 1) + "\n")).trim

  def treeString(toString: (T, Int) => String): String = treeString(toString, 0)

  def child(find: Predicate[T]): Option[Tree[T]] = {
    children.find((tree: Tree[T]) => find.test(tree.t))
  }

}