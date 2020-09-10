package org.exoplatform.wiki.builder;

import junit.framework.TestCase;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webui.utils.TimeConvertUtils;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.notification.builder.WikiTemplateBuilder;
import org.exoplatform.wiki.notification.provider.MailTemplateProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.Locale;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class WikiTemplateBuilderTest extends TestCase {

  @Mock
  private IdentityManager identityManager;

  @Mock
  private InitParams initParams;

  @PrepareForTest({ CommonsUtils.class, PluginKey.class, NotificationPluginUtils.class, TemplateContext.class,
      PropertyManager.class,
      HTMLEntityEncoder.class, NotificationMessageUtils.class, TimeConvertUtils.class,
      TemplateUtils.class, NotificationContextImpl.class })
  @Test
  public void shoudIntantiateTemplate() {
    // Given
    PowerMockito.mockStatic(CommonsUtils.class);
    when(CommonsUtils.getService(any())).thenReturn(null);
    PowerMockito.mockStatic(PluginKey.class);
    PluginKey plugin = mock(PluginKey.class);
    when(PluginKey.key(NotificationsUtils.EDIT_WIKI_NOTIFICATION_ID)).thenReturn(plugin);
    ValueParam channelParam = new ValueParam();
    channelParam.setName(TemplateProvider.CHANNEL_ID_KEY);
    channelParam.setValue("MAIL_CHANNEL");
    when(initParams.getValueParam(eq(TemplateProvider.CHANNEL_ID_KEY))).thenReturn(channelParam);
    MailTemplateProvider mailTemplate = new MailTemplateProvider(initParams);
    WikiTemplateBuilder wikiTemplateBuilder = (WikiTemplateBuilder) mailTemplate.getTemplateBuilder().get(plugin);
    PowerMockito.mockStatic(NotificationContextImpl.class);
    NotificationContext ctx = mock(NotificationContext.class);
    when(NotificationContextImpl.cloneInstance()).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.WIKI_URL, "/portal/spaceTest/WikiPage")).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.WIKI_PAGE_NAME, "Test Page")).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.CONTENT_CHANGE, "changes for test")).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.WIKI_EDITOR, "root")).thenReturn(ctx);
    NotificationInfo notification = mock(NotificationInfo.class);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    when(notification.getKey()).thenReturn(plugin);
    when(plugin.getId()).thenReturn("EditWikiNotificationPlugin");
    PowerMockito.mockStatic(NotificationPluginUtils.class);
    when(NotificationPluginUtils.getLanguage(anyString())).thenReturn("en");
    TemplateContext templateContext = mock(TemplateContext.class);
    ChannelKey key = mock(ChannelKey.class);
    MailTemplateProvider mailTemplateSpy = Mockito.spy(mailTemplate);
    when(mailTemplateSpy.getChannelKey()).thenReturn(key);
    PowerMockito.mockStatic(TemplateContext.class);
    when(TemplateContext.newChannelInstance(Matchers.any(),
                                            eq("EditWikiNotificationPlugin"),
                                            eq("en"))).thenReturn(templateContext);

    when(notification.getValueOwnerParameter("wiki_url")).thenReturn("/portal/spaceTest/WikiPage");
    when(notification.getValueOwnerParameter("wiki_page_name")).thenReturn("Test Page");
    when(notification.getValueOwnerParameter("content_change")).thenReturn("changes for test");
    when(notification.getValueOwnerParameter("wiki_editor")).thenReturn("root");

    HTMLEntityEncoder encoder = mock(HTMLEntityEncoder.class);
    PowerMockito.mockStatic(HTMLEntityEncoder.class);
    when(HTMLEntityEncoder.getInstance()).thenReturn(encoder);
    when(encoder.encode("title")).thenReturn("title");
    when(encoder.encode("jean")).thenReturn("jean");
    when(encoder.encode("root")).thenReturn("root");
    when(encoder.encode("/portal/spaceTest/WikiPage")).thenReturn("/portal/spaceTest/WikiPage");
    when(encoder.encode("changes for test")).thenReturn("changes for test");
    when(notification.getValueOwnerParameter("read")).thenReturn("true");
    when(notification.getId()).thenReturn("NotifId123");

    Date date = new Date();
    Long time = date.getTime();
    when(notification.getLastModifiedDate()).thenReturn(time);
    PowerMockito.mockStatic(TimeConvertUtils.class);
    when(TimeConvertUtils.convertXTimeAgoByTimeServer(date,
                                                      "EE, dd yyyy",
                                                      new Locale("en"),
                                                      TimeConvertUtils.YEAR)).thenReturn("9-09-2020");

    when(notification.getTo()).thenReturn("jean");
    Identity receiverIdentity = new Identity(OrganizationIdentityProvider.NAME, "jean");
    receiverIdentity.setRemoteId("jean");
    Profile profile = new Profile(receiverIdentity);
    receiverIdentity.setProfile(profile);
    profile.setProperty(Profile.FIRST_NAME, "jean");
    when(identityManager.getOrCreateIdentity(eq(OrganizationIdentityProvider.NAME),
                                             eq("jean"),
                                             anyBoolean())).thenReturn(receiverIdentity);
    when(encoder.encode("jean")).thenReturn("jean");

    PowerMockito.mockStatic(TemplateUtils.class);
    when(TemplateUtils.processSubject(templateContext)).thenReturn("root edited your wiki");
    when(TemplateUtils.processGroovy(templateContext)).thenReturn("root edit your wiki \"title\" ");
    when(templateContext.getException()).thenReturn(null);

    // When
    MessageInfo messageInfo = wikiTemplateBuilder.makeMessage(ctx);

    // Then
    assertNotNull(messageInfo);
    assertEquals("root edit your wiki \"title\" ", messageInfo.getBody());
    assertEquals("root edited your wiki", messageInfo.getSubject());
  }
}
