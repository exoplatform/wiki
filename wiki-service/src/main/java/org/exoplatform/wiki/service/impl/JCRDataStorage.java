package org.exoplatform.wiki.service.impl;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.StringUtils;
import org.chromattic.api.ChromatticSession;
import org.chromattic.common.IO;
import org.chromattic.core.api.ChromatticSessionImpl;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.Template;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.template.plugin.WikiTemplatePagePlugin;
import org.exoplatform.wiki.utils.Utils;

public class JCRDataStorage implements DataStorage{
  private static final Log log = ExoLogger.getLogger(JCRDataStorage.class);
  private static final int MAX_EXCERPT_LENGTH = 430;
  
  private WikiTemplatePagePlugin templatePlugin; 
  
  public void setTemplatePagePlugin(WikiTemplatePagePlugin plugin) {
    this.templatePlugin = plugin;
  }
  
  public PageList<SearchResult> search(ChromatticSession session, WikiSearchData data) throws Exception {
    List<SearchResult> resultList = new ArrayList<SearchResult>();
    
    long numberOfSearchForTitleResult = 0;
    if (!StringUtils.isEmpty(data.getTitle())) {
      // Search for title
      String statement = data.getStatementForSearchingTitle();
      QueryImpl q = (QueryImpl) ((ChromatticSessionImpl) session).getDomainSession().getSessionWrapper().createQuery(statement);
      QueryResult result = q.execute();
      RowIterator iter = result.getRows();
      numberOfSearchForTitleResult = iter.getSize();
      if (numberOfSearchForTitleResult > data.getOffset()) {
        if (data.getOffset() > 0) {
          iter.skip(data.getOffset());
        }
        long position = data.getOffset();
        while (iter.hasNext()) {
          if ((data.getLimit() == Integer.MAX_VALUE) || (position < data.getOffset() + data.getLimit())) {
            SearchResult tempResult = getResult(iter.nextRow(), data);
            // If contains, merges with the exist
            if (tempResult != null && !isContains(resultList, tempResult)) {
              resultList.add(tempResult);
            }
          } else {
            iter.nextRow();
          }
          position++;
        }
      }
    }
    
    // if we have enough result then return
    if ((resultList.size() >= data.getLimit()) || StringUtils.isEmpty(data.getContent())) {
      return new ObjectPageList<SearchResult>(resultList, resultList.size());
    }
    
    // Search for wiki content
    long searchForContentOffset = 0;
    long searchForContentLimit = 0;
    if (numberOfSearchForTitleResult >= data.getOffset()) {
      searchForContentOffset = 0;
      searchForContentLimit = data.getLimit() - (numberOfSearchForTitleResult - data.getOffset());
    } else {
      searchForContentOffset = data.getOffset() - numberOfSearchForTitleResult;
      if (data.getLimit() == Integer.MAX_VALUE) {
        searchForContentLimit = Integer.MAX_VALUE;
      } else {
        searchForContentLimit = searchForContentOffset + data.getLimit();
      }
    }
    
    if (searchForContentOffset >= 0 && searchForContentLimit > 0) {
      String statement = data.getStatementForSearchingContent();
      QueryImpl q = (QueryImpl) ((ChromatticSessionImpl) session).getDomainSession().getSessionWrapper().createQuery(statement);
      q.setOffset(searchForContentOffset);
      q.setLimit(searchForContentLimit);
      QueryResult result = q.execute();
      RowIterator iter = result.getRows();
      while (iter.hasNext()) {
        SearchResult tempResult = getResult(iter.nextRow(), data);
        // If contains, merges with the exist
        if (tempResult != null && !isContains(resultList, tempResult) && !isDuplicateTitle(resultList, tempResult)) {
          resultList.add(tempResult);
        }
      }
    }
    
    // Return all the result
    return new ObjectPageList<SearchResult>(resultList, resultList.size());
  }
  
