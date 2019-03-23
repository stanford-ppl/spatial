package models

import utils.io.files._

object Sensitivity {
	val area_cols = List("BRAM","DSPs","Regs","SLICEL","SLICEM","Slices","Valid")
	var filtered_data: Seq[Seq[String]] = Seq(Seq())

	def getCol(property: String): Int = filtered_data(0).indexWhere(_.toString == property)

	def rowsWhere(property: String, value: String): Seq[Int] = {
		val col = getCol(property)
		val col_data = filtered_data.drop(1).map(_(col))
		val rows = col_data.zipWithIndex.collect{case (d,i) if d == value => i}
		rows
	}

	def findRow(target: Seq[(String, String)]): Int = {
		val intersects = target.map{case (prop, value) => rowsWhere(prop, value)}
		intersects.fold[Seq[Int]](Seq.tabulate(filtered_data.size){i => i}){case (a,b) => a.intersect(b)}.head
	}


	def getProp(property: String, row: Int): String = {
		val col = getCol(property)
		filtered_data(row)(col)
	}

	def valueAbove(property: String, value: String): Option[String] = {
		val col = getCol(property)
		if (value != "true" && value != "false") {
			val col_data = filtered_data.drop(1).map(_(col)).distinct.map(_.toInt).sorted
			val row = col_data.map(_.toString).indexOf(value)
			if (row == 0) None else Some(col_data(row-1).toString)
		}
		else {
			val col_data = filtered_data.drop(1).map(_(col)).distinct.sorted
			if (value == "true" && col_data.contains("false")) Some("false")
			else None
		}
	}

	def valueBelow(property: String, value: String): Option[String] = {
		val col = getCol(property)
		if (value != "true" && value != "false") {
			val col_data = filtered_data.drop(1).map(_(col)).distinct.map(_.toInt).sorted.reverse
			val row = col_data.map(_.toString).indexOf(value)
			if (row == 0) None else Some(col_data(row-1).toString)
		}
		else {
			val col_data = filtered_data.drop(1).map(_(col)).distinct.sorted
			if (value == "false" && col_data.contains("true")) Some("true")
			else None
		}
	}

	def around(dse_file: String, params: Map[String,String]): Map[String, Int] = {
		val data = loadCSVNow2D[String](dse_file, ",")(x => x)

		// Filter out area and timestamp cols
		val filter_idx = area_cols.map(data(0).indexOf)
		filtered_data = data.map{r => r.zipWithIndex.collect{case (x,i) if !filter_idx.contains(i) => x}}.toSeq
		params.toList.zipWithIndex.foreach{case ((name, value), i) => 
			val others = params.toList.patch(i, Nil, 1)
			// Find current row, row above, row below
			val current_runtime = getProp("Cycles", findRow(others ++ List((name,value)))).toInt
			val valAbove = valueAbove(name, value)
			val above_runtime: Option[Int] = if (valAbove.isDefined) Some(getProp("Cycles", findRow(others ++ List((name, valAbove.get)))).toInt) else None
			val valBelow = valueBelow(name, value)
			val below_runtime: Option[Int] = if (valBelow.isDefined) Some(getProp("Cycles", findRow(others ++ List((name, valBelow.get)))).toInt) else None

			// Print Sensitivity
			val segment_width = 20
			println(s"Sensitivity for $name:")
			val right_string_param = if (valBelow.isDefined) "-"*(segment_width - 1 - s"${valBelow.get}".length) + s"${valBelow.get}" else " "*segment_width
			val left_string_param = if (valAbove.isDefined) s"${valAbove.get}" + "-"*(segment_width - 1 - s"${valAbove.get}".length) else " "*segment_width
			val right_string_runtime = if (below_runtime.isDefined) {
				val percent = ((below_runtime.get.toDouble) / current_runtime.toDouble) * 100.0
				"-"*(segment_width - 1 - (s"${below_runtime.get}" + f"($percent%2.2f)--").length) + f"($percent%2.2f)--" + s"${below_runtime.get}"
			} else " "*segment_width
			val left_string_runtime = if (above_runtime.isDefined) {
				val percent = ((above_runtime.get.toDouble) / current_runtime.toDouble) * 100.0
				s"${above_runtime.get}" + f"--($percent%2.2f)" + "-"*(segment_width - 1 - (f"--($percent%2.2f)" + s"${above_runtime.get}").length)
			} else " "*segment_width
			println(s"Param:  ${left_string_param}${value}${right_string_param}")
			println(s"Cycles: ${left_string_runtime}${current_runtime}${right_string_runtime}")
			println("")
		}
		Map()
	}
}