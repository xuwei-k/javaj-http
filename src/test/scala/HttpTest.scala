package javaj.http

import org.junit.Assert._
import org.junit.Test
import javaj.http.Http._
import fj.{Unit=>_,_}
import scala.{List => _,_}
import java.io._
import java.net._

class HttpTest {

  @Test
  def shouldPrependOptions(){
    val http = Http.get("http://localhost")
    val origOptions = http.options
    val origOptionsLength = origOptions.length
    val emptyEffect = new HttpOptions.HttpOption{
      def f(c:HttpURLConnection){}
    }
    val newOptions: data.List[HttpOptions.HttpOption] = data.List.list(emptyEffect,emptyEffect,emptyEffect)
    val http2 = http.options(newOptions)

    assertEquals(http2.options.length, origOptionsLength + 3)
    assertTrue(Equal.listEqual(Equal.anyEqual[HttpOptions.HttpOption]).eq(http2.options.take(3), newOptions))
    assertEquals(origOptions.length, origOptionsLength)
  }

  @Test
  def lastTimeoutValueShouldWin(){
    val getFunc: HttpExec = new HttpExec(){
      def e(r: Http.Request, c: java.net.HttpURLConnection){}
    }
    val r = Http.Request.apply(getFunc, Http.noopHttpUrl("http://localhost"), "GET").options(HttpOptions.connTimeout(1234)).options(HttpOptions.readTimeout(1234))
    r.process(
      new F[HttpURLConnection,fj.data.Either[Exception,Unit]](){
        def f(c:HttpURLConnection) = {
          assertEquals(c.getReadTimeout, 1234)
          assertEquals(c.getConnectTimeout, 1234)
          fj.data.Either.right(fj.Unit.unit)
        }
      }
    )
  }


}
