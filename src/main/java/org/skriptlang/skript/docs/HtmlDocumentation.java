package org.skriptlang.skript.docs;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Events;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.JavaFunction;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.registrations.Classes;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.bukkit.registration.BukkitRegistry;
import org.skriptlang.skript.docs.generators.BukkitGenerator;
import org.skriptlang.skript.docs.generators.GenerationResult;
import org.skriptlang.skript.docs.transformers.HtmlTransformer;
import org.skriptlang.skript.registration.SkriptRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Template engine, primarily used for generating Skript documentation
 * pages by combining data from annotations and templates.
 */
@ApiStatus.Internal
public final class HtmlDocumentation {

	private static final String SKRIPT_VERSION = Skript.getVersion().toString().replaceAll("-(dev|alpha|beta)\\d*", ""); // Filter branches
	private static final Pattern NEW_TAG_PATTERN = Pattern.compile(SKRIPT_VERSION + "(?!\\.)"); // (?!\\.) to avoid matching 2.6 in 2.6.1 etc.
	private static final Pattern RETURN_TYPE_LINK_PATTERN = Pattern.compile("( ?href=\"(classes\\.html|)#|)\\$\\{element\\.return-type-linkcheck}");

	private final File template;
	private final File output;
	private final String skeleton;

	public HtmlDocumentation(File templateDir, File outputDir) {
		this.template = templateDir;
		this.output = outputDir;
		
		// Skeleton which contains every other page
		this.skeleton = readFile(new File(template + "/template.html"));
	}
	
	private static final Comparator<ClassInfo<?>> classInfoComparator = (a, b) -> {
		if (a == null || b == null)
			throw new NullPointerException();
		
		String name1 = a.getDocName();
		if (name1 == null)
			name1 = a.getCodeName();
		String name2 = b.getDocName();
		if (name2 == null)
			name2 = b.getCodeName();
		
		return name1.compareTo(name2);
	};

