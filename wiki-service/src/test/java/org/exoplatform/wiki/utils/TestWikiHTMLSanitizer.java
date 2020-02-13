package org.exoplatform.wiki.utils;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 18/08/16.
 */
public class TestWikiHTMLSanitizer extends TestCase {

  public void testWikiEncodeMarkup() {

    /** Encode Full Template How-To guide content **/

    String html = "<!--startmacro:tip|-||-|[[Get some tips>>http://conflatulence.blogspot.com/2010/03/how-to-guide-recipe.html]] on using this template-->\n" +
            "<div class=\"box tipmessage\">\n" +
            "<!--startwikilink:false|-|url|-|http://conflatulence.blogspot.com/2010/03/how-to-guide-recipe.html-->\n" +
            "<span class=\"wikiexternallink\"><a href=\"http://conflatulence.blogspot.com/2010/03/how-to-guide-recipe.html\">Get some tips</a></span>\n" +
            "<!--stopwikilink--> \n" +
            "on using this template</div>\n" +
            "<!--stopmacro-->\n" +
            "<!--startmacro:section|-|justify=\"true\"|-|{{column}}\n" +
            "{{panel  title=\"Table of Contents\"}}\n" +
            "{{toc}}{{/toc}}\n" +
            "{{/panel}}\n" +
            "{{/column}}\n" +
            "\n" +
            "{{column}}\n" +
            "{{panel  title=\"Purpose\"}}\n" +
            "{{excerpt}}__This section should provide the overall purpose of the HOW-TO Guide. (e.g. intended audience, lesson)__{{/excerpt}}\n" +
            "{{/panel}}\n" +
            "{{/column}}-->\n" +
            "<div style=\"text-align:justify;\"><div style=\"float:left;width:49.2%;padding-right:1.5%;\"><div class=\"panel\"><div class=\"panelHeader\">Table of Contents</div><div class=\"panelContent\"><ul><li><ul><li><ul><li><!--startwikilink:true|-|doc|-|null|-|anchor=\"HRequirements\"--><span class=\"wikilink\"><a href=\"#HRequirements\">Requirements</a></span><!--stopwikilink--></li><li><!--startwikilink:true|-|doc|-|null|-|anchor=\"HInstructions\"--><span class=\"wikilink\"><a href=\"#HInstructions\">Instructions</a></span><!--stopwikilink--></li><li><!--startwikilink:true|-|doc|-|null|-|anchor=\"HTips26Warnings\"--><span class=\"wikilink\"><a href=\"#HTips26Warnings\">Tips &amp; Warnings</a></span><!--stopwikilink--></li><li><!--startwikilink:true|-|doc|-|null|-|anchor=\"HRelated\"--><span class=\"wikilink\"><a href=\"#HRelated\">Related</a></span><!--stopwikilink--></li></ul></li></ul></li></ul></div></div></div><div style=\"float:left;width:49.2%;\"><div class=\"panel\"><div class=\"panelHeader\">Purpose</div><div class=\"panelContent\"><div style=\"display: block\" class=\"ExcerptClass\"><ins>This section should provide the overall purpose of the HOW-TO Guide. (e.g. intended audience, lesson)</ins></div></div></div></div><div style=\"clear:both\"></div></div>\n" +
            "<!--stopmacro-->\n" +
            "<h3 id=\"HRequirements\"><span>Requirements</span></h3><ul><li><ins>This short section should outline the requirements of the reader to use the guide.</ins></li></ul><h3 id=\"HInstructions\"><span>Instructions</span></h3><ol><li><ins>This section provides step-by-step instructions for the reader to follow to perform the particular act described in the HOW-TO.</ins></li></ol><h3 id=\"HTips26Warnings\"><span>Tips &amp; Warnings</span></h3><ul><li><ins>List any particular common difficulties that the reader may come across</ins></li></ul><h3 id=\"HRelated\"><span>Related</span></h3><ul><li><ins>Link to any related HOW-TOs or External Links</ins></li><li><ins>One way to improve this template is by replacing \"Related\" with either the</ins> <ins>\n" +
            "<!--startwikilink:false|-|url|-|http://confluence.atlassian.com/x/mDgC-->\n" +
            "<span class=\"wikiexternallink\"><a href=\"http://confluence.atlassian.com/x/mDgC\">related-labels</a></span>\n" +
            "<!--stopwikilink-->\n" +
            "</ins> <ins>or</ins> <ins>\n" +
            "<!--startwikilink:false|-|url|-|http://confluence.atlassian.com/x/njgC-->\n" +
            "<span class=\"wikiexternallink\"><a href=\"http://confluence.atlassian.com/x/njgC\">contentbylabel</a></span>\n" +
            "<!--stopwikilink-->\n" +
            "</ins> <ins>macro</ins></li></ul>";
    String result = null;
    try {
      result = WikiHTMLSanitizer.markupSanitize(html);
    } catch (Exception e) {
      fail();
    }
    assertNotNull(result);
    assertTrue(result.startsWith("<!--startmacro:tip|-||-|[[Get some tips>>http://conflatulence.blogspot.com/2010/03/how-to-guide-recipe.html]] on using this template"));

    /** Encode image attachment **/

    html = "<!--startimage:false|-|attach|-|images2.jpg-->" + "<img alt=\"images2.jpg\""
            + " src=\"/portal/rest/wiki/attachments/draft/space/root/page/root_A_A_557557F072D724C38EA9791A6EDAE263/images2.jpg?width=981\""
            + " /><!--stopimage-->";
    result = null;
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
