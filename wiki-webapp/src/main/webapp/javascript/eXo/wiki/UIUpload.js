/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */(function(base, uiForm, webuiExt, $) {


if(!eXo.wiki) eXo.wiki = {}; 

function UIUpload() {
	this.listUpload = [];
	this.refreshTime = 1000;
	this.delayTime = 1000;  
};

  /**
   * Initialize upload and create a upload request to server
   * 
   * @param {String}
   *          uploadId identifier upload
   */
UIUpload.prototype.initUploadEntry = function(uploadId, isDynamicMode, uploadText) {
    if (isDynamicMode && uploadId.length > 1) {
      isDynamicMode = true;
    } else {
      isDynamicMode = false;
    }
    
    eXo.wiki.UIUpload.uploadText = uploadText;
    if (!eXo.wiki.UIUpload.progressURL) {
    	var context = eXo.env.server.context;
        eXo.wiki.UIUpload.progressURL = context + "/upload?action=progress&uploadId=";
        eXo.wiki.UIUpload.uploadURL = context + "/upload?action=upload&uploadId=";
        eXo.wiki.UIUpload.abortURL = context + "/upload?action=abort&uploadId=";
        eXo.wiki.UIUpload.deleteURL = context + "/upload?action=delete&uploadId=";
    }
    
    for ( var i = 0; i < uploadId.length; i++) {
      var url = eXo.wiki.UIUpload.progressURL + uploadId[i];
      var responseText = ajaxAsyncGetRequest(url, false);
      try {        
    	  eval("var response = " + responseText);
      } catch (err) {
        return;
      }
      
      var uploadCont = $("#UploadInputContainer" + uploadId[i]);
      uploadCont.on("click", ".deleteFileLable, .Abort, .removeFile", (function(id) {
    		  return function() {
    			  if ($(this).hasClass("removeFile")) {
    				  eXo.wiki.UIUpload.deleteUpload(id, isDynamicMode && uploadId.length > 1);    			      			      		 
    			  } else {
    				  eXo.wiki.UIUpload.abortUpload(id, isDynamicMode);
    			  }
    		  };
      })(uploadId[i]));
      
      uploadCont.on("change", ".file", (function(id) {
    	  return function() {
    	      $(".saveWikiPage", document.body).each(function(index, elem) {
    	      	$(elem).attr("disabled", "");
  	          });
    		  eXo.wiki.UIUpload.upload(id);    		  
    	  };
      })(uploadId[i]));
      
      if (response.upload[uploadId[i]] == undefined
          || response.upload[uploadId[i]].percent == undefined) {
        eXo.wiki.UIUpload.createEntryUpload(uploadId[i], isDynamicMode);
      } else if (response.upload[uploadId[i]].percent == 100) {
        eXo.wiki.UIUpload.showUploaded(uploadId[i], response.upload[uploadId[i]].fileName);
      }
    }
  }; 

  UIUpload.prototype.createEntryUpload = function(id, isDynamicMode) {
    var div = document.getElementById('UploadInput' + id);
    var url = document.getElementById('RemoveInputUrl' + id).value;
    var label = document.getElementById('RemoveInputLabel').value;
    var inputHTML = "<input id='file" + id + "' class='file fileHidden' style='width:130px' name='file' type='file' onkeypress='return false;'/>";
    inputHTML += "<button type='button'" + id + "' class='btn btn-primary'>" + eXo.wiki.UIUpload.uploadText + "</button>";
    if (isDynamicMode) {
      inputHTML += "<a class='actionLabel' href='javascript:void(0)' onclick=\""+ url + "\">" + label + "</a>";
    }
    div.style.display = 'block';
    div.innerHTML = inputHTML;
  };

  UIUpload.prototype.displayUploadButton = function(id) {
    var flag = true;
    if (id instanceof Array) {
      var img = document.getElementById('IconUpload' + id[0]);
      for ( var i = 0; i < id.length; i++) {
        var input = document.getElementById('file' + id[i]);
        if (input == null)
          flag = true;
        else if (input.value == null || input.value == '')
          flag = false;
      }
      if (flag)
        img.style.display = 'none';
    } else
      return;
  };

  UIUpload.prototype.showUploaded = function(id, fileName) {
    eXo.wiki.UIUpload.remove(id);
    var container = parent.document.getElementById('UploadInputContainer' + id);
    var element = document.getElementById('ProgressIframe' + id);
    element.innerHTML = "<span></span>";

    jCont = $(container);
    var UploadInput = jCont.find('#UploadInput' + id);
    UploadInput.hide();

    var progressIframe = jCont.find('#ProgressIframe' + id);
    progressIframe.hide();

    var selectFileFrame = jCont.find(".selectFileFrame").first();
    selectFileFrame.show();

    var fileNameLabel = selectFileFrame.find(".fileNameLabel").first();
    if (fileName.length)
      fileNameLabel.html(decodeURIComponent(fileName));

    var progressBarFrame = jCont.find(".progressBarFrame").first();
    progressBarFrame.hide();
  };

  UIUpload.prototype.refreshProgress = function(uploadId) {
    var list = eXo.wiki.UIUpload.listUpload;
    if (list.length < 1)
      return;
    var url = eXo.wiki.UIUpload.progressURL;

    for ( var i = 0; i < list.length; i++) {
      url = url + "&uploadId=" + list[i];
    }
    var responseText = ajaxAsyncGetRequest(url, false);
    if (eXo.wiki.UIUpload.listUpload.length > 0) {
      setTimeout(
          function() {eXo.wiki.UIUpload.refreshProgress(uploadId);},
          eXo.wiki.UIUpload.refreshTime);
    }

    try {
    	eval("var response = " + responseText);
    } catch (err) {
      return;
    }
    for (id in response.upload) {
      var container = parent.document.getElementById('UploadInputContainer'
          + id);
      var jCont = $(container);
      
      if (response.upload[id].status == "failed") {
        eXo.wiki.UIUpload.abortUpload(id);
        var message = jCont.children(".limitMessage").first().html();
        message = message.replace("{0}", response.upload[id].size);
        message = message.replace("{1}", response.upload[id].unit);
        alert(message);
        continue;
      }
      var element = document.getElementById('ProgressIframe' + id);
      var percent = response.upload[id].percent;
      var bar = jCont.find(".bar").first();
      bar.css("width", percent + "%");
      var label = jCont.find(".percent").first();
      label.html(percent + "%");

      if(percent == 100) {
          var postUploadActionNode = $(container).find('div.postUploadAction')[0];
          if(postUploadActionNode) {
            eXo.wiki.UIUpload.listUpload.remove(id);
            postUploadActionNode.onclick();
          } else {
            this.showUploaded(id, "");
          }
        }
    }

    if (eXo.wiki.UIUpload.listUpload.length < 1) {
      $(".saveWikiPage", document.body).each(function(index, elem) {
    	$(elem).removeAttr("disabled");
      });
      return;
    }

    if (element) {
      element.innerHTML = "Uploaded " + percent + "% "
          + "<span class='Abort'>Abort</span>";
    }
  };

  UIUpload.prototype.deleteUpload = function(id, isDynamicMode) {
    var url = eXo.wiki.UIUpload.deleteURL + id;
    ajaxRequest('GET', url, false);
    
    var container = parent.document.getElementById('UploadInputContainer' + id);
    var selectFileFrame = $(container).find(".selectFileFrame").first();
    selectFileFrame.hide();

    eXo.wiki.UIUpload.createEntryUpload(id, isDynamicMode);
  };

  UIUpload.prototype.abortUpload = function(id, isDynamicMode) {
    eXo.wiki.UIUpload.remove(id);
    var url = eXo.wiki.UIUpload.abortURL + id;
    ajaxRequest('GET', url, false);
    var container = parent.document.getElementById('UploadInputContainer' + id);
    var jCont = $(container);
    var progressIframe = jCont.find('#ProgressIframe' + id);
    progressIframe.hide();

    var progressBarFrame = jCont.find(".progressBarFrame").first();
    progressBarFrame.hide();

    eXo.wiki.UIUpload.createEntryUpload(id, isDynamicMode);
    if (eXo.wiki.UIUpload.listUpload.length < 1) {
	    $(".saveWikiPage", document.body).each(function(index, elem) {
	  	$(elem).removeAttr("disabled");
    });
  }
  };

  /**
   * Start upload file
   * 
   * @param {Object}
   *          clickEle
   * @param {String}
   *          id
   */
  UIUpload.prototype.doUpload = function(id) {
    var container = parent.document.getElementById('UploadInputContainer' + id);
    var jCont = $(container);
    eXo.wiki.UIUpload.displayUploadButton(id);
    if (id instanceof Array) {
      for ( var i = 0; i < id.length; i++) {
        eXo.wiki.UIUpload.doUpload(id[i]);
      }
    } else {
      var file = document.getElementById('file' + id);
      if (file == null || file == undefined)
        return;
      if (file.value == null || file.value == '')
        return;
      var temp = file.value;

      var progressBarFrame = jCont.find(".progressBarFrame").first();
      progressBarFrame.show();

      var bar = jCont.find(".bar").first();
      bar.css("width", "0%");
      var label = bar.children(".label").first();
      label.html("0%");

      var uploadAction = eXo.wiki.UIUpload.uploadURL + id;
      var formHTML = "<form id='form" + id
          + "' class='UIUploadForm' style='margin: 0px; padding: 0px' action='"
          + uploadAction
          + "' enctype='multipart/form-data' target='UploadIFrame" + id
          + "' method='post'></form>";
      var div = document.createElement("div");
      div.innerHTML = formHTML;
      var form = div.firstChild;

      form.appendChild(file);
      document.body.appendChild(div);
      form.submit();
      document.body.removeChild(div);

      if (eXo.wiki.UIUpload.listUpload.length == 0) {
        eXo.wiki.UIUpload.listUpload.push(id);
        setTimeout(function() {eXo.wiki.UIUpload.refreshProgress(id);},
            eXo.wiki.UIUpload.refreshTime);
      } else {
        eXo.wiki.UIUpload.listUpload.push(id);
      }

      var UploadInput = jCont.find('#UploadInput' + id);
      UploadInput.hide();
    }
  };

  UIUpload.prototype.upload = function(id) {
    setTimeout(function() {eXo.wiki.UIUpload.doUpload(id)}, eXo.wiki.UIUpload.delayTime);
  };
  
  UIUpload.prototype.remove = function(id) {
  	var idx = $.inArray(id, eXo.wiki.UIUpload.listUpload);
  	if (idx !== -1) {
  		eXo.wiki.UIUpload.listUpload.splice(idx, 1);  		
  	}
  };


eXo.wiki.UIUpload = new UIUpload();
return eXo.wiki.UIUpload;

})(base, uiForm, webuiExt, $);