	/**
	 * Generates documentation using template and output directories
	 * given in the constructor.
	 */
	public void generate() {
		for (File file : template.listFiles()) {
			if (file.getName().matches("css|js|assets")) { // Copy CSS/JS/Assets folders
				String slashName = "/" + file.getName();
				File fileTo = new File(output + slashName);
				fileTo.mkdirs();
				for (File filesInside : new File(template + slashName).listFiles()) {
					if (filesInside.isDirectory()) 
						continue;
						
					if (!filesInside.getName().toLowerCase(Locale.ENGLISH).endsWith(".png")) { // Copy images
						writeFile(new File(fileTo + "/" + filesInside.getName()), readFile(filesInside));
					}
					
					else if (!filesInside.getName().matches("(?i)(.*)\\.(html?|js|css|json)")) {
						try {
							Files.copy(filesInside, new File(fileTo + "/" + filesInside.getName()));
						} catch (IOException e) {
							e.printStackTrace();
						}
							
					}
				}
				continue;
			} else if (file.isDirectory()) // Ignore other directories
				continue;
			if (file.getName().endsWith("template.html") || file.getName().endsWith(".md"))
				continue; // Ignore skeleton and README

			Skript.info("Creating documentation for " + file.getName());

			String content = readFile(file);
			String page;
			if (file.getName().endsWith(".html"))
				page = skeleton.replace("${content}", content); // Content to inside skeleton
			else // Not HTML, so don't even try to use template.html
				page = content;

			page = page.replace("${skript.version}", Skript.getVersion().toString()); // Skript version
			page = page.replace("${skript.build.date}", new SimpleDateFormat("dd/MM/yyyy").format(new Date())); // Build date
			page = page.replace("${pagename}", file.getName().replace(".html", ""));

			List<String> replace = Lists.newArrayList();
			int include = page.indexOf("${include"); // Single file includes
			while (include != -1) {
				int endIncl = page.indexOf("}", include);
				String name = page.substring(include + 10, endIncl);
				replace.add(name);

				include = page.indexOf("${include", endIncl);
			}

			for (String name : replace) {
				String temp = readFile(new File(template + "/templates/" + name));
				temp = temp.replace("${skript.version}", Skript.getVersion().toString());
				page = page.replace("${include " + name + "}", temp);
			}

			int generate = page.indexOf("${generate"); // Generate expressions etc.
			while (generate != -1) {
				int nextBracket = page.indexOf("}", generate);
				String[] genParams = page.substring(generate + 11, nextBracket).split(" ");
				StringBuilder generated = new StringBuilder();

				String descTemp = readFile(new File(template + "/templates/" + genParams[1]));
				String genType = genParams[0];
				boolean isDocsPage = genType.equals("docs");
				
				HtmlTransformer transformer = new HtmlTransformer(template.toPath(), descTemp);
				if (genType.equals("expressions") || isDocsPage) {
					List<GenerationResult> results = new BukkitGenerator().generate(Skript.instance().registry(),
							SkriptRegistry.Key.EXPRESSION);
					results.sort(Comparator.comparing(GenerationResult::name));
					generated.append(transformer.transform(results).replaceAll(
						Pattern.quote("${element.type}"), "Expression"));
				}
				if (genType.equals("effects") || isDocsPage) {
					List<GenerationResult> results = new BukkitGenerator().generate(Skript.instance().registry(),
							SkriptRegistry.Key.EFFECT);
					results.sort(Comparator.comparing(GenerationResult::name));
					generated.append(transformer.transform(results).replaceAll(
						Pattern.quote("${element.type}"), "Effect"));
				}
				if (genType.equals("conditions") || isDocsPage) {
					List<GenerationResult> results = new BukkitGenerator().generate(Skript.instance().registry(),
							SkriptRegistry.Key.CONDITION);
					results.sort(Comparator.comparing(GenerationResult::name));
					generated.append(transformer.transform(results).replaceAll(
						Pattern.quote("${element.type}"), "Condition"));
				}
				if (genType.equals("sections") || isDocsPage) {
					List<GenerationResult> results = new BukkitGenerator().generate(Skript.instance().registry(),
							SkriptRegistry.Key.SECTION);
					results.sort(Comparator.comparing(GenerationResult::name));
					generated.append(transformer.transform(results).replaceAll(
						Pattern.quote("${element.type}"), "Section"));
				}
				if (genType.equals("events") || isDocsPage) {
					List<GenerationResult> results = new BukkitGenerator().generate(Skript.instance().registry(),
							BukkitRegistry.EVENT);
					results.sort(Comparator.comparing(GenerationResult::name));
					generated.append(transformer.transform(results).replaceAll(
						Pattern.quote("${element.type}"), "Event"));
				}
				if (genType.equals("classes") || isDocsPage) {
					List<ClassInfo<?>> classes = new ArrayList<>(Classes.getClassInfos());
					classes.sort(classInfoComparator);
					for (ClassInfo<?> info : classes) {
						if (!info.hasDocs())
							continue;
						generated.append(generateClass(descTemp, info, generated.toString()));
					}
				}
				if (genType.equals("functions") || isDocsPage) {
					List<JavaFunction<?>> functions = new ArrayList<>(Functions.getJavaFunctions());
					functions.sort(Comparator.comparing(Function::getName));
					for (JavaFunction<?> info : functions)
						generated.append(generateFunction(descTemp, info));
				}
				
				page = page.replace(page.substring(generate, nextBracket + 1), generated.toString());
				
				generate = page.indexOf("${generate", nextBracket);
			}
			
			String name = file.getName();
			if (name.endsWith(".html")) // Fix some stuff specially for HTML
				page = minifyHtml(page.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")); // Tab to 4 non-collapsible spaces
			writeFile(new File(output + File.separator + name), page);
		}
	}
	
	private static String minifyHtml(String page) {
		StringBuilder sb = new StringBuilder(page.length());
		boolean space = false;
		for (int i = 0; i < page.length();) {
			int c = page.codePointAt(i);
			if ((c == '\n' || c == ' ')) {
				if (!space) {
					sb.append(' ');
					space = true;
				}
			} else {
				space = false;
				sb.appendCodePoint(c);
			}
			
			i += Character.charCount(c);
		}
		return replaceBr(sb.toString());
	}
	
	private String generateClass(String descTemp, ClassInfo<?> info, @Nullable String page) {
		Class<?> c = info.getC();
		String desc;

		// Name
		String docName = getDefaultIfNullOrEmpty(info.getDocName(), "Unknown Name");
		desc = descTemp.replace("${element.name}", docName);

		// Since
		String since = getDefaultIfNullOrEmpty(info.getSince(), "Unknown");
		desc = desc.replace("${element.since}", since);

		// Description
		String[] description = getDefaultIfNullOrEmpty(info.getDescription(), "Missing description.");
		desc = desc.replace("${element.desc}", Joiner.on("\n").join(description).replace("\n\n", "<p>"));
		desc = desc
				.replace("${element.desc-safe}", Joiner.on("\\n").join(description)
				.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));

		// By Addon
		desc = handleIf(desc, "${if by-addon}", false);

		// Examples
		String[] examples = getDefaultIfNullOrEmpty(info.getExamples(), "Missing examples.");
		desc = desc.replace("${element.examples}", Joiner.on("\n<br>").join(examples));
		desc = desc.replace("${element.examples-safe}", Joiner.on("\\n").join(examples)
				.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));