  public boolean isDuplicateTitle(List<SearchResult> list, SearchResult result) {
    for (SearchResult searchResult : list) {
      if (result.getTitle().equals(searchResult.getTitle())) {
        return true;
      }
    } 
    return false;
  }
  
  public Page getWikiPageByUUID(ChromatticSession session, String uuid) throws Exception {
    StringBuilder statement = new StringBuilder();
    statement.append("SELECT path ");
    statement.append("FROM ").append(WikiNodeType.WIKI_PAGE).append(" ");
    statement.append("WHERE jcr:uuid = '").append(uuid).append("'");
    
    Query q = ((ChromatticSessionImpl) session).getDomainSession().getSessionWrapper().createQuery(statement.toString());
    QueryResult result = q.execute();
    RowIterator iter = result.getRows();
    if (iter.hasNext()) {
      Row row = iter.nextRow();
      String path = row.getValue(WikiNodeType.Definition.PATH).getString();
      Page page = (Page) Utils.getObject(path, WikiNodeType.WIKI_PAGE);
      return page;
    }
    return null;
  }
  
  public void initDefaultTemplatePage(ChromatticSession crmSession, ConfigurationManager configurationManager, String path) {
    if (templatePlugin != null) {
      try {
        Iterator<String> iterator = templatePlugin.getSourcePaths().iterator();
        Session session = crmSession.getJCRSession();
        InputStream is = null;
        while (iterator.hasNext()) {
          try {
            String sourcePath = iterator.next();
            is = configurationManager.getInputStream(sourcePath);
            int type = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
            if(((Node)session.getItem(path)).hasNode(WikiNodeType.WIKI_TEMPLATE_CONTAINER)) {
              type = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
            }
            session.importXML(path, is, type);
            session.save();
          } finally {
            if (is != null) {
              is.close();
            }
          }
        }
      } catch (Exception e) {
        log.info("Failed to init default template page because: " + e.getCause());
      }
    }
  }
  
  private SearchResult getResult(Row row, WikiSearchData data) throws Exception {
    String type = row.getValue(WikiNodeType.Definition.PRIMARY_TYPE).getString();
    String path = row.getValue(WikiNodeType.Definition.PATH).getString();
    
    String title = StringUtils.EMPTY;
    String excerpt = StringUtils.EMPTY;
    long jcrScore = row.getValue("jcr:score").getLong();
    Calendar updateDate = GregorianCalendar.getInstance();
    Calendar createdDate = GregorianCalendar.getInstance();
    PageImpl page = null;
    if (WikiNodeType.WIKI_ATTACHMENT_CONTENT.equals(type)) {
      // Transform to Attachment result
      type = WikiNodeType.WIKI_ATTACHMENT.toString();
      path = path.substring(0, path.lastIndexOf("/"));
      if(!path.endsWith(WikiNodeType.Definition.CONTENT)){
        AttachmentImpl searchAtt = (AttachmentImpl) Utils.getObject(path, WikiNodeType.WIKI_ATTACHMENT);
        updateDate = searchAtt.getUpdatedDate();
        page = searchAtt.getParentPage();
        createdDate.setTime(page.getCreatedDate());
        title = page.getTitle();
      } else {
        String pagePath = path.substring(0, path.lastIndexOf("/" + WikiNodeType.Definition.CONTENT));
        type = WikiNodeType.WIKI_PAGE_CONTENT.toString();
        page = (PageImpl) Utils.getObject(pagePath, WikiNodeType.WIKI_PAGE);
        title = page.getTitle();
        updateDate.setTime(page.getUpdatedDate());
        createdDate.setTime(page.getCreatedDate());
      }
    } else if (WikiNodeType.WIKI_ATTACHMENT.equals(type)) {
      AttachmentImpl searchAtt = (AttachmentImpl) Utils.getObject(path, WikiNodeType.WIKI_ATTACHMENT);
      updateDate = searchAtt.getUpdatedDate();
      page = searchAtt.getParentPage();
      createdDate.setTime(page.getCreatedDate());
      if ("nt:base".equals(data.getNodeType())) {
        title = searchAtt.getFullTitle();
      } else {
        title = page.getTitle();
      }
    } else if (WikiNodeType.WIKI_PAGE.equals(type)) {
      page = (PageImpl) Utils.getObject(path, type);
      updateDate.setTime(page.getUpdatedDate());
      createdDate.setTime(page.getCreatedDate());
      title = page.getTitle();
    } else {
      return null;
    }
    
    //get the excerpt from row result
    excerpt = getExcerpt(row, type);

    if (page == null || !page.hasPermission(PermissionType.VIEWPAGE) || page.getName().equals(WikiNodeType.Definition.WIKI_HOME_NAME)) {
      return null;
    }
    
    SearchResult result = new SearchResult(excerpt, title, path, type, updateDate, createdDate);
    result.setUrl(page.getURL());
    result.setJcrScore(jcrScore);
    return result;
  }

