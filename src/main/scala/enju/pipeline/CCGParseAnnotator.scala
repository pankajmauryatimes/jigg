package enju.pipeline

import enju.ccg.JapaneseShiftReduceParsing
import enju.ccg.lexicon.{ PoSTaggedSentence, Derivation, Point }
import enju.ccg.parser.KBestDecoder
import enju.util.PropertiesUtil

import java.util.Properties
import scala.xml._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer


/** Currently this class is ugly; it largely depends on global variables defined in enju.ccg.Options.
  * TODO: revise this class and ShiftReduceParsing class.
  */
class CCGParseAnnotator(val name: String, val props: Properties) extends SentencesAnnotator {

  val parsing = new JapaneseShiftReduceParsing
  configParsing

  val tagger = parsing.tagging.getTagger
  val decoder = parsing.getPredDecoder

  def configParsing = {
    import PropertiesUtil.findProperty
    findProperty(name + ".model", props) foreach { enju.ccg.InputOptions.loadModelPath = _ }
    findProperty(name + ".beta", props) foreach { x => enju.ccg.TaggerOptions.beta = x.toDouble }
    findProperty(name + ".maxK", props) foreach { x => enju.ccg.TaggerOptions.maxK = x.toInt }
    findProperty(name + ".beam", props) foreach { x => enju.ccg.ParserOptions.beam = x.toInt }
    parsing.load
  }

  val numKbest: Int = PropertiesUtil.findProperty(name + ".numKbest", props) map(_.toInt) getOrElse(1)
  val preferConnected: Boolean = PropertiesUtil.getBoolean(name + ".preferConnected", props) getOrElse(false)

  override def newSentenceAnnotation(sentence: Node) = {
    val sentenceID = (sentence \ "@id").toString // s12
    val tokens = sentence \ "tokens"
    val tokenSeq = tokens \ "token"

    val posTaggedSentence = SentenceConverter.toTaggedSentence(tokenSeq)
    val derivs: Seq[(Derivation, Double)] = getDerivations(posTaggedSentence)

    val point2id = getPoint2id(derivs.unzip._1)

    def ccgAnnotation(derivID: Int, deriv: Derivation, score: Double): Node = {
      val ccgID = sentenceID + "_ccg" + derivID // e.g., s12_ccg0

      val spans = new ArrayBuffer[Node]

      def spanID(pointid: Int) = sentenceID + "_sp" + pointid

      deriv.roots foreach { root =>
        deriv foreachPoint({ point =>
          val pid = point2id(derivID, point)

          val rule = deriv.get(point).get
          val ruleSymbol = rule.ruleSymbol match {
            case "" => None
            case symbol => Some(Text(symbol))
          }
          val childIDs = rule.childPoint.points map { p => spanID(point2id(derivID, p)) } match {
            case Seq() => None
            case ids => Some(Text(ids.mkString(" ")))
          }
          val terminalID = childIDs match {
            case None => tokenSeq(point.x).attribute("id")
            case _ => None
          }

          spans += <span id={ spanID(pid) } begin={ point.x.toString } end={ point.y.toString } category={ point.category.toString } rule={ ruleSymbol } child={ childIDs } terminal={ terminalID } />
        }, root)
      }

      val rootIDs = deriv.roots.map { p => spanID(point2id(derivID, p)) }.mkString(" ")

      <ccg root={ rootIDs } id={ ccgID } score={ score.toString }>{ spans }</ccg>
    }

    val ccgs = derivs.zipWithIndex map { case ((deriv, score), i) => ccgAnnotation(i, deriv, score) }

    enju.util.XMLUtil.addChild(sentence, ccgs)
  }

  object SentenceConverter {
    val dict = parsing.tagging.dict
    def toTaggedSentence(tokenSeq: NodeSeq) = {
      val terminalSeq = tokenSeq map { token =>
        val surf = dict.getWordOrCreate(token \ "@surf" toString())
        val base = dict.getWordOrCreate(token \ "@base" toString())
        val katsuyou = token \ "@inflectionForm" toString() match {
          case "*" => "_"; case x => x
        }
        val posSeq = Seq("@pos", "@pos1", "@pos2", "@pos3") map { token \ _ toString() }
        val pos = posSeq.indexOf("*") match {
          case -1 => posSeq.mkString("-")
          case idx => posSeq.take(idx).mkString("-")
        }

        val combinedPoS = dict.getPoSOrCreate(pos + "/" + katsuyou)

        (surf, base, combinedPoS)
      }
      new PoSTaggedSentence(terminalSeq.map(_._1), terminalSeq.map(_._2), terminalSeq.map(_._3))
    }
  }

  def getDerivations(sentence: PoSTaggedSentence): Seq[(Derivation, Double)] = {
    val beta = enju.ccg.TaggerOptions.beta
    val maxK = enju.ccg.TaggerOptions.maxK
    val beam = enju.ccg.ParserOptions.beam
    val superTaggedSentence = sentence.assignCands(tagger.candSeq(sentence, beta, maxK))

    decoder match {
      case decoder: KBestDecoder =>
        (numKbest, preferConnected) match {
          case (1, true) => Seq(decoder.predictConnected(superTaggedSentence))
          case (1, false) => Seq(decoder.predict(superTaggedSentence))
          case (k, prefer) => decoder.predictKbest(k, superTaggedSentence, prefer)
        }
      case decoder => Seq(decoder.predict(superTaggedSentence))
    }
  }

  def getPoint2id(derivs: Seq[Derivation]): Map[(Int, Point), Int] = {
    val map = new HashMap[(Int, Point), Int]
    var i = 0
    derivs.zipWithIndex foreach { case (deriv, derivID) =>
      deriv.roots foreach { root =>
        deriv foreachPoint({ point =>
          map += (derivID, point) -> i
          i += 1
        }, root)
      }
    }
    map.toMap
  }

  override def requires = Set(Annotator.JaTokenize)
  override def requirementsSatisfied = Set(Annotator.JaCCG)
}
