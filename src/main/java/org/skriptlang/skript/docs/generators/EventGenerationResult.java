package org.skriptlang.skript.docs.generators;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.DocumentationId;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.util.MarkedForRemoval;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

final class EventGenerationResult implements GenerationResult {
	
	private final BukkitSyntaxInfos.Event<?> info;
	private final Class<?> clazz;
	private final String id;
	
	public EventGenerationResult(BukkitSyntaxInfos.Event<?> info, Class<?> clazz) {
		this.info = info;
		this.clazz = clazz;
		id = consumer(DocumentationId.class, DocumentationId::value, GenerationResult.super::documentationId);
	}
	
	@Override
	public String documentationId() {
		return id;
	}
	
	@Override
	public String name() {
		return info.name();
	}
	
	@Override
	public String[] description() {
		return fail(Description.class, Description::value);
	}
	
	@Override
	public String[] examples() {
		return fail(Examples.class, Examples::value);
	}
	
	@Override
	public String since() {
		return fail(Since.class, Since::value);
	}
	
	@Override
	public Class<?> type() {
		return clazz;
	}
	
	@Override
	public List<String> patterns() {
		return info.patterns();
	}
	
	@Override
	public SyntaxInfo<?> info() {
		return info;
	}
	
	@Override
	@Nullable
	public String[] requiredPlugins() {
		return consumer(RequiredPlugins.class, RequiredPlugins::value, () -> new String[0]);
	}
	
	@Override
	public String[] keywords() {
		return consumer(Keywords.class, Keywords::value, () -> new String[0]);
	}
	
	@Override
	public String[] events() {
		return consumer(Events.class, Events::value, () -> new String[0]);
	}
	
	@Override
	public boolean deprecated() {
		return consumer(MarkedForRemoval.class, annotation -> true, () -> false);
	}
	
	private <T extends Annotation, R> R consumer(Class<? extends T> clazz, Function<T, R> consumer, Supplier<R> fallback) {
		T annotation = this.clazz.getDeclaredAnnotation(clazz);
		if (annotation == null) return fallback.get();
		R result = consumer.apply(annotation);
		return result == null ? fallback.get() : result;
	}
	
	private <T extends Annotation, R> R fail(Class<? extends T> clazz, Function<T, R> consumer) {
		T annotation = this.clazz.getDeclaredAnnotation(clazz);
		if (annotation == null) throw new IllegalStateException("Annotation must be present");
		R result = consumer.apply(annotation);
		if (result == null) throw new IllegalStateException("Value may not be null");
		return result;
	}
	
}
