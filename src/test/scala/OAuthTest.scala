package javaj.http

import org.junit.Assert._
import org.junit.Test
import fj._
import fj.P.p

class OAuthTest {

  @Test
  def oauthShoulCorrectlySign(){
    // from example http://hueniverse.com/2008/10/beginners-guide-to-oauth-part-iv-signing-requests/
    val params = fj.data.List.list(
      p("oauth_nonce","kllo9940pd9333jh"),
      p("oauth_timestamp","1191242096")
    )

    val url = "http://photos.example.net/photos"
    val req = Http.get(url).param("file", "vacation.jpg").param("size", "original")

    val a = OAuth.getSig(params, req,new Token("dpf43f3p2l4k3l03","kd94hf93k423kf44"), data.Option.some(new Token("nnch734d00sl2jdk","pfkkdhi9sl3r4s00")),data.Option.none[String])

    assertEquals(a._2, "tR3+Ty81lMeYAr/Fid0kMTYa/WM=")
  }
}
