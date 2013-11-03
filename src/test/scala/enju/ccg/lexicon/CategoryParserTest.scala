package enju.ccg.lexicon
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class CategoryParserTest extends FunSuite with ShouldMatchers {
  AVMInitializer.init
  
  test("extractAVM") {
    val reader = new CategoryParser.Reader
    val ni_nm = reader.extractAVM("ni,nm")
    ni_nm.toString should equal ("mod=nm,case=ni")
    //assert(ni_nm.toString == "mod=nm,case=ni")
  }
  
  test("createAomicCategory") {
    val cat1Str = "NP[nc,nm]1"
    val cat1 = CategoryParser.parse(cat1Str)
    cat1.toString should equal ("NP[mod=nm,case=nc]")
  }

  test("createComplexCategory") {
    CategoryParser.parse("NP[nc,nm]1／NP[nc,nm]1").toString should equal("NP[mod=nm,case=nc]/NP[mod=nm,case=nc]")
    CategoryParser.parse("(S[nm,stem,nm]＼NP[nc,nm])／NP[nc,nm]").toString should equal(
      """(S[mod=nm,form=stem]\NP[mod=nm,case=nc])/NP[mod=nm,case=nc]""")
    CategoryParser.parse("(((S＼NP)／NP[nc,nm])＼(S[nm,stem]1／NP[o,nm]sem))／NP[nc,nm]1").toString should equal(
      """(((S\NP)/NP[mod=nm,case=nc])\(S[mod=nm,form=stem]/NP[mod=nm,case=o]))/NP[mod=nm,case=nc]""")
    CategoryParser.parse("S1／S1").toString should equal("S/S")
    CategoryParser.parse("(S2／S2)1／(S3／S3)1").toString should equal("(S/S)/(S/S)")
  }
}
