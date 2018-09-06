package spatial.codegen.dotgen

import scala.xml._
import scala.xml.transform._

object DotHtmlCodegen {
  def load(fileName:String) = {
    val xml = XML.loadFile(fileName)

    val rule = new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case node: Elem if node \@ "class" == "node" =>
          val title = (node \ "title").text
          println(title)
          node.copy(
            child = node.child.flatMap {
              case node:Elem if node.attribute("rx").nonEmpty =>
                //node % 
                //Attribute(None, "onmousemove", Text(s""""showTooltip(evt, '${title}');"""")) % 
                //Attribute(None, "onmousemove", Text(s""""hideTooltip();"""")) 
                node
              case node => node
            }
          )
          print(node)
          node
          case node => node
      }
    }
    val transform = new RuleTransformer(rule)
    transform(xml)
  }

}
