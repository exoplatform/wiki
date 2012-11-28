package org.exoplatform.wiki.webui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.bean.DraftBean;
import org.exoplatform.wiki.webui.bean.WikiDraftListAccess;
import org.exoplatform.wiki.webui.commons.UIWikiDraftGrid;
import org.exoplatform.wiki.webui.popup.UIWikiPagePreview;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/wiki/webui/UIWikiMyDraftsForm.gtmpl",
    events = {
        @EventConfig(listeners = UIWikiMyDraftsForm.DeleteDraftActionListener.class, confirm = "UIWikiMyDraftsForm.msg.delete-confirm"),
        @EventConfig(listeners = UIWikiMyDraftsForm.ResumeDraftActionListener.class),
        @EventConfig(listeners = UIWikiMyDraftsForm.ViewDraftChangeActionListener.class),
        @EventConfig(listeners = UIWikiMyDraftsForm.SortDraftActionListener.class)
    }  
  )
public class UIWikiMyDraftsForm extends UIForm {
  
  public static final int       ITEMS_PER_PAGE     = 20;
  
  public static final String    DRAFT_GRID      = "UIWikiDraftGrid";
  
  public static final String    DRAFT_ITER      = "DraftIter";
  
  public static final String    ACTION_DELETE   = "DeleteDraft";
  
  public static final String    ACTION_RESUME   = "ResumeDraft";
  
  public static final String    ACTION_VIEW     = "ViewDraftChange";
  
  public static final String    ACTION_SORT     = "SortDraft";
  
  public static final String[]  DRAFT_FIELD     = {DraftBean.PAGE_TITLE, DraftBean.PLACE, DraftBean.LAST_EDITION};
  
  public static final String[]  USER_ACTIONS    = {ACTION_VIEW, ACTION_DELETE};
  
  public UIWikiMyDraftsForm() throws Exception {
    UIWikiDraftGrid grid = addChild(UIWikiDraftGrid.class, null, DRAFT_GRID);
    grid.getUIPageIterator().setId(DRAFT_ITER);
    grid.getUIPageIterator().setParent(this);
    grid.configure(DraftBean.ID, DRAFT_FIELD, USER_ACTIONS);
    grid.setActionForField(DraftBean.PAGE_TITLE, ACTION_RESUME);
    grid.setFieldToDisplayBreadCrumb(DraftBean.PLACE);
    initGrid();
  }
  
  public void initGrid() throws Exception {
    if ("__anonim".equals(org.exoplatform.wiki.utils.Utils.getCurrentUser())) {
      return;
    }
    
    WikiService wService = (WikiService) PortalContainer.getComponent(WikiService.class);
    List<DraftPage> drafts = wService.getDrafts(org.exoplatform.wiki.utils.Utils.getCurrentUser());
    List<DraftBean> draftBeans = new ArrayList<DraftBean>();
    UIWikiDraftGrid grid = getChild(UIWikiDraftGrid.class);
    grid.clearBreadcrum();
    
    ResourceBundle bundle = RequestContext.getCurrentInstance().getApplicationResourceBundle();
    String newPageHint = bundle.getString("UIWikiMyDraftsForm.title.new-page");
    
    // Get draft data
    for (DraftPage draftPage : drafts) {
      if (draftPage.getTargetPage() != null) {
        // Create breadcrumb
        Page pageImpl = wService.getPageByUUID(draftPage.getTargetPage());
        if (pageImpl == null) {
          continue;
        }
        List<BreadcrumbData> breadcrumbDatas = wService.getBreadcumb(pageImpl.getWiki().getType(), pageImpl.getWiki().getOwner(), pageImpl.getName());
        grid.putBreadCrumbDatas(draftPage.getName(), breadcrumbDatas);
        String draftTitle = draftPage.getTitle();
        if (draftPage.isNewPage()) {
          draftTitle += newPageHint;
        }
        
        // Add draft page to display
        draftBeans.add(new DraftBean(draftPage.getName(), draftTitle, grid.getWikiName(draftPage.getName()), draftPage.getUpdatedDate()));
      }
    }
    
    // Sort the draft list
    if (grid.getSortField() == null) {
      grid.setSortField(DraftBean.LAST_EDITION);
      grid.setASC(false);
    }
    sortDraft(draftBeans, grid.getSortField(), grid.isASC());
    
    // Create lazy list
    LazyPageList<DraftBean> lazylist = new LazyPageList<DraftBean>(new WikiDraftListAccess(draftBeans), ITEMS_PER_PAGE);
    grid.getUIPageIterator().setPageList(lazylist);
  }
  
