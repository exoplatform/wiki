/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.rendering.impl;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;

public class TestMacroRendering extends AbstractRenderingTestCase {  
  
  @Override
  protected void setUp() throws Exception {
    
    super.setUp();
    setupDefaultWikiContext();
  }

  public void testRenderNoteMacro() throws Exception {
    String expectedHtml = "<div class=\"box notemessage\">This is a note.</div>";
    assertEquals(expectedHtml, renderingService.render("{{note}}This is a note.{{/note}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("{note}This is a note.{note}", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testRenderTipMacro() throws Exception {
    String expectedHtml = "<div class=\"box tipmessage\">This is a tip.</div>";
    assertEquals(expectedHtml, renderingService.render("{{tip}}This is a tip.{{/tip}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("{tip}This is a tip.{tip}", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testRenderInfoMacro() throws Exception {
    String expectedHtml = "<div class=\"box infomessage\">This is an info.</div>";
    assertEquals(expectedHtml, renderingService.render("{{info}}This is an info.{{/info}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("{info}This is an info.{info}", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }

  public void testRenderWarningMacro() throws Exception {
    String expectedHtml = "<div class=\"box warningmessage\">This is an warning.</div>";
    assertEquals(expectedHtml, renderingService.render("{{warning}}This is an warning.{{/warning}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("{warning}This is an warning.{warning}", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }

  public void testRenderErrorMacro() throws Exception {
    String expectedHtml = "<div class=\"box errormessage\">This is an error.</div>";
    assertEquals(expectedHtml, renderingService.render("{{error}}This is an error.{{/error}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }

  /*public void testRenderCodeMacro() throws Exception {
    String expectedHtml = "<div class=\"box code\"><span style=\"font-weight: bold; color: #8B008B; \">&lt;html&gt;&lt;head&gt;</span>Cool!<span style=\"font-weight: bold; color: #8B008B; \">&lt;/head&gt;&lt;/html&gt;</span></div>";
    assertEquals(expectedHtml, renderingService.render("{{code language=\"java\"}}<html><head>Cool!</head></html>{{/code}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }*/
  
  public void testRenderSectionAndColumnMacro() throws Exception {
    String expectedHtml = "<div><div style=\"float:left;width:49.2%;padding-right:1.5%;\"><p>Column one text goes here</p></div><div style=\"float:left;width:49.2%;\"><p>Column two text goes here</p></div><div style=\"clear:both\"></div></div>";
    assertEquals(expectedHtml, renderingService.render("{{section}}\n\n{{column}}Column one text goes here{{/column}}\n\n{{column}}Column two text goes here{{/column}}\n\n{{/section}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("{section}\n{column}Column one text goes here{column}\n{column}Column two text goes here{column}\n{section}", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testRenderNoFormatMacro() throws Exception {
    String expectedXWikiHtml = "<pre>pre-formatted piece of text so **no** further __formatting__ is done here</pre>";
    String expectedConfluenceHtml = "<pre>pre-formatted piece of text so *no* further _formatting_ is done here</pre>";
    assertEquals(expectedXWikiHtml, renderingService.render("{{noformat}}pre-formatted piece of text so **no** further __formatting__ is done here{{/noformat}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedConfluenceHtml, renderingService.render("{noformat}pre-formatted piece of text so *no* further _formatting_ is done here{noformat}", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testRenderPanelMacro() throws Exception {
    String expectedHtml = "<div class=\"panel\"><div class=\"panelHeader\">My Title</div><div class=\"panelContent\"><p>Some text with a title</p></div></div>";
    assertEquals(expectedHtml, renderingService.render("{{panel title=\"My Title\"}}Some text with a title{{/panel}}", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("{panel:title=My Title}Some text with a title{panel}", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testTocMacro() throws Exception {
    String xwikiExpectedHtml = "<ol><li><span class=\"wikilink\"><a href=\"#HH1\">H1</a></span><ol><li><span class=\"wikilink\"><a href=\"#HH2\">H2</a></span><ol><li><span class=\"wikilink\"><a href=\"#HH3\">H3</a></span></li></ol></li></ol></li></ol><h1 id=\"HH1\"><span>H1</span></h1><p>&nbsp;</p><h2 id=\"HH2\"><span>H2</span></h2><p>&nbsp;</p><h3 id=\"HH3\"><span>H3</span></h3>";
    String confluenceExpectedHtml = "<ol><li><span class=\"wikilink\"><a href=\"#HH1\">H1&nbsp;</a></span><ol><li><span class=\"wikilink\"><a href=\"#HH2\">H2&nbsp;</a></span><ol><li><span class=\"wikilink\"><a href=\"#HH3\">H3&nbsp;</a></span></li></ol></li></ol></li></ol><h1 id=\"HH1\"><span>H1&nbsp;</span></h1><h2 id=\"HH2\"><span>H2&nbsp;</span></h2><h3 id=\"HH3\"><span>H3&nbsp;</span></h3>";
    assertEquals(xwikiExpectedHtml, renderingService.render("{{toc numbered=\"true\"}} {{/toc}}\n= H1 = \n == H2 == \n === H3 ===", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(confluenceExpectedHtml, renderingService.render("{toc:numbered=\"true\"}\nh1. H1 \nh2. H2 \nh3. H3 ", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testIncludePageMacro() throws Exception {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Page home = wiki.getWikiHome();
    String content = "Test include contents of a page";
    home.setContent(content);
    wikiService.updatePage(home, PageUpdateType.EDIT_PAGE_CONTENT);
    String expectedHtml = "<div class=\"IncludePage \" ><p>" + content + "</p></div>";
    assertEquals(expectedHtml, renderingService.render("{{includepage page=\"Wiki Home\"/}}",
                                                       Syntax.XWIKI_2_0.toIdString(),
                                                       Syntax.XHTML_1_0.toIdString(),
                                                       false));
    // Test recursive inclusion
    String content2 = "{includepage:page=\"Wiki Home\"}";
    home.setContent(content2);
    wikiService.updatePage(home, PageUpdateType.EDIT_PAGE_CONTENT);
    String renderedHTML =   renderingService.render("{includepage:page=\"Wiki Home\"}",
                                                    Syntax.CONFLUENCE_1_0.toIdString(),
                                                    Syntax.XHTML_1_0.toIdString(),
                                                    false);
    assertEquals(1, (StringUtils.countMatches(renderedHTML, "<div class=\"IncludePage \" >")));        
  }

  public void testChildrenMacro() throws Exception {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    wikiService.createPage(wiki, "WikiHome", new Page("samplePage", "samplePage"));
    wikiService.createPage(wiki, "samplePage", new Page("childPage1", "childPage1"));
    wikiService.createPage(wiki, "samplePage", new Page("childPage2", "childPage2"));
    wikiService.createPage(wiki, "childPage1", new Page("testPage", "testPage"));

    Execution ec = renderingService.getExecution();
    WikiContext wikiContext = (WikiContext) ec.getContext().getProperty(WikiContext.WIKICONTEXT);
    wikiContext.setPageName("samplePage");
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
    String xwikiExpectedHtml = "<div><ul><li><span class=\"wikilink\"><a href=\"http://localhost:8080/portal/classic/wiki/childPage1\">childPage1</a></span><ul><li><span class=\"wikilink\"><a href=\"http://localhost:8080/portal/classic/wiki/testPage\">testPage</a></span><ul></ul></li></ul></li><li><span class=\"wikilink\"><a href=\"http://localhost:8080/portal/classic/wiki/childPage2\">childPage2</a></span><ul></ul></li></ul></div>";

    assertEquals(xwikiExpectedHtml, renderingService.render("{{children descendant=\"true\"/}}",
                                                            Syntax.XWIKI_2_0.toIdString(),
                                                            Syntax.XHTML_1_0.toIdString(),
                                                            false));
  }

  public void testRenderPageTreeMacro() throws Exception {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    wikiService.createPage(wiki, "WikiHome", new Page("rootPage", "rootPage"));
    wikiService.createPage(wiki, "rootPage", new Page("testPageTree1", "testPageTree1"));
    wikiService.createPage(wiki, "rootPage", new Page("testPageTree2", "testPageTree2"));
    wikiService.createPage(wiki, "testPageTree1", new Page("testPageTree11", "testPageTree11"));

    Execution ec = renderingService.getExecution();
    WikiContext wikiContext = (WikiContext) ec.getContext().getProperty(WikiContext.WIKICONTEXT);
    wikiContext.setPageName("rootPage");
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
    StringBuilder xwikiExpectedHtml = new StringBuilder();
    xwikiExpectedHtml.append("<div class=\"uiTreeExplorer PageTreeMacro\">")
                     .append("  <div>")
                     .append("    <input class=\"ChildrenURL\" title=\"hidden\" type=\"hidden\" value=\"/wiki/tree/children/\" />")
                     .append("    <input class=\"InitParams\" title=\"hidden\" type=\"hidden\" value=\"?path=portal/classic/rootPage&excerpt=false&depth=1\" />")
                     .append("    <input class=\"BaseURL\" title=\"hidden\" type=\"hidden\" value=\"http://localhost:8080/portal/classic/wiki\" />")
                     .append("    <a class=\"SelectNode\" style=\"display:none\" href=\"http://localhost:8080/portal/classic/\" ></a>")
                     .append("  </div>")
                     .append("</div>");

    assertEquals(xwikiExpectedHtml.toString(), renderingService.render("{{pagetree /}}",
                                                                       Syntax.XWIKI_2_0.toIdString(),
                                                                       Syntax.XHTML_1_0.toIdString(),
                                                                       false));
  }

  public void testExcerptMacro() throws Exception {    
    String expectedHtml = "<div style=\"display: block\" class=\"ExcerptClass\"><div class=\"box tipmessage\">Test excerpt</div></div>";
    assertEquals(expectedHtml, renderingService.render("{{excerpt}}{{tip}}Test excerpt{{/tip}}{{/excerpt}}",
                                                            Syntax.XWIKI_2_0.toIdString(),
                                                            Syntax.XHTML_1_0.toIdString(),
                                                            false));
  }
  
  public void testRenderAnchorMacro() throws Exception {
    String expectedHtml = "<span class=\"wikilink\"><a name=\"Hlevel1\" href=\"#Hlevel1\"></a></span>";
    assertEquals(expectedHtml, renderingService.render("{{anchor name=\"level1\" /}}",
                                                       Syntax.XWIKI_2_0.toIdString(),
                                                       Syntax.XHTML_1_0.toIdString(),
                                                       false));
  }
  
  public void testRenderDivMacro() throws Exception {
    String expectedHtml = "<p style=\"text-align:left;color:red\">example div macro</p>";
    String outputConfluence = renderingService.render("{div:style=\"text-align:left;color:red\"}example div macro{div}",
                                                      Syntax.CONFLUENCE_1_0.toIdString(),
                                                      Syntax.XHTML_1_0.toIdString(),
                                                      false);
    assertEquals(expectedHtml, outputConfluence);
    String outputXwiki = renderingService.render("{{div style=\"text-align:left;color:red\"}}example div macro{{/div}}",
                                                 Syntax.XWIKI_2_0.toIdString(),
                                                 Syntax.XHTML_1_0.toIdString(),
                                                 false);
    assertEquals(expectedHtml, outputXwiki);
  }

  public void testRenderSpanMacro() throws Exception {
    String expectedHtml = "<span style=\"font:12pt;color:red\">example span macro</span>";
    String outputConfluence = renderingService.render("{span:style=\"font:12pt;color:red\"}example span macro{span}",
                                                      Syntax.CONFLUENCE_1_0.toIdString(),
                                                      Syntax.XHTML_1_0.toIdString(),
                                                      false);
    assertEquals(expectedHtml, outputConfluence);
    String outputXwiki = renderingService.render("{{span style=\"font:12pt;color:red\"}}example span macro{{/span}}",
                                                 Syntax.XWIKI_2_0.toIdString(),
                                                 Syntax.XHTML_1_0.toIdString(),
                                                 false);
    assertEquals(expectedHtml, outputXwiki);
  }
  
  private void setupDefaultWikiContext() throws ComponentLookupException, ComponentRepositoryException {
    Execution ec = renderingService.getExecution();
    ec.setContext(new ExecutionContext());
    WikiContext wikiContext = new WikiContext();
    wikiContext.setPortalURL("http://localhost:8080/portal/classic/");
    wikiContext.setTreeRestURI("/wiki/tree/children/");
    wikiContext.setRedirectURI("http://localhost:8080/portal/classic/");
    wikiContext.setPortletURI("wiki");
    wikiContext.setBaseUrl("http://localhost:8080/portal/classic/wiki");
    wikiContext.setType("portal");
    wikiContext.setOwner("classic");
    wikiContext.setPageName("WikiHome");
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
  }
  
}
