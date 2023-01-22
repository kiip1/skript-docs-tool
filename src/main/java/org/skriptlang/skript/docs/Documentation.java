package org.skriptlang.skript.docs;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Callback;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import com.google.common.base.CaseFormat;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO list special expressions for events and event values
 * TODO compare doc in code with changed one of the webserver and warn about differences?
 */
@ApiStatus.Internal
public final class Documentation {

	private static final Pattern CP_PARSE_MARKS_PATTERN = Pattern.compile("(?<=[(|])[-0-9]+?¦");
	private static final Pattern CP_EMPTY_PARSE_MARKS_PATTERN = Pattern.compile("\\(\\)");
	private static final Pattern CP_PARSE_TAGS_PATTERN = Pattern.compile("(?<=[(|\\[ ])[-a-zA-Z0-9!$#%^&*_+~=\"'<>?,.]*?:");
	private static final Pattern CP_EXTRA_OPTIONAL_PATTERN = Pattern.compile("\\[\\(((\\w+? ?)+)\\)]");
	private static final File DOCS_TEMPLATE_DIRECTORY = new File(Skript.getInstance().getScriptsFolder().getParent(), "doc-templates");
	private static final File DOCS_OUTPUT_DIRECTORY = new File(Skript.getInstance().getScriptsFolder().getParent(), "docs");
	private static final Map<String, AtomicInteger> DOCUMENTATION_CACHE = new ConcurrentHashMap<>();

	/**
	 * Force register hooks even if their plugins are not present in the server
	 */
	public static final boolean FORCE_HOOKS_SYSTEM_PROPERTY = "true".equals(System.getProperty("skript.forceregisterhooks"));

	public static boolean isDocsTemplateFound() {
		return getDocsTemplateDirectory().isDirectory();
	}

	/**
	 * Checks if server java args have 'skript.forceregisterhooks' property set to true and docs template folder is found
	 */
	public static boolean canGenerateUnsafeDocs() {
		return isDocsTemplateFound() && FORCE_HOOKS_SYSTEM_PROPERTY;
	}

	public static File getDocsTemplateDirectory() {
		return DOCS_TEMPLATE_DIRECTORY;
	}

	public static File getDocsOutputDirectory() {
		return DOCS_OUTPUT_DIRECTORY;
	}

	public static String cleanPatterns(String patterns) {
		return cleanPatterns(patterns, true);
	}

