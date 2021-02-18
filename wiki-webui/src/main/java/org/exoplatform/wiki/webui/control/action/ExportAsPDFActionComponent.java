package org.exoplatform.wiki.webui.control.action;

import com.lowagie.text.pdf.BaseFont;
import org.apache.commons.codec.binary.Base64;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.control.action.core.AbstractEventActionComponent;
import org.exoplatform.wiki.webui.control.filter.IsUserFilter;
import org.exoplatform.wiki.webui.control.filter.IsViewModeFilter;
import org.exoplatform.wiki.webui.control.listener.MoreContainerActionListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.resource.FSEntityResolver;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.Arrays;
import java.util.List;


@ComponentConfig (
		  template = "app:/templates/wiki/webui/control/action/AbstractActionComponent.gtmpl",
		  events = {
		        @EventConfig(listeners = ExportAsPDFActionComponent.ExportAsPDFActionListener.class)
		    }
		)

public class ExportAsPDFActionComponent extends AbstractEventActionComponent {
  public static final String ACTION  = "ExportAsPDF";
  private static final Log LOG  = ExoLogger.getLogger(ExportAsPDFActionComponent.class.getName());
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsViewModeFilter(), new IsUserFilter() });
  
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  
  public String getActionName() {
    return ACTION;
  }  
  
  @Override
  public boolean isAnchor() {
    return false;
  }  
  
  
  public static class ExportAsPDFActionListener extends MoreContainerActionListener<ExportAsPDFActionComponent> {
    protected void processEvent(Event<ExportAsPDFActionComponent> event) throws Exception {    	
      Page currentPage = Utils.getCurrentWikiPage();
      InputStream in = getClass().getResourceAsStream("/css/PDFStylesheet.css");
      DataInputStream dataIn = new DataInputStream(in);
      String line;
      StringBuilder stringBuilder = new StringBuilder();
      while ( (line = dataIn.readLine()) != null)
      	stringBuilder.append(line);
      dataIn.close();
      String css = "<head><style type=\"text/css\"> " + stringBuilder.toString() + " </style></head>";
      String title = currentPage.getTitle();
      String content = "<h1>" + title +"</h1><hr />" + htmlToxhtmlAndBase64Images(currentPage);
      String encoding = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
      content = encoding + "<!DOCTYPE xsl:stylesheet [<!ENTITY nbsp \"&#160;\">]><html>" + css + "<body>" + content + "</body></html>";
      
      File pdfFile = createPDFFile(title, content);
      DownloadService dservice = (DownloadService) ExoContainerContext.getCurrentContainer()
        .getComponentInstanceOfType(DownloadService.class);
      InputStreamDownloadResource dresource = new InputStreamDownloadResource(
					new BufferedInputStream(new FileInputStream(pdfFile)), "application/pdf, application/x-pdf, application/acrobat, " +
							"applications/vnd.pdf, text/pdf, text/x-pdf");
      dresource.setDownloadName(title + ".pdf") ;
      String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;

      RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();      
      requireJS.require("SHARED/UIWikiPortlet", "UIWikiPortlet").addScripts("UIWikiPortlet.ajaxRedirect('" + downloadLink + "');");
      super.processEvent(event);      
    }
    
    
    private File createPDFFile(String title, String content) throws IOException {
      File pdfFile = null;
      OutputStream os = null;
      if (title.length() < 3) {
        title += "tmp";
      }
      try {
        pdfFile = File.createTempFile(title, ".pdf");
        os = new FileOutputStream(pdfFile);
        ITextRenderer renderer = new ITextRenderer();
        renderer.getFontResolver().addFont("/fonts/ARIALUNI.TTF", 
                BaseFont.IDENTITY_H, 
                BaseFont.NOT_EMBEDDED);
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setValidating(false);
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        builder.setEntityResolver(FSEntityResolver.instance());
        Document document = builder.parse(new ByteArrayInputStream(content.toString().getBytes()));
        renderer.getSharedContext().setReplacedElementFactory(new CustomImageReplacedElementFactory(renderer.getSharedContext().getReplacedElementFactory()));
        renderer.getSharedContext().getTextRenderer().setSmoothingThreshold(0);
        renderer.setDocument(document, CommonsUtils.getCurrentDomain());
        renderer.layout();
        renderer.createPDF(os);
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Have unexpected exception while converting to PDF", e);
        }
      } finally {
          os.close();
      }
      return pdfFile;
    }
    private String htmlToxhtmlAndBase64Images(Page page) throws WikiException {
      WikiService wikiService = CommonsUtils.getService(WikiService.class);
      org.jsoup.nodes.Document doc = Jsoup.parse(page.getContent());
      doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
      Elements elements = doc.getAllElements();
      for(Element element : elements){
        if(element.tagName().equals("img") && !element.attr("src").isEmpty()){
          String src = element.attr("src");
          String imgName = src.substring(src.lastIndexOf("/") + 1);
          Attachment imageAttachment = wikiService.getAttachmentOfPageByName(imgName, page, true);
          if(imageAttachment == null) {
            Page parentPage = wikiService.getParentPageOf(page);
            imageAttachment = wikiService.getAttachmentOfPageByName(imgName, parentPage, true);
          }
          if(imageAttachment != null && imageAttachment.getMimeType().startsWith("image/")){
            byte[] bytes = imageAttachment.getContent();
            element.attr("src", "base64," + Base64.encodeBase64String(bytes));
            element.attr("style","width:100%;height:100%");
          }
        }
      }
      return doc.html();
    }
  }

}