  /**
   * gets except of row result based on specific properties, but all to get nice excerpt
   * @param row the result row
   * @param type the result type
   * @return the excerpt
   * @throws ItemNotFoundException
   * @throws RepositoryException
   * @throws ClassNotFoundException 
   * @throws NoSuchMethodException 
   * @throws InvocationTargetException 
   * @throws IllegalAccessException 
   * @throws SecurityException 
   * @throws IllegalArgumentException 
   */
  private String getExcerpt(Row row, String type) throws ItemNotFoundException, RepositoryException, ClassNotFoundException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    StringBuilder ret = new StringBuilder();
    String[] properties = (WikiNodeType.WIKI_PAGE_CONTENT.equals(type)) ? 
                          new String[]{"."} :
                          new String[]{"title", "url"};
    Class sanitizeUtils = Class.forName("org.exoplatform.wcm.webui.Utils");
    Object sanitize = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(sanitizeUtils);    
    for (String prop : properties) {
      Value excerptValue = row.getValue("rep:excerpt(" + prop + ")");
      if (excerptValue != null) {
        String excerptStr = excerptValue.getString();
        excerptStr = (String) sanitizeUtils.getDeclaredMethod("sanitize", String.class).invoke(sanitize, excerptStr);
        ret.append(excerptStr).append("...");
      }
    }
    if (ret.length() > MAX_EXCERPT_LENGTH) {
      return ret.substring(0, MAX_EXCERPT_LENGTH) + "...";
    }
    return ret.toString();
  }
  
  public List<SearchResult> searchRenamedPage(ChromatticSession session, WikiSearchData data) throws Exception {
    List<SearchResult> resultList = new ArrayList<SearchResult>() ;
    String statement = data.getStatementForRenamedPage() ;
    Query q = ((ChromatticSessionImpl)session).getDomainSession().getSessionWrapper().createQuery(statement);
    QueryResult result = q.execute();
    NodeIterator iter = result.getNodes() ;
    while(iter.hasNext()) {      
      try {
        resultList.add(getResult(iter.nextNode()));
      } catch (Exception e) {
        log.debug("Failed to add item search result", e);
      }
    }
    return resultList ;
  }
  
  private SearchResult getResult(Node node)throws Exception {
    SearchResult result = new SearchResult() ;
    result.setPageName(node.getName()) ;
    String title = node.getProperty(WikiNodeType.Definition.TITLE).getString();
    InputStream data = node.getNode(WikiNodeType.Definition.CONTENT).getNode(WikiNodeType.Definition.ATTACHMENT_CONTENT).getProperty(WikiNodeType.Definition.DATA).getStream();
    byte[] bytes = IO.getBytes(data);
    String content = new String(bytes, "UTF-8");
    if(content.length() > 100) content = content.substring(0, 100) + "...";
    result.setExcerpt(content) ;
    result.setTitle(title) ;
    return result ;
  }
  
  public InputStream getAttachmentAsStream(String path, ChromatticSession session) throws Exception {
    Node attContent = (Node)session.getJCRSession().getItem(path) ;
    return attContent.getProperty(WikiNodeType.Definition.DATA).getStream() ;    
  }
  
  private boolean isContains(List<SearchResult> list, SearchResult result) throws Exception {
    AttachmentImpl att = null;
    PageImpl page = null;
    if (WikiNodeType.WIKI_ATTACHMENT.equals(result.getType())) {
      att = (AttachmentImpl) Utils.getObject(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
    } else if (WikiNodeType.WIKI_ATTACHMENT_CONTENT.equals(result.getType())) {
      String attPath = result.getPath().substring(0, result.getPath().lastIndexOf("/"));
      att = (AttachmentImpl) Utils.getObject(attPath, WikiNodeType.WIKI_ATTACHMENT);
    } else if(WikiNodeType.WIKI_PAGE.equals(result.getType()) || WikiNodeType.WIKI_HOME.equals(result.getType())){
      page = (PageImpl) Utils.getObject(result.getPath(), WikiNodeType.WIKI_PAGE);
    } else if (WikiNodeType.WIKI_PAGE_CONTENT.equals(result.getType())) {
      att = (AttachmentImpl) Utils.getObject(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
      page = att.getParentPage();
    }
    if (att != null || page != null) {
      Iterator<SearchResult> iter = list.iterator();
      while (iter.hasNext()) {
        SearchResult child = iter.next();
        if (WikiNodeType.WIKI_ATTACHMENT.equals(child.getType()) || WikiNodeType.WIKI_PAGE_CONTENT.equals(child.getType())) {
          AttachmentImpl tempAtt = (AttachmentImpl) Utils.getObject(child.getPath(), WikiNodeType.WIKI_ATTACHMENT);
          if (att != null && att.equals(tempAtt)) {
            // Merge data
            if (child.getExcerpt()==null && result.getExcerpt()!=null ){
              child.setExcerpt(result.getExcerpt());
            }
            return true;
          }               
          if (page != null && page.getName().equals(tempAtt.getParentPage())) {
            return true;
          }     
        } else if (WikiNodeType.WIKI_PAGE.equals(child.getType())) {
          if (page != null && page.getPath().equals(child.getPath())) {
            iter.remove();
            return false;
          }
        }
      }
    }
    return false;
  }

  @Override
  public List<TemplateSearchResult> searchTemplate(ChromatticSession session,
                                                       TemplateSearchData data) throws Exception {

    List<TemplateSearchResult> resultList = new ArrayList<TemplateSearchResult>();
    String statement = data.getStatementForSearchingTitle();
    Query q = ((ChromatticSessionImpl)session).getDomainSession().getSessionWrapper().createQuery(statement);
    QueryResult result = q.execute();
    RowIterator iter = result.getRows();
    while (iter.hasNext()) {
      TemplateSearchResult tempResult = getTemplateResult(iter.nextRow());
      resultList.add(tempResult);
    }
   return resultList;
  }

  private TemplateSearchResult getTemplateResult(Row row) throws Exception {
    String type = row.getValue(WikiNodeType.Definition.PRIMARY_TYPE).getString();

    String path = row.getValue(WikiNodeType.Definition.PATH).getString();
    String title = (row.getValue(WikiNodeType.Definition.TITLE) == null ? null : row.getValue(WikiNodeType.Definition.TITLE).getString());
    
    Template template = (Template) Utils.getObject(path, WikiNodeType.WIKI_PAGE);
    String description = template.getDescription();
    TemplateSearchResult result = new TemplateSearchResult(template.getName(),
                                                           title,
                                                           path,
                                                           type,
                                                           null,
                                                           null,
                                                           description);
    return result;
  }
}
