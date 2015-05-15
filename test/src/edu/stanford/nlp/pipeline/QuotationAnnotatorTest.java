package edu.stanford.nlp.pipeline;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.List;
import java.util.Properties;

/**
 * @author Grace Muzny
 */
public class QuotationAnnotatorTest extends TestCase {

  private static StanfordCoreNLP pipeline;

  /**
   * Initialize the annotators at the start of the unit test.
   * If they've already been initialized, do nothing.
   */
  @Override
  public void setUp() {
    synchronized(QuotationAnnotatorTest.class) {
      if (pipeline == null) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, quote");
        pipeline = new StanfordCoreNLP(props);
      }
    }
  }

  public void testBasicInternalPunc() {
    String text = "\"Impossible, Mr. Bennet, impossible, when I am not acquainted with him\n" +
        " myself; how can you be so teasing?\"";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals(text, quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertInnerAnnotationValues(quotes.get(0), 0, 0, 0);
  }

  public void testBasicLatexQuotes() {
    String text = "`Hello,' he said, ``how are you doing?''";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("`Hello,'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("``how are you doing?''", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
    assertInnerAnnotationValues(quotes.get(0), 0, 0, 0);
    assertInnerAnnotationValues(quotes.get(1), 1, 0, 0);
  }

  public void testLatexQuotesWithDirectedApostrophes() {
    String text = "John`s he said, ``how are you doing?''";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("``how are you doing?''", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testEmbeddedLatexQuotes() {
    String text = "``Hello ``how are you doing?''''";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals(text, quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEmbedded("``how are you doing?''", text, quotes);
    assertInnerAnnotationValues(quotes.get(0), 0, 0, 0);
  }

  public void testEmbeddedSingleLatexQuotes() {
    String text = "`Hello `how are you doing?''";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals(text, quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEmbedded("`how are you doing?'", text, quotes);
  }

  public void testEmbeddedLatexQuotesAllEndSamePlace() {
    String text = "``Hello ``how `are ``you doing?'''''''";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals(text, quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEmbedded("``how `are ``you doing?'''''", text, quotes);
    assertEmbedded("`are ``you doing?'''", "``how `are ``you doing?'''''", quotes);
    assertEmbedded("``you doing?''", "`are ``you doing?'''", quotes);
  }

  public void testTripleEmbeddedLatexQuotes() {
    String text = "``Hel ``lo ``how'' are you'' doing?''";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals(text, quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEmbedded("``lo ``how'' are you''", text, quotes);
    assertEmbedded("``how''", "``lo ``how'' are you''", quotes);
  }

  public void testTripleEmbeddedUnicodeQuotes() {
    String text = "“Hel «lo “how” are you» doing?”";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals(text, quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEmbedded("«lo “how” are you»", text, quotes);
    assertEmbedded("“how”", "«lo “how” are you»", quotes);
  }

  public void testBasicUnicodeQuotes() {
    String text = "“Hello,” he said, “how are you doing?”";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("“Hello,”", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("“how are you doing?”", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testUnicodeQuotesWithBadUnicodeQuotes() {
    String text = "“Hello,” he said, “how‚ are‘ you doing?”";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("“Hello,”", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("“how‚ are‘ you doing?”", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testUnicodeQuotesWithApostrophes() {
    String text = "“Hello,” he said, “where is the dog‘s ball today?”";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("“Hello,”", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("“where is the dog‘s ball today?”", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testBasicDoubleQuotes() {
    String text = "\"Hello,\" he said, \"how are you doing?\"";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("\"Hello,\"", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals(quotes.get(0).get(CoreAnnotations.TokensAnnotation.class).size(), 4);
    assertEquals("\"how are you doing?\"", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testUnclosedInitialQuotes() {
    String text = "Hello,   \" he said, 'how are you doing?'";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("'how are you doing?'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testUnclosedLastDoubleQuotes() {
    String text = "\"Hello,\" he said, \"how are you doing?";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("\"Hello,\"", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testDoubleEnclosedInSingle() {
    String text = "'\"Hello,\" he said, \"how are you doing?\"'";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("'\"Hello,\" he said, \"how are you doing?\"'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEmbedded("\"Hello,\"", text, quotes);
    assertEmbedded("\"how are you doing?\"", text, quotes);
  }

  public void testSingleEnclosedInDouble() {
    String text = "\"'Hello,' he said, 'how are you doing?'\"";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals(text, quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEmbedded("'Hello,'", text, quotes);
    assertEmbedded("'how are you doing?'", text, quotes);
  }

  public void testEmbeddedQuotes() {
    String text = "\"'Enter,' said De Lacy; 'and I will\n" +
        "\n" +
        "try in what manner I can relieve your\n" +
        "\n" +
        "wants; but, unfortunately, my children\n" +
        "\n" +
        "are from home, and, as I am blind, I\n" +
        "\n" +
        "am afraid I shall find it difficult to procure\n" +
        "\n" +
        "food for you.'\"";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEmbedded("'Enter,'", text, quotes);
    String second = "'and I will\n" +
        "\n" +
        "try in what manner I can relieve your\n" +
        "\n" +
        "wants; but, unfortunately, my children\n" +
        "\n" +
        "are from home, and, as I am blind, I\n" +
        "\n" +
        "am afraid I shall find it difficult to procure\n" +
        "\n" +
        "food for you.'";
    assertEmbedded(second, text, quotes);
    assertInnerAnnotationValues(quotes.get(0), 0, 0, 0);
  }

  public void testEmbeddedQuotesTwo() {
    String text = "It was all very well to say 'Drink me,' but the wise little Alice was\n" +
        "not going to do THAT in a hurry. 'No, I'll \"look\" first,' she said, 'and\n" +
        "see whether it's marked \"poison\" or not';";
    List<CoreMap> quotes = runQuotes(text, 3);
    assertEmbedded("\"poison\"", "'and\n" +
        "see whether it's marked \"poison\" or not'", quotes);
    assertEmbedded("\"look\"", "'No, I'll \"look\" first,'", quotes);
    assertInnerAnnotationValues(quotes.get(0), 0, 0, 0);
    assertInnerAnnotationValues(quotes.get(1), 1, 1, 1);
  }


  public void testEmbeddedMixedComplicated() {
    String text = "It was all very 「well to say `Drink me,' but the wise little Alice was\n" +
        "not going to do THAT in a hurry. ‘No, I'll \"look\" first,’ she said, «and\n" +
        "see whether it's marked ``poison'' or \"not»";
    List<CoreMap> quotes = runQuotes(text, 3);
    assertEmbedded("``poison''", "«and\n" +
        "see whether it's marked ``poison'' or \"not»", quotes);
    assertEmbedded("\"look\"", "‘No, I'll \"look\" first,’", quotes);
  }

  public void testQuotesFollowEachother() {
    String text = "\"Where?\"\n" +
        "\n" +
        "\"I don't see 'im!\"\n" +
        "\n" +
        "\"Bigger, he's behind the trunk!\" the girl whimpered.";
    List<CoreMap> quotes = runQuotes(text, 3);
    assertEquals("\"Where?\"", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("\"I don't see 'im!\"", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("\"Bigger, he's behind the trunk!\"", quotes.get(2).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testBasicSingleQuotes() {
    String text = "'Hello,' he said, 'how are you doing?'";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("'Hello,'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("'how are you doing?'", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testUnclosedLastSingleQuotes() {
    String text = "'Hello,' he said, 'how are you doing?";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("'Hello,'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testMultiParagraphQuoteDouble() {
    String text = "Words blah bla \"Hello,\n\n \"I am the second paragraph.\n\n" +
        "\"I am the last.\" followed by more words";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("\"Hello,\n\n" +
        " \"I am the second paragraph.\n\n" +
        "\"I am the last.\"", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testMultiParagraphQuoteSingle() {
    String text = "Words blah bla 'Hello,\n\n 'I am the second paragraph.\n\n" +
        "'I am the second to last.\n\n'see there's more here.' followed by more words";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("'Hello,\n\n" +
        " 'I am the second paragraph.\n\n" +
        "'I am the second to last.\n\n" +
        "'see there's more here.'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertInnerAnnotationValues(quotes.get(0), 0, 0, 2);

  }

  public void testMultiLineQuoteDouble() {
    String text = "Words blah bla \"Hello,\nI am the second paragraph.\n" +
        "I am the last.\" followed by more words";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("\"Hello,\n" +
        "I am the second paragraph.\n" +
        "I am the last.\"", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testMultiLineQuoteSingle() {
    String text = "Words blah bla 'Hello,\nI am the second paragraph.\n" +
        "I am the last.' followed by more words";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("'Hello,\n" +
        "I am the second paragraph.\n" +
        "I am the last.'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testWordBeginningWithApostropheAtQuoteBeginningSingleQuotes() {
    String text = "''Tis nobler' Words blah bla 'I went to the house yesterday,' he said";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("''Tis nobler'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("'I went to the house yesterday,'", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
  }

//  public void testWordsWithApostropheTerminalsInSingleQuote() {
//    String text = "'Jones' cow is cuter!'";
//    List<CoreMap> quotes = runQuotes(text, 1);
//    assertEquals("'Jones' cow is cuter!'", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
//  }

  public void testWordsWithApostropheTerminalsInOneDoubleQuote() {
    String text = "\"Jones' cow is cuter!\"";
    List<CoreMap> quotes = runQuotes(text, 1);
    assertEquals("\"Jones' cow is cuter!\"", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
  }

  public void testWordsWithApostropheTerminalsInDoubleQuotes() {
    String text = "\"I said that Jones' cow was better,\" but then he " +
        "rebutted. I was shocked--\"My cow is better than any one of Jones' bovines!\"";
    List<CoreMap> quotes = runQuotes(text, 2);
    assertEquals("\"I said that Jones' cow was better,\"", quotes.get(0).get(CoreAnnotations.TextAnnotation.class));
    assertEquals("\"My cow is better than any one of Jones' bovines!\"", quotes.get(1).get(CoreAnnotations.TextAnnotation.class));
  }

  public static List<CoreMap> runQuotes(String text, int numQuotes) {
    Annotation doc = new Annotation(text);
    pipeline.annotate(doc);

    // now check what's up...
    List<CoreMap> quotes = doc.get(CoreAnnotations.QuotationsAnnotation.class);

    // look for embedded quotes and make sure they are already being reported
//    for(CoreMap s : quotes) {
//
//      String quote = s.get(CoreAnnotations.TextAnnotation.class); // what's wrong here?
//      System.out.print("text: ");
//      System.out.println(quote);
//    }

    Assert.assertNotNull(quotes);
    Assert.assertEquals(numQuotes, quotes.size());
    return quotes;
  }

  public static void assertInnerAnnotationValues(CoreMap quote, int quoteIndex,
                                                 int sentenceBegin, int sentenceEnd) {
//    System.out.println(quote);
//    System.out.println(quote.get(CoreAnnotations.QuotationIndexAnnotation.class));
//    System.out.println(quote.get(CoreAnnotations.SentenceBeginAnnotation.class));
//    System.out.println(quote.get(CoreAnnotations.SentenceEndAnnotation.class));
    assertEquals((int) quote.get(CoreAnnotations.QuotationIndexAnnotation.class), quoteIndex);
    assertEquals((int) quote.get(CoreAnnotations.SentenceBeginAnnotation.class), sentenceBegin);
    assertEquals((int) quote.get(CoreAnnotations.SentenceEndAnnotation.class), sentenceEnd);
  }


  public static void assertEmbedded(String embedded, String bed, List<CoreMap> quotes) {
    // find bed
    boolean found = assertEmbeddedHelper(embedded, bed, quotes);
    assertTrue(found);
  }

  public static boolean assertEmbeddedHelper(String embedded, String bed, List<CoreMap> quotes) {
    // find bed
    for(CoreMap b : quotes) {
      if (b.get(CoreAnnotations.TextAnnotation.class).equals(bed)) {
        // get the embedded quotes
        List<CoreMap> eqs = b.get(CoreAnnotations.QuotationsAnnotation.class);
        for (CoreMap eq : eqs) {
          if (eq.get(CoreAnnotations.TextAnnotation.class).equals(embedded)) {
            return true;
          }
        }
      } else {
        List<CoreMap> bEmbed = b.get(CoreAnnotations.QuotationsAnnotation.class);
        boolean recurse = assertEmbeddedHelper(embedded, bed, bEmbed);
        if (recurse) return true;
      }
    }
    return false;
  }
}