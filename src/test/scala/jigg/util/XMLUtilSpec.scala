package jigg.util

/*
 Copyright 2013-2015 Hiroshi Noji

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

package jigg.pipeline

import java.util.Properties

import org.scalatest._

class XMLUtilSpec extends FlatSpec with Matchers {
  import XMLUtil._

  "replaceAll" should "visit all elements" in {
    val xml =
      <root>
        <document>
          <sentence id={"s1"}>{ "hoge" }</sentence>
          <sentence id={"s2"}>{ "huga" }</sentence>
        </document>
      </root>

    val newXml = replaceAll(xml, "sentence") { sentence =>
      addChild(sentence, <child>{ "child" }</child>)
    }
    val sentence = newXml \ "document" \ "sentence"

    sentence.size should be (2)
    (sentence(0) \ "child").text should be ("child")
    (sentence(1) \ "child").text should be ("child")
  }

  "addOrOverwriteChild" should "override child when only attrs differ" in {
    val xml = <sentence><dependencies type="basic"/></sentence>
    val newChild = <dependencies type="collapsed"/>

    val newXml = XMLUtil.addOrOverwriteChild(xml, newChild)

    newXml should be (<sentence><dependencies type="collapsed"/></sentence>)
  }

  it should "not override child when attrs differ if attr is specified" in {
    val xml = <sentence><dependencies type="basic"/></sentence>
    val newChild = <dependencies type="collapsed"/>

    val newXml = XMLUtil.addOrOverwriteChild(xml, newChild, Some("type"))

    newXml should be (
      <sentence><dependencies type="basic"/><dependencies type="collapsed"/></sentence>)
  }
}
