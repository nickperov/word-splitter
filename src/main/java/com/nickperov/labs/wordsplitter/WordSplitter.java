package com.nickperov.labs.wordsplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class WordSplitter {
	
	/**
	 * Use RegExpr and cycle over strings array to split text into words
	 */
	public static List<String> splitWordsRegExpCycle(final String text) {
		final List<String> words = new ArrayList<>();
		
		final String[] strs = text.trim().split("\\W+");
		for (final String str : strs)
			words.add(str.trim());
		
		return words;
	}
	
	/**
	 * Use character array and cycle to split text into words
	 */
	public static List<String> splitWordsCharArrayCycle(final String text) {
		final List<String> words = new ArrayList<>();
		
		final char[] chars = text.toCharArray();
		final StringBuilder sb = new StringBuilder();
		boolean wordStart = false;
		for (final char c : chars) {
			if (Character.isAlphabetic(c)) {
				if (!wordStart)
					wordStart = true;
				sb.append(c);
			} else if (wordStart) {
				words.add(sb.toString());
				sb.setLength(0);
				wordStart = false;
			}
		}
		
		if (sb.length() > 0)
			words.add(sb.toString());
		
		return words;
	}
	
	/**
	 * Use character array and recursion to split text into words
	 */
	public static List<String> splitWordsCharArrayRecursion(final String text) {
		final List<String> words = new ArrayList<>();
		final char[] textArray = text.toCharArray();
		
		if (textArray.length > 1000)
			return Collections.emptyList();
		
		splitWordsCharArrayRecursion(text.toCharArray(), 0, new StringBuilder(), words);
		return words;
	}
	
	private static void splitWordsCharArrayRecursion(final char[] text, int i, final StringBuilder sb, final List<String> words) {
		if (text.length == i) {
			if (sb.length() != 0)
				words.add(sb.toString());
			return;
		}
		
		final char c = text[i];
		appendChar(sb, words, c);
		splitWordsCharArrayRecursion(text, ++i, sb, words);
	}

	private static void appendChar(final StringBuilder sb, final List<String> words, final char c) {
		if (Character.isAlphabetic(c)) {
			sb.append(c);
		} else if (sb.length() != 0) {
			words.add(sb.toString());
			sb.setLength(0);
		}
	}

	/**
	 * Use Stream API to split text into words
	 */
/*	public static List<String> splitWordsStream(String text) {
		
		Stream.of(text.toCharArray()).
		
		
	}*/
	
	
	/**
	 * Use ForkJoinPool to split text into words
	 */
	private static List<String> splitWordsForkJoin(final String text, final int cores) {
		final ForkJoinPool pool = new ForkJoinPool();
		
		final SplitTextTask task = new SplitTextTask(text.toCharArray(), 0, text.length() - 1, Math.max(1000, text.length()/cores));
		final List<String> words = pool.invoke(task);
		
		checkAndRemoveLastCS(words);
		checkAndRemoveFirstCS(words);
		
		return words;
	}
	
	public static List<String> splitWordsForkJoin(final String text) {
		return splitWordsForkJoin(text, Runtime.getRuntime().availableProcessors() / 4);
	}
	
	private static void checkAndRemoveLastCS(final List<String> words) {
		final int lastWordIndex = words.size() - 1;
		final String lastWord = words.get(lastWordIndex);
		final int lastCharIndex = lastWord.length() - 1;
		if (lastWord.charAt(lastCharIndex) == SplitTextTask.CONCAT_SYMBOL)
			words.set(lastWordIndex, lastWord.substring(0, lastCharIndex));
	}
	
	private static void checkAndRemoveFirstCS(final List<String> words) {
		final String firstWord = words.get(0);
		if (firstWord.charAt(0) == SplitTextTask.CONCAT_SYMBOL)
			words.set(0, firstWord.substring(1, firstWord.length()));
	}
	
	private static class SplitTextTask extends RecursiveTask<List<String>> {
		
		private static final long serialVersionUID = 1L;
		
		private static final char CONCAT_SYMBOL = '#';
		
		private static int threshold;
		private final char[] text;
		private final int begin, end;
		
		private SplitTextTask(final char[] text, final int begin, final int end, final int threshold) {
			this(text, begin, end);
			SplitTextTask.threshold = threshold;
		}
		
		private SplitTextTask(final char[] text, final int begin, final int end) {
			this.text = text;
			this.begin = begin;
			this.end = end;
		}

		@Override
		protected List<String> compute() {
			final int delta = this.end - this.begin;
			if (delta > threshold) {
				final int gap = (this.end - this.begin) / 2;
				final SplitTextTask splitTaskOne = new SplitTextTask(this.text, this.begin, this.begin + gap);
				final SplitTextTask splitTaskTwo = new SplitTextTask(this.text, this.begin + gap + 1, this.end);
				splitTaskOne.fork();
				final List<String> wordsTwo = splitTaskTwo.invoke();
				final List<String> wordsOne = splitTaskOne.join();
				
				return joinWordLists(wordsOne, wordsTwo);
			} else
				return splitCharArray(this.text, this.begin, this.end);
		}
		
		private static List<String> joinWordLists(final List<String> wordsOne, final List<String> wordsTwo) {
			// TODO refactor 
			if (wordsOne.isEmpty() && !wordsTwo.isEmpty()) {
				checkAndRemoveFirstCS(wordsTwo);
				return wordsTwo;
			} else if (wordsTwo.isEmpty() && !wordsOne.isEmpty()) {
				checkAndRemoveLastCS(wordsOne);
				return wordsOne;
			} else if (wordsTwo.isEmpty() && wordsOne.isEmpty()) {
				return wordsOne;
			}
			
			final String lastWordL1 = wordsOne.get(wordsOne.size() - 1);
			final int lastSymbolIndex = lastWordL1.length() - 1;
			
			final String firstWordL2 = wordsTwo.get(0);
			
			if (lastWordL1.charAt(lastSymbolIndex) == CONCAT_SYMBOL) {
				if (firstWordL2.charAt(0) == CONCAT_SYMBOL) {
					wordsOne.set(wordsOne.size() - 1, lastWordL1.substring(0, lastSymbolIndex) + firstWordL2.substring(1, firstWordL2.length()));
					wordsOne.addAll(wordsTwo.subList(1, wordsTwo.size()));					
				} else {
					wordsOne.set(wordsOne.size() - 1, lastWordL1.substring(0, lastSymbolIndex));
					wordsOne.addAll(wordsTwo);
				}
			} else if (firstWordL2.charAt(0) == CONCAT_SYMBOL) {
				wordsTwo.set(0, firstWordL2.substring(1));
				wordsOne.addAll(wordsTwo);
			} else {
				wordsOne.addAll(wordsTwo);
			}
			
			return wordsOne;
		}
		
		private static List<String> splitCharArray(final char[] text, final int begin, final int end) {
			final List<String> words = new ArrayList<>();
			final StringBuilder sb = new StringBuilder();
			
			// check first symbol
			if (Character.isAlphabetic(text[begin])) {
				sb.append(CONCAT_SYMBOL);
				sb.append(text[begin]);
			}
			
			for (int i = begin + 1; i < end; i++) {
				final char c = text[i];
				appendChar(sb, words, c);
			}
			
			// check last symbol
			if (Character.isAlphabetic(text[end])) {
				sb.append(text[end]);
				sb.append(CONCAT_SYMBOL);
			}
			
			if (sb.length() != 0)
				words.add(sb.toString());
			
			return words;
		}
	}
}