package org.skriptlang.skript.docs.transformers;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import com.google.common.base.Joiner;
import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.docs.Documentation;
import org.skriptlang.skript.docs.generators.GenerationResult;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.skriptlang.skript.docs.HtmlDocumentation.*;

@ApiStatus.Internal
public final class HtmlTransformer implements Transformer<String, GenerationResult> {
	
	private static final String SKRIPT_VERSION = Skript.getVersion().toString().replaceAll("-(dev|alpha|beta)\\d*", ""); // Filter branches
	private static final Pattern NEW_TAG_PATTERN = Pattern.compile(SKRIPT_VERSION + "(?!\\.)"); // (?!\\.) to avoid matching 2.6 in 2.6.1 etc.
	
	private final Path root;
	private final String type;
	private String desc;
	
	public HtmlTransformer(Path root, String type, String desc) {
		this.root = root;
		this.type = type;
		this.desc = desc;
	}
	
	@Override
	public String transform(GenerationResult result) {
		desc = desc.replace("${element.name}", result.name());
		desc = desc.replace("${element.since}", result.since());
		desc = desc.replace("${element.keywords}", Joiner.on(", ").join(result.keywords()));
		desc = desc.replace("${element.desc}", Joiner.on("\n").join(result.description()).replace("\n\n", "<p>"));
		desc = desc.replace("${element.desc-safe}", Joiner.on("\n").join(result.description())
				.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
		desc = desc.replace("${element.examples}", Joiner.on("<br>").join(result.examples()));
		desc = desc.replace("${element.examples-safe}", Joiner.on("\\n").join(result.examples())
				.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
		desc = desc.replace("${element.id}", result.documentationId());
		String[] events = result.events();
		desc = handleIf(desc, "${if events}", events.length > 0);
		if (events.length > 0) {
			String[] eventLinks = new String[events.length];
			for (int i = 0; i < events.length; i++) {
				String eventName = events[i];
				eventLinks[i] = "<a href=\"events.html#" + eventName.replaceAll("( ?/ ?| +)", "_") + "\">" + eventName + "</a>";
			}
			desc = desc.replace("${element.events}", Joiner.on(", ").join(eventLinks));
		}
		desc = desc.replace("${element.events-safe}", events.length == 0 ? "" : Joiner.on(", ").join(events));
		desc = handleIf(desc, "${if required-plugins}", result.requiredPlugins().length > 0);
		desc = desc.replace("${element.required-plugins}", Joiner.on(", ").join(result.requiredPlugins()));
		ClassInfo<?> returnType = result.info() instanceof SyntaxInfo.Expression
				? Classes.getSuperClassInfo(((SyntaxInfo.Expression<?, ?>) result.info()).returnType()) : null;
		desc = replaceReturnType(desc, returnType);
		desc = handleIf(desc, "${if by-addon}", false);
		desc = handleIf(desc, "${if new-element}", NEW_TAG_PATTERN.matcher(result.since()).find());
		desc = desc.replace("${element.type}", type);
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
			String pattern = readFile(root.resolve("templates").resolve(split[1]).toFile());
			StringBuilder patterns = new StringBuilder();
			for (String line : result.patterns())
				patterns.append(pattern.replace("${element.pattern}", Documentation.cleanPatterns(line)));
			
			String toReplace = "${generate element.patterns " + split[1] + "}";
			desc = desc.replace(toReplace, patterns.toString());
			desc = desc.replace("${generate element.patterns-safe " + split[1] + "}", patterns.toString().replace("\\", "\\\\"));
		}
		
		return desc;
	}
	
	@Override
	public String combine(String a, String b) {
		return a + b;
	}
	
}
