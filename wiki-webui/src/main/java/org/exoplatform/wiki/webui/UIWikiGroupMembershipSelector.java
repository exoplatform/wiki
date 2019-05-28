package org.exoplatform.wiki.webui;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ComponentConfigs({
        @ComponentConfig(template = "system:/groovy/organization/webui/component/UIGroupMembershipSelector.gtmpl", events = {
                @EventConfig(phase = Event.Phase.DECODE, listeners = UIGroupMembershipSelector.ChangeNodeActionListener.class),
                @EventConfig(phase = Event.Phase.DECODE, listeners = UIGroupMembershipSelector.SelectMembershipActionListener.class),
                @EventConfig(phase = Event.Phase.DECODE, listeners = UIGroupMembershipSelector.SelectPathActionListener.class) }),
        @ComponentConfig(type = UITree.class, id = "UITreeGroupSelector", template = "system:/groovy/webui/core/UITree.gtmpl", events = @EventConfig(phase = Event.Phase.DECODE, listeners = UITree.ChangeNodeActionListener.class)),
        @ComponentConfig(type = UIBreadcumbs.class, id = "BreadcumbGroupSelector", template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl", events = @EventConfig(phase = Event.Phase.DECODE, listeners = UIBreadcumbs.SelectPathActionListener.class)) })
@Serialized
public class UIWikiGroupMembershipSelector extends UIGroupMembershipSelector {

  public UIWikiGroupMembershipSelector() throws Exception {
  }

  @Override
  protected List<Group> getChildrenGroups(Group parentGroup) throws Exception {
    OrganizationService organizationService = getApplicationComponent(OrganizationService.class);
    UserACL userACL = getApplicationComponent(UserACL.class);

    ConversationState conversationState = ConversationState.getCurrent();
    if(conversationState != null && conversationState.getIdentity() != null) {
      return organizationService.getGroupHandler()
              .findGroups(parentGroup)
              .stream()
              .filter(group -> userACL.hasPermission(conversationState.getIdentity(), group, "wiki_permissions"))
              .collect(Collectors.toList());
    } else {
      return Collections.EMPTY_LIST;
    }
  }
}