		// Documentation ID
		String ID = info.getDocumentationID() == null ? info.getCodeName() : info.getDocumentationID();
		desc = desc.replace("${element.id}", ID);

		// Events
		Events events = c.getAnnotation(Events.class);
		desc = handleIf(desc, "${if events}", events != null);
		if (events != null) {
			String[] eventNames = events != null ? events.value() : null;
			String[] eventLinks = new String[eventNames.length];
			for (int i = 0; i < eventNames.length; i++) {
				String eventName = eventNames[i];
				eventLinks[i] = "<a href=\"events.html#" + eventName.replaceAll(" ?/ ?", "_").replaceAll(" +", "_") + "\">" + eventName + "</a>";
			}
			desc = desc.replace("${element.events}", Joiner.on(", ").join(eventLinks));
		}
		desc = desc.replace("${element.events-safe}", events == null ? "" : Joiner.on(", ").join((events != null ? events.value() : null)));

		// Required Plugins
		String[] requiredPlugins = info.getRequiredPlugins();
		desc = handleIf(desc, "${if required-plugins}", requiredPlugins != null);
		desc = desc.replace("${element.required-plugins}", Joiner.on(", ").join(requiredPlugins == null ? new String[0] : requiredPlugins));

		// New Elements
		desc = handleIf(desc, "${if new-element}", NEW_TAG_PATTERN.matcher(since).find());

		// Type
		desc = desc.replace("${element.type}", "Type");

		// Return Type
		desc = handleIf(desc, "${if return-type}", false);

		// Generate Templates
		List<String> toGen = new ArrayList<>();
		int generate = desc.indexOf("${generate");
		while (generate != -1) {
			int nextBracket = desc.indexOf("}", generate);
			String data = desc.substring(generate + 11, nextBracket);
			toGen.add(data);
			
			generate = desc.indexOf("${generate", nextBracket);
		}
		
		// Assume element.pattern generate
		for (String data : toGen) {
			String[] split = data.split(" ");
			String pattern = readFile(new File(template + "/templates/" + split[1]));
			StringBuilder patterns = new StringBuilder();
			String[] lines = getDefaultIfNullOrEmpty(info.getUsage(), "Missing patterns.");
			for (String line : lines) {
				assert line != null;
				String parsed = pattern.replace("${element.pattern}", line);
				patterns.append(parsed);
			}
			
			desc = desc.replace("${generate element.patterns " + split[1] + "}", patterns.toString());
			desc = desc.replace("${generate element.patterns-safe " + split[1] + "}", patterns.toString().replace("\\", "\\\\"));
		}
		