  private void sortDraft(List<DraftBean> drafts, String field, boolean isASC) {
    if (field == null) {
      return;
    }
    
    if (DraftBean.PAGE_TITLE.equals(field)) {
      Collections.sort(drafts, new Comparator<DraftBean>() {
        @Override
       public int compare(DraftBean o1, DraftBean o2) {
          return o1.getPageTitle().compareTo(o2.getPageTitle());
        }
      });
    } else if (DraftBean.PLACE.equals(field)) {
      Collections.sort(drafts, new Comparator<DraftBean>() {
        @Override
        public int compare(DraftBean o1, DraftBean o2) {
          return o1.getPlace().compareTo(o2.getPlace());
        }
      });
    } else if (DraftBean.LAST_EDITION.equals(field)) {
      Collections.sort(drafts, new Comparator<DraftBean>() {
        @Override
        public int compare(DraftBean o1, DraftBean o2) {
          return (int) (o1.getLastEditionInDate().getTime() - o2.getLastEditionInDate().getTime());
        }
      });
    }
    
    if (!isASC) {
      Collections.reverse(drafts);
    }
  }
   
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
  }
  
  protected String getActionLink(String action, String beanId) throws Exception {
    UIWikiDraftGrid grid = getChild(UIWikiDraftGrid.class);
    return org.exoplatform.wiki.commons.Utils.createFormActionLink(grid, action, beanId);
  }  
  
  public static class ResumeDraftActionListener extends EventListener<UIWikiMyDraftsForm> {
    @Override
    public void execute(Event<UIWikiMyDraftsForm> event) throws Exception {
      String draftId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIWikiMyDraftsForm myDraftForm = event.getSource();
      UIWikiPortlet wikiPortlet = myDraftForm.getAncestorOfType(UIWikiPortlet.class);
      
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      DraftPage draftPage = wikiService.getDraft(draftId);
      if (draftPage != null) {
        if (draftPage.getTargetPage() != null) {
          Page targetPage = wikiService.getPageByUUID(draftPage.getTargetPage());
          if (targetPage != null) {
            WikiPageParams targetParam = new WikiPageParams(targetPage.getWiki().getType(), targetPage.getWiki().getOwner(), targetPage.getName());
            WikiMode mode = WikiMode.ADDPAGE;
            if (!draftPage.isNewPage()) {
              mode = WikiMode.EDITPAGE;
            }
            
            UIWikiPageEditForm pageEditForm = wikiPortlet.findFirstComponentOfType(UIWikiPageEditForm.class);
            UIFormStringInput titleInput = pageEditForm.getChild(UIWikiPageTitleControlArea.class).getUIStringInput();
            UIFormTextAreaInput markupInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_CONTENT);
            String title = draftPage.getTitle();
            String content = draftPage.getContent().getText();
            titleInput.setEditable(true);
            titleInput.setValue(title);
            pageEditForm.setTitle(title);
            markupInput.setValue(content);
            UIWikiRichTextArea wikiRichTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
            if (wikiRichTextArea.isRendered()) {
              Utils.feedDataForWYSIWYGEditor(pageEditForm, null);
            }
            pageEditForm.setInitDraftName(draftPage.getName());
            wikiPortlet.changeMode(mode);            
            Utils.redirect(targetParam, mode);
          }
        }
      }
    }
  }

  public static class ViewDraftChangeActionListener extends EventListener<UIWikiMyDraftsForm> {
    @Override
    public void execute(Event<UIWikiMyDraftsForm> event) throws Exception {
      String draftId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIWikiMyDraftsForm pageEditForm = event.getSource();
      UIWikiPortlet wikiPortlet = pageEditForm.getAncestorOfType(UIWikiPortlet.class);
      
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      DraftPage draftPage = wikiService.getDraft(draftId);
      if (draftPage != null) {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        ResourceBundle res = context.getApplicationResourceBundle();
        UIWikiMaskWorkspace uiMaskWS = wikiPortlet.getChild(UIWikiMaskWorkspace.class);
        UIWikiPagePreview wikiPagePreview = uiMaskWS.createUIComponent(UIWikiPagePreview.class, null, null);
        wikiPagePreview.setPageTitle(draftPage.getTitle());
        wikiPagePreview.setContent(draftPage.getChanges().getDiffHTML());
        uiMaskWS.setUIComponent(wikiPagePreview);
        uiMaskWS.setShow(true);
        uiMaskWS.setPopupTitle(res.getString("DraftPage.title.draft-changes"));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiMaskWS);
      }
    }
  }

  public static class DeleteDraftActionListener extends EventListener<UIWikiMyDraftsForm> {
    @Override
    public void execute(Event<UIWikiMyDraftsForm> event) throws Exception {
      String draftId = event.getRequestContext().getRequestParameter(OBJECTID);
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      wikiService.removeDraft(draftId);
      event.getSource().initGrid();
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
    }
  }
  
  public static class SortDraftActionListener extends EventListener<UIWikiMyDraftsForm> {
    @Override
    public void execute(Event<UIWikiMyDraftsForm> event) throws Exception {
      String sortId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIWikiMyDraftsForm uiWikiMyDraftsForm = event.getSource();
      UIWikiDraftGrid grid = uiWikiMyDraftsForm.getChildById(DRAFT_GRID);
      int underscoreIndex = sortId.indexOf('_');
      if (underscoreIndex > -1) {
        grid.setSortField(sortId.substring(0, underscoreIndex));
        grid.setASC(UIWikiDraftGrid.SORT_ASC.equals(sortId.substring(underscoreIndex + 1)));
        event.getSource().initGrid();
        event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
      }
    }
  }
}
