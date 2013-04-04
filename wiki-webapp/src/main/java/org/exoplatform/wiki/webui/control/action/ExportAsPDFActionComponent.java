package org.exoplatform.wiki.webui.control.action;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.control.action.core.AbstractEventActionComponent;
import org.exoplatform.wiki.webui.control.filter.AdminPagesPermissionFilter;
import org.exoplatform.wiki.webui.control.filter.IsUserFilter;
import org.exoplatform.wiki.webui.control.filter.IsViewModeFilter;
import org.exoplatform.wiki.webui.control.listener.MoreContainerActionListener;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


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
      Utils.setUpWikiContext(event.getSource().getAncestorOfType(UIWikiPortlet.class));
      RenderingService renderingService = (RenderingService) ExoContainerContext.getCurrentContainer()
          .getComponentInstanceOfType(RenderingService.class);  
    	
      PageImpl currentPage = (PageImpl) Utils.getCurrentWikiPage();      
      InputStream in = getClass().getResourceAsStream("/css/PDFStylesheet.css");
      DataInputStream dataIn = new DataInputStream(in);
      String line;
      StringBuilder stringBuilder = new StringBuilder();
      while ( (line = dataIn.readLine()) != null)
      	stringBuilder.append(line);
      dataIn.close();
            
      String css = "<head><style type=\"text/css\"> " + stringBuilder.toString() + " </style></head>";
      String title = currentPage.getTitle();
      String content = "<h1>" + title +"</h1><hr />" + renderingService.render("[[image:wiki.png]]"
        + currentPage.getContent().getText(), currentPage.getSyntax(), Syntax.XHTML_1_0.toIdString(), false);
      content = "<!DOCTYPE xsl:stylesheet [<!ENTITY nbsp \"&#160;\">]><html>" + css + "<body>" + content + "</body></html>"; 	
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
      try {
        pdfFile = File.createTempFile(title, ".pdf");
        os = new FileOutputStream(pdfFile);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(content);
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
  }
}