	// TODO Clean this
	public static String cleanPatterns(String patterns, boolean escapeHTML) {

		String cleanedPatterns = escapeHTML ? escapeHTML(patterns) : patterns;

		cleanedPatterns = CP_PARSE_MARKS_PATTERN.matcher(cleanedPatterns).replaceAll(""); // Remove marks
		cleanedPatterns = CP_EMPTY_PARSE_MARKS_PATTERN.matcher(cleanedPatterns).replaceAll(""); // Remove empty mark setting groups (mark¦)
		cleanedPatterns = CP_PARSE_TAGS_PATTERN.matcher(cleanedPatterns).replaceAll(""); // Remove new parse tags, see https://regex101.com/r/mTebpn/1
		cleanedPatterns = CP_EXTRA_OPTIONAL_PATTERN.matcher(cleanedPatterns).replaceAll("[$1]"); // Remove unnecessary parentheses such as [(script)]

		Callback<String, Matcher> callback = m -> { // Replace optional parentheses with optional brackets
			String group = m.group();

			boolean startToEnd = group.contains("(|"); // Due to regex limitation we search from the beginning to the end but if it has '|)' we will begin from the opposite direction

			int depth = 0;
			int charIndex = 0;
			char[] chars = group.toCharArray();
			for (int i = (startToEnd ? 0 : chars.length - 1); (startToEnd ? i < chars.length : i >= 0); i = (startToEnd ? i+1 : i-1)) {
				char c = chars[i];
				if (c == ')') {
					depth++;
					if (startToEnd && depth == 0) { // Break if the nearest closing parentheses pair is found if startToEnd == true
						charIndex = i;
						break;
					}
				} else if (c == '(') {
					depth--;
					if (!startToEnd && depth == 0) { // Break if the nearest opening parentheses pair is found if startToEnd == false
						charIndex = i;
						break;
					}
				} else if (c == '\\') { // Escape escaping characters
					i--;
				}
			}
			if (depth == 0 && charIndex != 0) {
				if (startToEnd) {
					return "[" +
						group.substring(0, charIndex)
						.replace("(|", "") + // Ex. (|(t|y)) -> [(t|y)] & (|x(t|y)) -> [x(t|y)]
						"]" +
						group.substring(charIndex + 1, chars.length); // Restore the unchanged after part
				}
				else {
					return group.substring(0, charIndex) + // Restore the unchanged before part
						"[" +
						group.substring(charIndex + 1, chars.length)
						.replace("|)", "") + // Ex. ((t|y)|) -> [(t|y)] & ((t|y)x|) -> [(t|y)x]
						"]";
				}
			} else {
				return group;
			}
		};

		cleanedPatterns = cleanedPatterns.replaceAll("\\(([^()]+?)\\|\\)", "[($1)]"); // Matches optional syntaxes that doesn't have nested parentheses
		cleanedPatterns = cleanedPatterns.replaceAll("\\(\\|([^()]+?)\\)", "[($1)]");

		cleanedPatterns = StringUtils.replaceAll(cleanedPatterns, "\\((.+)\\|\\)", callback); // Matches complex optional parentheses that has nested parentheses
		assert cleanedPatterns != null;
		cleanedPatterns = StringUtils.replaceAll(cleanedPatterns, "\\((.+?)\\|\\)", callback);
		assert cleanedPatterns != null;
		cleanedPatterns = StringUtils.replaceAll(cleanedPatterns, "\\(\\|(.+)\\)", callback);
		assert cleanedPatterns != null;
		cleanedPatterns = StringUtils.replaceAll(cleanedPatterns, "\\(\\|(.+?)\\)", callback);
		assert cleanedPatterns != null;

		String s = StringUtils.replaceAll(cleanedPatterns, "(?<!\\\\)%(.+?)(?<!\\\\)%", // Convert %+?% (aka types) inside patterns to links
				m -> {
					String s1 = m.group(1);
					if (s1.startsWith("-"))
						s1 = s1.substring(1);
					String flag = "";
					if (s1.startsWith("*") || s1.startsWith("~")) {
						flag = s1.substring(0, 1);
						s1 = s1.substring(1);
					}
					final int a = s1.indexOf("@");
					if (a != -1)
						s1 = s1.substring(0, a);
					final StringBuilder b = new StringBuilder("%");
					b.append(flag);
					boolean first = true;
					for (final String c : s1.split("/")) {
						assert c != null;
						if (!first)
							b.append("/");
						first = false;
						final NonNullPair<String, Boolean> p = Utils.getEnglishPlural(c);
						final ClassInfo<?> ci = Classes.getClassInfoNoError(p.getFirst());
						if (ci != null && ci.hasDocs()) { // equals method throws null error when doc name is null
							b.append("<a href='./classes.html#").append(p.getFirst()).append("'>").append(ci.getName().toString(p.getSecond())).append("</a>");
						} else {
							b.append(c);
							if (ci != null && ci.hasDocs())
								Skript.warning("Used class " + p.getFirst() + " has no docName/name defined");
						}
					}
					return "" + b.append("%");
				});
		assert s != null : patterns;
		return s;
	}
	
	public static String documentationId(String name) {
		name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name).replace(" ", "-");
		return name + "-" + DOCUMENTATION_CACHE.computeIfAbsent(name, $ -> new AtomicInteger()).incrementAndGet();
	}

	private static String escapeHTML(@Nullable String input) {
		if (input == null) {
			assert false;
			return "";
		}
		
		return input.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}

}