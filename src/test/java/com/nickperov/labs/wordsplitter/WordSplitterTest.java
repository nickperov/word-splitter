package com.nickperov.labs.wordsplitter;

import static org.junit.Assert.assertArrayEquals;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class WordSplitterTest {

	@Test
	public void testWordSplitLongText() {
		final String[] expectedResult = new String[] {"That", "is", "one", "small", "step", "for", "a", "man", "one", "giant", "leap", "for", "mankind"};
		testSplitTextAllMethods("That is one small step for [a] man, one giant leap for mankind.", expectedResult);
	}
	
	@Test
	public void testWordSplitMultiSpaceText() {
		final String[] expectedResult = new String[] {"Example", "of", "some", "text", "formated", "non"};
		testSplitTextAllMethods("  Example  of   some  non  formated  text", expectedResult);
	}
	
	@Test
	public void testWordSplitSeparatedWords() {
		final String[] expectedResult = new String[] {"List", "of", "words", "separated", "by", "different", "symbols"};
		testSplitTextAllMethods("List,of,words|separated;by:different---symbols!!!", expectedResult);
	}
	
	@Test
	public void testWordSplitSingleWord() {
		final String[] expectedResult = new String[] {"BigSingleWord"};
		testSplitTextAllMethods("BigSingleWord                                                                                     ", expectedResult);
	}
	
	@Test
	public void testWordSplitVeryLongText() {
		final String text = readTextFile();
		testSplitTextAllMethods(text, new String[]{}, false);
	}
	
	private static String readTextFile() {
		final char[] cbuf = new char[100];
		final StringBuilder sb = new StringBuilder();
		try (final FileReader fr = new FileReader("./src/test/resources/Text.txt")) {
			int index;
			while ((index = fr.read(cbuf)) > 0) {
				sb.append(cbuf, 0, index);
			}
			
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	private void testSplitTextAllMethods(final String text, final String[] words) {
		testSplitTextAllMethods(text, words, true);
	}
	
	private void testSplitTextAllMethods(final String text, final String[] words, final boolean check) {
		Arrays.sort(words);
		final Method[] methods = WordSplitter.class.getMethods();
		for (final Method method : methods) {
			if (isMethodQualified(method))
				testSplitText(text, words, method, check);
		}
	}
	
	private void testSplitText(final String text, final String[] words, final Method method, final boolean check) {
		final Object[] result = splitText(text, method).toArray();
		if (check) {
			Arrays.sort(result);
			assertArrayEquals(words, result);
		}
	}
	
	private List<String> splitText(final String text, final Method method) {
		try {
			return (List<String>) method.invoke(null, text);
		} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}
	
	private static boolean isMethodQualified(final Method method) {
		if (!method.isAccessible()) {
			final Class<?>[] parameters = method.getParameterTypes();
			return parameters.length == 1 && parameters[0].equals(String.class);
		}
		return false;
	}
}