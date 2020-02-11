package org.exoplatform.wiki.utils;

import org.exoplatform.commons.utils.HTMLSanitizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wiki html sanitation : Prevent XSS/XEE attacks by encoding user HTML inputs.
 * This class will be used to encode data wiki input.
 */
public class WikiHTMLSanitizer extends HTMLSanitizer {
  public static String markupSanitize(String html) throws Exception {
    String htmlSanitized = "";
    if (html.contains("<!--")) {
      String[] splittedHTML = html.replaceAll("\n", "").split("(?=<!?--)");
      String[] commentSplitted = new String[splittedHTML.length];
      String[] contentSplitted = new String[splittedHTML.length];
      Pattern p = Pattern.compile("(<!--.*-->)(.*)");
      for (int i = 0; i < splittedHTML.length; i++) {
        if (splittedHTML[i].startsWith("<!--")) {
          Matcher matcher = p.matcher(splittedHTML[i]);
          while (matcher.find()) {
            commentSplitted[i] = matcher.group(1);
            contentSplitted[i] = HTMLSanitizer.sanitize(matcher.group(2));
          }
        } else {
          contentSplitted[i] = splittedHTML[i];
          commentSplitted[i] = "";
        }
        htmlSanitized = htmlSanitized.concat(commentSplitted[i]).concat(contentSplitted[i]);
      }
    } else {
      htmlSanitized = sanitize(html);
    }

    return htmlSanitized;
  }
}
