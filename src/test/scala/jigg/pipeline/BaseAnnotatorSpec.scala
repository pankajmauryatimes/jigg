package jigg.pipeline

/*
 Copyright 2013-2016 Hiroshi Noji

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

import java.util.Properties
import org.scalactic.Equality
import org.scalatest._
import scala.xml._

trait BaseAnnotatorSpec extends FlatSpec with Matchers {

  val sameElem = new Equality[Node] {
    import scala.xml.Utility.trim
    override def areEqual(a: Node, b: Any) = b match {
      case n: Node => trim(a) == trim(n)
      case _ => false
    }
  }

}
