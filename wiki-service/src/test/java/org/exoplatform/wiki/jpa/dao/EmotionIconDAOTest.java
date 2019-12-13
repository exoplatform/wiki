/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa.dao;


import java.util.Arrays;

import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.EmotionIconEntity;

public class EmotionIconDAOTest extends BaseWikiJPAIntegrationTest {

  @Test
  public void testEmotionIconByName() {
    //Given
    EmotionIconEntity emotionIcon1 = new EmotionIconEntity();
    emotionIcon1.setName("emotionIcon1");
    emotionIcon1.setImage("image1".getBytes());
    emotionIconDAO.create(emotionIcon1);

    EmotionIconEntity emotionIcon2 = new EmotionIconEntity();
    emotionIcon2.setName("emotionIcon2");
    emotionIcon2.setImage("image2".getBytes());
    emotionIconDAO.create(emotionIcon2);

    //When
    EmotionIconEntity fetchedEmotionIcon1 = emotionIconDAO.getEmotionIconByName("emotionIcon1");
    EmotionIconEntity fetchedEmotionIcon2 = emotionIconDAO.getEmotionIconByName("emotionIcon2");
    EmotionIconEntity fetchedEmotionIcon3 = emotionIconDAO.getEmotionIconByName("emotionIcon3");

    //Then
    assertEquals(2, emotionIconDAO.findAll().size());
    assertNotNull(fetchedEmotionIcon1);
    assertEquals("emotionIcon1", fetchedEmotionIcon1.getName());
    assertTrue(Arrays.equals("image1".getBytes(), fetchedEmotionIcon1.getImage()));
    assertNotNull(fetchedEmotionIcon2);
    assertEquals("emotionIcon2", fetchedEmotionIcon2.getName());
    assertTrue(Arrays.equals("image2".getBytes(), fetchedEmotionIcon2.getImage()));
    assertNull(fetchedEmotionIcon3);
  }

}
