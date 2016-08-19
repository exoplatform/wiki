package org.exoplatform.wiki.utils;

import org.exoplatform.commons.utils.HTMLSanitizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wiki html sanitation : Prevent XSS/XEE attacks by encoding user HTML inputs.
 * This class will be used to encode data wiki input.
 */
public class WikiHTMLSanitizer extends HTMLSanitizer {
  private static final String  LINK_START_TAG   = "startwikilink:";

  private static final String  IMAGE_START_TAG  = "startimage:";

  private static final String  MACRO_START_TAG  = "startmacro:";

  private static final String  LINK_END_TAG     = "stopwikilink";

  private static final String  IMAGE_END_TAG    = "stopimage";

  private static final String  MACRO_END_TAG    = "stopmacro";

  private static final Pattern LINK_REGEX       =
                                          Pattern.compile("<!--" + LINK_START_TAG + "(.*)-->(.*)<!--" + LINK_END_TAG + "-->");

  private static final Pattern IMAGE_REGEX      =
                                           Pattern.compile("<!--" + IMAGE_START_TAG + "(.*)-->(.*)<!--" + IMAGE_END_TAG + "-->");

  private static final Pattern MACRO_REGEX      =
                                           Pattern.compile("<!--" + MACRO_START_TAG + "(.*)-->(.*)<!--" + MACRO_END_TAG + "-->");

  private static final Pattern REDO_IMAGE_REGEX = Pattern.compile("<wikiimage wikiparam=\"(.*)\"> (.*)</wikiimage>");

  private static final Pattern REDO_LINK_REGEX  = Pattern.compile("<wikilink wikiparam=\"(.*)\"> (.*)</wikilink>");

  private static final Pattern REDO_MACRO_REGEX = Pattern.compile("<wikimacro wikiparam=\"(.*)\"> (.*)</wikimacro>");

  public static String markupSanitize(String html) throws Exception {
    // Replace xwiki comment with specific tags
    Matcher matcher = LINK_REGEX.matcher(html);
    while (matcher.find()) {
      html =
           html.replace(matcher.group(0), "<wikilink wikiparam='" + matcher.group(1) + "'> " + matcher.group(2) + "</wikilink>");
    }

    matcher = IMAGE_REGEX.matcher(html);

    while (matcher.find()) {
      html = html.replace(matcher.group(0),
                          "<wikiimage wikiparam='" + matcher.group(1) + "'> " + matcher.group(2) + "</wikiimage>");
    }

    matcher = MACRO_REGEX.matcher(html);

    while (matcher.find()) {
      html = html.replace(matcher.group(0),
                          "<wikimacro wikiparam='" + matcher.group(1) + "'> " + matcher.group(2) + "</wikimacro>");
    }

    // Sanitizes the given HTML by applying the given policy to it.
    html = sanitize(html);

    // Replace specific tags by xwiki comments
    matcher = REDO_IMAGE_REGEX.matcher(html);

    while (matcher.find()) {
      html = html.replace(matcher.group(0),
                          "<!--" + IMAGE_START_TAG + decode(matcher.group(1)) + "-->" + decode(matcher.group(2)) + "<!--"
                              + IMAGE_END_TAG + "-->");
    }

    matcher = REDO_LINK_REGEX.matcher(html);

    while (matcher.find()) {
      html = html.replace(matcher.group(0),
                          "<!--" + LINK_START_TAG + decode(matcher.group(1)) + "-->" + decode(matcher.group(2)) + "<!--"
                              + LINK_END_TAG + "-->");
    }

    matcher = REDO_MACRO_REGEX.matcher(html);

    while (matcher.find()) {
      html = html.replace(matcher.group(0),
                          "<!--" + MACRO_START_TAG + decode(matcher.group(1)) + "-->" + decode(matcher.group(2)) + "<!--"
                              + MACRO_END_TAG + "-->");
    }

    return html;
  }

  private static String decode(String input) {
    if (input != null) {
      input = input.replace("&#" + ((int) '"') + ";", "\"")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&#" + ((int) '\'') + ";", "'")
                   .replaceAll("&#" + ((int) '+') + ";", "+")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&#" + ((int) '=') + ";", "=")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&#" + ((int) '@') + ";", "@")
                   .replaceAll("&#" + ((int) '`') + ";", "`");
    }
    return input;
  }
}
