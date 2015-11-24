/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;

public class TestRenderingService extends AbstractRenderingTestCase {

  private WikiService wikiService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
  }

  public void testRender() throws Exception {
    assertEquals("<p>This is <strong>bold</strong></p>", renderingService.render("This is **bold**", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void  testRenderExternalLink() throws Exception{
    String expectedHttpHtml = "<p><span class=\"wikiexternallink\"><a href=\"http://exoplatform.com\">eXo</a></span></p>";
    assertEquals(expectedHttpHtml, renderingService.render("[eXo|http://exoplatform.com]", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    String expectedMailtoHtml = "<p><span class=\"wikiexternallink\"><a href=\"mailto:exoplatform.com\">Mail to eXo</a></span></p>";
    assertEquals(expectedMailtoHtml, renderingService.render("[Mail to eXo|mailto:exoplatform.com]", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testRenderAnExistedInternalLink() throws Exception {
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");

    Page page = wikiService.createPage(wiki, "WikiHome", new Page("CreateWikiPage-002", "CreateWikiPage-002"));

    Attachment attachment = new Attachment();
    attachment.setName("eXoWikiHome.png");
    attachment.setMimeType("image/png");
    attachment.setContent("logo".getBytes());
    wikiService.addAttachmentToPage(attachment, page);
    
    Execution ec = renderingService.getExecution();
    ec.setContext(new ExecutionContext());
    WikiContext wikiContext = new WikiContext();
    wikiContext.setPortalURL("http://loclahost:8080/portal/classic/");
    wikiContext.setPortletURI("wiki");
    wikiContext.setType("portal");
    wikiContext.setOwner("classic");
    wikiContext.setPageName("CreateWikiPage-002");
    
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
    
    String expectedHtml = "<p><span class=\"wikilink\"><a href=\"http://loclahost:8080/portal/classic/wiki/CreateWikiPage-002\">CreateWikiPage-002</a></span></p>";
    assertEquals(expectedHtml, renderingService.render("[[CreateWikiPage-002>>CreateWikiPage-002]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("[[CreateWikiPage-002>>classic.CreateWikiPage-002]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("[[CreateWikiPage-002>>portal:classic.CreateWikiPage-002]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }
  
  public void testRenderCreatePageLink() throws Exception {
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    
    Execution ec = renderingService.getExecution();
    ec.setContext(new ExecutionContext());
    WikiContext wikiContext = new WikiContext();
    wikiContext.setPortalURL("http://loclahost:8080/portal/classic/");
    wikiContext.setPortletURI("wiki");
    wikiContext.setType("portal");
    wikiContext.setOwner("classic");
    wikiContext.setPageName("WikiHome");
    
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
    
    String expectedHtml = "<p><span class=\"wikicreatelink\"><a href=\"http://loclahost:8080/portal/classic/wiki/WikiHome?action=AddPage&amp;pageTitle=NonExistedWikiPage-001&amp;wiki=classic&amp;wikiType=portal\">NonExistedWikiPage-001</a></span></p>";
    assertEquals(expectedHtml, renderingService.render("[[NonExistedWikiPage-001>>NonExistedWikiPage-001]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("[[NonExistedWikiPage-001>>classic.NonExistedWikiPage-001]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedHtml, renderingService.render("[[NonExistedWikiPage-001>>portal:classic.NonExistedWikiPage-001]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }

  public void testRenderAttachmentsAndImages() throws Exception {
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");

    Page page = wikiService.createPage(wiki, "WikiHome", new Page("CreateWikiPage-003", "CreateWikiPage-003"));

    Attachment attachment1 = new Attachment();
    attachment1.setName("space in name.png");
    attachment1.setContent("logo".getBytes());
    attachment1.setMimeType("image/png");
    wikiService.addAttachmentToPage(attachment1, page);
    Attachment attachment2 = new Attachment();
    attachment2.setName("eXoWikiHome.png");
    attachment2.setContent("logo".getBytes());
    attachment2.setMimeType("image/png");
    wikiService.addAttachmentToPage(attachment2, page);
        
    Execution ec = renderingService.getExecution();
    ec.setContext(new ExecutionContext());
    WikiContext wikiContext = new WikiContext();
    wikiContext.setPortalURL("http://loclahost:8080/portal/classic");
    wikiContext.setPortletURI("wiki");
    wikiContext.setType("portal");
    wikiContext.setOwner("classic");
    wikiContext.setPageName("CreateWikiPage-003");
    
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
    
    String expectedXwikiAttachmentHtml = "<p><span class=\"wikiattachmentlink\"><a href=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\">eXoWikiHome.png</a></span></p>";
     assertEquals(expectedXwikiAttachmentHtml, renderingService.render("[[eXoWikiHome.png>>attach:eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedXwikiAttachmentHtml, renderingService.render("[[eXoWikiHome.png>>attach:CreateWikiPage-003@eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedXwikiAttachmentHtml, renderingService.render("[[eXoWikiHome.png>>attach:classic.CreateWikiPage-003@eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    assertEquals(expectedXwikiAttachmentHtml, renderingService.render("[[eXoWikiHome.png>>attach:portal:classic.CreateWikiPage-003@eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
     
    String expectedConfluenceAttachmentHtml = "<p><span class=\"wikiattachmentlink\"><a href=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\"><span class=\"wikigeneratedlinkcontent\">eXoWikiHome.png</span></a></span></p>";
     assertEquals(expectedConfluenceAttachmentHtml, renderingService.render("[^eXoWikiHome.png]", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
    String expectedConfluenceLabelAttachmentHtml = "<p><span class=\"wikiattachmentlink\"><a href=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\">eXoWikiHome.png</a></span></p>";
     assertEquals(expectedConfluenceLabelAttachmentHtml, renderingService.render("[eXoWikiHome.png|^eXoWikiHome.png]", Syntax.CONFLUENCE_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
     
    String expectedImageHtml = "<p><img src=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\" alt=\"eXoWikiHome.png\"/></p>";
     assertEquals(expectedImageHtml, renderingService.render("[[image:eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
     
    String expectedImageHtmlPageName = "<p><img src=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\" alt=\"CreateWikiPage-003@eXoWikiHome.png\"/></p>";
    assertEquals(expectedImageHtmlPageName, renderingService.render("[[image:CreateWikiPage-003@eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
     
    String expectedImageHtmlWikiName = "<p><img src=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\" alt=\"classic.CreateWikiPage-003@eXoWikiHome.png\"/></p>";
    assertEquals(expectedImageHtmlWikiName, renderingService.render("[[image:classic.CreateWikiPage-003@eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
     
    String expectedImageHtmlSpaceName = "<p><img src=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\" alt=\"portal:classic.CreateWikiPage-003@eXoWikiHome.png\"/></p>";
    assertEquals(expectedImageHtmlSpaceName, renderingService.render("[[image:portal:classic.CreateWikiPage-003@eXoWikiHome.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
     
    String expectedFreeStandingImageHtml = "<p><img src=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/eXoWikiHome.png\" class=\"wikimodel-freestanding\" alt=\"eXoWikiHome.png\"/></p>";
     assertEquals(expectedFreeStandingImageHtml, renderingService.render("image:eXoWikiHome.png", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
     
    String expectedImageSpaceInName = "<p><img src=\"http://loclahost:8080/portal/rest/wiki/images/portal/space/classic/page/CreateWikiPage-003/space_in_name.png\" alt=\"space in name.png\"/></p>";
     assertEquals(expectedImageSpaceInName, renderingService.render("[[image:space in name.png]]", Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false));
  }

  public void testGetContentOfSection() throws Exception {
    String content = "= Section 1 =\n== Section 1.1 ==\n== Section 1.2 ==\n= Section 2 =\n== Section 2.1 ==\n== Section 2.2 ==";
    assertEquals("= Section 1 =\n\n== Section 1.1 ==\n\n== Section 1.2 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "1"));
    assertEquals("== Section 1.1 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "2"));
    assertEquals("== Section 1.2 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "3"));
    assertEquals("= Section 2 =\n\n== Section 2.1 ==\n\n== Section 2.2 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "4"));
    assertEquals("== Section 2.1 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "5"));
    assertEquals("== Section 2.2 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "6"));
  }

  public void testUpdateContentOfSection() throws Exception {
    String content = "= Section 1 =\n== Section 1.1 ==\n== Section 1.2 ==\n= Section 2 =\n== Section 2.1 ==\n== Section 2.2 ==";
    content = renderingService.updateContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "1", "= Section 1 updated =\n\n== Section 1.1 ==\n\n== Section 1.2 ==");
    assertEquals("= Section 1 updated =\n\n== Section 1.1 ==\n\n== Section 1.2 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "1"));
    content = renderingService.updateContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "2", "== Section 1.1 updated ==");
    assertEquals("== Section 1.1 updated ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "2"));
    content = renderingService.updateContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "3", "== Section 1.2 updated ==");
    assertEquals("== Section 1.2 updated ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "3"));
    content = renderingService.updateContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "4", "= Section 2 updated =\n\n== Section 2.1 ==\n\n== Section 2.2 ==");
    assertEquals("= Section 2 updated =\n\n== Section 2.1 ==\n\n== Section 2.2 ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "4"));
    content = renderingService.updateContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "5", "== Section 2.1 updated ==");
    assertEquals("== Section 2.1 updated ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "5"));
    content = renderingService.updateContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "6", "== Section 2.2 updated ==");
    assertEquals("== Section 2.2 updated ==", renderingService.getContentOfSection(content, Syntax.XWIKI_2_0.toIdString(), "6"));
  }
  
  public void testEscapeString() throws Exception {
    String expectedHtml = "<p><tt class=\"wikimodel-verbatim\">_</tt>hello</p>";
    String outputConfluence = renderingService.render("\\_hello",
                                                      Syntax.CONFLUENCE_1_0.toIdString(),
                                                      Syntax.XHTML_1_0.toIdString(),
                                                      false);
    assertEquals(expectedHtml, outputConfluence);
  }
}