		return desc;
	}
	
	private String generateFunction(String descTemp, JavaFunction<?> info) {
		// Name
		String docName = getDefaultIfNullOrEmpty(info.getName(), "Unknown Name");
		String desc = descTemp.replace("${element.name}", docName);

		// Since
		String since = getDefaultIfNullOrEmpty(info.getSince(), "Unknown");
		desc = desc.replace("${element.since}", since);

		// Description
		String[] description = getDefaultIfNullOrEmpty(info.getDescription(), "Missing description.");
		desc = desc.replace("${element.desc}", Joiner.on("\n").join(description));
		desc = desc.replace("${element.desc-safe}", Joiner.on("\\n").join(description)
				.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));

		desc = handleIf(desc, "${if by-addon}", false);

		// Examples
		String[] examples = getDefaultIfNullOrEmpty(info.getExamples(), "Missing examples.");
		desc = desc.replace("${element.examples}", Joiner.on("\n<br>").join(examples));
		desc = desc
				.replace("${element.examples-safe}", Joiner.on("\\n").join(examples)
				.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));

		// Documentation ID
		desc = desc.replace("${element.id}", info.getName());

		// Events
		desc = handleIf(desc, "${if events}", false); // Functions do not require events nor plugins (at time writing this)

		// Required Plugins
		desc = handleIf(desc, "${if required-plugins}", false);

		// Return Type
		ClassInfo<?> returnType = info.getReturnType();
		desc = replaceReturnType(desc, returnType);

		// New Elements
		desc = handleIf(desc, "${if new-element}", NEW_TAG_PATTERN.matcher(since).find());

		// Type
		desc = desc.replace("${element.type}", "Function");

		// Generate Templates
		List<String> toGen = Lists.newArrayList();
		int generate = desc.indexOf("${generate");
		while (generate != -1) {
			int nextBracket = desc.indexOf("}", generate);
			String data = desc.substring(generate + 11, nextBracket);
			toGen.add(data);
			
			generate = desc.indexOf("${generate", nextBracket);
		}
		
		// Assume element.pattern generate
		for (String data : toGen) {
			String[] split = data.split(" ");
			String pattern = readFile(new File(template + "/templates/" + split[1]));
			String patterns = "";
			Parameter<?>[] params = info.getParameters();
			String[] types = new String[params.length];
			for (int i = 0; i < types.length; i++) {
				types[i] = params[i].toString();
			}
			String line = docName + "(" + Joiner.on(", ").join(types) + ")"; // Better not have nulls
			patterns += pattern.replace("${element.pattern}", line);
			
			desc = desc.replace("${generate element.patterns " + split[1] + "}", patterns);
			desc = desc.replace("${generate element.patterns-safe " + split[1] + "}", patterns.replace("\\", "\\\\"));
		}
		
		return desc;
	}
	
	private static String getDefaultIfNullOrEmpty(@Nullable String string, String message) {
		return (string == null || string.isEmpty()) ? message : string; // Null check first otherwise NullPointerException is thrown
	}
	
	private static String[] getDefaultIfNullOrEmpty(@Nullable String[] string, String message) {
		return (string == null || string.length == 0 || string[0].equals("")) ? new String[]{ message } : string; // Null check first otherwise NullPointerException is thrown
	}
	
	public static String readFile(File file) {
		try {
			return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static void writeFile(File file, String data) {
		try {
			Files.write(data.getBytes(StandardCharsets.UTF_8), file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String handleIf(String desc, String start, boolean value) {
		int ifStart = desc.indexOf(start);
		while (ifStart != -1) {
			int ifEnd = desc.indexOf("${end}", ifStart);
			String data = desc.substring(ifStart + start.length() + 1, ifEnd);
			
			String before = desc.substring(0, ifStart);
			String after = desc.substring(ifEnd + 6);
			if (value)
				desc = before + data + after;
			else
				desc = before + after;
			
			ifStart = desc.indexOf(start, ifEnd);
		}
		
		return desc;
	}
	
	public static String replaceReturnType(String desc, @Nullable ClassInfo<?> returnType) {
		if (returnType == null)
			return handleIf(desc, "${if return-type}", false);
		
		boolean noDoc = returnType.hasDocs();
		String returnTypeName = noDoc ? returnType.getCodeName() : returnType.getDocName();
		String returnTypeLink = noDoc ? "" : "$1" + (returnType.getDocumentationID() == null ? returnType.getCodeName() : returnType.getDocumentationID());
		
		desc = handleIf(desc, "${if return-type}", true);
		desc = RETURN_TYPE_LINK_PATTERN.matcher(desc).replaceAll(returnTypeLink);
		desc = desc.replace("${element.return-type}", returnTypeName);
		return desc;
	}
	
	private static String replaceBr(String page) {
		return page.replaceAll("<br/>", "\n");
	}

}
