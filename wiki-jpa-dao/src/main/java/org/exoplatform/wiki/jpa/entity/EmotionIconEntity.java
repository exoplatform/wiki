package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

@Entity(name = "WikiEmotionIconEntity")
@ExoEntity
@Table(name = "WIKI_EMOTION_ICONS")
@NamedQueries({
        @NamedQuery(name = "emotionIcon.getEmotionIconByName", query = "SELECT e FROM WikiEmotionIconEntity e WHERE e.name = :name")
})
public class EmotionIconEntity {
  @Id
  @SequenceGenerator(name="SEQ_WIKI_EMOTION_ICONS_ICON_ID", sequenceName="SEQ_WIKI_EMOTION_ICONS_ICON_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_EMOTION_ICONS_ICON_ID")
  @Column(name = "EMOTION_ICON_ID")
  private long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "IMAGE", length = 20971520)
  private byte[] image;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}
