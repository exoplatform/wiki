package org.exoplatform.wiki.webui.control.action;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import org.apache.commons.codec.binary.Base64;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

import java.io.IOException;

public class CustomImageReplacedElementFactory implements ReplacedElementFactory {
    private static final Log LOG = ExoLogger.getLogger(CustomImageReplacedElementFactory.class);
    private final ReplacedElementFactory superFactory;

    public CustomImageReplacedElementFactory(ReplacedElementFactory superFactory) {
        this.superFactory = superFactory;
    }

    public ReplacedElement createReplacedElement(LayoutContext layoutContext, BlockBox blockBox, UserAgentCallback userAgentCallback, int cssWidth, int cssHeight) {
        Element element = blockBox.getElement();
        if (element == null) {
            return null;
        }
        String nodeName = element.getNodeName();
        if (nodeName.equals("img")) {
            String attribute = element.getAttribute("src");
            FSImage fsImage;
            try {
                fsImage = buildImage(attribute, userAgentCallback);
            } catch (IOException | BadElementException e) {
                LOG.debug("Error while decoding base64 wiki image", e);
                fsImage = null;
            }
            if (fsImage != null) {
                if (cssWidth != -1 || cssHeight != -1) {
                    fsImage.scale(cssWidth, cssHeight);
                }
                return new ITextImageElement(fsImage);
            }
        }
        return null;
    }

    protected FSImage buildImage(String srcAttr, UserAgentCallback userAgentCallback) throws IOException, BadElementException {
        FSImage fsImage = null;
        if(srcAttr.startsWith("base64,")) {
            String base64 = srcAttr.substring(srcAttr.indexOf("base64,") + "base64,".length());
            byte[] decodedBytes = Base64.decodeBase64(base64);
            fsImage = new ITextFSImage(Image.getInstance(decodedBytes));
        } else {
            fsImage = userAgentCallback.getImageResource(srcAttr).getImage();
        }
        return fsImage;
    }

    @Override
    public void remove(Element e) {
        this.superFactory.remove(e);
    }

    @Override
    public void reset() {
        this.superFactory.reset();
    }

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {
        this.superFactory.setFormSubmissionListener(listener);
    }
}