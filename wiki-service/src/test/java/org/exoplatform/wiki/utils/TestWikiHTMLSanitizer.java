package org.exoplatform.wiki.utils;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 18/08/16.
 */
public class TestWikiHTMLSanitizer extends TestCase {

  public void testWikiEncodeMarkup() {
    /** Encode image attachment **/
    String html = "<!--startimage:false|-|attach|-|images2.jpg-->" + "<img alt=\"images2.jpg\""
        + " src=\"/portal/rest/wiki/attachments/draft/space/root/page/root_A_A_557557F072D724C38EA9791A6EDAE263/images2.jpg?width=981\""
        + " /><!--stopimage-->";
    String result = null;
    try {
      result = WikiHTMLSanitizer.markupSanitize(html);
    } catch (Exception e) {
      fail();
    }
    assertNotNull(result);
    assertTrue(result.startsWith("<!--startimage:false|-|attach|-|images2.jpg-->"));
    assertTrue(result.endsWith("<!--stopimage-->"));

    /** Encode file attachment **/

    html = "<!--startwikilink:true|-|attach|-|thread_dump-20160721-1441.txt-->" + "<span class=\"wikiattachmentlink\">"
        + "<a href=\"/portal/rest/wiki/attachments/draft/space/root/page/root_A_A_88C3C79992BD40BC1FB19E09CDA9E333/thread_dump-20160721-1441.txt\">"
        + "thread_dump-20160721-1441.txt</a></span><!--stopwikilink-->";

    try {
      result = WikiHTMLSanitizer.markupSanitize(html);
    } catch (Exception e) {
      fail();
    }
    assertNotNull(result);
    assertTrue(result.startsWith("<!--startwikilink:true|-|attach|-|thread_dump-20160721-1441.txt-->"));
    assertTrue(result.endsWith("<!--stopwikilink-->"));

    /** Encode wikilink **/
    html = "<ul><li><!--startwikilink:false|-|doc|-||-|anchor=\"HRationale\"--><span class=\"wikilink\">"
        + "<a rel=\"nofollow\" href=\"#HRationale\">Rationale</a></span><!--stopwikilink-->"
        + "</li></ul><p><br/></p><h1 id=\"HRationale\"><span>Rationale</span></h1>";

    try {
      result = WikiHTMLSanitizer.markupSanitize(html);
    } catch (Exception e) {
      fail();
    }
    assertNotNull(result);
    assertTrue(result.contains("<!--startwikilink:false|-|doc|-||-|anchor=\"HRationale\"-->"));
    assertTrue(result.contains("<!--stopwikilink-->"));
  }
}
