/* 
* Copyright (C) 2003-2016 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.wiki.jpa.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

import java.io.Serializable;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 1/19/16
 */
public abstract class WikiBaseDAO<E, ID extends Serializable>  extends GenericDAOJPAImpl<E, ID> {

  public String validateGroupWikiOwner(String wikiOwner){
    if(wikiOwner == null || wikiOwner.length() == 0){
      return null;
    }
    if(!wikiOwner.startsWith("/")){
      wikiOwner = "/" + wikiOwner;
    }
    if(wikiOwner.endsWith("/")){
      wikiOwner = wikiOwner.substring(0,wikiOwner.length()-1);
    }
    return wikiOwner;
  }

}

